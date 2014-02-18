/** Written by Matthew Rodriguez, 2007.
 * A parsenode leaf. Contains a terminal. no children, obviously.
 * It's a container class.
 */

package edu.cmu.cs.sasylf.grammar;

public class TerminalNode implements ParseNode {
	private Terminal data;
	
	/**
	 * @param rule The production rule that this node represents.
	 * @param nodes The symbols that the rule produces.
	 */
	public TerminalNode(Terminal symbol) {
		data = symbol;
	}
	
	/**
	 * @return the terminal symbol this class is holding.
	 */
	public Terminal getTerminal() {
		return data;
	}
	
	/**
	 * returns the hashcode of the symbol.
	 */
	public int hashCode() {
		return data.hashCode();
	}	
	
	/**
	 * true if the other is a TerminalNode and they're holding the same terminal.
	 */
	public boolean equals(Object o) {
		if (!(o instanceof TerminalNode))
			return false;
		TerminalNode tn = (TerminalNode) o;
		return tn.getTerminal().equals(getTerminal());
	}
	
	/**
	 * Prints out the toString of the terminal symbol.
	 */
	public String toString() {
		return data.toString();
	}
}
