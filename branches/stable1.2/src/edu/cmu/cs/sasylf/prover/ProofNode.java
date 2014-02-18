/** Written by Matthew Rodriguez, 2008.
 * An interface representing a proof node.
 */

package edu.cmu.cs.sasylf.prover;

import edu.cmu.cs.sasylf.term.Substitution;

public interface ProofNode {
	/** The judgment this node is trying to prove */
	Judgment getJudgment();

	/** return string with i tabs before it */
	String toString(int i);
	
	void prettyPrint(Substitution sub);

	int getId();
}
