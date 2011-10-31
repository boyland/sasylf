package edu.cmu.cs.sasylf.ast;

import java.util.*;
import java.io.*;

import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.util.ErrorHandler;


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

		for (Derivation d : derivations) {
			d.typecheck(ctx);
		}

		Theorem.verifyLastDerivation(ctx, ctx.currentGoal, ctx.currentGoalClause, derivations, this);
		
		ctx.derivationMap = oldMap;
	}

	// verify: that last derivation is what i.h. requires

	private List<Derivation> derivations = new ArrayList<Derivation>();
}

