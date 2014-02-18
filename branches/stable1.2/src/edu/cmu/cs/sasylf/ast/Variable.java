package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.List;

import edu.cmu.cs.sasylf.grammar.Symbol;
import edu.cmu.cs.sasylf.term.BoundVar;
import edu.cmu.cs.sasylf.term.Pair;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Util;

public class Variable extends Element {
	public Variable(String s, Location l) { super(l); symbol = s; }

	public String getSymbol() { return symbol; }
	public Syntax getType() { return type; }
	public ElemType getElemType() { return type; }
	public Symbol getGrmSymbol() {
		if (type == null)
			System.err.println("null type for " + this);
		return type.getSymbol();
	}

	@Override
	public String getTerminalSymbolString() {
		return type.getTermSymbolString();
	}

	public int hashCode() { return symbol.hashCode(); }
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Variable)) return false;
		Variable v = (Variable) obj;
		return symbol.equals(v.symbol);
	}

	
	public void setType(Syntax t) {
		if (type != null)
			ErrorHandler.report("The same variable may not appear in multiple syntax definitions", this);
		if (t == null)
			ErrorHandler.report("No type can be found for variable " + symbol + ": did you forget to make it a case of a BNF syntax definition?", this);
		type = t;
	}
	
	public Element typecheck(Context ctx) {
		if (type == null) {
			String strippedName = Util.stripId(getSymbol());
			Variable var = ctx.varMap.get(strippedName);
			if (var != null) {
				setType(var.getType());
			} else {
				ErrorHandler.report("No type can be found for variable " + symbol + ": did you forget to make it a case of a BNF syntax definition?", this);
			}
		}
		return this;
	}

	private String symbol;
	private Syntax type;

	@Override
	public void prettyPrint(PrintWriter out, PrintContext ctx) {
		out.print(symbol);
	}

	public BoundVar computeTerm(List<Pair<String, Term>> varBindings) {
		int index = -1;
		for (int i = 0; i < varBindings.size(); ++i)
			if (varBindings.get(i).first.equals(symbol)) {
				index = i;
				break;
			}
		
		if (index == -1)
			ErrorHandler.report("Variable " + symbol + " is not bound", this);

		//return new BoundVar(index + 1);
		return new BoundVar(varBindings.size()-index);
	}
}
