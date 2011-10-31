package edu.cmu.cs.sasylf.grammar;

public interface LRParseTable {

	public Action nextAction(int state, Symbol s);

	public Action nextGoto(int state, Symbol s);

}
