/** Written by Matthew Rodriguez, 2008.
 * A class representing a proof node that is proven.
 */

package edu.cmu.cs.sasylf.prover;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import edu.cmu.cs.sasylf.term.Substitution;

public class ProvedNodeImpl implements ProvedNode {

	private List<ProofNode> premises;
	private List<UnprovedNode> unproved;
	private Rule rule;
	private Judgment judgment;
	private Stack<ProvedNode> undoStates;
	private Stack<UnprovedNode> undoNodes;
	private int id;
	private static int idcounter = 0;
	
	/**
	 * @param r The Rule used to prove this node is true.
	 * @param pre The premises of Rule r.
	 */
	public ProvedNodeImpl (Rule r, List<ProofNode> pre) {
		id = idcounter;
		idcounter++;
		
		judgment = r.getResult();
		rule = r;
		premises = pre;
		
		undoStates = new Stack<ProvedNode>();
		undoNodes = new Stack<UnprovedNode>();
		
		//finds the unproved nodes in the premises and maintains a list of them.
		unproved = new LinkedList<UnprovedNode>();
		for(ProofNode pn: premises) {
			if(pn instanceof UnprovedNode) {
				unproved.add((UnprovedNode)pn);
			}
		}
	}

	/**
	 * returns a list of premises
	 */
	public List<ProofNode> getPremises() {
		return premises;
	}
	
	/**
	 * returns the rule
	 */
	public Rule getRule() {
		return rule;
	}

	/**
	 * returns the judgment
	 */
	public Judgment getJudgment() {
		return judgment;
	}

	/**
	 * returns the leftmost unproved node
	 */
	public UnprovedNode getLeftmostUnprovedNode() {
		return unproved.get(0);
	}
	
	/**
	 * returns a list of unproved nodes
	 */
	public List<UnprovedNode> getUnprovedNodes() {
		return unproved;
	}
	
	/**
	 * true if this node has unproved children
	 */
	public boolean hasUnprovedChildren() {
		return unproved.size() > 0;
	}
	
	/**
	 * replaces the unproved node with the proved one
	 */
	public void applyRule(UnprovedNode un, ProvedNode pn) {
		unproved.remove(un);
		premises.remove(un);
		premises.add(pn);	
		
		//updates the undo stacks
		undoStates.push(pn);
		undoNodes.push(un);
	}
	
	/**
	 * undoes the last applyRule this node performed
	 */
	public ProvedNode undoApplyRule() {
		ProvedNode pn = undoStates.pop();
		UnprovedNode un = undoNodes.pop();
		premises.remove(pn);
		premises.add(un);
		unproved.add(un);
		return pn;
	}
	
	/**
	 * prints out this proofnode and its children
	 */
	public String toString() {
		String s = judgment.toString() + "\n";
		for(ProofNode pn: premises) {
			s += pn.toString(1) + "\n";
		}
		return s;
	}

	/**
	 * prints out this proofnode and its children, along with their depths
	 */
	public String toString(int depth) {
		String s = "";
		for(int i = 0; i < depth; i++) {
			s += "\t";
		}
		s += judgment.toString() + "\n";
		for(ProofNode pn: premises) {
			s += pn.toString(depth+1) + "\n";
		}
		return s;
	}

	/**
	 * pretty prints this node and its children
	 */
	public void prettyPrint(Substitution sub) {
		for(ProofNode pn: premises) {
			pn.prettyPrint(sub);
		}
		
		String s = "d" + getId() + ": " + judgment.prettyPrint(sub) + " by rule " + rule.prettyPrint();
		if(premises.size() > 0) {
			s += " on";
			for(ProofNode pn: premises) {
				s += " d" + pn.getId() + ",";
			}
			s = s.substring(0, s.length()-1);
		}
		System.out.println(s);
	}

	/**
	 * returns a unique id for this node
	 */
	public int getId() {
		return idcounter - id;
	}
}
