package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.ast.Errors.DERIVATION_NOT_FOUND;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Pair;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Util;

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
		ClauseUse targetClause = (ClauseUse)targetDerivation.getElement();
		if (!(targetClause.getType() instanceof Judgment)) {
		  ErrorHandler.report(Errors.INVERSION_REQUIRES_CLAUSE,this);
		}
		Judgment judge = (Judgment)targetClause.getType();
    RuleLike rulel = ctx.ruleMap.get(ruleName);
    if (!(rulel instanceof Rule)) {
      ErrorHandler.report(Errors.RULE_NOT_FOUND, ruleName, this);
      return;
    }
		if (((Rule)rulel).getJudgment() != judge) {
		  ErrorHandler.report(Errors.EXTRA_CASE, ": rule " + ruleName + " cannot be used to derive " + targetClause, this);
		}
    
    // Do a mini-case analysis, and see if we find result in premises
        
    Term targetTerm = DerivationByAnalysis.adapt(targetClause.asTerm(), targetClause, ctx, true);
    
    boolean found_rulel = false;
    
    // see if each rule, in turn, applies
    for (Rule rule : judge.getRules()) {
      Set<Pair<Term,Substitution>> caseResult;
      caseResult = rule.caseAnalyze(ctx, targetTerm, targetClause);
      if (caseResult.isEmpty()) continue;
      Iterator<Pair<Term, Substitution>> iterator = caseResult.iterator();
      if (rule == rulel) {
        Util.debug("inversion: caseResult = " + caseResult);
        Pair<Term,Substitution> pair = iterator.next();
        if (iterator.hasNext()) {
          ErrorHandler.report("Cannot use inversion: two or more instance of '" + ruleName + "' apply.", this);
        }
        Term result = DerivationByAnalysis.adapt(getClause().asTerm(),getClause(),ctx,false);
        result = result.substitute(pair.second);
        Util.debug("  after adapt/subst, result = " + result);
        ctx.composeSub(pair.second);
        Application ruleInstance = (Application)pair.first;
        List<Term> pieces = new ArrayList<Term>(ruleInstance.getArguments());
        pieces.remove(pieces.size()-1);
        // If there are multiple clauses, or if 
        if (pieces.size() <= 1 || this.getClause() instanceof AndClauseUse) {
          List<ClauseUse> clauses;
          if (this.getClause() instanceof AndClauseUse) {
            clauses = ((AndClauseUse)this.getClause()).getClauses();
          } else {
            clauses = new ArrayList<ClauseUse>();
            clauses.add((ClauseUse)this.getClause());
          }
          if (pieces.size() != clauses.size()) {
            // If clauses.size9) == 0, we are "use inversion" which can
            // ignore all results.
            if (clauses.size() > 0) { 
              ErrorHandler.report("inversion yields " + pieces.size() + " but only accepting " + clauses.size(), this);
            }
          }
          for (int i=0; i < clauses.size(); ++i) {
            ClauseUse cu = clauses.get(i);
            Term mt = DerivationByAnalysis.adapt(cu.asTerm(), cu, ctx, false);
            Derivation.checkMatch(cu, ctx, mt, pieces.get(i).substitute(ctx.currentSub), 
                  "inversion result #" + (i+1) + " does not match given derivation");
            // If the derivation has no implicit context, we
            // skip the context check
            if (targetClause.isRootedInVar()) {
              checkRootMatch(ctx,rule.getPremises().get(i),clauses.get(i),this);
            }
          }
        } else {
          // backward compatibility: just look for result in the pieces
          if (!pieces.contains(result)) {
            ErrorHandler.report(Errors.INVERSION_NOT_FOUND, this,
                "\t SASyLF did not find " + result + " in " + pieces);
          }
          if (targetClause.isRootedInVar()) {
            int i = pieces.indexOf(result);
            checkRootMatch(ctx,rule.getPremises().get(i),this.getElement(),this);
          }
        }
        found_rulel = true;
      } else {
        ErrorHandler.report(Errors.MISSING_CASE,
            rule.getErrorDescription(iterator.next().first, ctx), this);       
      }
    }
    if (!found_rulel) {
      ErrorHandler.report(Errors.EXTRA_CASE, ": rule " + ruleName + " cannot be used to derive " + ctx.currentCaseAnalysisElement, this);
    }
    
    // Permit induction on this term if source was a subderivation
    //XXX: This probably should handle an AndClause differently
    //XXX: Also, if the targetDerivation is the inductive argument, we should also add.
    if (ctx.subderivations.contains(targetDerivation))
      ctx.subderivations.add(this);
    }
}
