/** Written by Matthew Rodriguez, 2007.
 * A class representing an conflict in a parse table.
 * Can store any number of conflicting actions. 
 */

package edu.cmu.cs.sasylf.grammar;

import java.util.LinkedList;
import java.util.List;

class Conflict extends Action {
	private List<Action> actions;
	
	/** Create a new Conflict representing two different actions being placed in the
	 *  same place in the lookup table.
	 * @param a1 The first action.
	 * @param a2 The second action.
	 */
	public Conflict(Action a1, Action a2) {
		super(ActionType.CONFLICT);
		actions = new LinkedList<Action>();
		actions.add(a1);
		actions.add(a2);
	}
	
	/** Add a new action into the mix.
	 * @param a Action to be added.
	 * @return false if duplicate
	 */
	public boolean add(Action a) {
		for(Action b: actions) {
			if(a.equals(b)) {
				return false;
			}
		}
		actions.add(a);
		return true;
	}
	
	/** Returns a list of all actions in this conflict.
	 */
	public List<Action> getActions() {
		return new LinkedList<Action>(actions);
	}
	
	/** prints out all actions in this conflict in a compact form, like "s5/r3".
	 */
	public String toString() {
		String s = "";
		for(Action a: actions) {
			s += a.toString() + "/";
		}
		return s.substring(0, s.length()-1);
	}
}
