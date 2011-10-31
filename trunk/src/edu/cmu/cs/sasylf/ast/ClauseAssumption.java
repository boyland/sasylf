package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.Collections;

public class ClauseAssumption extends Fact {

	public ClauseAssumption(Clause c, Location location) {
		super(null,location);
		clause = c;
	}
	
	private Clause clause;

	@Override
	public Element getElement() {
		return clause;
	}

	@Override
	public void prettyPrint(PrintWriter out) {
		clause.prettyPrint(out);
	}

	@Override
	public void printReference(PrintWriter out) {
		out.print('(');
		prettyPrint(out);
		out.print(')');
	}

	@Override
	public void typecheck(Context ctx, boolean addToMap) {
		clause = (Clause) clause.typecheck(ctx);
	}
	
	// ClauseAssumptions don't get added to the derivation map
	@Override
    public void addToDerivationMap(Context ctx) {
		throw new UnsupportedOperationException();
	}
}
