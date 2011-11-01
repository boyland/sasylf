package edu.cmu.cs.sasylf.ast;

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
          if (inductiveArg == self.getForalls().get(self.getInductionIndex())) {
            if (self.getGroupIndex() <= other.getGroupIndex()) {
              ErrorHandler.report(Errors.MUTUAL_NOT_EARLIER, this);
            }
          } else {
            ErrorHandler.report(Errors.MUTUAL_NOT_SUBDERIVATION, this);
          }
        }
      }
    }
	}
	
	public String prettyPrintByClause() {
		return " by rule " + ruleName;
	}

	private String ruleName;
	private RuleLike rule;
}
