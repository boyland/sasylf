package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.term.Constant;

/** Marker type for all types to which a Clause could refer.
 * Only Syntax and Judgment implement this interface.
 */
public interface ClauseType {
	public Constant typeTerm();
}
