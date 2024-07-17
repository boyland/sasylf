package edu.cmu.cs.sasylf.ast;

import java.util.function.Consumer;

import edu.cmu.cs.sasylf.CloneData;
import edu.cmu.cs.sasylf.SubstitutionData;
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
				ErrorHandler.error(Errors.RULE_NOT_FOUND, ruleName.toString(), this);
			}
			if (!(resolution instanceof RuleLike)) {
				ErrorHandler.error(Errors.RULE_EXPECTED, QualName.classify(resolution) + " " + ruleName, this);
			}
			rule = (RuleLike)resolution;
			if (!rule.isInterfaceOK()) {
				ErrorHandler.error(Errors.RULE_BAD, ruleName.toString(), this);
			}
			if (!(rule instanceof Rule)) {
				Theorem th = (Theorem)rule;
				ErrorHandler.recoverableError(Errors.THEOREM_NOT_RULE, ruleName.toString(), ruleName, "rule\n" + th.getKind());
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
	
	@Override
	public void collectQualNames(Consumer<QualName> consumer) {
		ruleName.visit(consumer);
	}

	@Override
	public void substitute(SubstitutionData sd) {
		if (sd.didSubstituteFor(this)) return;
		super.substitute(sd);
		sd.setSubstitutedFor(this);

		ruleName.substitute(sd);
		rule.substitute(sd);
	}

	@Override
	public DerivationByRule copy(CloneData cd) {
		if (cd.containsCloneFor(this)) return (DerivationByRule) cd.getCloneFor(this);
		DerivationByRule clone = (DerivationByRule) super.copy(cd);
		cd.addCloneFor(this, clone);
		clone.ruleName = ruleName.copy(cd);
		clone.rule = clone.rule.copy(cd);
		return clone;
	}
}
