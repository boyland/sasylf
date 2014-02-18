/** Written by Matthew Rodriguez, 2007.
 * A node in a parse tree. Not a leaf. Contains a rule, and a list of
 * child nodes representing the symbols in that rule.
 */

package edu.cmu.cs.sasylf.grammar;

import java.util.List;

public class RuleNode implements ParseNode {
	private Rule data;
	private List<ParseNode> children;
	
	/**
	 * @param rule The production rule that this node represents.
	 * @param nodes The symbols that the rule produces.
	 */
	public RuleNode(Rule rule, List<ParseNode> nodes) {
		data = rule;
		children = nodes;
	}
	
	/**
	 * @return the rule this node is based on
	 */
	public Rule getRule() {
		return data;
	}
	
	/**
	 * @return the list of children of this node
	 */
	public List<ParseNode> getChildren() {
		return children;
	}

	/**
	 * True if the other object is a RuleNode, the rule is the same, and the list of 
	 * children is the same
	 */
	public boolean equals(Object o) {
		if (!(o instanceof RuleNode))
			return false;
		RuleNode rn = (RuleNode) o;
		return rn.getRule().equals(getRule()) && rn.getChildren().equals(getChildren());
	}
	
	/**
	 * hashes based on rule.hash + list.hash()
	 */
	public int hashCode() {
		return data.hashCode() + children.hashCode();
	}	
	
	/**
	 * Prints the list of children
	 */
	public String toString() {
		String s = "(";
		for(ParseNode n: children) {
			s += n;
		}
		s += ")";
		return s;
	}
	
	/**
	 * Prints the left side of the rule.
	 */
	public String toStringAsNonTerminal() {
		return data.getLeftSide().toString();
	}
}
