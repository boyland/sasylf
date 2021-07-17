package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Errors.JUDGMENT_EXPECTED;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.BoundVar;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.term.UnificationIncomplete;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.Util;

public class Rule extends RuleLike implements CanBeCase {
	public Rule(Location loc, String n, List<Clause> l, Clause c) { 
		super(n, loc); 
		premises=l; 
		conclusion=c; 
		super.setEndLocation(c.getEndLocation());
	}

	@Override
	public List<Clause> getPremises() { return premises; }
	@Override
	public Clause getConclusion() { return conclusion; }

	/** rules should have no existential variables
	 */
	@Override
	public Set<FreeVar> getExistentialVars() {
		return new HashSet<FreeVar>();
	}

	@Override
	public String getKind() {
		return "rule";
	}

	private List<Clause> premises;
	private Clause conclusion;
	private int isAssumpt = 0; // 0 = not an assumption, > 0 number of abstractions represented

	@Override
	public void prettyPrint(PrintWriter out) {
		for (Clause c : premises) {
			out.print("premise ");
			c.prettyPrint(out);
			out.println();
		}

		out.print("--------------------- ");
		out.println(getName());
		conclusion.prettyPrint(out);
		out.println("\n");
	}

	
	@Override
	protected Constant getRuleAppBase() {
		return judgment.typeTerm();
	}

	public void typecheck(Context ctx, Judgment judge) {
		judgment = judge;
		Map<String, List<ElemType>> bindingTypes = new HashMap<String, List<ElemType>>();
		conclusion = conclusion.typecheck(ctx);
		Element computed = conclusion.computeClause(ctx, false);
		if (!(computed instanceof ClauseUse)) {
			ErrorHandler.error(JUDGMENT_EXPECTED, computed);				
		}
		ClauseUse myConc = (ClauseUse) computed;
		final ClauseType myType = myConc.getConstructor().getType();
		if (!(myType instanceof Judgment))
			ErrorHandler.error(JUDGMENT_EXPECTED, myConc);

		if (!(myType == judge))
			ErrorHandler.error(Errors.JUDGMENT_WRONG, ((Judgment)myType).getName(), myConc);

		myConc.checkBindings(bindingTypes, this);
		conclusion = myConc;

		if (ctx.ruleMap.containsKey(getName())) {
			if (ctx.ruleMap.get(getName()) != this) {
				ErrorHandler.recoverableError(Errors.RULE_LIKE_REDECLARED, this);
			}
		} else ctx.ruleMap.put(getName(), this);

		boolean assumptionCheckError = false;
		try {
			computeAssumption(ctx);
			myConc.checkVariables(new HashSet<String>(), false);
		} catch (SASyLFError ex) {
			assumptionCheckError = true; // so we don't complain about context errors
		}

		for (int i = 0; i < premises.size(); ++i) {
			Clause c = premises.get(i);
			c.typecheck(ctx);
			computed = c.computeClause(ctx, false);
			if (!(computed instanceof ClauseUse)) {
				ErrorHandler.error(JUDGMENT_EXPECTED, computed);				
			}
			ClauseUse premiseClause = (ClauseUse) computed;
			if (!(premiseClause.getConstructor().getType() instanceof Judgment)) {
				ErrorHandler.error(JUDGMENT_EXPECTED, premiseClause);
			}
			premiseClause.checkBindings(bindingTypes, this);
			premises.set(i, premiseClause);
			premiseClause.checkVariables(new HashSet<String>(), false);
			//premises.set(i, new ClauseUse(c, ctx.parseMap));
			NonTerminal nt = premiseClause.getRoot();
			if (nt != null) {
				if (!nt.equals(myConc.getRoot())) {
					ErrorHandler.recoverableError(Errors.PREMISE_CONTEXT_MISMATCH, c);
				}
			}
		}

		if (judge.getAssume() != null && !isAssumption() && !assumptionCheckError) { // bad15
			NonTerminal nt = myConc.getRoot();
			if (nt == null) {
				ErrorHandler.error(Errors.EMPTY_CONCLUSION_CONTEXT, conclusion);
			} else if (myConc.hasVariables()) {
				ErrorHandler.error(Errors.VAR_CONCLUSION_CONTEXT, conclusion);
			}
		}

		Set<NonTerminal> neverRigid = getNeverRigid();
		if (!neverRigid.isEmpty()) {
			ErrorHandler.warning(Errors.NEVER_RIGID, neverRigid.toString(), this);
		}
		ruleIsOk = true;
	}

