package edu.cmu.cs.sasylf.util;

import edu.cmu.cs.sasylf.CopyData;
import edu.cmu.cs.sasylf.SubstitutionData;

/**
 * A span of text in the input file.
 */
public abstract class Span implements Cloneable {
	public abstract Location getLocation();
	public abstract Location getEndLocation();
	
	/**
	 * Create a copy of this span, with the given clone data.
	 * @param cd clone data to use
	 * @return a copy of this span
	 */
	public abstract Span copy(CopyData cd);

	/**
	 * Substitute in this span using the given substitution data.
	 * @param sd substitution data to use
	 */
	public abstract void substitute(SubstitutionData sd);

	@Override
	public Span clone() {
		try {
			return (Span) super.clone();
		} catch (CloneNotSupportedException e) {
			UpdatableErrorReport report = new UpdatableErrorReport(Errors.INTERNAL_ERROR, "Clone not supported in class: " + getClass(), null);
			throw new SASyLFError(report);
		}
	}

}
