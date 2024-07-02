package edu.cmu.cs.sasylf.util;

import edu.cmu.cs.sasylf.CloneData;
import edu.cmu.cs.sasylf.SubstitutionData;

/**
 * A span of text in the input file.
 */
public interface Span extends Cloneable {
	public Location getLocation();
	public Location getEndLocation();
	public Span copy(CloneData cd);
	public void substitute(SubstitutionData sd);
}
