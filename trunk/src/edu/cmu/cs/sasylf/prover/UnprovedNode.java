package edu.cmu.cs.sasylf.prover;

import java.util.List;

public interface UnprovedNode extends ProofNode {
	/** Which rules can be used to derive the judgment represented by this
	 * UnprovedNode?
	 */
	List<Rule> getRulesThatApply(Proof proof);

	/** How deep in the proof tree is this node? */
	int getDepth();

	/** How many choice points are their in the proof tree up to this point?
	 * Similar to getDepth(), but nodes where there is only one choice are excluded.
	 */
	int getChoiceDepth();
}
