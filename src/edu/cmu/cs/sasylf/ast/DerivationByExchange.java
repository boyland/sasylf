package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.util.ErrorHandler;

public class DerivationByExchange extends DerivationWithArgs {
	public DerivationByExchange(String n, Location l, Clause c) {
		super(n,l,c);
	}

	public String prettyPrintByClause() {
		return " by exchange";
	}

	public void typecheck(Context ctx) {
		super.typecheck(ctx);
		
		Fact arg = getArgs().get(0);
		if (ctx.subderivations.contains(arg))
			ctx.subderivations.add(this);
	}
}
