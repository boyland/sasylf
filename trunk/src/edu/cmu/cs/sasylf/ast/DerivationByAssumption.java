package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.util.Location;



public class DerivationByAssumption extends Derivation {
	public DerivationByAssumption(String n, Location l, Clause c) {
		super(n,l,c);
	}

	// verify: error if this is actually part of a derivation--it's for foralls only!
}
