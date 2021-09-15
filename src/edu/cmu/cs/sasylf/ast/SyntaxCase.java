package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Errors.SYNTAX_CASE_FOR_DERIVATION;
import static edu.cmu.cs.sasylf.util.Util.debug;
import static edu.cmu.cs.sasylf.util.Util.verify;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Atom;
import edu.cmu.cs.sasylf.term.BoundVar;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.util.DefaultSpan;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Util;


public class SyntaxCase extends Case {
	public SyntaxCase(Location l, Clause c) { 
		super(l,c.getLocation(),c.getEndLocation()); 
		conclusion = c; 
	}
	public SyntaxCase(Location l, Clause c, Clause ca) { 
		this(l,c); 
		assumes = ca; 
		if (assumes != null) {
			((DefaultSpan)super.getSpan()).setEndLocation(ca.getEndLocation());
		}
	}

	public Clause getConclusion() { return conclusion; }

	@Override
	public void prettyPrint(PrintWriter out) {
		out.print("case ");
		conclusion.prettyPrint(out);
		out.println(" is ");

		super.prettyPrint(out);
	}

	@Override
	public void typecheck(Context parent, Pair<Fact,Integer> isSubderivation) {
		Context ctx = parent.clone();
		debug("    ******* case line ", getLocation().getLine());
		conclusion = conclusion.typecheck(ctx);

		// make sure we were case-analyzing a nonterminal
		if (!(ctx.currentCaseAnalysisElement instanceof NonTerminal) &&
				!(ctx.currentCaseAnalysisElement instanceof AssumptionElement)) {
			if (ctx.currentCaseAnalysisElement instanceof OrClauseUse) {
				ErrorHandler.error(Errors.SYNTAX_CASE_FOR_DISJUNCTION, this);
			}
			ErrorHandler.error(SYNTAX_CASE_FOR_DERIVATION, this);
		}
		NonTerminal caseNT;
		if (ctx.currentCaseAnalysisElement instanceof AssumptionElement) {
			AssumptionElement ae = (AssumptionElement)ctx.currentCaseAnalysisElement;
			Element base = ae.getBase();
			if (base instanceof Binding) caseNT = ((Binding)base).getNonTerminal();
			else caseNT = (NonTerminal)base;
		} else caseNT = (NonTerminal)ctx.currentCaseAnalysisElement;

		Element concElem = conclusion.computeClause(ctx, caseNT);
		Clause concDef = null;

		if (assumes != null) {
			Util.debug("assumes is ",assumes);
			assumes = assumes.typecheck(ctx);
			if (assumes instanceof Clause) {
				assumes = ((Clause)assumes).computeClause(ctx,false);
			}
		}
		
		if (concElem.getType() != caseNT.getType()) {
			ErrorHandler.error(Errors.CASE_WRONG_TYPE, concElem.getType().getName(), conclusion);
		}

		if (concElem instanceof Variable) {
			for (Clause c :  caseNT.getType().getClauses()) {
				if (c.isVarOnlyClause()) {
					concDef = c;
				}
			}
			if (concDef == null) {
				ErrorHandler.error(Errors.INTERNAL_ERROR,"no variable clause for nonterminal: " + 
						ctx.currentCaseAnalysisElement,this);
			}
		} else if (concElem instanceof NonTerminal) {
			ErrorHandler.error(Errors.CASE_NONTERM, this);     
		} else if (concElem instanceof Binding) {
			ErrorHandler.error(Errors.CASE_NONTERM, this);     
		} else if (concElem instanceof ClauseUse) {
			ClauseUse concUse = (ClauseUse) concElem;
			concUse.checkBindings(ctx.bindingTypes, this);
			concDef = concUse.getConstructor();
			if (concUse.getConstructor().getType() != caseNT.getType()) {
				ErrorHandler.error(Errors.INTERNAL_ERROR, "case given is not actually a case of " + caseNT, this);
			}
		} else {
			// not clear when this error happens
			ErrorHandler.error(Errors.INTERNAL_ERROR, "unknownCase "+ concElem, this);
		}		  

		if (assumes != null) {
			Util.debug("concElem = ",concElem," : ", concElem.getClass());
			concElem = new AssumptionElement(getLocation(),concElem,assumes);
			concElem.typecheck(ctx);
		}
		
		Term concTerm = concElem.asTerm();
		if (!concDef.isVarOnlyClause()) {
			concDef = ((ClauseDef)concDef).getBaseClauseDef();
		}
		Util.debug("concTerm = ", concTerm);
		Util.debug("concDef = ", concDef,", of class ", concDef.getClass());

		int diff = concTerm.countLambdas() - ctx.currentCaseAnalysis.countLambdas();
		// this check is redundant:
		if (diff < 0) {
			ErrorHandler.error(Errors.CASE_CONTEXT_CHANGED, conclusion);
			return;
		}
		
		if (diff > 0) {
			Util.debug("concDef = ",concDef);
			if (!concDef.isVarOnlyClause()) {
				ErrorHandler.error(Errors.CASE_CONTEXT_CHANGED, assumes);
			} else if (diff > 2) { //XXX Extension!
				ErrorHandler.error(Errors.CASE_SINGLE, assumes);
			}
		}

		// This check is optional; but if we neglect to do it, we'll get a confusing message later.
		for (FreeVar fv : getCaseFreeVars(ctx,concTerm)) {
			if (ctx.isLocallyKnown(fv.toString())) {
				Util.debug("ctx.inputsVars = ",ctx.inputVars);
				ErrorHandler.error(Errors.CASE_STRICT_NEED_VAR, fv.toString(), this);
			}
		}

		Term matching = Term.getWrappingAbstractions(concTerm, null, diff);
		// this check, however, is essential.
		checkContextMatch(ctx,matching);

		// look up case analysis for this rule
		Set<Pair<Term,Substitution>> caseResult = ctx.caseTermMap.get(concDef);
		
		if (concElem instanceof ClauseUse) { // only variables can have more than one case
			verify(caseResult.size() <= 1, "internal invariant violated");
		}

		Term computedCaseTerm = null;

		for (Pair<Term, Substitution> pair : caseResult) {
			Substitution computedSub;
			try {
				Util.debug("case unify ", concTerm, " and ", pair.first);
				computedSub = pair.first.unify(concTerm);
				Util.debug("result is ", computedSub);
			} catch (UnificationFailed uf) {
				Util.debug("Case ",this," does not apply to ",ctx.currentCaseAnalysisElement);
				continue;
			}
			computedCaseTerm = pair.first;
			// verify(pair.second.getMap().size() == 0, "syntax case substitution should be empty, not " + pair.second);
			caseResult.remove(pair);

			Set<FreeVar> free = pair.first.getFreeVariables();
			Util.debug("freevars of pair.first = ",free);
			free = computedSub.selectUnavoidable(free);
			if (!free.isEmpty()) {
				TermPrinter tp = new TermPrinter(ctx,ctx.assumedContext,conclusion.getLocation(),false);
				FreeVar fv = free.iterator().next();
				computedSub.avoid(concTerm.getFreeVariables());
				Term t = computedSub.getSubstituted(fv);
				// trying to classify the error:
				int nWraps = t.countLambdas();
				Term base = Term.getWrappingAbstractions(t, null);
				int args = base instanceof Application ? ((Application)base).getArguments().size() : 0;
				Atom atom = base instanceof Application ? ((Application)base).getFunction() : (Atom)base;
				if (atom instanceof Constant) {
					String inside = tp.toString(t, false);
					ErrorHandler.recoverableError(Errors.CASE_STRICT_NEED_VAR, inside, this, "The following LF term was expected to be a variable: "+t);
				} else if (args < nWraps) {
					ErrorHandler.recoverableError(Errors.CASE_STRICT_NEED_DEPEND, atom.toString(), this);
				} else {
					ErrorHandler.recoverableError(Errors.CASE_STRICT_DUP_VAR, ": "+atom, this);
				}
			}
			break;
		}

		if (computedCaseTerm == null) {
			ErrorHandler.error(Errors.CASE_REDUNDANT, this, "Suggestion: remove it");
		}

		Term adaptedCaseAnalysis = ctx.currentCaseAnalysis;

		int lambdaDifference =  computedCaseTerm.countLambdas() - adaptedCaseAnalysis.countLambdas();
		if (lambdaDifference > 0) {
			if (lambdaDifference > 2) {
				ErrorHandler.error(Errors.INTERNAL_ERROR,"New assumption can only add one variable",this);
			}
			// JTB: The code here is tricky because syntax adds just one variable,
			// but adaptation info must use both variables.
			verify(concElem instanceof AssumptionElement,"not an assumption element? " + concElem);
			AssumptionElement ae = (AssumptionElement)concElem;
			verify(ae.getBase() instanceof Variable, "not a variable? " + ae.getBase());
			verify(ae.getAssumes() instanceof ClauseUse, "not a clause use? " + assumes);
			NonTerminal newRoot = concElem.getRoot();
			if (ctx.isKnownContext(newRoot)) {
				ErrorHandler.error(Errors.CASE_ASSUMPTION_OLD, this);
			}

			// If currentCaseAnalysis is an abstraction, bind it to a fresh variable,
			// and use this variable in the relaxation.
			FreeVar relaxationVar;
			if (adaptedCaseAnalysis instanceof FreeVar) {
				relaxationVar = (FreeVar)adaptedCaseAnalysis;
			} else {
				List<Abstraction> localContext = new ArrayList<Abstraction>();
				Term bare = Term.getWrappingAbstractions(adaptedCaseAnalysis, localContext);
				FreeVar fVar;
				if (bare instanceof Atom) {
					fVar = (FreeVar)bare; 
				} else {
					fVar = (FreeVar)((Application)bare).getFunction();
				}
				relaxationVar = FreeVar.fresh(fVar.getName(), fVar.getType().baseTypeFamily());
				Substitution relaxSub = adaptedCaseAnalysis.unify(Term.wrapWithLambdas(localContext, relaxationVar));
				Util.debug("relaxationSub = ",relaxSub);
				adaptedCaseAnalysis = adaptedCaseAnalysis.substitute(relaxSub);
				ctx.composeSub(relaxSub);
				ctx.inputVars.add(relaxationVar);
			}

			List<Abstraction> addedContext = new ArrayList<Abstraction>();
			Term.getWrappingAbstractions(computedCaseTerm,addedContext,lambdaDifference);

			Relaxation relax = new Relaxation(addedContext,Collections.singletonList(relaxationVar),ctx.currentCaseAnalysisElement.getRoot());
			ctx.addRelaxation(newRoot, relax);

			adaptedCaseAnalysis = ctx.adapt(adaptedCaseAnalysis, addedContext, true);/*
      Abstraction first = addedContext.get(0);
      Substitution adaptSub = new Substitution();
      adaptedCaseAnalysis.bindInFreeVars(first.varType, adaptSub);
      adaptedCaseAnalysis = adaptedCaseAnalysis.substitute(adaptSub);
      adaptedCaseAnalysis = Facade.Abs(first.varName,first.varType, adaptedCaseAnalysis);*/
		} else {
			NonTerminal newRoot = concElem.getRoot();
			Util.debug("checking ",newRoot," against ",ctx.currentCaseAnalysisElement.getRoot());
			if (!ctx.isKnownContext(newRoot)) {
				ErrorHandler.error(Errors.UNKNOWN_CONTEXT, newRoot.toString(), this);
			}
			if (newRoot != null) {
				NonTerminal subjectRoot = ctx.currentCaseAnalysisElement.getRoot();
				if (!newRoot.equals(subjectRoot)) {
					ErrorHandler.error(Errors.CASE_CONTEXT_CHANGED_EX, subjectRoot.toString(), this);
				}
			}
		}

		// make sure rule's conclusion unifies with the thing we're doing case analysis on
		// NB: need to handle the lambdaDifference here.
		concTerm = concTerm.substitute(ctx.currentSub);
		Substitution unifyingSub = null;
		try {
			unifyingSub = concTerm.unify(adaptedCaseAnalysis);
		} catch (UnificationFailed uf) {
			uf.printStackTrace();
			ErrorHandler.error(Errors.INTERNAL_ERROR,": Should not have unification error here!\n concTerm = " + concTerm + ", case analysis = "+ adaptedCaseAnalysis,this);
		}

		unifyingSub.avoid(concTerm.getFreeVariables());
		Util.debug("improved unifyingSub",unifyingSub);
		
		// update the current substitution
		ctx.composeSub(unifyingSub); // modifies in place		

		super.typecheck(ctx, isSubderivation);

	}

