/** Written by Matthew Rodriguez, 2008.
 * An interface representing a proof node that is proven.
 */

package edu.cmu.cs.sasylf.prover;

import java.util.List;

/** Allows us to traverse a proof and verify the reasoning therein.
 */
public interface ProvedNode extends ProofNode {
	Rule getRule();
	List<ProofNode> getPremises();
	UnprovedNode getLeftmostUnprovedNode();
	boolean hasUnprovedChildren();
	void applyRule(UnprovedNode un, ProvedNode pn);
	ProvedNode undoApplyRule();
	List<UnprovedNode> getUnprovedNodes();
}