	private void computeAssumption(Context ctx) {
		ClauseUse concClauseUse = (ClauseUse)this.getConclusion();
		ClauseDef concClauseDef = concClauseUse.getConstructor();
		Judgment parent = (Judgment)concClauseDef.getType();
		// must have no premises, parent must assume something
		if (premises.size() > 0 || parent.getAssume() == null)
			return; // default false

		// must have Gamma, E(x) for Gamma in conclusion
		int assumeIndex = ((ClauseDef)parent.getForm()).getAssumeIndex();
		Element assumeElement = getConclusion().getElements().get(assumeIndex);
		if (!(assumeElement instanceof ClauseUse))
			return; // default false
		
		// At this point, we have a rule without premises
		// which has a non-NT assumption.  

		// look for sub-part of gamma clause, a NonTerminal with same type as gamma
		ClauseUse assumeClauseUse = (ClauseUse) assumeElement;
		ClauseDef assumeClauseDef = assumeClauseUse.getConstructor();
		SyntaxDeclaration gammaType = (SyntaxDeclaration) assumeClauseDef.getType();
		// XXX: in extension, ignore append case too:
		if (assumeClauseDef == gammaType.getTerminalCase()) return; // error given elsewhere

		// We consider this rule to be an attempt of an assumption
		// and will generate errors for problems noticed.

		// First we check that we use exactly one clause of the context
		// verbatim without forcing terms together
		int assumeSize = assumeClauseUse.getElements().size();
		Set<String> names = new HashSet<>();
		Set<NonTerminal> defined = new HashSet<>();
		int countVars = 0;
		for (int i=0; i < assumeSize; ++i) {
			Element use = assumeClauseUse.getElements().get(i);
			Element def = assumeClauseDef.getElements().get(i);
			if (use.getClass() != def.getClass()) {
				ErrorHandler.error(Errors.ASSUMES_CONTEXT_RESTRICT, ": " + assumeClauseDef, use);
			}
			if (use instanceof NonTerminal || use instanceof Variable) {
				if (def.getType().equals(gammaType)) continue; // skip this one
				if (!names.add(use.toString())) {
					ErrorHandler.error(Errors.ASSUMES_DUPLICATE, use.toString(), use);
				}
				if (use instanceof Variable) ++countVars;
				else defined.add((NonTerminal)use);
			} else if (use instanceof Binding) {
				Binding b = (Binding)use;
				if (!names.add(b.getNonTerminal().getSymbol())) {
					ErrorHandler.error(Errors.ASSUMES_DUPLICATE, b.getNonTerminal().getSymbol(), use);
				}
				defined.add(b.getNonTerminal());
				Set<String> seen = new HashSet<>();
				for (Element e : b.getElements()) {
					if (!(e instanceof Variable)) {
						ErrorHandler.error(Errors.ASSUMES_CONTEXT_RESTRICT, ": " + assumeClauseDef, use);
					}
					if (!seen.add(e.toString())) {
						ErrorHandler.error(Errors.ASSUMES_DUPLICATE, e.toString(), e);
					}
				}
			}
		}
		
		if (assumeClauseDef.assumptionRule != null &&
				assumeClauseDef.assumptionRule != this) // idempotency
			ErrorHandler.error(Errors.ASSUMES_MULTI_USE,assumeClauseDef.toString(), this);

		if (countVars != 1) { // XXX: Extension point
			ErrorHandler.error(Errors.INTERNAL_ERROR, "Not expecting countVars == " + countVars, this);
			// For extension: there are a very large number of
			// implicit dependencies on "isAssumpt" being exactly 2.
		}
		isAssumpt = 1 + countVars;
		assumeClauseDef.assumptionRule = this;

		Set<NonTerminal> used = new HashSet<>();
		for (Element e : concClauseUse.getElements()) {
			if (e == assumeClauseUse) continue; // skip this part
			e.getFree(used,false);
		}
		Set<NonTerminal> notDefined = new HashSet<>(used);
		notDefined.removeAll(defined);
	
		// If something is used but not defined, it means the translation to LF
		// doesn't know what to use for the nonterminal when forming the internal derivation
		if (!notDefined.isEmpty()) {
			ErrorHandler.error(Errors.ASSUMES_UNDEFINED, used.toString(), concClauseUse);
		}

		Set<NonTerminal> notUsed = new HashSet<>(defined);
		notUsed.removeAll(used);
		
		// If something is defined but not used, it means that the surface syntax
		// (elements) of environments mentions things that are not in the LF.
		// At the very least, this is confusing.  It means these parts of
		// the context can be substituted with anything else with no effect at all.
		// I expect it could also lead to problems later when looking at
		// meta-variables used versus free.
		for (Element e : notUsed) {
			ErrorHandler.error(Errors.ASSUMES_UNUSED, e.toString(), concClauseUse);
		}
		
		// NB: previously, we also require that all variables in the assumption
		// also occur in the judgment, but the variables can be ignored without
		// causing problems in the theory.
	}

