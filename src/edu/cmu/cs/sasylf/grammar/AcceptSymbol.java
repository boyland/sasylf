/** Written by Matthew Rodriguez, 2007.
 * A class representing an accept symbol in a parser.
 * The class only allows the construction of a single accept symbol, as they
 * are all identical and there's no reason to have a million different ones
 * floating around.
 */

package edu.cmu.cs.sasylf.grammar;

class AcceptSymbol implements Terminal {
	
	private static AcceptSymbol any;
	
	/** Creates a generic accept symbol.
	 */
	private AcceptSymbol() {}
	
	/**
	 * @return A symbol representing the end of a sentence. 
	 */
	public static AcceptSymbol getAcceptSymbol() {
		if(any == null) {
			any = new AcceptSymbol();
		}
		return any;
	}
	
	/** Returns "$", the character generally used to represent accept symbols.
	 */
	public String toString() {
		return "$";
	}
	
	/** Returns true if s is also an accept symbol.
	 */
	public boolean equals(Object s) {
		return s instanceof AcceptSymbol;
	}
}