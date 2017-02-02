package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.StringSpan;

public class DerivationByRule extends DerivationByIHRule {
	public DerivationByRule(String n, Location l, Clause c, StringSpan rn) {
		super(n,l,c); ruleName = rn.toString();
		setEndLocation(rn.getEndLocation());
	}

	@Override
	public String getRuleName() { return ruleName; }
	@Override
	public RuleLike getRule(Context ctx) {
		if (rule == null) {
			// make sure we can find the rule
			rule = ctx.ruleMap.get(ruleName);
			if (rule == null) {
				ErrorHandler.report(Errors.RULE_NOT_FOUND, ruleName, this);
			}
			if (!rule.isInterfaceOK()) {
				ErrorHandler.report(Errors.RULE_BAD, ruleName, this);
			}
			if (!(rule instanceof Rule)) {
				Theorem th = (Theorem)rule;
				ErrorHandler.recoverableError(Errors.THEOREM_NOT_RULE, ruleName, this, "rule\n" + th.getKind());
			}
		}
		return rule;
	}

	@Override
	public String prettyPrintByClause() {
		return " by rule " + ruleName;
	}

	private String ruleName;
	private RuleLike rule;
}