	public boolean isAssumption() {
		return isAssumpt>0 ;
	}
	public int isAssumptionSize() {
		return isAssumpt;
	}

	/**
	 * Return true if the rule has some free variables that never occur in rigid positions.
	 */
	public Set<NonTerminal> getNeverRigid() {
		Set<NonTerminal> nts = new HashSet<NonTerminal>();
		for (Clause p : premises) {
			p.getFree(nts, true);
		}
		conclusion.getFree(nts, true);
		Set<NonTerminal> rigid = new HashSet<NonTerminal>(nts);
		for (Clause p : premises) {
			p.getFree(nts, false);
		}
		conclusion.getFree(nts, false);
		nts.removeAll(rigid);
		return nts;
	}

	/**
	 * Returns a fresh term for the rule and a substitution that matches the term.
	 * The result will be empty if no case analysis is possible.
	 * The result can return a set of more than one possibility if variables are involved.
	 * @param ctx context, must not be null
	 * @param term target term being analyzed
	 * @param clause source-level of term
	 * @param source location to indicate errors and debugging messages
	 * @return possibilities that this rule could match the target
	 */
	@Override
	public Set<Pair<Term,Substitution>> caseAnalyze(Context ctx, Term term, Element target, Node source) {
		Util.verify(target instanceof ClauseUse, "Case analyzing for a rule must be a clause: " + target);
		ClauseUse clause = (ClauseUse)target;

		Set<Pair<Term,Substitution>> pairs = new HashSet<Pair<Term,Substitution>>();

		List<Abstraction> abs = new ArrayList<Abstraction>();
		Term bare = Term.getWrappingAbstractions(term, abs);
		Application appTerm = this.getFreshAdaptedRuleTerm(abs, null);
		Term goalTerm = appTerm.getArguments().get(premises.size());

		if (isAssumption()) {
			Util.debug("** On line (for assumption) ", source.getLocation().getLine());
			// assumption rules, unlike all other rules, have abstractions in the goal: 
			// newAbs gets the ones from the rule, currently this always has the form 
			//   lambda x:SynType . lambda _: (judgment using x) . (judgment using x)
			// the (two) abstractions can fit anywhere within the abstractions "abs" 
			// *or* outside all of them.
			List<Abstraction> goalAbs = new ArrayList<Abstraction>();
			Term bareGoal = Term.getWrappingAbstractions(goalTerm, goalAbs);
			Term subject = Term.wrapWithLambdas(abs, Facade.App(getRuleAppConstant(), bare));
			if (clause.getRoot() != null) { // context has unknown size beyond abs
				if (ctx.assumedContext == null) {
					ErrorHandler.error(Errors.UNKNOWN_CONTEXT, clause.getRoot().toString(), source);
				}
				// We need to consider matching into the context.
				// Problem: The appTerm and everything from it (goalTerm/bareGoal/newAbs)
				// were adapted assuming the "abs" were *outside* the abstractions in the goal,
				// but the opposite is true.  So we make a new goalTerm
				Term newGoalTerm = this.conclusion.asTerm();
				Substitution freshSub = newGoalTerm.freshSubstitution(new Substitution());
				newGoalTerm = newGoalTerm.substitute(freshSub);
				List<Abstraction> newAbs = new ArrayList<>();
				Term newBareGoal = Term.getWrappingAbstractions(newGoalTerm, newAbs);
				List<Abstraction> oldAbs = newAbs; // change if needed to handle a relaxVar
				List<Term> newTypes = new ArrayList<Term>();
				for (Abstraction a : newAbs) {
					newTypes.add(a.getArgType());
				}
				Substitution newAdaptSub = new Substitution();
				term.bindInFreeVars(newTypes, newAdaptSub);
				// Before we do the adaptation, we consider 
				// (A) variable-less meta-variables cannot be adapted
				// (B) meta-variables known to be variables outside of the context should not be adapted.
				// Then we look at the bare goal itself, whose meta-variables
				// will have values from the outermost context's variable, and
				// (C) so should not be adapted unless the term in the outer context is 
				// (D) And if this is the variable in question, and we have a current
				//     relaxation variable, then the relaxation types should be used
				//     rather than the generated types.
				NonTerminal root = clause.getRoot();
				for (FreeVar v : term.getFreeVariables()) {
					if (ctx.isVarFree(v)) { // A
						Util.debug("  removing adaptation for ", v);
						newAdaptSub.remove(v);
					} else if (ctx.isRelaxationInScope(root, v)) { // B
						Util.debug("  removing adaptation for relaxVar ", v);
						newAdaptSub.remove(v);
					}
				}

				int n = ((Application)newBareGoal).getArguments().size();
				for (int i=0; i < n; ++i) {
					final Term argi = ((Application)newBareGoal).getArguments().get(i);
					Util.debug("argi = ",argi,", isClosed? ", argi.isClosed());
					if (argi.isClosed()) {
						// any variables in the corresponding place should not be adapted.
						Set<FreeVar> free = ((Application)bare).getArguments().get(i).getFreeVariables();
						for (FreeVar fv : free) {
							Util.debug("  removing adaptation for ",fv);
							newAdaptSub.remove(fv);
						}
					} else if (argi instanceof BoundVar) { // this is the variable
						// check to see the subject is a relaxation variable
						// that could be hidden in the root (not "in scope")
						Term subi = ((Application)bare).getArguments().get(i);
						if (subi instanceof FreeVar && ctx.isRelaxationVar((FreeVar)subi)) {
							Util.debug("Found relax var ", subi);
							FreeVar fv = (FreeVar)subi;
							if (!ctx.isRelaxationInScope(clause.getRoot(), fv)) {
								Util.debug("  need to handle relaxation ", fv);
								List<Term> oldTypes = ctx.getRelaxationTypes(fv);
								oldAbs = new ArrayList<>();
								for (Term t : oldTypes) {
									oldAbs.add((Abstraction) Abstraction.make("", t, new BoundVar(1)));
								}
							}
						}
					}
				}
				Term newSubject = Term.wrapWithLambdas(newAbs, subject.substitute(newAdaptSub));
				Term barePattern = Facade.App(getRuleAppConstant(),newBareGoal.incrFreeDeBruijn(abs.size()));
				Term newPattern = Term.wrapWithLambdas(oldAbs, Term.wrapWithLambdas(abs, barePattern).substitute(newAdaptSub));
				checkCaseApplication(ctx,pairs, newSubject, newPattern, newSubject, null, source);
			} else {
				Util.debug("no root, so no special assumption rule");
			}
			/* now we try to find the assumption goals inside the existing context */
			tryInsert: for (int i = abs.size() - goalAbs.size(); i >=0; --i) {
				// make sure types match:
				for (int k=0; k < goalAbs.size(); ++k) {
					Constant oldFam = abs.get(k+i).getArgType().baseTypeFamily();
					Constant newFam = goalAbs.get(k).getArgType().baseTypeFamily();
					if (!newFam.equals(oldFam)) {
						// incompatible: don't even try (can get type error during unification)
						continue tryInsert;
					}
				}
				int j = i + goalAbs.size();
				Term shiftedGoal = Facade.App(getRuleAppConstant(),bareGoal.incrFreeDeBruijn(abs.size()-j));
				Term pattern = Term.wrapWithLambdas(abs,Term.wrapWithLambdas(goalAbs, Term.wrapWithLambdas(abs, shiftedGoal,j,abs.size())),0,i);
				checkCaseApplication(ctx,pairs, subject,pattern, subject, null, source);
			}		  
		} else { // not assumption
			Util.debug("** On line (non assumption) ",source.getLocation().getLine());
			checkCaseApplication(ctx, pairs, Term.wrapWithLambdas(abs, appTerm), goalTerm, bare, null, source);
		}
		return pairs;
	}

