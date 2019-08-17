package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.term.Term;

/**
 * The type for an Element.
 * Not to be confused with {@link ElemType}
 * which (unfortunately) doesn't handle all elements.
 */
public interface ElementType {
	public String getName();
	
	/**
	 * Return this type as LF.
	 * Throws an exception if called on a terminal.
	 * @return LF type.
	 */
	public Term typeTerm();
}
