package edu.cmu.cs.sasylf.util;

import edu.cmu.cs.sasylf.CloneData;
import edu.cmu.cs.sasylf.SubstitutionData;

/**
 * A span of text in the input file.
 */
public interface Span extends Cloneable {
	public Location getLocation();
	public Location getEndLocation();
	
	/**
	 * Create a copy of this span, with the given clone data.
	 * @param cd clone data to use
	 * @return a copy of this span
	 */
	public Span copy(CloneData cd);

	/**
	 * Substitute in this span using the given substitution data.
	 * @param sd substitution data to use
	 */
	public void substitute(SubstitutionData sd);
}
