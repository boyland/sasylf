package edu.cmu.cs.sasylf.util;

import edu.cmu.cs.sasylf.CloneData;
import edu.cmu.cs.sasylf.SubstitutionData;
import edu.cmu.cs.sasylf.parser.Token;
import edu.cmu.cs.sasylf.term.Substitution;

/**
 * The span of a single token.
 */
public class TokenSpan implements Span {
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
	public Span copy(CloneData cd) {
		if (cd.containsCloneFor(this)) return (TokenSpan) cd.getCloneFor(this);

		TokenSpan clone;

		try {
			clone = (TokenSpan) super.clone();
		} catch (CloneNotSupportedException e) {
			System.out.println("CloneNotSupportedException in TokenSpan");
			System.exit(1);
			return null;
		}

		clone.starting = clone.starting.copy(cd);
		clone.ending = clone.ending.copy(cd);

		cd.addCloneFor(this, clone);

		return clone;
	}

	public void substitute(SubstitutionData sd) {
		if (sd.didSubstituteFor(this)) return;
		sd.setSubstitutedFor(this);
	}

}
