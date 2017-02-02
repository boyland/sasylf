package org.sasylf.util;

import java.util.Comparator;

import org.eclipse.jface.text.Position;

/**
 * Comparator for positions.  Ordering is by offset and then by length.
 * Hence the results are not very useful for overlapping positions.
 */
public class PositionComparator implements Comparator<Position> {

	public PositionComparator() { }

	@Override
	public int compare(Position arg0, Position arg1) {
		int diffOffset = arg0.getOffset() - arg1.getOffset();
		if (diffOffset != 0) return diffOffset;
		int diffLength = arg0.getLength() - arg1.getLength();
		return diffLength;
	}

	static volatile PositionComparator instance;

	public static PositionComparator getDefault() {
		if (instance == null) {
			synchronized (PositionComparator.class) {
				if (instance == null) {
					instance = new PositionComparator(); 
				}
			}
		}
		return instance;
	}
}
