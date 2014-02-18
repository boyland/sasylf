/** Written by Matthew Rodriguez, 2007.
 * An exception that is thrown when parsing and there is more than one way to parse 
 * a sentence. Contains either a conflict object that it encountered while parsing,
 * or the multiple parse trees it generated as potential parsings.
 */

package edu.cmu.cs.sasylf.grammar;

import java.util.Set;

public class AmbiguousSentenceException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -716076963796923796L;
	
	private Conflict conflict;
	private Set<RuleNode> parseTrees;

	/** An exception thrown when an Automaton encounters a conflict.
	 * @param trees The (possibly incomplete) set of possible parse trees
	 */
	public AmbiguousSentenceException(Set<RuleNode> trees) {
		this.parseTrees = trees;
	}
	
	/** An exception thrown when an Automaton encounters a conflict.
	 * @param c The conflict in question.
	 */
	public AmbiguousSentenceException(Conflict c) {
		conflict = c;
	}
	
	/** An exception thrown when the GLR parser finds more than one way to parse
	 *  a sentence.
	 */
	public AmbiguousSentenceException() {
		conflict = null;
	}

	public Conflict getConflict() {
		return conflict;
	}

	public Set<RuleNode> getParseTrees() {
		return parseTrees;
	}

}