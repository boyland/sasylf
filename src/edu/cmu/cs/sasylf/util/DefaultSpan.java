package edu.cmu.cs.sasylf.util;

public class DefaultSpan implements Span {

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
}
