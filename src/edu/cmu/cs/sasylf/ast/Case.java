package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import edu.cmu.cs.sasylf.util.DefaultSpan;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Span;


public class Case extends Node {
	public Case(Location l, Location l1, Location l2) { 
		super(l); 
		span = new DefaultSpan(l1,l2);
	}

	public List<Derivation> getDerivations() { return derivations; }

	public Span getSpan() {
		return span;
	}

	@Override
	public void prettyPrint(PrintWriter out) {
		for (Derivation d : derivations) {
			d.prettyPrint(out);
		}
		out.println("end case\n");
	}

	public void typecheck(Context ctx, Pair<Fact,Integer> isSubderivation) {
		ErrorHandler.recordLastSpan(this);
		Map<String, Fact> oldMap = ctx.derivationMap;
		ctx.derivationMap = new HashMap<String, Fact>(oldMap);

		Derivation.typecheck(this, ctx, derivations);

		ctx.derivationMap = oldMap;
	}

	// verify: that last derivation is what i.h. requires

	private List<Derivation> derivations = new ArrayList<Derivation>();
	private final Span span;
	
	@Override
	public void collectQualNames(Consumer<QualName> consumer) {
		for (Derivation derivation : derivations) {
			derivation.collectQualNames(consumer);
		}
	}
}

