package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;

public class DerivationByRule extends DerivationByIHRule {
	public DerivationByRule(String n, Location l, Clause c, String rn) {
		super(n,l,c); ruleName = rn;
	}

	@Override
	public String getRuleName() { return ruleName; }
	public RuleLike getRule(Context ctx) {
		if (rule == null) {
			// make sure we can find the rule
			rule = ctx.ruleMap.get(ruleName);
			if (rule == null) {
				rule = ctx.recursiveTheorems.get(ruleName);
				if (rule == null)
					ErrorHandler.report(Errors.RULE_NOT_FOUND, ruleName, this);
			}
			if (!rule.isInterfaceOK()) {
			  ErrorHandler.report(Errors.RULE_BAD, ruleName, this);
			}
		}
		return rule;
	}

	@Override
	public void typecheck(Context ctx) {
    super.typecheck(ctx);
    
    if (rule instanceof Theorem) {
      Theorem self = ctx.currentTheorem;
      Theorem other = (Theorem)rule;
      if (self.getGroupLeader() == other.getGroupLeader()) {
        Fact inductiveArg = getArgs().get(other.getInductionIndex());
        if (!ctx.subderivations.contains(inductiveArg)) {
          Fact inductionVariable = self.getForalls().get(self.getInductionIndex());
          if (inductiveArg.equals(inductionVariable)) {
            if (self.getGroupIndex() <= other.getGroupIndex()) {
              ErrorHandler.report(Errors.MUTUAL_NOT_EARLIER, this);
            }
          } else if (inductiveArg instanceof SyntaxAssumption) {
            Term inductionTerm = inductionVariable.getElement().asTerm();
            Term inductiveTerm = inductiveArg.getElement().asTerm();
            Term inductionSub = inductionTerm.substitute(ctx.currentSub);
            Term inductiveSub = inductiveTerm.substitute(ctx.currentSub);
            // System.out.println("Is " + inductiveSub + " subterm of " + inductionSub + "?");
            if (!inductionSub.containsProper(inductiveSub)) {
              ErrorHandler.report(Errors.NOT_SUBDERIVATION, this);
            }
          } else {
            ErrorHandler.report(Errors.MUTUAL_NOT_SUBDERIVATION, this);
          }
        }
      }
      /*if (self.getAssumes() != null && !self.getAssumes().equals(other.getAssumes())) {
        ErrorHandler.warning("Possible loss of assume: " + self.getAssumes() + " != " + other.getAssumes(), this);
      }*/
    }
	}
	
	public String prettyPrintByClause() {
		return " by rule " + ruleName;
	}

	private String ruleName;
	private RuleLike rule;
}
