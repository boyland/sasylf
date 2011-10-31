package edu.cmu.cs.sasylf.ast;

import java.util.*;
import java.io.*;

import edu.cmu.cs.sasylf.ast.grammar.GrmTerminal;
import edu.cmu.cs.sasylf.grammar.Symbol;
import edu.cmu.cs.sasylf.term.Pair;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;

/*
 * Concrete subclasses include Clause, Terminal, NonTerminal, Variable, and Binding
 */
public abstract class Element extends Node {
	public Element(Location l) { super(l); }

	public abstract ElemType getElemType();
	public abstract Symbol getGrmSymbol();

	private GrmTerminal terminal;
	public final GrmTerminal getTerminalSymbol() {
		if (terminal == null)
			terminal = new GrmTerminal(getTerminalSymbolString(), this);
		return terminal;		
	}
	
	protected abstract String getTerminalSymbolString();

	public abstract Element typecheck(Context ctx);

	@Override
	public final void prettyPrint(PrintWriter out) {
		prettyPrint(out, null);
	}

	/** t may be null, and if so, boundVars may be null too
	 */
	public abstract void prettyPrint(PrintWriter out, PrintContext ctx);
	
	/**
	 * For each binding in this clause, check that the list of element types bound in
	 * the variable is consistent with what the map says.  If the variable is not already
	 * in the map, we add it.
	 * 
	 * default empty implementation for Terminal and Variable
	 */
	void checkBindings(Map<String, List<ElemType>> bindingTypes, Node nodeToBlame) {}
	public Term getTypeTerm() { throw new UnsupportedOperationException(this.getClass().toString()); }
	protected abstract Term computeTerm(List<Pair<String, Term>> varBindings);

	public Term asTerm() {
		if (term == null)
			term = computeTerm(new ArrayList<Pair<String, Term>>());
		return term;
	}
	private Term term;
	
	/** For ClauseUse, checks that this is an assumption list and adds
	 * assumptions to varBindings and assumedVars.  For varBindings we
	 * only add actual variables, but for assumed vars we also add a variable
	 * for the derivation represented by the hypothetical judgment.
	 * 
	 * TODO: not sure, may need to add it to both
	 * 
	 * For other elements, does nothing.
	 * 
	 * Returns true if the innermost assumption is a NonTerminal.
	 */
	NonTerminal readAssumptions(List<Pair<String, Term>> varBindings, boolean includeAssumptionTerm) {
		return null;
	}

	/** If environment is a variable, add lambdas to term to make it match matchTerm.
	 * Also changes free variables so they bind new the new bound variables in them
	 * Modifies the substitution to reflect changes.
	 * Only ClauseUse actually does adaptation; the other cases simply return term.
	 */
	public Term adaptTermTo(Term term, Term matchTerm, Substitution sub) {
		return term;
	}
}
