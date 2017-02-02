package edu.cmu.cs.sasylf.term;

/**
 * Unification failed because we can't figure out whether a unifer exists.
 * This cannot be considered a normal failure.
 * @author boyland
 */
public class UnificationIncomplete extends UnificationFailed {

	/**
	 * Keep Eclipse Happy
	 */
	private static final long serialVersionUID = 1L;

	public UnificationIncomplete(String text, Term t1, Term t2) {
		super(text, t1, t2);
	}
}
