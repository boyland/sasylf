package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.Set;

import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;

/**
 * Common superclass of Derivation, SyntaxAssumption, and ClauseAssumption
 */
public abstract class Fact extends Node {
	
	private String name;

	public Fact(String n, Location l) {
		super(l);
		name = n;
		if (l != null && n != null) {
			setEndLocation(l.add(n.length()));
		}
	}

	public String getName() { return name; }
	public abstract Element getElement();

	@Override
	public abstract void prettyPrint(PrintWriter out);
	public void printReference(PrintWriter out) { out.print(getName()); }
	public abstract void typecheck(Context ctx);

	public void addToDerivationMap(Context ctx) {
		if (this instanceof PartialCaseAnalysis) return; // do NOT add to map
		boolean wasKnown = ctx.isLocallyKnown(getName());
		Fact old = ctx.derivationMap.put(getName(), this);  
		if (wasKnown && !(this instanceof SyntaxAssumption) && 
				old != this && !getName().equals("_") && !getName().equals("proof")) {
			ErrorHandler.warning(Errors.DERIVATION_NAME_REUSED, ": " + getName(), this);
		}
		Set<FreeVar> free = getElement().asTerm().getFreeVariables();
		free.removeAll(ctx.currentSub.getMap().keySet());
		ctx.inputVars.addAll(free);
		if (getElement().getRoot() == null) {
			ctx.addVarFree(free,getLocation());
		}
	}
}
