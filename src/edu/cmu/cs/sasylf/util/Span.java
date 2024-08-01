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
	public void substitute(SubstitutionData sd) {
		// The default implementation doesn't do anything. It just marks that it has been substituted.
		if (sd.didSubstituteFor(this)) return;
		sd.setSubstitutedFor(this);
	}

	@Override
	public Span clone() {
		try {
			return (Span) super.clone();
		} catch (CloneNotSupportedException e) {
			ErrorReport report = new ErrorReport(Errors.INTERNAL_ERROR, "Clone not supported in class: " + getClass(), null, null, true);
			throw new SASyLFError(report);
		}
	}

}
