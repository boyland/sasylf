package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Util.debug;
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
		
		Fact inductiveArg = getArgs().get(ctx.inductionPosition);
		debug("subderivations: " + ctx.subderivations);
		if (!ctx.subderivations.contains(inductiveArg))
			ErrorHandler.report(Errors.NOT_SUBDERIVATION, this);
	}
	
}
