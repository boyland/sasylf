package edu.cmu.cs.sasylf.prover;

import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.term.FreeVar;

public interface Proof {
	
	Set<FreeVar> getInputVars();
	
	/** Gets all the nodes with unproved children in this 
	 * Proof, in left-to-right order.
	 */
	List<ProvedNode> getUnprovedNodes();
	
	/** Gets the leftmost node with unproved children, in the 
	 * ordering defined by the ordering of premises of rules 
	 * used so far in this Proof.
	 */
	ProvedNode getLeftmostUnprovedNodeParent();
	
	/** What are we trying to prove? */
	ProofNode getGoal();

	/** Is the prove complete?  This is true if and only if
	 * getUnprovedNodes() returns an empty set.
	 */
	boolean isCompleteProof();
	
	/** Applies rule to node, resulting in a new Proof object.
	 * Note that node must be one of the nodes returned by
	 * getUnprovedNodes on this Proof object.  The old Proof
	 * object is unchanged, so it can be used again (e.g. to
	 * try applying a different rule to this node, or trying
	 * another node first.
	 */
	void applyRule(ProvedNode pn, UnprovedNode un, Rule rule);
	/* applyRule is implemented by unifying the conclusion of rule
	 * with node.getJudgment.  We first freshify all the existential
	 * variables in rule, and we apply the current substitution to
	 * node.getJudgment. 
	 * 
	 * We check that the unification did not
	 * instantiate any universally quantified variables.  We extend
	 * the current substitution with any newly instantiated
	 * existential variables.  We create a new Proof object that
	 * has node replaced with a ProvedNode, the extended substitution,
	 * and new UnprovedNodes for the premises of rule.
	 */
	
	void undoApplyRule();
	
	void prettyPrint();
	
	/*Judgment getGoal();
	Rule getRule();
	List<Proof> getPremises();*/
}
