package edu.cmu.cs.sasylf.ast;

import java.util.*;
import java.io.*;

import edu.cmu.cs.sasylf.util.ErrorHandler;


public class SyntaxAssumption extends Fact {
	public SyntaxAssumption(String n, Location l, boolean isTheoremArg) {
		this(new NonTerminal(n, l));
		this.isTheoremArg = isTheoremArg;
		/*super(n, l);
		nonTerminal = new NonTerminal(n,l);*/
	}
	public SyntaxAssumption(String n, Location l) {
		this(new NonTerminal(n, l));
		/*super(n, l);
		nonTerminal = new NonTerminal(n,l);*/
	}
	public SyntaxAssumption(NonTerminal nt) {
		super(nt.getSymbol(), nt.getLocation());
		nonTerminal = nt;
	}

	public Syntax getSyntax() { return nonTerminal.getType(); }
	public Element getElement() { return nonTerminal; }
	public boolean isTheoremArg() { return isTheoremArg; }

	@Override
	public int hashCode() { return nonTerminal.hashCode(); }

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof SyntaxAssumption)) return false;
		SyntaxAssumption sa = (SyntaxAssumption) obj;
		return nonTerminal.equals(sa.nonTerminal);
	}

	public void prettyPrint(PrintWriter out) {
		out.print(getName());
	}

	@Override
	public void typecheck(Context ctx, boolean addToMap) {
		Element e = nonTerminal.typecheck(ctx);
		if (e != nonTerminal)
			ErrorHandler.report("No syntax match for " + getName(), this);
		/*String strippedName = Util.stripId(getName());
		syntax = ctx.synMap.get(strippedName);
		if (syntax == null)*/
		
		if (addToMap)
			this.addToDerivationMap(ctx);
	}

	private NonTerminal nonTerminal;
	private boolean isTheoremArg = false;
}
