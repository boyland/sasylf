package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;

import edu.cmu.cs.sasylf.prover.Proof;
import edu.cmu.cs.sasylf.prover.ProofImpl;
import edu.cmu.cs.sasylf.prover.Prover;
import edu.cmu.cs.sasylf.prover.SolveReport;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;

public class DerivationBySolve extends Derivation {
	public DerivationBySolve(String n, Location l, Clause c) {
		super(n,l,c);
	}

	@Override
	public void prettyPrint(PrintWriter pw) {
		super.prettyPrint(pw);
		pw.print(" by solve");
	}


	@Override
	public void typecheck(Context ctx) {
		super.typecheck(ctx);

		Prover prover = new Prover();
		Term term = getElement().asTerm().substitute(ctx.currentSub);
		Proof partial = new ProofImpl((Judgment)getClause().getType(),term);
		Proof complete = prover.prove(partial, 5);

		if (complete == null)
			ErrorHandler.error(Errors.SOLVE_FAILED, this);
		else {
			ErrorHandler.report(new SolveReport(this,complete));
			ErrorHandler.warning(Errors.SOLVE_UNRELIABLE, this);
		}
	}
}
