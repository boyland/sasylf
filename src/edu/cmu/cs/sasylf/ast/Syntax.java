package edu.cmu.cs.sasylf.ast;

import java.util.Set;

import edu.cmu.cs.sasylf.util.Location;

/**
 * Definition of syntax.
 * These definitions affect the way SASyLF parses syntax.
 */
public abstract class Syntax extends Node {
	/** Create a syntax node with a single location.
	 * @param l location this syntax starts at
	 */
	public Syntax(Location l) {
		super(l);
	}

	/**
	 * Create a syntax node with a starting and ending location
	 * @param l1 starting location, must not be null
	 * @param l2 ending location, must not be null
	 */
	public Syntax(Location l1, Location l2) {
		super(l1, l2);
	}

	/// Checking (multiple passes)
	// We require multiple passes to check syntax because
	// syntax can be used before it is defined, and can be recursive.
	// The following four methods handle the four passes.
	
	/**
	 * Update the contexts syntax maps so that we know about each declared entity.
	 * @param ctx TODO
	 */
	public void updateContext(Context ctx) {}

	/**
	 * Some last checks before we can type check.
	 * Currently this is needed only to make sure each variable is connected with a particular nonterminal.
	 * @param ctx context, must not be null
	 */
	public void precheck(Context ctx) {}

	/**
	 * Type check the syntax and update the grammar.
	 * @param ctx context, must not be null
	 */
	public abstract void typecheck(Context ctx);

	/**
	 * Checks that require that all syntax has been type checked.  Anything that
	 * needs to parse using the syntax should be done here.
	 * @param ctx context, must not be null
	 */
	public void postcheck(Context ctx) {}

	/**
	 * Get all the terminals used in this syntax definition.
	 * @return a new set of terminals appearing here.
	 */
	public abstract Set<Terminal> getTerminals();

	/**
	 * Compute Subordination relations using this syntax.
	 */
	public void computeSubordination() {}
	
	/**
	 * Check subordination among bindings in syntax
	 * definitions.
	 */
	public void checkSubordination() {}
}