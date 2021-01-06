package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.grammar.Symbol;
import edu.cmu.cs.sasylf.term.BoundVar;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Util;

public class Variable extends Element {
	public Variable(String s, Location l) { 
		super(l); 
		symbol = s; 
		setEndLocation(l.add(s.length())); 
	}

	public String getSymbol() { return symbol; }
	@Override
	public SyntaxDeclaration getType() { return type; }
	@Override
	public ElemType getElemType() { return type; }
	@Override
	public Symbol getGrmSymbol() {
		if (type == null)
			System.err.println("null type for " + this);
		return type.getSymbol();
	}

	@Override
	public String getTerminalSymbolString() {
		return type.getTermSymbolString();
	}

	@Override
	public int hashCode() { return symbol.hashCode(); }
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Variable)) return false;
		Variable v = (Variable) obj;
		return symbol.equals(v.symbol);
	}


	public void setType(SyntaxDeclaration t) {
		if (type != null && type == t) return; // idempotency
		if (type != null) {
			ErrorHandler.recoverableError(Errors.SYNTAX_VARIABLE_TWICE, ": " + symbol, t);
			return;
		}
		if (t == null)
			ErrorHandler.error(Errors.SYNTAX_VARIABLE_MISSING, this);
		type = t;
	}

	@Override
	public Element typecheck(Context ctx) {
		if (type == null) {
			String strippedName = Util.stripId(getSymbol());
			Variable var = ctx.varMap.get(strippedName);
			if (var != null) {
				setType(var.getType());
			} else {
				ErrorHandler.error(Errors.SYNTAX_VARIABLE_MISSING, this);
			}
		}
		return this;
	}

	@Override
	void checkVariables(Set<String> bound, boolean defining) {
		// System.out.println("Checking " + getSymbol() + " as defining? " + defining);
		if (defining) {
			if (!bound.add(getSymbol())) {
				ErrorHandler.error(Errors.VAR_REBOUND, getSymbol(),this);
			}
		} else {
			if (!bound.contains(getSymbol())) {
				ErrorHandler.error(Errors.VAR_UNBOUND, getSymbol(),this);
			}
		}
	}

	private String symbol;
	private SyntaxDeclaration type;

	@Override
	public void prettyPrint(PrintWriter out, PrintContext ctx) {
		out.print(symbol);
	}

	@Override
	public Fact asFact(Context ctx, Element assumes) {
		return new VariableAssumption(this);
	}

	/**
	 * Generate a context clause for this variable around the given root.
	 * @return a context clause that binds the variable.  It will have fresh variables.
	 */
	public ClauseUse genContext(Element base, Context ctx) {
		ClauseDef contextClause = getType().getContextClause();
		SyntaxDeclaration contextSyntax = (SyntaxDeclaration)contextClause.getType();
		Location location = getLocation();
		if (base == null) {
			ClauseDef termCase = contextSyntax.getTerminalCase();
			base = new ClauseUse(location,new ArrayList<Element>(termCase.getElements()),termCase);
		}
		List<Element> newElements = new ArrayList<Element>();
		for (Element e : contextClause.getElements()) {
			if (e instanceof NonTerminal) {
				NonTerminal nt = (NonTerminal)e;
				if (nt.getType() == contextSyntax) {
					newElements.add(base);
				} else {
					newElements.add(new NonTerminal(ctx.genFresh(nt.getSymbol()),location,nt.getType()));
				}
			} else if (e instanceof Terminal) {
				newElements.add(e);
			} else if (e instanceof Variable) {
				newElements.add(this);
			} else {
				throw new RuntimeException("internal error: context clause has strange thing it: " + e);
			}
		}
		return new ClauseUse(location,newElements,contextClause);
	}

	@Override
	public Term computeTerm(List<Pair<String, Term>> varBindings) {
		int index = -1;
		for (int i = varBindings.size()-1; i >= 0; --i) {
			if (varBindings.get(i).first.equals(symbol)) {
				index = i;
				break;
			}
		}

		if (index == -1) {
			ErrorHandler.recoverableError(Errors.VAR_UNBOUND, symbol, this);
			return new FreeVar("_"+symbol,type.typeTerm()).freshify();
		}

		return new BoundVar(varBindings.size()-index);
	}
}
