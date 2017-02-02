package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.term.Constant;

/** Marker type for all types to which a Clause could refer.
 * Only Syntax and Judgment implement this interface.
 */
public interface ClauseType extends ElementType {
	public Constant typeTerm();

	/**
	 * Return whether this type has its inhabitants defined here.
	 * Case analysis is not permitted on things of abstract 
	 * @return whether this judgment or syntax is abstract.
	 */
	public boolean isAbstract();
}
