package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.util.CopyData;
import edu.cmu.cs.sasylf.util.Location;



public class DerivationByAssumption extends Derivation {
	public DerivationByAssumption(String n, Location l, Clause c) {
		super(n,l,c);
	}

	// verify: error if this is actually part of a derivation--it's for foralls only!

	@Override
	public DerivationByAssumption copy(CopyData cd) {
		if (cd.containsCopyFor(this)) return (DerivationByAssumption) cd.getCopyFor(this);
		DerivationByAssumption clone = (DerivationByAssumption) super.copy(cd);
		cd.addCopyFor(this, clone);
		return clone;
	}

}
