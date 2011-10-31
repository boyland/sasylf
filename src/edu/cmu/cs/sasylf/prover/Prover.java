/** Written by Matthew Rodriguez, 2008.
 * A class that proves something
 */

package edu.cmu.cs.sasylf.prover;

public class Prover {
	/** Returns a correct proof of the property encoded by partial, or null if no such proof can be found
	 * within the given search bound.
	 * 
	 * @param partial
	 * @param bound
	 * @return
	 */
	public Proof prove(Proof partial, int bound) {
		//Gets the leftmost unproved node and its parent
		ProvedNode leftmostParent = partial.getLeftmostUnprovedNodeParent();
		UnprovedNode leftmost = leftmostParent.getLeftmostUnprovedNode();
		
		//If we've exceeded the depth bound, return
		if(leftmost.getDepth() > bound) {
			return null;
		}

		//Try each rule
	    for(Rule r: leftmost.getRulesThatApply(partial)) {
	    	//Apply the rule.
	        partial.applyRule(leftmostParent, leftmost, r);
	        
	        //If we're done, return.
	        if(partial.isCompleteProof()) {
	        	return partial;
	        }
	        
	        //Otherwise, continue trying to prove this.
	        Proof result = prove(partial, bound);
			if (result != null) {
				return result;
			}
			
			//If the search failed, undo and try something else.
			partial.undoApplyRule();
	    }
	    return null;
	}
}
