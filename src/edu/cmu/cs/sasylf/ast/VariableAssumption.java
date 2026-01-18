package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.util.CopyData;
import edu.cmu.cs.sasylf.util.Location;

public class VariableAssumption extends SyntaxAssumption {

	public VariableAssumption(String n, Location l, Element assumes) {
		super(n, l, assumes);
		variable = new Variable(n,l);
	}

	public VariableAssumption(Variable v) {
		super(v.getSymbol(),v.getLocation(),null);
		variable = v;
	}

	@Override
	public Element getElementBase() {
		return variable;
	}

	private Variable variable;
	
	@Override
	public void substitute(SubstitutionData sd) {
		if (sd.didSubstituteFor(this)) return;
		super.substitute(sd);
		sd.setSubstitutedFor(this);
		variable.substitute(sd);
	}

	@Override
	public VariableAssumption copy(CopyData cd) {
		if (cd.containsCopyFor(this)) return (VariableAssumption) cd.getCopyFor(this);
		VariableAssumption clone = (VariableAssumption) super.copy(cd);
		cd.addCopyFor(this, clone);
		clone.variable = clone.variable.copy(cd);
		return clone;
	}
}
