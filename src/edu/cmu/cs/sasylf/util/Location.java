package edu.cmu.cs.sasylf.util;

import edu.cmu.cs.sasylf.parser.DSLToolkitParser;
import edu.cmu.cs.sasylf.parser.Token;

public class Location implements Span {
	public Location(Token t) {
		beginLine = t.beginLine;
		beginColumn = t.beginColumn;
		file = DSLToolkitParser.currentFile;
	}
	public Location(String f, int line, int column) {
		beginLine = line;
		beginColumn = column;
		file = f;
	}
	/**
	 * Return an <em>exclusive</em> endpoint given the token.
	 * In other words, give the location just after the end of this token. 
	 * @param t token to compute end location from (must not be null)
	 * @return location just after end of token.
	 */
	public static Location endOf(Token t) {
		return new Location(DSLToolkitParser.currentFile,t.endLine,t.endColumn+1);
	}

	public int getLine() {
		return beginLine;
	}
	public int getColumn() {
		return beginColumn;
	}

	public String getFile() {
		return file;
	}

	public Location add(int cs) {
		return new Location(file,beginLine,beginColumn+cs);
	}

	private int beginLine, beginColumn /*, endLine, endColumn*/;
	private String file;

	@Override
	public String toString() {
		return file + ":" + beginLine;
	}


	/// as a degenerate case: a location is a zero-length span

	@Override
	public Location getLocation() {
		return this;
	}
	@Override
	public Location getEndLocation() {
		return this;
	}


}
