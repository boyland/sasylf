/** Written by Matthew Rodriguez, 2007.
 * A class representing an ItemSet. Used in the construction of LR Parse Tables.
 */

package edu.cmu.cs.sasylf.grammar;

import java.util.LinkedList;
import java.util.List;

public class ItemSet {
	private Symbol symbol;
	private List<ItemRule> kernelRules;
	private List<ItemRule> nonKernelRules;
	
	private List<ItemSet> nextSets;
	
	private LRZeroParseTable table;
	
	public final int setNumber;
	
	/** Creates a new ItemSet and all of its children.
	 * @param s The symbol of this set.
	 * @param lrz The parse table keeping track of this set and its children.
	 * @param allRules A list of every rule from the augmented grammar.
	 * @param parentRules A list of every rule the parent contains.
	 * @throws DuplicateItemSetException if identical to existing set.
	 */
	public ItemSet(Symbol s, LRZeroParseTable lrz, List<ItemRule> allRules, List<ItemRule> parentRules) throws DuplicateItemSetException {	
		symbol = s;
		table = lrz;
		
		nextSets = new LinkedList<ItemSet>();
		kernelRules = new LinkedList<ItemRule>();
		nonKernelRules = new LinkedList<ItemRule>();
		
		//If parentRules is empty, that means this ItemSet has no parent, so it is the root.
		//As such, look for all rules in the original grammar produced by the start symbol.
		if(parentRules.isEmpty()) {
			kernelRules = duplicateAll(allRules, s);
		} else {
			//Else, simply take all the parent rules that begin with s and move the read
			//symbol one place to the right.
			kernelRules = childAll(parentRules, s);
		}
		
		//if this is identical to another given set, throw an exception.
		for(ItemSet is: table.getAllSets()) {
			if(this.equals(is)) {
				throw new DuplicateItemSetException(is);
			}
		}
		
		// Now close the set using the kernelRules and the list of all rules.
		close(kernelRules, allRules);
		
		//Add this to the table, give it an id
		table.addItemSet(this);
		setNumber = table.getNextID();
		
		//And finally, generate all children of this set.
		for(ItemRule r: kernelRules) {
			generateChild(r.readSymbol(), allRules, table);
		}
		for(ItemRule r: nonKernelRules) {
			generateChild(r.readSymbol(), allRules, table);
		}
	}

	/** Creates a child ItemSet.
	 * @param readSymbol the child's symbol.
	 * @param allRules a list of all rules, unaltered from the augmented grammar.
	 * @param lrz the parse table keeping track of this set and its family.
	 */
	private void generateChild(Symbol readSymbol, List<ItemRule> allRules, LRZeroParseTable lrz) {
		//If no symbol was read, there are no children to produce
		if(readSymbol == null) {
			return;
		}
		
		//If there already exists a set in nextSets based on this symbol, return.
		for(ItemSet s: nextSets) {
			if(s.symbol == readSymbol) {
				return;
			}
		}
		
		//Try to construct an ItemSet based on the next symbol. If it already exists,
		//simply use the original instead.
		ItemSet nextSet;
		try {
			nextSet = new ItemSet(readSymbol, table, allRules, allRules());
		} catch (DuplicateItemSetException e) {
			nextSet = e.originalCopy();
		}
		nextSets.add(nextSet);
	}

	/** Searches the original list of rules for rules required by this set for "closure."
	 * @param ourRules A list of rules to close.
	 * @param allRules An unaltered list of all rules.
	 */
	private void close(List<ItemRule> searchRules, List<ItemRule> allRules) {
		//For each rule being searched through, determine if it should be added.
		for(ItemRule i: searchRules) {
			Symbol next = i.readSymbol();
			List<ItemRule> newRules = findAll(allRules, next);
			List<ItemRule> uniqueRules = new LinkedList<ItemRule>();
			for(ItemRule j: newRules) {
				boolean dupe = false;
				//If the rule is already in the set, don't add it.
				for(ItemRule k: kernelRules) {
					if(j.equals(k)) {
						dupe = true;
					}
				}
				for(ItemRule k: nonKernelRules) {
					if(j.equals(k)) {
						dupe = true;
					}
				}
				if(!dupe) {
					uniqueRules.add(j);
				}
			}
			//Now, close the set again using all added rules. Continue until no rules are added.
			nonKernelRules.addAll(uniqueRules);
			close(uniqueRules, allRules);
		}
	}

