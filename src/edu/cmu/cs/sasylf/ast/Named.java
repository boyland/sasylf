package edu.cmu.cs.sasylf.ast;

/**
 * Declarations that have a name:
 * rule-like, Judgment, Syntax, ModulePart and others.
 * This name should be one declared by the user.
 */
public interface Named {
	/**
	 * Get the identifying name for this declaration.
	 * @return
	 */
	public String getName();
}
