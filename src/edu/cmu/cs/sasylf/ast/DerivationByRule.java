package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;

public class DerivationByRule extends DerivationByIHRule {
	public DerivationByRule(String n, Location l, Clause c, QualName rn) {
		super(n,l,c); ruleName = rn;
		setEndLocation(rn.getEndLocation());
	}

	@Override
	public String getRuleName() { return ruleName.toString(); }
	@Override
	public RuleLike getRule(Context ctx) {
		if (rule == null) {
			Object resolution = ruleName.resolve(ctx);
			if (resolution == null || resolution instanceof String[]) {
				ErrorHandler.report(Errors.RULE_NOT_FOUND, ruleName.toString(), this);
			}
			if (!(resolution instanceof RuleLike)) {
				ErrorHandler.report(ruleName + " does not appear to be a rule", this, "SASyLF computed that it is a " + resolution.getClass().getSimpleName());
			}
			rule = (RuleLike)resolution;
			if (!rule.isInterfaceOK()) {
				ErrorHandler.report(Errors.RULE_BAD, ruleName.toString(), this);
			}
			if (!(rule instanceof Rule)) {
				Theorem th = (Theorem)rule;
				ErrorHandler.recoverableError(Errors.THEOREM_NOT_RULE, ruleName.toString(), this, "rule\n" + th.getKind());
			}
		}
		return rule;
	}

	@Override
	public String prettyPrintByClause() {
		return " by rule " + ruleName;
	}

	private QualName ruleName;
	private RuleLike rule;
}
