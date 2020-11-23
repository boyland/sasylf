package edu.cmu.cs.sasylf.ast;

import java.util.Collections;
import java.util.List;

import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Span;

public class AndClauseUse extends AndOrClauseUse {

	public AndClauseUse(Location loc, List<Element> elems, ClauseDef cd, List<ClauseUse> clauses) {
		super(loc, elems, cd, clauses);
	}

	public static AndClauseUse makeAndClause(Span sp, Context ctx, List<ClauseUse> parts) {
		return (AndClauseUse)makeEmptyAndClause(sp.getLocation()).create(sp, ctx, parts);
	}

	public static AndClauseUse makeEmptyAndClause(Location loc) {
		ClauseDef cd = (ClauseDef)AndJudgment.makeEmptyAndJudgment(loc).getForm();
		return new AndClauseUse(loc,Collections.<Element>emptyList(),cd,Collections.<ClauseUse>emptyList());
	}
}
