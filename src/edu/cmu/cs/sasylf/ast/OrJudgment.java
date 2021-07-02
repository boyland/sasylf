package edu.cmu.cs.sasylf.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Span;

public class OrJudgment extends AndOrJudgment {
	private static Terminal makeOrTerminal(Span loc) {
		return new OpTerminal("or", loc);
	}

	@Override
	public Terminal makeSeparator(Span l) {
		return makeOrTerminal(l);
	}

	public static void addOr(Clause cl, Location or, Clause more) {
		cl.getElements().add(makeOrTerminal(or));
		cl.getElements().addAll(more.getElements());
	}

	private OrJudgment(Location l, List<Judgment> parts, List<ClauseUse> uses) {
		super("or",l,parts,uses);
	}

	@Override
	protected OrClauseUse makeClauseUse(Location loc, List<Element> elems,
			ClauseDef cd, List<ClauseUse> clauses) {
		return new OrClauseUse(loc,elems,cd,clauses);
	}
	
	@Override
	protected void setRules(Location l, String name, List<ClauseUse> premises,
			Clause result) {
		int i=1;
		for (ClauseUse premise : premises) {
			ArrayList<Clause> premiseList = new ArrayList<Clause>(1);
			premiseList.add(premise);
			Rule rule = new Rule(l,name+"#"+i,premiseList,result);
			++i;
			super.getRules().add(rule);
		}
	}


	private static Map<List<Judgment>,OrJudgment> cache = new HashMap<List<Judgment>,OrJudgment>();

	/**
	 * Generate an "or" judgment for 'or'ing a series of judgments together.
	 * @param loc location where generation was (first) required
	 * @param parts list of judgments 
	 * @return judgment that is the disjunction of the parts
	 */
	public static OrJudgment makeOrJudgment(Location loc, Context ctx, List <Judgment> parts, List<ClauseUse> uses) {
		OrJudgment result = cache.get(parts);
		if (result == null) {
			parts = new ArrayList<Judgment>(parts); // defensive programming
			result = new OrJudgment(loc,parts,uses);
			result.defineConstructor(ctx);
			result.typecheck(ctx);
			cache.put(parts,result);
		}
		return result;
	}

	public static OrJudgment makeEmptyOrJudgment(Location loc) {
		List<Judgment> empty = Collections.<Judgment>emptyList();
		OrJudgment result = cache.get(empty);
		if (result == null) {
			result = new OrJudgment(loc,empty,Collections.emptyList());
			cache.put(empty, result);
		}
		return result;
	}
		
	/**
	 * Get the special term "or[]" to represent the empty or clause,
	 * and thus "false" in the logic.
	 * @return constant for or[]
	 */
	public static Constant getContradictionConstant() {
		Judgment contra = makeEmptyOrJudgment(new Location("",0,0));
		return (Constant)contra.getForm().asTerm();
	}
}
