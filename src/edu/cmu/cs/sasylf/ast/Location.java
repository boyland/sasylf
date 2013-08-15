package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.parser.DSLToolkitParser;
import edu.cmu.cs.sasylf.parser.Token;

public class Location {
	public Location(Token t) {
		beginLine = t.beginLine;
		beginColumn = t.beginColumn;
		file = DSLToolkitParser.currentFile;
	}
	public int getLine() {
		return beginLine;
	}
	public int getColumn() {
		return beginColumn;
	}
	private int beginLine, beginColumn /*, endLine, endColumn*/;
	private String file;

	public String toString() {
		return file + ":" + beginLine;
	}
}
