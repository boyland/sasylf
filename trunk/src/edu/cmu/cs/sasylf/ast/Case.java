package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Case extends Node {
	public Case(Location l) { super(l); }

	public List<Derivation> getDerivations() { return derivations; }

	public void prettyPrint(PrintWriter out) {
		for (Derivation d : derivations) {
			d.prettyPrint(out);
		}
		out.println("end case\n");
	}

	public void typecheck(Context ctx, boolean isSubderivation) {
		Map<String, Fact> oldMap = ctx.derivationMap;
		ctx.derivationMap = new HashMap<String, Fact>(oldMap);

		Derivation.typecheck(this, ctx, derivations);
		
		ctx.derivationMap = oldMap;
	}

	// verify: that last derivation is what i.h. requires

	private List<Derivation> derivations = new ArrayList<Derivation>();
}

