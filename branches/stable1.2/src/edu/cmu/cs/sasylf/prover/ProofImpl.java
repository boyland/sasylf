/** Written by Matthew Rodriguez, 2008.
 * A class representing a proof.
 */

package edu.cmu.cs.sasylf.prover;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;

public class ProofImpl implements Proof {
	
	private RootNode root;
	private List<ProvedNode> unproved; //A list of nodes with unproved children
	private Substitution substitution;
	private Set<FreeVar> inputVars = new HashSet<FreeVar>();
	private Stack<Substitution> undoSubs;
	private Stack<ProvedNode> undoStates;
	
	/** Constructs a new empty Proof.
	 * @param j The Judgment to be proved.
	 */
	public ProofImpl(Judgment j) {
		unproved = new LinkedList<ProvedNode>();
		root = new RootNode(new UnprovedNodeImpl(j, 0, 0));
		unproved.add(root);
		substitution = new Substitution();
		undoSubs = new Stack<Substitution>();
		undoStates = new Stack<ProvedNode>();
	}

	/** Applies a rule to a node.
	 * @param rule the rule to apply.
	 * @param node the node to be replaced.
	 * @param pn its parent.
	 */
	public void applyRule(ProvedNode pn, UnprovedNode node, Rule rule) {
		//Creates a list of UnprovedNodes as children for the new node
		List<ProofNode> children = new LinkedList<ProofNode>();
		for(Judgment j: rule.getPreconditions()) {
			children.add(new UnprovedNodeImpl(j, node.getDepth()+1, node.getChoiceDepth()+1));
		}
		
		//Creates the new node
		ProvedNode newNode = new ProvedNodeImpl(rule, children);
		
		//If there are one or more children, 
		//add this node to the list of nodes with unproved children
		if(children.size()>0) {
			unproved.add(newNode);
		}
		
		//Update the parent node with a link to the new node.
		pn.applyRule(node, newNode);
		
		//If the parent node has no unproved children left, 
		//remove it from the list of nodes with unproved children.
		if(!pn.hasUnprovedChildren()) {
			unproved.remove(pn);
		}
		
		//update the undo stack
		undoStates.push(pn);
		
		//update the substitution and push the old one onto the undo stack
		Substitution newSubstitution = new Substitution(substitution);
		undoSubs.push(substitution);
		newSubstitution.compose(rule.getSubstitution()); // modifies newProof.substitution in place
		substitution = newSubstitution;
	}
	
	/**
	 * Undo the last applyRule() action. Returns to exactly the state it was in before.
	 */
	public void undoApplyRule() {
		//Pop the last substitution and use it instead.
		substitution = undoSubs.pop();
		
		//Get the last provednode we applied a rule on
		ProvedNode pn = undoStates.pop();
		
		//If we removed it from the list of nodes with unproved children, put it back on
		if(!unproved.contains(pn)) {
			unproved.add(pn);
		}
		
		//Fix the state of the node itself
		ProvedNode un = pn.undoApplyRule();
		
		//Remove the node that we're about to delete 
		//from the list of nodes with unproved children
		unproved.remove(un);
	}

	/**
	 * return the root node we're trying to prove.
	 */
	public ProofNode getGoal() {
		return root;
	}

	/**
	 * returns the leftmost unproved node's parent.
	 */
	public ProvedNode getLeftmostUnprovedNodeParent() {
		return unproved.get(0);
	}

	/**
	 * returns a list of nodes with unproved children,
	 */
	public List<ProvedNode> getUnprovedNodes() {
		return unproved;
	}

	/**
	 * returns true if this proof is complete.
	 */
	public boolean isCompleteProof() {
		return unproved.size() == 0;
	}

	/**
	 * returns the substitution this proof is using.
	 */
	public Substitution getSubstitution() {
		return substitution;
	}
	
	public Set<FreeVar> getInputVars() {
		return inputVars;
	}
	
	/**
	 * prints this proof.
	 */
	public String toString() {
		return root.toString() + "\n\tsub: " + substitution;
	}

	/**
	 * pretty prints this proof.
	 */
	public void prettyPrint() {
		root.prettyPrint(substitution);
	}
}
