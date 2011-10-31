package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Util.*;

import java.util.*;
import java.io.*;

import edu.cmu.cs.sasylf.ast.grammar.GrmTerminal;
import edu.cmu.cs.sasylf.grammar.Symbol;
import edu.cmu.cs.sasylf.term.*;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Util;
import static edu.cmu.cs.sasylf.ast.Errors.*;

public class NonTerminal extends Element {
	public NonTerminal(String s, Location l) { super(l); symbol = s; }

	public String getSymbol() { return symbol; }
	public Syntax getType() { return type; }
	public ElemType getElemType() { return type; }

	@Override
	public Symbol getGrmSymbol() {
		return type.getSymbol();
	}

	@Override
	public String getTerminalSymbolString() {
		return type.getTermSymbolString();
	}

	@Override
	public Term getTypeTerm() {
		return getType().typeTerm();
	}

	@Override
	public int hashCode() { return symbol.hashCode(); }
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof NonTerminal)) return false;
		NonTerminal nt = (NonTerminal) obj;
		return symbol.equals(nt.symbol);
	}

	public void setType(Syntax t) {
		if (type != null && type != t)
			ErrorHandler.report("Internal error: can't reset a NonTerminal's type", this);
		type = t;
	}

	private String symbol;
	private Syntax type;

	@Override
	public void prettyPrint(PrintWriter out, PrintContext ctx) {
		if (ctx != null) {
			if (ctx.term instanceof FreeVar) {
				out.print(ctx.getStringFor((FreeVar)ctx.term, this.getSymbol()));
			} else if (ctx.term instanceof Application) {
				Application app = (Application)ctx.term;
				String functionName = app.getFunction().getName();
				if (app.getFunction() instanceof Constant)
					printSubclause(out, ctx, functionName);
				else { // its a binding, I think
					FreeVar var = (FreeVar) app.getFunction();
					out.print(ctx.getStringFor(var, this.getSymbol()));
					for (Term t : app.getArguments()) {
						out.print('[');
						prettyPrint(out, new PrintContext(t, ctx));
						//out.print(t);
						//e.prettyPrint(out);
						out.print(']');
					}
					//out.print(ctx.term);
				}
			} else if (ctx.term instanceof Constant) {
				printSubclause(out, ctx, ((Constant)ctx.term).getName());
			} else if (ctx.term instanceof BoundVar) {
				out.print(ctx.boundVars.get(ctx.boundVars.size() - ((BoundVar)ctx.term).getIndex() ));
			} else {
				out.print(ctx.term);
			}
		} else
			out.print(symbol);
	}

	/**
	 * @param out
	 * @param ctx
	 * @param functionName
	 */
	private void printSubclause(PrintWriter out, PrintContext ctx,
			String functionName) {
		ClauseDef cd = null;
		for (Clause c : type.getClauses()) {
			if (c instanceof ClauseDef && ((ClauseDef)c).getConstructorName().equals(functionName)) {
				cd = (ClauseDef) c;
			}
		}
		if (cd == null)
			System.err.println("couldn't pretty print: " + functionName);
		else
			cd.prettyPrint(out, new PrintContext(ctx.term, ctx));
	}

	public Element typecheck(Context ctx) {
		NonTerminal nt = this;
		Element e = this;
		String strippedName = Util.stripId(nt.getSymbol());
		Variable var = ctx.varMap.get(strippedName);
		if (var != null) {
			// convert NonTerminal into Variable
			// TODO: work for numbers/primes
			Variable v = new Variable(nt.getSymbol(),nt.getLocation());
			v.setType(var.getType());
			e = v;
		} else {
			// find appropriate syntax
			Syntax syn = ctx.synMap.get(strippedName);
			if (syn != null) {
				nt.setType(syn);
			} else {
				ErrorHandler.report(Errors.UNDECLARED_NONTERMINAL, "no nonterminal match for " + nt.getSymbol() + "; did you forget to declare " + nt.getSymbol() + " as a terminal?", nt);
			}
		}
		return e;
	}

	@Override
	public FreeVar computeTerm(List<Pair<String, Term>> varBindings) {
		return new FreeVar(symbol, type.typeTerm());
	}
	
	@Override
	NonTerminal readAssumptions(List<Pair<String, Term>> varBindings, boolean includeAssumptionTerm) {
		return this;
	}
	
	@Override
	void checkBindings(Map<String, List<ElemType>> bindingTypes, Node nodeToBlame) {
		List<ElemType> myType = new ArrayList<ElemType>();
		List<ElemType> prevType = bindingTypes.get(getSymbol());
		debug("binding for " + this + " is " + bindingTypes);
		if (prevType == null) {
			bindingTypes.put(getSymbol(), myType);
		} else {
			if (!prevType.equals(myType))
				ErrorHandler.report(BINDING_INCONSISTENT, "meta-variable " + this + " must have consistent numbers and types of bindings throughout a rule or branch of a theorem", nodeToBlame);
		}
	}
}
