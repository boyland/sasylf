package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
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
					ErrorHandler.report(Errors.THEOREM_NOT_FOUND, theoremName.toString(), this);
			}
			if (!(resolution instanceof RuleLike)) {
				ErrorHandler.report(theoremName + " does not appear to name a theorem or rule", this, "SASyLF computed that it is a " + resolution.getClass().getSimpleName());
			}
			theorem = (RuleLike)resolution;
			if (!theorem.isInterfaceOK()) {
				ErrorHandler.report(Errors.RULE_BAD, theoremName.toString(), this);
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
	private QualName theoremName;
	private RuleLike theorem;

}
