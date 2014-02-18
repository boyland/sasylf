/** Written by Matthew Rodriguez, 2007.
 * A class representing an action in an LR parse table. Can represent an accept action,
 * a reduce action, a shift action, or a goto action. For shift and goto actions, 
 * stores an integer for which state to go to next. For reduce actions, stores an
 * integer for which rule to reduce by.
 */

package edu.cmu.cs.sasylf.grammar;

class Action {
	private ActionType type;
	private int next;
	
	/** Creates an Action representing what a parsing Automaton should do when encountered.
	 * @param t The type of action. Tells the Automaton what to do.
	 * @param i The number associated with this action. For Shift and Goto, this is the next
	 * 			state the Automaton should take. For reduce, this is the Rule that should be
	 * 			reduced with.
	 */
	public Action(ActionType t, int i) {
		type = t;
		next = i;
	}
	
	/** Creates an Action representing what a parsing Automaton should do when encountered.
	 * This constructor is generally meant to be used for accept actions, as other kinds
	 * of actions require additional information.
	 * @param t The type of action. Tells the Automaton what to do.
	 */
	public Action(ActionType t) {
		type = t;
		next = -1;
	}
	
	/**
	 * @return What type of action this is.
	 */
	public ActionType getType() {
		return type;
	}
	
	/**
	 * @return The next state to be visited, or the rule to reduce by, depending on what
	 * kind of action this is.
	 */
	public int getNext() {
		return next;
	}
	
	/** Returns true if the other action is the same type and has the same integer for
	 * next state or rule number.
	 */
	public boolean equals(Object other) {
		if(!(other instanceof Action))
			return false;
		Action o = (Action) other;
		return type == o.type && next == o.next;
	}
	
	/** Prints out a compact string representing this action. $ represents an accept action.
	 * g4 means "goto state 4." s4 means "shift, then goto state 4." r4 means "reduce using
	 * rule number 4." "$" means accept.
	 */
	public String toString() {
		switch(type) {
		case ACCEPT:
			return "$";
		case GOTO:
			return "g" + next;
		case SHIFT:
			return "s" + next;
		case REDUCE:
			return "r" + next;
		case CONFLICT:
		  return "c" + next;
		}
		return null;
	}
}
