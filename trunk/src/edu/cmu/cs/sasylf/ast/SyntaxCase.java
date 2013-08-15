package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.ast.Errors.EXTRA_CASE;
import static edu.cmu.cs.sasylf.ast.Errors.INVALID_CASE;
import static edu.cmu.cs.sasylf.ast.Errors.NONTERMINAL_CASE;
import static edu.cmu.cs.sasylf.ast.Errors.REUSED_CONTEXT;
import static edu.cmu.cs.sasylf.ast.Errors.SYNTAX_CASE_FOR_DERIVATION;
import static edu.cmu.cs.sasylf.util.Util.debug;
import static edu.cmu.cs.sasylf.util.Util.verify;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Pair;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.util.ErrorHandler;


public class SyntaxCase extends Case {
	public SyntaxCase(Location l, Clause c) { super(l); conclusion = c; }
	public SyntaxCase(Location l, Clause c, Clause ca) { this(l,c); assumes = ca; }
	
	public Clause getConclusion() { return conclusion; }

	public void prettyPrint(PrintWriter out) {
		out.print("case ");
		conclusion.prettyPrint(out);
		out.println(" is ");

		super.prettyPrint(out);
	}

	public void typecheck(Context ctx, boolean isSubderivation) {
		debug("    ******* case line " + getLocation().getLine());
		Map<String, List<ElemType>> oldBindingTypes = new HashMap<String, List<ElemType>>(ctx.bindingTypes);
    conclusion.typecheck(ctx);

    // make sure we were case-analyzing a nonterminal
    if (!(ctx.currentCaseAnalysisElement instanceof NonTerminal) &&
        !(ctx.currentCaseAnalysisElement instanceof AssumptionElement))
      ErrorHandler.report(SYNTAX_CASE_FOR_DERIVATION, this);
    
    NonTerminal caseNT;
    Clause caseAssumptions = null; // These may be unnecessary
    if (ctx.currentCaseAnalysisElement instanceof AssumptionElement) {
      AssumptionElement ae = (AssumptionElement)ctx.currentCaseAnalysisElement;
      Element base = ae.getBase();
      if (base instanceof Binding) caseNT = ((Binding)base).getNonTerminal();
      else caseNT = (NonTerminal)base;
      caseAssumptions = ae.getAssumes();
    } else caseNT = (NonTerminal)ctx.currentCaseAnalysisElement;

    edu.cmu.cs.sasylf.grammar.NonTerminal nt = caseNT.getType().getSymbol();
    edu.cmu.cs.sasylf.grammar.Grammar g = new edu.cmu.cs.sasylf.grammar.Grammar(nt, ctx.getGrammar().getRules());
    Element concElem = conclusion.computeClause(ctx, false, g);
    Clause concDef = null;
    
    if (assumes != null) {
		  assumes = (Clause) assumes.typecheck(ctx);
		  Element assumeE = assumes.computeClause(ctx,false);
		  if (assumeE instanceof ClauseUse) assumes = (Clause)assumeE;
    }
    
    if (concElem instanceof Variable) {
		  for (Clause c :  caseNT.getType().getClauses()) {
		    if (c.isVarOnlyClause()) {
		      concDef = c;
		    }
		  }
		  if (concDef == null) {
		      throw new InternalError("no variable clause for nonterminal: " + 
		          ctx.currentCaseAnalysisElement);
		  }
    } else if (concElem instanceof Clause) {
      ClauseUse concUse = (ClauseUse) concElem;
      concUse.checkBindings(ctx.bindingTypes, this);
      concDef = concUse.getConstructor();
      if (caseAssumptions == null) {
        if (assumes != null) ErrorHandler.report(Errors.INVALID_CASE, "Cannot add assumptions in case", this);
      } else if (assumes == null) {
        boolean lostAssumptions = false;
        for (Element ea: caseAssumptions.getElements()) {
          if (ea instanceof Variable) lostAssumptions = true;
        }
        if (lostAssumptions) {
          ErrorHandler.report(Errors.INVALID_CASE, "Cannot change assumptions in case", this);
        }
      } else {
        if (!caseAssumptions.equals(assumes)) {
          ErrorHandler.report(Errors.INVALID_CASE, "Must keep assumptions identical in case", this);
        }
      }
      if (concUse.getConstructor().getType() != caseNT.getType()) {
        ErrorHandler.report(INVALID_CASE, "case given is not actually a case of " + caseNT, this);
      }
    } else if (concElem instanceof NonTerminal) {
      // must have been analyzing a nonterminal n and case analyzed with a case of n'
      ErrorHandler.report(NONTERMINAL_CASE, "Case " + conclusion + " is a nonterminal; it must be a decomposition of " + ctx.currentCaseAnalysisElement, this);     
    } else {
      ErrorHandler.report("Case analysis of syntax may only be a syntax clause, not a variable, nonterminal, or binding", this);
    }		  
	
    // reuse code:
    if (assumes != null) concElem = new AssumptionElement(getLocation(),concElem,assumes);
    Term concTerm = concElem.asTerm();
    
    for (FreeVar fv : concTerm.getFreeVariables()) {
      if (ctx.inputVars.contains(fv)) {
        ErrorHandler.report(Errors.INVALID_CASE, "A case must use new variables, cannot reuse " + fv, this);
      }
    }
    
    // look up case analysis for this rule
    Set<Pair<Term,Substitution>> caseResult = ctx.caseTermMap.get(concDef);

    if (concElem instanceof ClauseUse) {
      verify(caseResult.size() <= 1, "internal invariant violated");
    }
    
    Term computedCaseTerm = null;
    
    for (Pair<Term, Substitution> pair : caseResult) {
      try {
        debug("case unify " + concTerm + " and " + pair.first);
        Substitution computedSub = pair.first.instanceOf(concTerm);
        debug("result is " + computedSub);
      } catch (UnificationFailed uf) {
        System.out.println("Case " + this + " does not apply to " + ctx.currentCaseAnalysisElement);
        continue;
      }
      computedCaseTerm = pair.first;
      verify(pair.second.getMap().size() == 0, "syntax case substitution should be empty, not " + pair.second);
      caseResult.remove(pair);
      break;
    }
    
    if (computedCaseTerm == null) {
      ErrorHandler.report(EXTRA_CASE, this);
    }
		
    Substitution oldAdaptationSub = ctx.adaptationSub;
    Map<NonTerminal, AdaptationInfo> oldAdaptationMap = new HashMap<NonTerminal, AdaptationInfo>(ctx.adaptationMap);
    NonTerminal oldInnermostGamma = ctx.innermostGamma;
    Term oldMatchTerm = ctx.matchTermForAdaptation;

    Term adaptedCaseAnalysis = ctx.currentCaseAnalysis;

    int lambdaDifference =  computedCaseTerm.countLambdas() - adaptedCaseAnalysis.countLambdas();
    // TODO: SHould check matchTermForAdaptation (RuleCase code doesn't look right to me)
    if (lambdaDifference > 0) {
      if (lambdaDifference != 2) {
        ErrorHandler.report("New assumption can only add one variable",this);
      }
      verify(concElem instanceof AssumptionElement,"not an assumption element? " + concElem);
      AssumptionElement ae = (AssumptionElement)concElem;
      if (!(ae.getBase() instanceof Variable)) {
        ErrorHandler.report("new assumption can only be used with variable", this);
      }
      verify(ae.getAssumes() instanceof ClauseUse, "not a clause use? " + assumes);
      NonTerminal newRoot = ((ClauseUse)ae.getAssumes()).getRoot();
      if (newRoot == ctx.innermostGamma || ctx.adaptationMap.containsKey(newRoot)) {
        ErrorHandler.report(REUSED_CONTEXT,"May not re-use context name " +newRoot, this);
      }
      
      // JTB: Much of the following code copied from RuleCase, perhaps assumption element should do something?
      Substitution adaptationSub = new Substitution();
      adaptedCaseAnalysis = ae.getAssumes().adaptTermTo(adaptedCaseAnalysis, concTerm, adaptationSub);
      debug("adaptedCaseAnalysis = " + adaptedCaseAnalysis);
      AdaptationInfo info = new AdaptationInfo(newRoot);
      ClauseUse.readNamesAndTypes((Abstraction)adaptedCaseAnalysis, lambdaDifference, info.varNames, info.varTypes);

      ctx.adaptationSub = adaptationSub;
      ctx.adaptationMap.put(ctx.innermostGamma, info);
      ctx.innermostGamma = info.nextContext;
      ctx.matchTermForAdaptation = adaptedCaseAnalysis;
      
      // ErrorHandler.report("Cannot yet handle case with exposed variables", this);
    }

		// make sure rule's conclusion unifies with the thing we're doing case analysis on
    // NB: need to handle the lambdaDifference here.
		concTerm = concTerm.substitute(ctx.currentSub);
		Substitution unifyingSub = null;
		try {
			unifyingSub = concTerm.unify(adaptedCaseAnalysis);
		} catch (UnificationFailed uf) {
		  throw new InternalError("Should not have unification error here!\n concTerm = " + concTerm);
		}
		
		// update the current substitution
		Substitution oldSub = new Substitution(ctx.currentSub);
		ctx.currentSub.compose(unifyingSub); // modifies in place
		
		// update the set of free variables
		Set<FreeVar> oldInputVars = ctx.inputVars;
		ctx.inputVars = new HashSet<FreeVar>(oldInputVars);
		ctx.inputVars.addAll(concTerm.getFreeVariables());
		debug("current case analysis: " + ctx.currentCaseAnalysis);
		for (FreeVar fv : ctx.currentCaseAnalysis.getFreeVariables()) {
			ctx.inputVars.remove(fv);
		}

		// update the set of subderivations
		List<Fact> oldSubderivations = new ArrayList<Fact>(ctx.subderivations);
		if (isSubderivation && concElem instanceof ClauseUse) {
			// add each part of the clause to the list of subderivations
			ctx.subderivations.addAll(((ClauseUse)concElem).getNonTerminals());
		}
		
		try {
		super.typecheck(ctx, isSubderivation);

		} finally {
		// restore the current substitution and input vars
		ctx.currentSub = oldSub;
		ctx.inputVars = oldInputVars;
		ctx.subderivations = oldSubderivations;
		ctx.bindingTypes = oldBindingTypes;
    ctx.adaptationSub = oldAdaptationSub;
    ctx.adaptationMap = oldAdaptationMap;
    ctx.innermostGamma = oldInnermostGamma;
    ctx.matchTermForAdaptation = oldMatchTerm;
		}
	}

	private Clause conclusion;
	private Clause assumes;
}

