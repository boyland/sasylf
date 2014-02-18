package edu.cmu.cs.sasylf.ast.grammar;

import edu.cmu.cs.sasylf.grammar.NonTerminal;

public class GrmNonTerminal implements NonTerminal {
	public GrmNonTerminal(String s) { string = s; }
	
	public String toString() { return string; }

	private String string;

	@Override
	public int hashCode() {
		return string.hashCode();
	}

	@Override
	public boolean equals(Object s) {
		return s instanceof GrmNonTerminal && string.equals(((GrmNonTerminal)s).string);
	}

}
