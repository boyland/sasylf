package edu.cmu.cs.sasylf.ast;

import java.util.*;
import java.io.*;

import static edu.cmu.cs.sasylf.util.Util.*;

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
    public abstract void typecheck(Context ctx, boolean addToMap);
	public void addToDerivationMap(Context ctx) {
		ctx.derivationMap.put(getName(), this);    	
    }
}
