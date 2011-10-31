/** Written by Matthew Rodriguez, 2007.
 * An exception thrown when the algorithm attempting to construct a parse table
 * generates a duplicate item set. Contains a reference to the original set for
 * convenience and easy catching.
 */

package edu.cmu.cs.sasylf.grammar;

class DuplicateItemSetException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5355735248140414166L;
	
	private ItemSet original;
	
	/** Thrown when user attempts to create an ItemSet identical to one that already exists.
	 * @param is The ItemSet this is a duplicate of.
	 */
	public DuplicateItemSetException(ItemSet is) {
		original = is;
	}
	
	/** Returns the itemset this is a duplicate of.
	 */
	public ItemSet originalCopy() {
		return original;
	}

}
