package edu.cmu.cs.sasylf.ast.grammar;

import edu.cmu.cs.sasylf.ast.Element;
import edu.cmu.cs.sasylf.grammar.*;

public class GrmTerminal implements Terminal {
	public GrmTerminal(String s, Element e) { string = s; element = e; }
	
	@Override
	public String toString() { return string; }

	private String string;
	private Element element;
	
	@Override
	public boolean equals(Object s) {
		return s instanceof GrmTerminal && string.equals(((GrmTerminal)s).string);
	}

	@Override
	public int hashCode() {
		return string.hashCode();
	}

	public Element getElement() {
		return element;
	}

}
