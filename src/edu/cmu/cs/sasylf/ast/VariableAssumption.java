package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.CloneData;
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
	
	public void substitute(String from, String to) {
		// Do nothing
		// TODO: should we do something?
	}

	@Override
	public Fact copy(CloneData cd) {
		if (cd.containsCloneFor(this)) return (Fact) cd.getCloneFor(this);

		VariableAssumption clone;
		try {
			clone = (VariableAssumption) super.clone();
		} catch (CloneNotSupportedException e) {
			System.out.println("Clone not supported in VariableAssumption");
			System.exit(1);
			return null;
		}

		cd.addCloneFor(this, clone);

		clone.variable = clone.variable.copy(cd);

		return clone;
	}
}
