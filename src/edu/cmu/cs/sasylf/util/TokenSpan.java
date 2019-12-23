package edu.cmu.cs.sasylf.util;

import edu.cmu.cs.sasylf.parser.Token;

/**
 * The span of a single token.
 */
public class TokenSpan implements Span {
	private final Location starting;
	private final Location ending;
	
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

}
