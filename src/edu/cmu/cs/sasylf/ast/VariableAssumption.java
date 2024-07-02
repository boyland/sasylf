package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.CloneData;
import edu.cmu.cs.sasylf.SubstitutionData;
import edu.cmu.cs.sasylf.term.Substitution;
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
	
	public void substitute(SubstitutionData sd) {
		// Do nothing
		// TODO: should we do something?
		super.substitute(sd);
		if (sd.didSubstituteFor(this)) return;
		sd.setSubstitutedFor(this);
		variable.substitute(sd);
	}

	@Override
	public VariableAssumption copy(CloneData cd) {
		if (cd.containsCloneFor(this)) return (VariableAssumption) cd.getCloneFor(this);

		VariableAssumption clone = (VariableAssumption) super.copy(cd);

		cd.addCloneFor(this, clone);

		clone.variable = clone.variable.copy(cd);
		
		return clone;
	}
}
