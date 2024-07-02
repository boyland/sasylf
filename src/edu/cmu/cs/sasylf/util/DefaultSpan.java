package edu.cmu.cs.sasylf.util;

import edu.cmu.cs.sasylf.CloneData;
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

	public DefaultSpan copy(CloneData cd) {
		if (cd.containsCloneFor(this)) return (DefaultSpan) cd.getCloneFor(this);
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

		cd.addCloneFor(this, clone);

		return clone;
	}

	public void substitute(SubstitutionData sd) {
		if (sd.didSubstituteFor(this)) return;
		sd.setSubstitutedFor(this);
	}
}
