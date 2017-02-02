package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;

public class DerivationUnproved extends DerivationWithArgs {
	public DerivationUnproved(String n, Location l, Clause c) {
		super(n,l,c);
	}

	@Override
	public String prettyPrintByClause() {
		return " by unproved";
	}

	@Override
	public void typecheck(Context ctx) {
		super.typecheck(ctx);
		Clause cl = getClause();
		Term t = ctx.toTerm(cl);
		String form = "";
		if (cl instanceof ClauseUse) {
			try {
				TermPrinter termPrinter = new TermPrinter(ctx,((ClauseUse)cl).getRoot(),getLocation());
				form = ": " + termPrinter.toString(termPrinter.asClause(t));
			} catch (RuntimeException ex) {
				ex.printStackTrace();
				form = ": (internal) " + t;
				// ErrorHandler.report(Errors.INTERNAL_ERROR, ": Couldn't print " + t, this);
			}
		}
		ErrorHandler.warning(Errors.DERIVATION_UNPROVED, form, this, t.toString());
	}
}
