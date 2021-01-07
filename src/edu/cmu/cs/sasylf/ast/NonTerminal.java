package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Errors.BINDING_INCONSISTENT;
import static edu.cmu.cs.sasylf.util.Util.debug;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.grammar.Symbol;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.BoundVar;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Util;

public class NonTerminal extends Element {
	public NonTerminal(String s, Location l) { this(s,l,null); }
	public NonTerminal(String s, Location l, SyntaxDeclaration ty) {
		super(l);
		symbol = s;
		type = ty;
		if (l != null) {
			super.setEndLocation(l.add(s.length()));
		}
	}

	public String getSymbol() { return symbol; }
	@Override
	public SyntaxDeclaration getType() { return type; }
	@Override
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

	public void setType(SyntaxDeclaration t) {
		if (type != null && type != t)
			ErrorHandler.error(Errors.INTERNAL_ERROR, "Internal error: can't reset a NonTerminal's type", this);
		type = t;
	}

	private String symbol;
	private SyntaxDeclaration type;

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

	@Override
	public Element typecheck(Context ctx) {
		if (ctx.isTerminalString(symbol)) {
			return new Terminal(symbol,this);
		}
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
			SyntaxDeclaration syn = ctx.getSyntax(strippedName);
			if (syn != null) {
				nt.setType(syn);
			} else if (nt.getSymbol().equals("or")) {
				e = new AndOrJudgment.OpTerminal("or", this);
			} else if (nt.getSymbol().equals("not")) {
				e = new NotJudgment.NotTerminal(this);
			} else {
				e = new Terminal(nt.getSymbol(),nt.getLocation());
				ErrorHandler.recoverableError(Errors.SYNTAX_UNDECLARED, nt);
			}
		}
		return e;
	}

	@Override
	public Fact asFact(Context ctx, Element assumes) {
		if (ctx.bindingTypes.containsKey(symbol) &&
				!ctx.bindingTypes.get(symbol).isEmpty()) {
			ErrorHandler.error(Errors.BINDING_INCONSISTENT, symbol, this);
		}
		// System.out.println(this+".asFact(_," + assumes + ")");
		if (ctx.isVarFree(this) || assumes == null ||
				!((SyntaxDeclaration)assumes.getType()).canAppearIn(getTypeTerm()))
			return new NonTerminalAssumption(this);
		else return new NonTerminalAssumption(this,assumes);
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
		debug("binding for ", this, " is ", bindingTypes);
		if (prevType == null) {
			bindingTypes.put(getSymbol(), myType);
		} else {
			if (!prevType.equals(myType))
				ErrorHandler.error(BINDING_INCONSISTENT, this.toString() , nodeToBlame);
		}
	}
	@Override
	public NonTerminal getRoot() {
		// This is correct if this element came by way of a Fact,
		// otherwise it would have the correct context.
		return null;
	}
	@Override
	void getFree(Set<NonTerminal> freeSet, boolean rigidOnly) {
		freeSet.add(this);
	}


}
