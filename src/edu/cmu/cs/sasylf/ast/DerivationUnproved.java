package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;

import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;

public class DerivationUnproved extends Derivation {
	public DerivationUnproved(String n, Location l, Clause c) {
		super(n,l,c);
	}

	@Override
	public void prettyPrint(PrintWriter pw) {
		super.prettyPrint(pw);
		pw.print(" by unproved");
	}

	@Override
	public void typecheck(Context ctx) {
		super.typecheck(ctx);
		Clause cl = getClause();
		Term t = ctx.toTerm(cl);
		String form = "???";
		if (cl instanceof ClauseUse) {
			try {
				TermPrinter termPrinter = new TermPrinter(ctx,((ClauseUse)cl).getRoot(),getLocation());
				form = termPrinter.toString(termPrinter.asClause(t));
			} catch (RuntimeException ex) {
				System.out.println("At " + this.getLocation());
				ex.printStackTrace();
				form = "" + t;
				ErrorHandler.recoverableError(Errors.INTERNAL_ERROR, ": Couldn't print " + t, this);
			}
		}
		ErrorHandler.warning(Errors.DERIVATION_UNPROVED, form, this, t.toString());
	}
}
