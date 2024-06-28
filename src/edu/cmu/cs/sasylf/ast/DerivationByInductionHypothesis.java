package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.CloneData;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Span;



public class DerivationByInductionHypothesis extends DerivationByIHRule {
	public DerivationByInductionHypothesis(String n, Location l, Clause c) {
		super(n,l,c);
	}
	@Override
	public String prettyPrintByClause() {
		return " by induction hypothesis";
	}
	@Override
	public RuleLike getRule(Context ctx) {
		return ctx.currentTheorem;
	}
	@Override
	public String getRuleName() { return "induction hypothesis"; }

	@Override
	public void typecheck(Context ctx) {
		super.typecheck(ctx);

		this.checkInduction(ctx, ctx.currentTheorem, ctx.currentTheorem);
	}
	@Override
	public DerivationByInductionHypothesis copy(CloneData cd) {
		if (cd.containsCloneFor(this)) return (DerivationByInductionHypothesis) cd.getCloneFor(this);
		DerivationByInductionHypothesis clone = (DerivationByInductionHypothesis) super.copy(cd);
		cd.addCloneFor(this, clone);
		return clone;
	}

}
