/** Written by Matthew Rodriguez, 2007.
 * An interface representing an rule in a grammar.
 */

package edu.cmu.cs.sasylf.grammar;

import java.util.List;

public interface Rule {
	//public void setLeftSide(NonTerminal s);
	//public void setRightSide(List<Symbol> s);
	public NonTerminal getLeftSide();
	public List<Symbol> getRightSide();
}
