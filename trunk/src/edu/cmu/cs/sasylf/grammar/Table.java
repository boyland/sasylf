/** Written by Matthew Rodriguez, 2007.
 * A class representing a table of actions. 
 * Implemented here as the basis of an LRZeroParseTable.
 */

package edu.cmu.cs.sasylf.grammar;

import java.util.List;
import java.util.ArrayList;

class Table {
	private List<Symbol> header;
	private Action[][] table;
	private LRZeroParseTable lrz;
	
	/** Constructs a new table. 
	 * @param ls The list of symbols to be used as this table's header.
	 * @param transTable The LRZeroParseTable whose itemsets will be used.
	 */
	public Table(List<Symbol> ls, LRZeroParseTable transTable) {
		header = new ArrayList<Symbol>(ls.size());
		lrz = transTable;
		header.addAll(ls);
		table = new Action[lrz.numberOfSets()][header.size()];
	}
	
	/** Places action in the table at state i under symbol a.
	 */
	public void addAction(int i, Symbol s, Action action) {
		Action a = table[i][header.indexOf(s)];
		//If there's already a symbol in that slot, we have a conflict!
		if(a != null){
			//If there's already a conflict there, just add this to the conflict.
			if(a instanceof Conflict) {
				Conflict c = (Conflict)a;
				c.add(action);
				return;
			}
			//If there's no conflict here yet and this isn't the same action, 
			//create a new conflict here.
 			if (!a.equals(action)) {
				Conflict c = new Conflict(a, action);
				table[i][header.indexOf(s)] = c;
			}
			return;
		}
		table[i][header.indexOf(s)] = action;
	}
	
	/**
	 * @param index the state
	 * @param key the symbol
	 * @return the action under the symbol and state given.
	 */
	public Action getAction(int index, Symbol key) {
		int keyIndex = header.indexOf(key);
		if (keyIndex == -1)
			return null;
		return table[index][keyIndex];
	}

	/** Fill an entire row with an action. Used for reduce actions in LRZero.
	 * @param index the state to fill with the action
	 * @param action the action to fill the row with
	 */
	public void fillRow(int index, Action action) {
		for(int i = 0; i < table[index].length; i++) {
			addAction(index, header.get(i), action);
		}
	}
	
	/** Takes a table and fills it with gotos based on this class's parse table.
	 * @param t the table to be filled with gotos.
	 */
	public static void makeGotoTable(Table t) {
		for(ItemSet state: t.lrz.getAllSets()) {
			for(ItemSet next: state.nextSets()) {
				if(next.getSymbol() instanceof NonTerminal) {
					t.addAction(state.setNumber, next.getSymbol(), new Action(ActionType.GOTO, next.setNumber));
				}
			}
		}
	}

	/** Takes a table and fills it with actions based on this class's parse table.
	 * @param t the table to be filled with actions.
	 */
	public static void makeActionTable(Table t) {
		for(ItemSet state: t.lrz.getAllSets()) {
			for(ItemRule r: state.allRules()) {
				//Fills the table with accepts and reduce actions.
				if(r.isReadRule()) {
					if(r.getLeftSide() instanceof StartSymbol) {
						t.addAction(state.setNumber, AcceptSymbol.getAcceptSymbol(), new Action(ActionType.ACCEPT));
					} else {
						t.fillRow(state.setNumber, new Action(ActionType.REDUCE, r.getOriginalRule()));
					}
				}
			}
			//Fills the table with shift actions.
			for(ItemSet next: state.nextSets()) {
				if(next.getSymbol() instanceof Terminal) {
					t.addAction(state.setNumber, next.getSymbol(), new Action(ActionType.SHIFT, next.setNumber));
				}
			}
		}
	}

	/**
	 * Prints this table using tabs. Not guaranteed to be formatted well.
	 */
	public String toString() {
		String s = "State\t";
		for(Symbol s2: header) {
			s+= s2 + "\t";
		}
		s += "\n";
		for(int i = 0; i < table.length; i++) {
			s += i + "\t";
			Action[] aa = table[i];
			for(Action a: aa) {
				s += a + "\t";
			}
			s += "\n";
		}
		return s;
	}
}
