package edu.cmu.cs.sasylf.ast;

import java.util.function.Consumer;

import edu.cmu.cs.sasylf.CloneData;
import edu.cmu.cs.sasylf.SubstitutionData;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Span;
import edu.cmu.cs.sasylf.util.StringSpan;

public class DerivationByTheorem extends DerivationByIHRule {

	public DerivationByTheorem(String n, Location l, Clause c, QualName name, StringSpan kind) {
		super(n, l, c);
		theoremName = name;
		setEndLocation(name.getEndLocation());
		theoremKind = kind;
	}

	@Override
	public String getRuleName() { return theoremName.toString(); }

	@Override
	public RuleLike getRule(Context ctx) {
		if (theorem == null) {
			Object resolution = theoremName.resolve(ctx);
			if (resolution == null || resolution instanceof String[]) {
				resolution = theorem = ctx.recursiveTheorems.get(theoremName.toString());
				if (theorem == null)
					ErrorHandler.error(Errors.THEOREM_NOT_FOUND, theoremName.toString(), this);
			}
			if (!(resolution instanceof RuleLike)) {
				ErrorHandler.error(Errors.THEOREM_EXPECTED, QualName.classify(resolution) + " " + theoremName, this);
			}
			theorem = (RuleLike)resolution;
			if (!theorem.isInterfaceOK()) {
				ErrorHandler.error(Errors.RULE_BAD, theoremName.toString(), this);
			}
			if (!(theorem instanceof Theorem)) {
				if (theoremKind == null || theoremKind.length() == 0) {
					String kind = "rule";
					ErrorHandler.recoverableError(Errors.THEOREM_KIND_MISSING, kind, theoremName, "by\nby "+kind);
				} else {
					ErrorHandler.recoverableError(Errors.RULE_NOT_THEOREM, theoremName.toString(), theoremKind, theoremKind +"\nrule");
				}
			} else { 
				String kind = ((Theorem)theorem).getKind();
				if (theoremKind == null || theoremKind.length() == 0) {
					ErrorHandler.recoverableError(Errors.THEOREM_KIND_MISSING, kind, theoremName, "by\nby "+kind);
				} else if (!kind.equals(theoremKind.toString())) {
					ErrorHandler.recoverableError(Errors.THEOREM_KIND_WRONG, theoremKind + " " + theoremName, theoremKind, theoremKind+"\n"+kind);
				}
			}
		}
		return theorem;
	}

	@Override
	public void typecheck(Context ctx) {
		super.typecheck(ctx);

		if (theorem instanceof Theorem) {
			Theorem self = ctx.currentTheorem;
			Theorem other = (Theorem)theorem;
			if (self.getGroupLeader() == other.getGroupLeader()) {
				checkInduction(ctx, self, other);
			}
		}
	}

	@Override
	public String prettyPrintByClause() {
		return " by " + theoremKind + " " + theoremName;
	}

	private StringSpan theoremKind;
	public QualName theoremName; // changed this to public for debugging purposes TODO: Change it back to private
	private RuleLike theorem;

	@Override
	public void collectQualNames(Consumer<QualName> consumer) {
		theoremName.visit(consumer);
	}

	@Override
	public void substitute(SubstitutionData sd) {
		if (sd.didSubstituteFor(this)) return;
		super.substitute(sd);
		sd.setSubstitutedFor(this);

		theoremKind.substitute(sd);
		theoremName.substitute(sd);
		theorem.substitute(sd);
	}

	@Override
	public DerivationByTheorem copy(CloneData cd) {
		if (cd.containsCloneFor(this)) return (DerivationByTheorem) cd.getCloneFor(this);
		DerivationByTheorem clone = (DerivationByTheorem) super.copy(cd);
		cd.addCloneFor(this, clone);

		// TODO: clone StringSpan
		//clone.theoremKind = clone.theoremKind.copy(cd);
		clone.theoremName = clone.theoremName.copy(cd);
		clone.theorem = clone.theorem.copy(cd);

		return clone;

	}
}
