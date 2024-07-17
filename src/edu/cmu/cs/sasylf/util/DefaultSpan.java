package edu.cmu.cs.sasylf.util;

import edu.cmu.cs.sasylf.CopyData;
import edu.cmu.cs.sasylf.SubstitutionData;

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

	@Override
	public DefaultSpan copy(CopyData cd) {
		if (cd.containsCopyFor(this)) return (DefaultSpan) cd.getCopyFor(this);
		DefaultSpan clone;
		try {
			clone = (DefaultSpan) super.clone();
		} catch (CloneNotSupportedException e) {
			System.out.println("CloneNotSupportedException in DefaultSpan");
			System.exit(1);
			return null;
		}

		clone.start = clone.start.copy(cd);
		clone.end = clone.end.copy(cd);

		cd.addCopyFor(this, clone);

		return clone;
	}

	@Override
	public void substitute(SubstitutionData sd) {
		if (sd.didSubstituteFor(this)) return;
		sd.setSubstitutedFor(this);
	}
}
