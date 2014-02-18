/** Written by Matthew Rodriguez, 2007.
 * A class representing an start symbol in a parser.
 * The class only allows the construction of a single start symbol, as they
 * are all identical and there's no reason to have a million different ones
 * floating around.
 */

package edu.cmu.cs.sasylf.grammar;

class StartSymbol implements NonTerminal {
	private static StartSymbol any;
	private StartSymbol() {}
	
	/**
	 * @return a new symbol for an augmented grammar.
	 */
	public static StartSymbol getStartSymbol() {
		if(any == null) {
			any = new StartSymbol();
		}
		return any;
	}
	
	/**
	 * Prints "S", the character used to represent the start symbol.
	 */
	public String toString() {
		return "S";
	}
	
	/**
	 * true if the other object is also a start symbol.
	 */
	public boolean equals(Object o) {
		return o instanceof StartSymbol;
	}
}