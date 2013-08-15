package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.util.ErrorHandler;

public class DerivationUnproved extends DerivationWithArgs {
	public DerivationUnproved(String n, Location l, Clause c) {
		super(n,l,c);
	}

	public String prettyPrintByClause() {
		return " by unproved";
	}

	public void typecheck(Context ctx) {
		//tdebug("at line: " + this.getLocation().getLine());
		super.typecheck(ctx);
		ErrorHandler.warning(Errors.DERIVATION_UNPROVED, this);
	}
}
