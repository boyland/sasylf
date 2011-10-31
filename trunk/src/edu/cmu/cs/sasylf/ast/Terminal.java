package edu.cmu.cs.sasylf.ast;

import java.util.*;
import java.io.*;

import edu.cmu.cs.sasylf.ast.grammar.GrmTerminal;
import edu.cmu.cs.sasylf.ast.grammar.GrmUtil;
import edu.cmu.cs.sasylf.grammar.Symbol;
import edu.cmu.cs.sasylf.term.*;

public class Terminal extends Element implements ElemType {
	public Terminal(String s, Location l) { super(l); symbol = s; }

	public int hashCode() { return symbol.hashCode(); }
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Terminal)) return false;
		Terminal t = (Terminal) obj;
		return symbol.equals(t.symbol);
	}

	public String getTerminalSymbolString() { return getSymbol(); }
	public String getSymbol() { return symbol; }
	private String symbol;


	public Symbol getGrmSymbol() {
		return getTerminalSymbol();
	}
	public ElemType getElemType() { return this; }
	
	public Element typecheck(Context ctx) {
		return this;
	}

	@Override
	public void prettyPrint(PrintWriter out, PrintContext ctx) {
		if (symbol.length() == 1 && symbol.equals("\\"))
			out.print(symbol);
		else {
			out.print('\"');
			out.print(symbol);
			out.print('\"');
		}
	}
	public Term computeTerm(List<Pair<String, Term>> varBindings) {
		throw new RuntimeException("internal error: can't compute the term of a Terminal");
	}
}
