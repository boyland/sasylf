package edu.cmu.cs.sasylf.ast;

import java.util.*;
import java.io.*;

import edu.cmu.cs.sasylf.util.ErrorHandler;


public abstract class Derivation extends Fact {
	public Derivation(String n, Location l, Clause c) {
		super(n, l); clause = c;
	}

	public Clause getClause() { return clause; }
	public Clause getElement() { return clause; }

	public void prettyPrint(PrintWriter out) {
		out.print(getName() + ": ");
		getClause().prettyPrint(out);
	}

	public void typecheck(Context ctx) {
		this.typecheck(ctx, true);
	}
	
	@Override
	public void typecheck(Context ctx, boolean addToMap) {
		clause.typecheck(ctx);

		Element newClause = clause.computeClause(ctx, false);
		if (!(newClause instanceof Clause))
			ErrorHandler.report("Expected a judgment, but found a nonterminal.  Did you forget to name the derivation?", this);

		clause = (Clause) newClause;
		clause.checkBindings(ctx.bindingTypes, this);
		//clause = new ClauseUse(clause, ctx.parseMap);
		if (addToMap)
			this.addToDerivationMap(ctx);
	}

	protected Clause clause;
}
