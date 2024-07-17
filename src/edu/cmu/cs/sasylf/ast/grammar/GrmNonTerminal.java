package edu.cmu.cs.sasylf.ast.grammar;

import edu.cmu.cs.sasylf.CopyData;
import edu.cmu.cs.sasylf.SubstitutionData;
import edu.cmu.cs.sasylf.grammar.NonTerminal;

public class GrmNonTerminal implements NonTerminal {
	public GrmNonTerminal(String s) { string = s; }

	@Override
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

	public void substitute(SubstitutionData sd) {
		if (sd.didSubstituteFor(this)) return;
		sd.setSubstitutedFor(this);
		
		if (sd.containsSyntaxReplacementForByString(string)) {
			string = sd.to;
		}
	}

	public GrmNonTerminal copy(CopyData cd) {
		if (cd.containsCopyFor(this)) return (GrmNonTerminal) cd.getCopyFor(this);
		GrmNonTerminal clone = new GrmNonTerminal(string);
		cd.addCopyFor(this, clone);
		return clone;
	}

}
