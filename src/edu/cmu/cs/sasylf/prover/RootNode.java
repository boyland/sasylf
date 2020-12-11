/** Written by Matthew Rodriguez, 2008.
 * A dummy node for the root. Makes some implementation easier.
 */

package edu.cmu.cs.sasylf.prover;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import edu.cmu.cs.sasylf.term.Substitution;

public class RootNode implements ProvedNode {

	private ProofNode child;
	private boolean unproved;
	private UnprovedNode undo;

	/** Creates a new RootNode with its child as the given node.
	 * @param node
	 */
	public RootNode(UnprovedNode node) {
		child = node;
		unproved = true;
	}

	/**
	 * replaces the unproved node with a proved one
	 */
	@Override
	public void applyRule(UnprovedNode un, ProvedNode pn) {
		if(unproved) {
			child = pn;
			unproved = false;
			undo = un;
		}
	}

	/**
	 * returns this node to the state it was in before last applyRule
	 */
	@Override
	public ProvedNode undoApplyRule() {
		if(!unproved) {
			ProvedNode pn = (ProvedNode)child;
			child = undo;
			unproved = true;
			undo = null;
			return pn;
		}
		return null;
	}

	/**
	 * return this node's child, if it is indeed unproved
	 */
	@Override
	public UnprovedNode getLeftmostUnprovedNode() {
		if(child instanceof UnprovedNode)
			return (UnprovedNode) child;
		else
			return null;
	}

	/**
	 * Returns a list of this node's premises
	 */
	@Override
	public List<ProofNode> getPremises() {
		List<ProofNode> result = new LinkedList<ProofNode>();
		result.add(child);
		return result;
	}

	/**
	 * returns this node's rule
	 */
	@Override
	public Rule getRule() {
		if(unproved) {
			return null;
		} else {
			return ((ProvedNode)child).getRule();
		}
	}

	/**
	 * returns a list of this node's unproved children
	 */
	@Override
	public List<UnprovedNode> getUnprovedNodes() {
		List<UnprovedNode> result = new LinkedList<UnprovedNode>();
		result.add((UnprovedNode)child);
		return result;
	}

	/**
	 * returns whether this list's child is unproved
	 */
	@Override
	public boolean hasUnprovedChildren() {
		return unproved;
	}

	/**
	 * null. this is a dummy node.
	 */
	@Override
	public Judgment getJudgment() {
		return null;
	}

	/**
	 * prints this node's child.
	 */
	@Override
	public String toString() {
		return child.toString();
	}

	/**
	 * prints this node's child, and its depth.
	 */
	@Override
	public String toString(int depth) {
		return child.toString(depth+1);
	}

	/**
	 * true if unproved.
	 */
	public boolean isUnproved() {
		return unproved;
	}

	/**
	 * returns zero. the root's id is always zero.
	 */
	@Override
	public int getId() {
		return 0;
	}

	/**
	 * pretty prints the child.
	 */
	@Override
	public void prettyPrint(Substitution sub, PrintWriter out) {
		child.prettyPrint(sub, out);
	}
}