	/** Checks if this rule applies to term, assuming ruleTerm is the term for the rule
	 * and appliedTerm is the rule term built up from term.  
	 * @param ctx global context: must not be null
	 * @param result place to put any resulting pair
	 * @param term full pattern to use (after substituting with unifier)
	 * @param pattern kernel of pattern
	 * @param subject kernel of subject
	 * @param adaptSub adaptation substitution (may be null if no adaptation)
	 *    currently not used.
	 * @param source location to drop errors
	 */
	private void checkCaseApplication(Context ctx, Set<Pair<Term,Substitution>> result,
			Term term, Term pattern, Term subject, Substitution adaptSub, Node source) {
		Util.debug("pattern ", pattern);
		Util.debug("subject ",subject);
		Substitution sub = null;
		try {
			sub = pattern.unify(subject);
			Util.debug("found sub ", sub, " for case analyzing ", term, " with rule ", getName());
		} catch (UnificationIncomplete e) {
			Util.debug("unification incomplete on ", pattern, " and ", subject);
			ErrorHandler.recoverableError(Errors.CASE_UNIFICATION_INCOMPLETE, source, "SASyLF tried to unify " + e.term1 + " and " + e.term2);
		} catch (UnificationFailed e) {
			Util.debug("failure: " + e.getMessage());
			Util.debug("unification failed on ", pattern, " and ", subject);
			sub = null;
		}
		if (sub != null) {
			sub.avoid(ctx.inputVars); // try to avoid so we don't unnecessarily replace input vars
			Util.debug("at check, sub = " + sub);
			if (!ctx.canCompose(sub)) return;
			Util.debug("\t added result: ", term, sub);
			result.add(new Pair<Term,Substitution>(term.substitute(sub),sub));
		}
	}

