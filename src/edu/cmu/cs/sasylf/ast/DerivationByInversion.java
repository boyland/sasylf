package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.ast.Errors.DERIVATION_NOT_FOUND;
import static edu.cmu.cs.sasylf.util.Util.debug;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Pair;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;

public class DerivationByInversion extends DerivationWithArgs {
  private final String ruleName;
  private final String inputName;
  
	public DerivationByInversion(String n, Location l, Clause c, String rule, String relation) {
		super(n,l,c);
		ruleName = rule;
		inputName = relation;
	}

	public String prettyPrintByClause() {
		return " by inversion of " + ruleName + " on " + inputName;
	}

	public void typecheck(Context ctx) {
		super.typecheck(ctx);
		
		Fact targetDerivation = ctx.derivationMap.get(inputName);
		if (targetDerivation == null) {
		  ErrorHandler.report(DERIVATION_NOT_FOUND, "Cannot find a derivation named "+ inputName, this);
		  return;
		}
		if (!(targetDerivation.getElement() instanceof ClauseUse)) {
		  ErrorHandler.report(Errors.INVERSION_REQUIRES_CLAUSE,this);
		}
		
    RuleLike rulel = ctx.ruleMap.get(ruleName);
    if (rulel == null) {
      ErrorHandler.report(Errors.RULE_NOT_FOUND, ruleName, this);
      return;
    }
    if (!(rulel instanceof Rule)) {
      ErrorHandler.report(Errors.RULE_NOT_FOUND, ruleName, this);
      return;
    }
		
    // Do a mini-case analysis, and see if we find result in premises
    
    Term oldCase = ctx.currentCaseAnalysis;
    Element oldElement = ctx.currentCaseAnalysisElement;
    Term oldGoal = ctx.currentGoal;
    Clause oldGoalClause = ctx.currentGoalClause;
    Map<CanBeCase,Set<Pair<Term,Substitution>>> oldCaseTermMap = ctx.caseTermMap;
    ctx.currentCaseAnalysis = DerivationByAnalysis.adapt(targetDerivation.getElement().asTerm(), targetDerivation.getElement(), ctx, true);
    debug("setting current case analysis to " + ctx.currentCaseAnalysis);
    //ctx.currentCaseAnalysis = targetDerivation.getElement().asTerm().substitute(ctx.currentSub);
    ctx.currentCaseAnalysisElement = targetDerivation.getElement();
    ctx.currentGoal = getElement().asTerm().substitute(ctx.currentSub);
    ctx.currentGoalClause = getClause();
    ctx.caseTermMap = new HashMap<CanBeCase,Set<Pair<Term,Substitution>>>();

    Judgment judge= (Judgment) ((ClauseUse)ctx.currentCaseAnalysisElement).getConstructor().getType();
    boolean found_rulel = false;
    debug("*********** case analyzing line " + getLocation().getLine());
    
    // see if each rule, in turn, applies
    for (Rule rule : judge.getRules()) {
      Set<Pair<Term,Substitution>> caseResult = rule.caseAnalyze(ctx);
      if (caseResult.isEmpty()) continue;
      if (rule == rulel) {
        // TODO: look for premise in substituted premises.
        ErrorHandler.warning(Errors.INVERSION_UNCHECKED, this);
        found_rulel = true;
      } else {
        ErrorHandler.report(Errors.MISSING_CASE,
            rule.getErrorDescription(caseResult.iterator().next().first, ctx), this);       
      }
    }
    if (!found_rulel) {
      ErrorHandler.report(Errors.EXTRA_CASE, ": rule " + ruleName + " cannot be used to derive " + ctx.currentCaseAnalysisElement, this);
    }

    ctx.caseTermMap = oldCaseTermMap;
    ctx.currentCaseAnalysis = oldCase;
    ctx.currentCaseAnalysisElement = oldElement;
    ctx.currentGoal = oldGoal ;
    ctx.currentGoalClause = oldGoalClause;
    
    // Permit induction on this term if source was a subderivation
    if (ctx.subderivations.contains(targetDerivation))
      ctx.subderivations.add(this);
    }
}
