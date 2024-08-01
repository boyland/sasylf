package edu.cmu.cs.sasylf.util;

import edu.cmu.cs.sasylf.CopyData;

public class DefaultSpan extends Span {

	public DefaultSpan(Location l) {
		start = end = l;
	}

	public DefaultSpan(Location l1, Location l2) {
		start = l1;
		end = l2;
	}

	@Override
	public Location getLocation() {
		return start;
	}

	@Override
	public Location getEndLocation() {
		return end;
	}

	public void setEndLocation(Location l) {
		end = l;
	}
	
	protected void shiftEndLocation(int amt) {
		end = end.add(amt);
	}

	private Location start, end;

	@Override
	public DefaultSpan copy(CopyData cd) {
		if (cd.containsCopyFor(this)) return (DefaultSpan) cd.getCopyFor(this);
		DefaultSpan clone = (DefaultSpan) super.clone();

		clone.start = clone.start.copy(cd);
		clone.end = clone.end.copy(cd);

		cd.addCopyFor(this, clone);

		return clone;
	}
}
