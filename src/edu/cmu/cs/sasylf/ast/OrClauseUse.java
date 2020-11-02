package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.cmu.cs.sasylf.util.Location;

public class OrClauseUse extends AndOrClauseUse {

	public OrClauseUse(Location loc, List<Element> elems, ClauseDef cd, List<ClauseUse> clauses) {
		super(loc, elems, cd, clauses);
	}

	public static OrClauseUse makeOrClause(Location loc, Context ctx, List<ClauseUse> parts) {
		List<Element> elems = new ArrayList<Element>();
		List<Judgment> judgments = new ArrayList<Judgment>();
		for (ClauseUse u : parts) {
			if (!elems.isEmpty()) elems.add(new OrJudgment.OrTerminal(loc));
			judgments.add((Judgment)u.getConstructor().getType());
			for (Element e : u.getElements()) {
				elems.add(e);
			}
		}
		ClauseDef cd = (ClauseDef)OrJudgment.makeOrJudgment(loc, ctx, judgments).getForm();
		return new OrClauseUse(loc,elems,cd,parts);
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
