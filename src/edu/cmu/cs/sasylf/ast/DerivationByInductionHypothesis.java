package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.util.ErrorHandler;


public class DerivationByInductionHypothesis extends DerivationByIHRule {
	public DerivationByInductionHypothesis(String n, Location l, Clause c) {
		super(n,l,c);
	}
	public String prettyPrintByClause() {
		return " by induction hypothesis";
	}
	public RuleLike getRule(Context ctx) {
		return ctx.currentTheorem;
	}
	@Override
	public String getRuleName() { return "induction hypothesis"; }

	public void typecheck(Context ctx) {
		super.typecheck(ctx);
		
    if (ctx.inductionVariable == null) {
      ErrorHandler.recoverableError(Errors.INDUCTION_MISSING,this);
      return;
    }

    this.checkInduction(ctx, ctx.currentTheorem, ctx.currentTheorem);
	}
	
}