	/**
	 * @return The symbol of this set.
	 */
	public Symbol getSymbol() {
		return symbol;
	}
	
	/** Returns a list of all rules produced by S in this set.
	 */
	public List<ItemRule> findAll(Symbol s) {
		return findAll(allRules(), s);
	}
	
	/** Returns a list of all rules, kernel and non-kernel, in this set.
	 */
	public List<ItemRule> allRules() {
		LinkedList<ItemRule> result = new LinkedList<ItemRule>();
		result.addAll(kernelRules);
		result.addAll(nonKernelRules);
		return result;
	}
	
	/** True if all rules are the same. Luckily, this is true if and only if all kernel rules are the same.
	 * @param other ItemSet being compared to.
	 */
	public boolean equals(Object obj) {
		if(!(obj instanceof ItemSet)) {
			return false;
		}
		ItemSet o = (ItemSet)obj;
		if (!(o instanceof ItemSet))
			return false;
		ItemSet other = (ItemSet) o;
		if (kernelRules.size() != other.kernelRules.size())
			return false;
		for(int i = 0; i < kernelRules.size(); i++) {
			if(!kernelRules.get(i).equals(other.kernelRules.get(i))) {
				return false;
			}
		}
		return true;
	}
	
	/** Hashes based on List.hash() of the list of kernel rules
	 */
	public int hashCode() {
		return kernelRules.hashCode();
	}
	
	/** Returns a list of all sets that this set points to.
	 */
	public List<ItemSet> nextSets() {
		return nextSets;
	}
	
	/** Finds all rules in list produced by s.
	 */
	public static List<ItemRule> findAll(List<ItemRule> list, Symbol s) {
		LinkedList<ItemRule> result = new LinkedList<ItemRule>();
		for(ItemRule r: list) {
			if(r.getLeftSide().equals(s)) {
				result.add(r);
			}
		}
		return result;
	}
	
	/** Finds all rules in list produced by S, duplicates each, and returns all duplicates.
	 */
	public static List<ItemRule> duplicateAll(List<ItemRule> list, Symbol s) {
		LinkedList<ItemRule> result = new LinkedList<ItemRule>();
		for(ItemRule r: list) {
			if(r.getLeftSide().equals(s)) {
				result.add(new ItemRule(r));
			}
		}
		return result;
	}
	
	/** Finds all rules in list produced by S, finds each child, and returns all children.
	 */
	private static List<ItemRule> childAll(List<ItemRule> list, Symbol s) {
		LinkedList<ItemRule> result = new LinkedList<ItemRule>();
		for(ItemRule r: list) {
			if(s.equals(r.readSymbol())) {
				result.add(r.childRule());
			}
		}
		return result;
	}
	
	/** Returns a string containing all of the kernel and non-kernel rules, and all transitions.
	 * 
	 */
	public String toString() {
		String result = "Item set " + setNumber + ":\n";
		for(Rule r: kernelRules) {
			result += r.getLeftSide() + " ->";
			for(Symbol s: r.getRightSide()){ 
				result += " " + s;
			}
			result += "\n";
		}
		for(Rule r: nonKernelRules) {
			result += "+ " + r.getLeftSide()+ " ->";
			for(Symbol s: r.getRightSide()){ 
				result += " " + s;
			}
			result += "\n";
		}
		for(ItemSet is: nextSets) {
			result += "Transition to set " + is.setNumber + " on symbol " + is.symbol + "\n";
		}
		return result;
	}
}
