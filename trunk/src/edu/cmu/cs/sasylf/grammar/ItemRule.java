/** Written by Matthew Rodriguez, 2007.
 * A class representing an ItemRule. Used for creating ItemSets.
 */

package edu.cmu.cs.sasylf.grammar;

import java.util.LinkedList;
import java.util.List;

class ItemRule implements Rule {
	private NonTerminal left;
	private List<Symbol> right;
	private int orig;
	
	/** Create an item rule for use in an ItemSet.
	 * @param originalRule Rule to be "itemized."
	 * @param index The index of the original rule in the grammar object.
	 * @throws IllegalArgumentException if there are already one or more readsymbols in the
	 * 		   original rule.
	 */
	public ItemRule(Rule r, int index) {
		//If there are already readsymbols in the rule, it will break this class's functionality
		for(Symbol s: r.getRightSide()) {
			if(s instanceof ReadSymbol) {
				throw new IllegalArgumentException("No read symbols allowed in the original rule");
			}
		}
		
		left = r.getLeftSide();
		right = new LinkedList<Symbol>(r.getRightSide());
		right.add(0, ReadSymbol.getReadSymbol());
		orig = index;
	}
	
	/** private constructor used for creating childrules.
	 * @param t ItemRule to be "read".
	 * @param index The index of the original rule in the grammar object.
	 */
	private ItemRule(ItemRule r, int index) {
		right = new LinkedList<Symbol>();
		
		for(int i = 0; i < r.getRightSide().size(); i++) {
			if(r.getRightSide().get(i) == ReadSymbol.getReadSymbol()) {
				right.add(r.getRightSide().get(i+1));
				right.add(ReadSymbol.getReadSymbol());
				i++;
			} else {
				right.add(r.getRightSide().get(i));
			}
		}
	
		left = r.left;
		orig = index;
	}
	
	/** Create an item rule for use in an ItemSet.
	 * @param l left side of the rule
	 * @param r right side of the rule
	 * @param index The index of the original rule in the grammar object.
	 * @throws IllegalArgumentException if there are already readsymbols in the right side.
	 */
	public ItemRule(NonTerminal l, List<Symbol> r, int index) {
		//If there are already readsymbols in the right side, it will break this class's functionality
		for(Symbol s: r) {
			if(s instanceof ReadSymbol) {
				throw new IllegalArgumentException("No read symbols allowed in the right side");
			}
		}
		
		left = l;
		right = new LinkedList<Symbol>(r);
		right.add(0, ReadSymbol.getReadSymbol());
		orig = index;
	}
	
	/** Takes a rule and "reads" it, creating a child rule with the ReadSymbol one to the right.
	 * @param r Original rule.
	 * @return new rule.
	 */
	ItemRule childRule() {
		if(getRightSide().get(getRightSide().size()-1) == ReadSymbol.getReadSymbol()) {
			return null;
		}
		return new ItemRule(this, getOriginalRule());
	}
	
	/** Copy constructor. Duplicates a given ItemRule.
	 */
	public ItemRule(ItemRule r) {
		left = r.left;
		right = r.right;
		orig = r.orig;
	}

	/**
	 * @return the left side of this rule
	 */
	public NonTerminal getLeftSide() {
		return left;
	}
	
	/**
	 * @return the right side of this rule
	 */
	public List<Symbol> getRightSide() {
		return right;
	}
	
	/**
	 * @return the rule this itemrule is based on. Should not contain a readsymbol.
	 */
	public int getOriginalRule() {
		return orig;
	}
	
	/** Determines if this rule has been completely read--i.e., the ReadSymbol is all the way
	 *  on the right.
	 */
	public boolean isReadRule() {
		return (right.get(right.size()-1) instanceof ReadSymbol);
	}
	
	/**
	 * Prints out this rule, for example "a -> b ~ c"
	 */
	public String toString() {
		String r = left + " -> ";
		for(Symbol s: right) {
			r += s + " ";
		}
		return r;
	}
	
	/** Returns the first symbol after the Read Symbol. 
	 *  If there is none, returns null.
	 */
	public Symbol readSymbol() {
		//If the readsymbol is on the far right, return null.
		if(right.get(right.size()-1) instanceof ReadSymbol) {
			return null;
		}
		
		//finds the readsymbol and returns the symbol after it.
		for(int i = 0; i < right.size(); i++) {
			if(right.get(i) instanceof ReadSymbol) {
				return right.get(i+1);
			}
		}
		return null;
	}
	
	/**
	 * @return true if other is also an itemrule and its left and right sides are 
	 * identical.
	 */
	public boolean equals(Object other) {
		ItemRule ir = (ItemRule)other;
		return left.equals(ir.left) && right.equals(ir.right);
	}
}