package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

import edu.cmu.cs.sasylf.util.Location;

public class OrClauseUse extends AndOrClauseUse {

	public OrClauseUse(Location loc, List<Element> elems, ClauseDef cd, List<ClauseUse> clauses) {
		super(loc, elems, cd, clauses);
	}

	public static OrClauseUse makeOrClause(Location loc, Context ctx, List<ClauseUse> parts) {
		return (OrClauseUse)makeEmptyOrClause(loc).create(loc, ctx, parts);
	}

	public static OrClauseUse makeEmptyOrClause(Location loc) {
		ClauseDef cd = (ClauseDef)OrJudgment.makeEmptyOrJudgment(loc).getForm();
		return new OrClauseUse(loc,Collections.<Element>emptyList(),cd,Collections.<ClauseUse>emptyList());
	}

	@Override
	public void prettyPrint(PrintWriter out, PrintContext ctx) {
		if (getClauses().isEmpty()) {
			out.print("contradiction");
		} else {
			super.prettyPrint(out, ctx);
		}
	}
}
