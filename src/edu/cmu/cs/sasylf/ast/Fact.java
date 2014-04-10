package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.Set;

import edu.cmu.cs.sasylf.term.FreeVar;

/**
 * Common superclass of Derivation, SyntaxAssumption, and ClauseAssumption
 */
public abstract class Fact extends Node {
    public Fact(String n, Location l) {
    	super(l);
		name = n;
    }
    private String name;

    public String getName() { return name; }
    public abstract Element getElement();

    public abstract void prettyPrint(PrintWriter out);
    public void printReference(PrintWriter out) { out.print(getName()); }
    public abstract void typecheck(Context ctx);
	  public void addToDerivationMap(Context ctx) {
		  ctx.derivationMap.put(getName(), this);  
		  Set<FreeVar> free = getElement().asTerm().getFreeVariables();
		  free.removeAll(ctx.currentSub.getMap().keySet());
		  if (ctx.adaptationSub != null) free.removeAll(ctx.adaptationSub.getMap().keySet());
		  ctx.inputVars.addAll(free);
    }
}
