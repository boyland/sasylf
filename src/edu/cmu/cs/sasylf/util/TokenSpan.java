package edu.cmu.cs.sasylf.util;

import edu.cmu.cs.sasylf.CopyData;
import edu.cmu.cs.sasylf.SubstitutionData;
import edu.cmu.cs.sasylf.parser.Token;

/**
 * The span of a single token.
 */
public class TokenSpan extends Span {
	private Location starting;
	private Location ending;
	
	public TokenSpan(Token t) {
		starting = new Location(t);
		ending = Location.endOf(t);
	}
	
	@Override
	public Location getLocation() {
		return starting;
	}

	@Override
	public Location getEndLocation() {
		return ending;
	}

	@Override
	public Span copy(CopyData cd) {
		if (cd.containsCopyFor(this)) return (TokenSpan) cd.getCopyFor(this);

		TokenSpan clone = (TokenSpan) super.clone();

		clone.starting = clone.starting.copy(cd);
		clone.ending = clone.ending.copy(cd);

		cd.addCopyFor(this, clone);

		return clone;
	}

	@Override
	public void substitute(SubstitutionData sd) {
		if (sd.didSubstituteFor(this)) return;
		sd.setSubstitutedFor(this);
	}

}
