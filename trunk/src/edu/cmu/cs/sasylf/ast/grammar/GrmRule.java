package edu.cmu.cs.sasylf.ast.grammar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.cmu.cs.sasylf.ast.ClauseDef;
import edu.cmu.cs.sasylf.grammar.NonTerminal;
import edu.cmu.cs.sasylf.grammar.Rule;
import edu.cmu.cs.sasylf.grammar.Symbol;

public class GrmRule implements Rule {
	private List<Symbol> rightSide;
	private NonTerminal leftSide;
	private ClauseDef clauseDef;

	public GrmRule(NonTerminal lhs, Symbol[] rhs, ClauseDef cd) {
		leftSide = lhs;
		rightSide = Arrays.asList(rhs);
		clauseDef = cd;
	}

	public GrmRule(NonTerminal lhs, List<Symbol> rhs, ClauseDef cd) {
		leftSide = lhs;
		rightSide = rhs;
		clauseDef = cd;
	}

	public GrmRule(NonTerminal lhs, ClauseDef cd) {
		leftSide = lhs;
		rightSide = new ArrayList<Symbol>();
		clauseDef = cd;
	}

	public ClauseDef getClauseDef() { return clauseDef; }
	
	@Override
	public int hashCode() {
		return leftSide.hashCode() + rightSide.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GrmRule))
			return false;
		GrmRule other = (GrmRule)o;
		return other.leftSide.equals(leftSide) && other.rightSide.equals(rightSide);
	}
	
	public NonTerminal getLeftSide() { return leftSide; }

	public List<Symbol> getRightSide() { return rightSide; }
	
	public String toString() {
		String r = leftSide + " -> ";
		for(Symbol s: rightSide) {
			r += s + " ";
		}
		return r;
	}

}