	@Override
	public String getErrorDescription(Term t, Context ctx) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.print("rule " + this.getName());

		// TODO: comment this back in to get explicit error messages
		/*if (t != null && t instanceof Application) {
			List<? extends Term> elemTerms = ((Application)t).getArguments();
			pw.println(":");
			//pw.println(t);
			PrintContext pctx = null;
			for(int i = 0; i < elemTerms.size()-1; ++i) {
				t = elemTerms.get(i);
				pctx = (pctx == null) ? new PrintContext(t, ctx.inputVars, ctx.innermostGamma) : new PrintContext(t, pctx);
				((ClauseUse)getPremises().get(i)).getConstructor().prettyPrint(pw, pctx);
				pw.println();
			}
			pw.println("--------------------");
			t = elemTerms.get(elemTerms.size()-1);
			pctx = (pctx == null) ? new PrintContext(t, ctx.inputVars, ctx.innermostGamma) : new PrintContext(t, pctx);
			((ClauseUse)getConclusion()).getConstructor().prettyPrint(pw, pctx);
		}*/
		return sw.toString();
	}


	@Override
	public boolean isInterfaceOK() {
		return ruleIsOk;
	}

	@Override
	public NonTerminal getAssumes() {
		return getJudgment().getAssume();
	}

	public Judgment getJudgment() {
		if (judgment == null) throw new InternalError("judgment not yet set!");
		return judgment;
	}

	private Judgment judgment;
	private boolean ruleIsOk = false;
}

