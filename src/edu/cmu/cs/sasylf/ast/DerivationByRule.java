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
					ErrorHandler.report(Errors.RULE_NOT_FOUND, ruleName, this);
			}
			if (!(rule instanceof Rule)) {
			  ErrorHandler.recoverableError(Errors.THEOREM_NOT_RULE, ruleName, this);
			}
			if (!rule.isInterfaceOK()) {
			  ErrorHandler.report(Errors.RULE_BAD, ruleName, this);
			}
		}
		return rule;
	}
	
	public String prettyPrintByClause() {
		return " by rule " + ruleName;
	}

	private String ruleName;
	private RuleLike rule;
}
