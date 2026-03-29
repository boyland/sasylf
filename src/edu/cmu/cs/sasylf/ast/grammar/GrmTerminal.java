package edu.cmu.cs.sasylf.ast.grammar;

import edu.cmu.cs.sasylf.ast.Element;
import edu.cmu.cs.sasylf.ast.SubstitutionData;
import edu.cmu.cs.sasylf.grammar.*;
import edu.cmu.cs.sasylf.util.CopyData;
import edu.cmu.cs.sasylf.util.ErrorReport;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.SASyLFError;

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
		
		if (string.equals(sd.getFrom())) {
			string = sd.getTo();
		}

		if (element != null) element.substitute(sd);
	}

	public GrmTerminal copy(CopyData cd) {
		if (cd.containsCopyFor(this)) return (GrmTerminal) cd.getCopyFor(this);
		
		GrmTerminal clone;
		try {
			clone = (GrmTerminal) super.clone();
		}
		catch (CloneNotSupportedException e) {
			ErrorReport report = new ErrorReport(Errors.INTERNAL_ERROR, "Clone not supported in class: " + getClass(), null, null, true);
			throw new SASyLFError(report);
		}

		cd.addCopyFor(this, clone);

		clone.element = clone.element.copy(cd);
		
		return clone;
	}

}
