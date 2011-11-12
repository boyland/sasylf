package edu.cmu.cs.sasylf.ast;

import java.util.*;
import java.io.*;

import edu.cmu.cs.sasylf.ast.grammar.GrmNonTerminal;
import edu.cmu.cs.sasylf.ast.grammar.GrmRule;
import edu.cmu.cs.sasylf.ast.grammar.GrmTerminal;
import edu.cmu.cs.sasylf.ast.grammar.GrmUtil;
import edu.cmu.cs.sasylf.grammar.Symbol;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Term;

import static edu.cmu.cs.sasylf.util.Util.*;


public class Syntax extends Node implements ClauseType, ElemType {
	public Syntax(NonTerminal nt, List<Clause> l) { nonTerminal = nt; elements = l; }
	public NonTerminal getNonTerminal() { return nonTerminal; }
	public List<Clause> getClauses() { return elements; }

	private List<Clause> elements;
	private NonTerminal nonTerminal;

	public void prettyPrint(PrintWriter out) {
		nonTerminal.prettyPrint(out);
		out.print("\t::= ");
		boolean prev = false;
		for (Clause c : getClauses()) {
			if (prev)
				out.print("\t|   ");
			c.prettyPrint(out);
			prev = true;
			out.println();
		}
		out.println("\n");
	}

	public Set<Terminal> getTerminals() {
		Set<Terminal> s = new HashSet<Terminal>();
		for (Clause c : getClauses()) {
			s.addAll(c.getTerminals());
		}
		return s;
	}

	public void getVariables(Map<String,Variable> map) {
		for (Clause c : getClauses()) {
			if (c == null)
				System.err.println("null clause in Syntax " + this);
			c.getVariables(map);
		}
	}

	public void typecheck(Context ctx) {
		for (int i = 0; i < elements.size(); ++i) {
			Clause c = elements.get(i);
			c.typecheck(ctx);
			if (!c.isVarOnlyClause()) {
				ClauseDef cd = new ClauseDef(c, this);
				//cd.checkVarUse(isInContextForm());
				elements.set(i, cd);
				ctx.parseMap.put(cd.getElemTypes(), cd);

				GrmRule r = new GrmRule(getSymbol(), cd.getSymbols(), cd);
				ctx.ruleSet.add(r);

				if (r.getRightSide().size() > 1 || r.getRightSide().get(0) instanceof GrmTerminal) {
					GrmRule rParens = new GrmRule(getSymbol(), new ArrayList<Symbol>(r.getRightSide()), cd);
					rParens.getRightSide().add(0, GrmUtil.terminalFor("("));
					rParens.getRightSide().add(GrmUtil.terminalFor(")"));					
					ctx.ruleSet.add(rParens);
				}
			}
		}
		
		// check variable uses
		for (int i = 0; i < elements.size(); ++i) {
			Clause c = elements.get(i);
			if (!c.isVarOnlyClause()) {
				ClauseDef cd = (ClauseDef) c;
				cd.checkVarUse(isInContextForm());
			}
		}
		
		// compute a rule mapping the terminal for this Syntax to the NonTerminal for this Syntax, with and without parens
		GrmRule termRule = new GrmRule(getSymbol(), new GrmTerminal[] { getTermSymbol() }, null);
		ctx.ruleSet.add(termRule);
		termRule = new GrmRule(getSymbol(), new GrmTerminal[] { GrmUtil.terminalFor("("), getTermSymbol(), GrmUtil.terminalFor(")") }, null);
		ctx.ruleSet.add(termRule);

		// compute a rule mapping the start symbol to the NonTerminal for this Syntax
		GrmRule startRule = new GrmRule(GrmUtil.getStartSymbol(), new Symbol[] { getSymbol() }, null);
		ctx.ruleSet.add(startRule);
	}
	
	public boolean isInContextForm() {
		// one case must have only terminals
		int terminalCaseCount = 0;
		int contextCaseCount = 0;
		for (Clause c : getClauses()) {
			if (isTerminalCase(c))
				terminalCaseCount++;
			else if (isContextCase(c))
				contextCaseCount++;
		}
		boolean isContext = terminalCaseCount == 1 && contextCaseCount == getClauses().size()-1; 
		if (isContext)
			debug("Found a context: " + this.getNonTerminal());
		return isContext;
	}

	/** A context case has a recursive reference to the syntax and a variable
	 */
	private boolean isContextCase(Clause c) {
		// look for sub-part of gamma clause, a NonTerminal with same type as this
		boolean found = false;
		for (ElemType eType: c.getElemTypes()) {
			if (eType == this)
				found = true;
			else if (eType instanceof Syntax)
				debug("Not it: " + ((Syntax)eType).getNonTerminal());
			else
				debug("Not it: " + eType);				
		}
		if (!found) {
			debug("Not found: " + c);
			return false;
		}
		
		// look for sub-part of gamma clause that is a variable
		for (Element e : c.getElements()) {
			if (e instanceof Variable) {
				debug("Found a context case: " + c);
				return true;
			} else {
				debug("Not a variable: " + e);
			}
		}
		return false;
	}
	
	/** A terminal case has only Terminals
	 */
	private boolean isTerminalCase(Clause c) {
		for (Element e : c.getElements()) {
			if (!(e instanceof Terminal))
				return false;
		}
		return true;
	}
	
	public String toString() {
		return nonTerminal.toString();
	}

	private Set<Syntax> varTypes;
	/**
	 * Return the variable types that this context nonterminal can include
	 * @return set of variable types
	 */
	public Set<Syntax> getVarTypes() {
	  if (varTypes == null) {
	    varTypes = new HashSet<Syntax>();
	    for (Clause c : getClauses()) {
	      if (isContextCase(c)) {
	        for (Element e : c.getElements()) {
	          if (e instanceof Variable) varTypes.add(((Variable)e).getType());
	        }
	      }
	    }
	  }
	  return varTypes;
	}
	
	/**
	 * Return true if any of the variables bound by this gamma context
	 * could occur inside the given type.
	 * @param type type to check out
	 * @return true if a variable could be used in a term of the given type.
	 */
	public boolean canAppearIn(Term type) {
	  for (Syntax s : getVarTypes()) {
	    if (FreeVar.canAppearIn(s.typeTerm(), type)) return true;
	  }
	  return false;
	}
	
	public void computeVarTypes(Map<String,Variable> varMap) {
		for (Clause c : getClauses()) {
			c.computeVarTypes(this, varMap);
		}	
	}
	
	public Constant typeTerm() {
		if (term == null)
			term =new Constant(nonTerminal.getSymbol(), Constant.TYPE); 
		return term;
	}

	private Constant term = null;
	private GrmNonTerminal gnt;
	private GrmTerminal gt;
	
	public edu.cmu.cs.sasylf.grammar.NonTerminal getSymbol() {
		if (gnt == null)
			gnt = new GrmNonTerminal(nonTerminal.getSymbol());
		return gnt;
	}

	public String getTermSymbolString() {
		return "__TERM_FOR_" + nonTerminal.getSymbol();
	}
	public GrmTerminal getTermSymbol() {
		if (gt == null)
			gt = new GrmTerminal("__TERM_FOR_" + nonTerminal.getSymbol(), nonTerminal);
		return gt;
	}
}
