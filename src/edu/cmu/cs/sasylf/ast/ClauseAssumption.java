package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;

public class ClauseAssumption extends Fact {

	public ClauseAssumption(Clause c, Location location) {
		this(c,location,null);
	}
	
  public ClauseAssumption(Clause c, Location location, Clause a) {
    super(null,location);
    clause = c;
    assumes = a;
  }
  
	private Clause clause;
	private Clause assumes; //XXX: this field is apparently useless
	
	@Override
	public Element getElement() {
	  //if (assumes != null) return new AssumptionElement(getLocation(),clause,assumes);
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
	public void typecheck(Context ctx) {
		clause = (Clause) clause.typecheck(ctx);
	}
	
	// ClauseAssumptions don't get added to the derivation map
	@Override
    public void addToDerivationMap(Context ctx) {
		throw new UnsupportedOperationException();
	}
}
