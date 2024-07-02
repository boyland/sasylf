package edu.cmu.cs.sasylf.ast.grammar;

import edu.cmu.cs.sasylf.CloneData;
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
		
		if (sd.containsSyntaxReplacementFor(string)) {
			string = sd.to;
		}
	}

	public GrmNonTerminal copy(CloneData cd) {
		if (cd.containsCloneFor(this)) return (GrmNonTerminal) cd.getCloneFor(this);
		GrmNonTerminal clone = new GrmNonTerminal(string);
		cd.addCloneFor(this, clone);
		return clone;
	}

}
