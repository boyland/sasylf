/** Written by Matthew Rodriguez, 2007.
 * A class representing an read symbol in an itemrule in an itemset.
 * The class only allows the construction of a single read symbol, as they
 * are all identical and there's no reason to have a million different ones
 * floating around.
 */

package edu.cmu.cs.sasylf.grammar;

class ReadSymbol implements Symbol {
	private static ReadSymbol any;
	private ReadSymbol() {}
	
	/**
	 * @return a symbol representing a cursor reading this rule.
	 */
	public static ReadSymbol getReadSymbol() {
		if(any == null) {
			any = new ReadSymbol();
		}
		return any;
	}
	
	/** Returns a "~" to represent a readsymbol.
	 * 
	 */
	public String toString() {
		return "~";
	}
	
	/**
	 * True if the other object is a readsymbol, as all are identical.
	 */
	public boolean equals(Object o) {
		return o instanceof ReadSymbol;
	}
}