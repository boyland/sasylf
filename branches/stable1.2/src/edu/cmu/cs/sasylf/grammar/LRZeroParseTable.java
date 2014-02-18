/** Written by Matthew Rodriguez, 2007.
 * A class that builds an LR(0) parse table based on a grammar. Used in LR(0) parsing
 * and GLR(0) parsing.
 */

package edu.cmu.cs.sasylf.grammar;

import java.util.LinkedList;
import java.util.List;

class LRZeroParseTable implements LRParseTable {
	
	private Table actionTable, gotoTable;
	
	private List<ItemSet> allSets;
	private int numSets;
	
	/** Constructs a new Action and Goto Table for Grammar g. 
	 */
	public LRZeroParseTable(Grammar g) {
		allSets = new LinkedList<ItemSet>();
		numSets = 0;
		
		//Constructs ItemSets for use in constructing the tables.
		try {
			new ItemSet(StartSymbol.getStartSymbol(), this, g.augment().allItemRules(), new LinkedList<ItemRule>());
		} catch (DuplicateItemSetException e) {
			//Can't happen
			e.printStackTrace();
		}
		
		//Constructs the tables.
		actionTable = new Table(allTerminals(), this);
		gotoTable = new Table(allNonTerminals(), this);
		
		Table.makeActionTable(actionTable);
		Table.makeGotoTable(gotoTable);
	}
	
	/** Looks up the next Action for state and symbol.
	 */
	public Action nextAction(int state, Symbol s) {
		return actionTable.getAction(state, s);
	}
	
	/** Looks up the next Goto for state and symbol.
	 */
	public Action nextGoto(int state, Symbol s) {
		return gotoTable.getAction(state, s);
	}
	
	/** Gives children ItemSets a number.
	 * @return the next unused number from the set {0, 1, 2 ... }
	 */
	public int getNextID() {
		return numSets++;
	}
	
	/** returns the number of sets in this table.
	 */
	public int numberOfSets() {
		return numSets;
	}
	
	/** Adds an itemset to the list if it is empty. Otherwise, do nothing.
	 */
	public void addFirstItemSet(ItemSet i) {
		if(allSets.isEmpty()) {
			allSets.add(i);
		}
	}
	
	/** Return a list of all sets.
	 */
	public List<ItemSet> getAllSets() {
		return allSets;
	}
	
	/** Adds an ItemSet to the list.
	 * @throws DuplicateItemSetException if it is identical to another set in the list.
	 */
	public void addItemSet(ItemSet i) throws DuplicateItemSetException {
		for(ItemSet is: allSets) {
			if(i.equals(is)) {
				throw new DuplicateItemSetException(is);
			}
		}
		allSets.add(i);
	}
	
	/** Gets ItemSet at index i.
	 */
	public ItemSet getItemSet(int i) {
		return allSets.get(i);
	}
	
	/** Creates a list of all Terminals in the grammar.
	 */
	private LinkedList<Symbol> allTerminals() {
		LinkedList<Symbol> result = new LinkedList<Symbol>();
		for(ItemSet is: allSets) {
			if(is.getSymbol() instanceof Terminal && !result.contains(is.getSymbol())) {
				result.add((Terminal) is.getSymbol());
			}
		}
		result.add(AcceptSymbol.getAcceptSymbol());
		return result;
	}
	
	
	/** Creates a list of all NonTerminals in the grammar.
	 */
	private LinkedList<Symbol> allNonTerminals() {
		LinkedList<Symbol> result = new LinkedList<Symbol>();
		for(ItemSet is: allSets) {
			Symbol s = is.getSymbol();
			if(s instanceof NonTerminal && !(s instanceof StartSymbol) && !result.contains(s)) {
				result.add((NonTerminal) is.getSymbol());
			}
		}
		return result;
	}

	/**
	 * prints out the action and goto tables. Good for debugging.
	 */
	public String toString() {
		String s = actionTable.toString();
		s += gotoTable.toString();
		return s;
	}
}