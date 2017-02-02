/** Written by Matthew Rodriguez, 2008.
 * implements a rule for use in proving.
 */

package edu.cmu.cs.sasylf.prover;

import java.util.List;

import edu.cmu.cs.sasylf.term.Substitution;

public class RuleInstance implements Rule {
	private Judgment result;
	private List<Judgment> preconditions;
	private Substitution sub;
	private edu.cmu.cs.sasylf.ast.Rule ruleUsed;

	public RuleInstance(Judgment result, List<Judgment> preconditions, Substitution sub, edu.cmu.cs.sasylf.ast.Rule ruleUsed) {
		this.result = result;
		this.preconditions = preconditions;
		this.sub = sub;
		this.ruleUsed = ruleUsed;
	}

	@Override
	public List<Judgment> getPreconditions() {
		return preconditions;
	}

	@Override
	public Substitution getSubstitution() {
		return sub;
	}

	@Override
	public Judgment getResult() {
		return result;
	}

	@Override
	public boolean hasPreconditions() {
		return preconditions.size() > 0;
	}

	@Override
	public String prettyPrint() {
		return ruleUsed.getName();
	}

}
