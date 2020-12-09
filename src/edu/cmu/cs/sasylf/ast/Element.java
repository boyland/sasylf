package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.ast.grammar.GrmTerminal;
import edu.cmu.cs.sasylf.grammar.Symbol;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.SASyLFError;

/*
 * Concrete subclasses include Clause, Terminal, NonTerminal, Variable, and Binding
 */
public abstract class Element extends Node {
	
	private Term term;
	private GrmTerminal terminal;

	public Element(Location l) { super(l); }

	public abstract ElementType getType();
	public abstract ElemType getElemType();
	public abstract Symbol getGrmSymbol();

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
	 * Compute a fact for this element.
	 * This method is used to handle an argument to a Derivation,
	 * and also to identify sub-terms of an inductive syntax term.
	 * <p>
	 * It can handle syntax, perhaps with an assumption,
	 * and can handle existing derivations, but cannot handle
	 * a written out judgment, because this would be a derivation without a
	 * justification.
	 * @param ctx global information
	 * @param assumes context in which this element is seen.
	 * One of: (a) null, (b) a NonTerminal, (c) a ClauseUse.
	 * @return fact for this argument
	 * @throws SASyLFError if this element is a Clause for a Judgment.
	 */
	public abstract Fact asFact(Context ctx, Element assumes);

	/**
	 * For each binding in this clause, check that the list of element types bound in
	 * the variable is consistent with what the map says.  If the variable is not already
	 * in the map, we add it.
	 * 
	 * default empty implementation for Terminal and Variable
	 */
	void checkBindings(Map<String, List<ElemType>> bindingTypes, Node nodeToBlame) {}
	/**
	 * For each Variable in this clause check that it is bound exactly once.
	 * @param bound set of variables that are already found to be bound.
	 * @param defining true if this element is a defining occurrence.
	 */
	void checkVariables(Set<String> bound, boolean defining) {}
	public Term getTypeTerm() { return getType().typeTerm(); }
	protected abstract Term computeTerm(List<Pair<String, Term>> varBindings);

	/**
	 * Return the nonterminals in this element.
	 * These correspond to the free variable in the LF term,
	 * but this method can be called on non-type-checked elements.
	 * @param rigidOnly if true, only find occurrences in rigid position
	 * @return set of nonterminals in this element (perhaps restricted to rigid positions)
	 */
	public Set<NonTerminal> getFree(boolean rigidOnly) {
		Set<NonTerminal> result = new HashSet<NonTerminal>();
		getFree(result,rigidOnly);
		return result;
	}

	/**
	 * Add variables that occur in this element to the set
	 * if it is a rigid position or rigidOnly is not set.
	 * @param freeSet set of free nonterminals, never null
	 * @param rigidOnly
	 */
	void getFree(Set<NonTerminal> freeSet, boolean rigidOnly) {
		// do nothing by default
	}

	public NonTerminal getRoot() {
		throw new UnsupportedOperationException("need to type check first before calling getRoot()");
	}

	public Term asTerm() {
		if (term == null)
			term = computeTerm(new ArrayList<Pair<String, Term>>());
		return term;
	}
	
	public Term asTerm(List<Pair<String, Term>> varBindings) {
		term = computeTerm(varBindings);
		return term;
	}

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
	 * @deprecated
	 */
	@Deprecated
	public Term adaptTermTo(Term term, Term matchTerm, Substitution sub) {
		return term;
	}
}