	public Set<FreeVar> getCaseFreeVars(Context ctx, Term concTerm) {
		Set<FreeVar> result = new HashSet<FreeVar>();
		List<Abstraction> wrappers = new ArrayList<Abstraction>();
		Term bare = Term.getWrappingAbstractions(concTerm, wrappers);
		result.addAll(bare.getFreeVariables());
		int expected = ctx.currentCaseAnalysis.countLambdas();
		int n = wrappers.size() - expected;
		// skip expected wrappers
		for (int i=0; i < n; ++i) {
			result.addAll(wrappers.get(i).varType.getFreeVariables());
		}
		return result;
	}

	/**
	 * We check that the context of the syntax case doesn't bind something from
	 * the input vars.  Normally cases are allowed to bind input vars;
	 * indeed that's how pattern matching gets its power from, but the context
	 * is not pattern-matched, just the base relation.
	 * @param ctx
	 * @param matchTerm
	 */
	protected void checkContextMatch(Context ctx, Term matchTerm) {
		List<Abstraction> matchContext = new ArrayList<Abstraction>();
		Term.getWrappingAbstractions(matchTerm, matchContext);
		int n = matchContext.size();
		if (n == 0) return;
		FreeVar fv = FreeVar.fresh("X", Term.wrapWithLambdas(matchContext,Constant.UNKNOWN_TYPE));
		List<BoundVar> args = new ArrayList<BoundVar>();
		for (int i=0; i < n; ++i) {
			args.add(new BoundVar(n-i));
		}
		Application base = Facade.App(fv, args);
		Term newMatch = Term.wrapWithLambdas(matchContext, base);
		try {
			Substitution sub = newMatch.unify(ctx.currentCaseAnalysis);
			Set<FreeVar> bound = sub.selectUnavoidable(ctx.inputVars);
			Util.debug("unavoidable: ",bound);
			if (!bound.isEmpty()) {
				ErrorHandler.error(Errors.CASE_CONTEXT_RESTRICTS, bound.toString(), this);
				// ErrorHandler.error(Errors.INVALID_CASE, "The case is too specialized, the context in the case binds " + bound.iterator().next(), this);
			}
		} catch (UnificationFailed e) {
			// problems will be caught later
			Util.debug("checkContextMatch unification failed: ",e);
			return;
		}
	}

	private Clause conclusion;
	private Element assumes;
}

