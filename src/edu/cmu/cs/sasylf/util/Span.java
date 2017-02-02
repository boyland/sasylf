package edu.cmu.cs.sasylf.util;

/**
 * A span of text in the input file.
 */
public interface Span {
	public Location getLocation();
	public Location getEndLocation();
}
