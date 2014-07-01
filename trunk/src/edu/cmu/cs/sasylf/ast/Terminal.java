package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.List;

import edu.cmu.cs.sasylf.grammar.Symbol;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Span;

public class Terminal extends Element implements ElemType {
	public Terminal(String s, Span sp) { 
	  super(sp.getLocation()); 
	  symbol = s; 
	  super.setEndLocation(sp.getEndLocation());
	}

	public String getName() { return symbol; }
	
	public int hashCode() { return symbol.hashCode(); }
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Terminal)) return false;
		Terminal t = (Terminal) obj;
		return symbol.equals(t.symbol);
	}

	public String getTerminalSymbolString() { return getSymbol(); }
	public String getSymbol() { return symbol; }
	
	public void setMustQuote() {
	  mustQuote = true;
	}
	
	private String symbol;
	private boolean mustQuote;

	public Symbol getGrmSymbol() {
		return getTerminalSymbol();
	}
	public ElemType getType() { return this; }
	
	public ElemType getElemType() { return this; }
	
	public Element typecheck(Context ctx) {
		return this;
	}

	@Override
	public void prettyPrint(PrintWriter out, PrintContext ctx) {
		if (symbol.length() == 1 && symbol.equals("\\"))
			out.print(symbol);
		else {
			if (mustQuote) out.print('\"'); //XXX: doesn't correctly handle nested quotes
			out.print(symbol);
			if (mustQuote) out.print('\"');
		}
	}
	
	
	@Override
  public Fact asFact(Context ctx, Element assumes) {
    throw new RuntimeException("internal error: can't compute a Fact for a Terminal.");
  }

  public Term computeTerm(List<Pair<String, Term>> varBindings) {
		throw new RuntimeException("internal error: can't compute the term of a Terminal");
	}
}
