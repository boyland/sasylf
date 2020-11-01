package edu.cmu.cs.sasylf.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.cmu.cs.sasylf.util.Location;

public class AndClauseUse extends AndOrClauseUse {

	public AndClauseUse(Location loc, List<Element> elems, ClauseDef cd, List<ClauseUse> clauses) {
		super(loc, elems, cd, clauses);
	}

	public static AndClauseUse makeAndClause(Location loc, Context ctx, List<ClauseUse> parts) {
		List<Element> elems = new ArrayList<Element>();
		List<Judgment> judgments = new ArrayList<Judgment>();
		for (ClauseUse u : parts) {
			if (!elems.isEmpty()) elems.add(new AndJudgment.AndTerminal(loc));
			judgments.add((Judgment)u.getConstructor().getType());
			for (Element e : u.getElements()) {
				elems.add(e);
			}
		}
		ClauseDef cd = (ClauseDef)AndJudgment.makeAndJudgment(loc, ctx, judgments).getForm();
		return new AndClauseUse(loc,elems,cd,parts);
	}

	public static AndClauseUse makeEmptyAndClause(Location loc) {
		ClauseDef cd = (ClauseDef)AndJudgment.makeEmptyAndJudgment(loc).getForm();
		return new AndClauseUse(loc,Collections.<Element>emptyList(),cd,Collections.<ClauseUse>emptyList());
	}
}
