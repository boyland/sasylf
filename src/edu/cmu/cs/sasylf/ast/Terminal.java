package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.List;

import edu.cmu.cs.sasylf.CloneData;
import edu.cmu.cs.sasylf.SubstitutionData;
import edu.cmu.cs.sasylf.grammar.Symbol;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Span;

public class Terminal extends Element implements ElemType {
	private Span sp;
	public Terminal(String s, Span sp) { 
		super(sp.getLocation()); 
		symbol = s; 
		super.setEndLocation(sp.getEndLocation());
		this.sp = sp;
	}

	@Override
	public String getName() { return symbol; }

	@Override
	public int hashCode() { return symbol.hashCode(); }
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Terminal)) return false;
		Terminal t = (Terminal) obj;
		return symbol.equals(t.symbol);
	}

	@Override
	public String getTerminalSymbolString() { return getSymbol(); }
	public String getSymbol() { return symbol; }

	public boolean mustQuote() {
		return mustQuote;
	}
	
	public void setMustQuote() {
		mustQuote = true;
	}

	private String symbol;
	private boolean mustQuote;

	@Override
	public Symbol getGrmSymbol() {
		return getTerminalSymbol();
	}
	@Override
	public ElemType getType() { return this; }

	@Override
	public ElemType getElemType() { return this; }

	@Override
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

	@Override
	public Term computeTerm(List<Pair<String, Term>> varBindings) {
		throw new RuntimeException("internal error: can't compute the term of a Terminal");
	}
	
	/**
	 * Return true if the element is a terminal with the given string
	 * @param e element to check
	 * @param s string to match against
	 * @return whether the element equals a terminal with the given string
	 */
	public static boolean matches(Element e, String s) {
		return e instanceof Terminal && ((Terminal)e).symbol.equals(s);
	}
	
	@Override
	public Term typeTerm() {
		throw new RuntimeException("internal error: can't compute the term of a Terminal");
	}
	
	public void substitute(SubstitutionData sd) {
		super.substitute(sd);
		if (sd.didSubstituteFor(this)) return;
		sd.setSubstitutedFor(this);

		/*
			We want to check if symbol matches from

			We need to remove these characters from the back of the string, then check if they are equal

			**Match** means `from` is a prefix of `symbol`, and all characters after the prefix matching are **filler characters**
		
		*/

		// First, check that from is a prefix of symbol

		if (!symbol.startsWith(sd.from)) {
			return;
		}


		// Check that all characters after the prefix match are filler characters

		int fromLength = sd.from.length();

		String filler = symbol.substring(fromLength);

		// the filler characters are 0-9, _, and '

		if (filler.matches("^[0-9_']*$")) {
			symbol = sd.to + filler;
		}
	
	}

	public Terminal copy(CloneData cd) {
		if (cd.containsCloneFor(this)) return (Terminal) cd.getCloneFor(this);

		// make a clone
		Terminal clone = (Terminal) super.copy(cd);
		
		cd.addCloneFor(this, clone);

		/*
			We have to clone the following attributes

			private Span sp;
			private String symbol;
			private boolean mustQuote;
		*/

		clone.sp = sp.copy(cd);

		// the other attributes are immutable, so we don't need to clone them
		
		return clone;
	}
}
