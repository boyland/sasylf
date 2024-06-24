package edu.cmu.cs.sasylf.ast.grammar;

import edu.cmu.cs.sasylf.CloneData;
import edu.cmu.cs.sasylf.SubstitutionData;
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

	public void substitute(SubstitutionData sd) {
		if (sd.didSubstituteFor(this)) return;
		sd.setSubstitutedFor(this);
		
		if (string.equals(sd.from)) {
			string = sd.to;
		}

		if (element != null) element.substitute(sd);
	}

	public GrmTerminal copy(CloneData cd) {
		if (cd.containsCloneFor(this)) return (GrmTerminal) cd.getCloneFor(this);
		
		GrmTerminal clone;
		try {
			clone = (GrmTerminal) super.clone();
		}
		catch (CloneNotSupportedException e) {
			System.out.println("CloneNotSupportedException in GrmTerminal");
			System.exit(1);
			return null;
		}

		cd.addCloneFor(this, clone);

		clone.element = clone.element.copy(cd);
		
		return clone;
	}

}
