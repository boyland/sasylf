package edu.cmu.cs.sasylf.ast;

import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.util.Location;

public abstract class AndOrClauseUse extends ClauseUse {

	public AndOrClauseUse(Location loc, List<Element> elems, ClauseDef cd, List<ClauseUse> clauses) {
		super(loc, elems, cd);
		this.clauses = clauses;
	}

	public List<ClauseUse> getClauses() { return clauses; }

	private List<ClauseUse> clauses;


	@Override
	void checkVariables(Set<String> bound, boolean defining) {
		for (ClauseUse u: clauses) {
			u.checkVariables(bound, defining);
		}
	}
}
