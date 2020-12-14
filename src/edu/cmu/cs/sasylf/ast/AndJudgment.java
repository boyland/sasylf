package edu.cmu.cs.sasylf.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Span;

public class AndJudgment extends AndOrJudgment {
	private static Terminal makeAndTerminal(Span loc) {
		return new AndOrJudgment.OpTerminal("and", loc);
	}

	@Override
	public Terminal makeSeparator(Span l) {
		return makeAndTerminal(l);
	}

	public static void addAnd(Clause cl, Span and, Clause more) {
		cl.getElements().add(makeAndTerminal(and));
		cl.getElements().addAll(more.getElements());
	}

	private AndJudgment(Location l, List<Judgment> parts, List<ClauseUse> uses) {
		super("and",l,parts,uses);
	}

	@Override
	protected AndClauseUse makeClauseUse(Location loc, List<Element> elems,
			ClauseDef cd, List<ClauseUse> clauses) {
		return new AndClauseUse(loc,elems,cd,clauses);
	}

	@Override
	protected void setRules(Location l, String name, List<ClauseUse> premises,
			Clause result) {
		super.getRules().add(new Rule(l,name,new ArrayList<>(premises),result));
	}

	private static Map<List<Judgment>,AndJudgment> cache = new HashMap<List<Judgment>,AndJudgment>();

	/**
	 * Generate an "and" judgment for conjoining a series of judgments together.
	 * @param loc location where generation was (first) required
	 * @param parts list of judgments 
	 * @return judgment that is the conjunction of the parts
	 */
	public static AndJudgment makeAndJudgment(Location loc, Context ctx, List <Judgment> parts, List<ClauseUse> uses) {
		AndJudgment result = cache.get(parts);
		if (result == null) {
			parts = new ArrayList<Judgment>(parts); // defensive programming
			result = new AndJudgment(loc,parts,uses);
			result.defineConstructor(ctx);
			result.typecheck(ctx);
			cache.put(parts,result);
		}
		return result;
	}

	public static AndJudgment makeEmptyAndJudgment(Location loc) {
		List<Judgment> empty = Collections.<Judgment>emptyList();
		AndJudgment result = cache.get(empty);
		if (result == null) {
			result = new AndJudgment(loc,empty,Collections.emptyList());
			cache.put(empty, result);
		}
		return result;
	}

}
