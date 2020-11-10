package edu.cmu.cs.sasylf.ast;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Util;


public class DerivationByPrevious extends DerivationWithArgs {
	public DerivationByPrevious(String n, Location l, Clause c) {
		super(n,l,c);
	}
	@Override
	public String prettyPrintByClause() {
		return " by ";
	}


	// verify: that this is the same as the previous one, except with any new subs
	@Override
	public void typecheck(Context ctx) {
		super.typecheck(ctx);

		Clause cl = this.getClause();

		List<ClauseUse> sourceClauses = new ArrayList<ClauseUse>();
		for (Fact f : this.getArgs()) {
			if (f instanceof Derivation) {
				sourceClauses.add((ClauseUse)f.getElement());
				continue;
			}
			ErrorHandler.report("can only conjoin derivations, not syntax: " + f.getName(),this);
		}
		Util.verify(sourceClauses.size() > 0, "should have at least one");
		ClauseUse source = sourceClauses.get(0);
		if (sourceClauses.size() > 1) {
			Util.debug("Making and clause for " + sourceClauses);
			source = AndClauseUse.makeAndClause(source.getLocation(), ctx, sourceClauses);
		}
		/*if (this.getArgStrings().size() > 1) {
			if (!(cl instanceof AndClauseUse)) {
				ErrorHandler.report("Claimed fact is not a conjunction, remove extra arguments", this);
			}
			List<ClauseUse> results = ((AndClauseUse)cl).getClauses();
			int n = results.size();
			if (n != getArgStrings().size()) {
				ErrorHandler.report("Wrong number of facts for conjuction, expected " + n, this);
			}
			for (int i=0; i < n; ++i) {
				ClauseUse source = sourceClauses.get(i);
				ClauseUse result = results.get(i);
				Derivation.checkMatchWithImplicitCoercions(this,ctx,result,source,"Claimed conjunct #" + (i+1) + ": " + result + " is not equivalent to previous: " + source);
			}
			return;
		}*/

		Derivation.checkMatchWithImplicitCoercions(this, ctx, cl, source, "Claimed " + cl + " not justified by " + source);
		Pair<Fact,Integer> p = ctx.subderivations.get(getArgs().get(0));
		if (p != null) ctx.subderivations.put(this, p);
	}
}