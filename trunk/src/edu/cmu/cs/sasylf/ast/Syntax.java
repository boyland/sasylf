package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Util.debug;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.ast.grammar.GrmNonTerminal;
import edu.cmu.cs.sasylf.ast.grammar.GrmRule;
import edu.cmu.cs.sasylf.ast.grammar.GrmTerminal;
import edu.cmu.cs.sasylf.ast.grammar.GrmUtil;
import edu.cmu.cs.sasylf.grammar.Symbol;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Status;


public class Syntax extends Node implements ClauseType, ElemType {
	public Syntax(Location loc, NonTerminal nt, List<Clause> l) { 
	  super(loc); 
	  nonTerminal = nt; 
	  elements = l; 
	}
	
	public NonTerminal getNonTerminal() { return nonTerminal; }
	public List<Clause> getClauses() { return elements; }

	private List<Clause> elements;
	private NonTerminal nonTerminal;
	private Variable variable;
	private ClauseDef context;

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

	/**
	 * Return the variable name for the (first) variable of this nonterminal,
	 * or null if this nonterminal does not permit variables.
	 */
	public Variable getVariable() {
	  return variable;
	}
	
	public void getVariables(Map<String,Variable> map) {
		for (Clause c : getClauses()) {
			if (c == null)
				System.err.println("null clause in Syntax " + this);
			c.getVariables(map);
		}
	}

	public void typecheck(Context ctx) {
	  int countVarOnly = 0;
		for (int i = 0; i < elements.size(); ++i) {
			Clause c = elements.get(i);
			c.typecheck(ctx);
			if (c.isVarOnlyClause()) {
			  if (countVarOnly == 0) variable = (Variable)c.getElements().get(0);
			  ++countVarOnly;
			} else {
				ClauseDef cd;
				if (c instanceof ClauseDef) cd = (ClauseDef) c;
				else cd = new ClauseDef(c, this);
				//cd.checkVarUse(isInContextForm());
				elements.set(i, cd);
				ctx.prodMap.put(cd.getConstructorName(),cd);
				ctx.parseMap.put(cd.getElemTypes(), cd);

				GrmRule r = new GrmRule(getSymbol(), cd.getSymbols(), cd);
				ctx.ruleSet.add(r);

				if (r.getRightSide().size() > 1 || r.getRightSide().get(0) instanceof GrmTerminal) {
					GrmRule rParens = new GrmRule(getSymbol(), new ArrayList<Symbol>(r.getRightSide()), cd);
					rParens.getRightSide().add(0, GrmUtil.getLeftParen());
					rParens.getRightSide().add(GrmUtil.getRightParen());					
					ctx.ruleSet.add(rParens);
				}
			}
		}
		
		// more than one variable cannot be distinguished
		if (countVarOnly > 1) {
		  ErrorHandler.recoverableError(Errors.TOO_MANY_VARIABLES, this);
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
		termRule = new GrmRule(getSymbol(), new GrmTerminal[] { GrmUtil.getLeftParen(), getTermSymbol(), GrmUtil.getRightParen() }, null);
		ctx.ruleSet.add(termRule);

		// compute a rule mapping the start symbol to the NonTerminal for this Syntax
		GrmRule startRule = new GrmRule(GrmUtil.getStartSymbol(), new Symbol[] { getSymbol() }, null);
		ctx.ruleSet.add(startRule);
	}
	
	private boolean isProductive;
	private Status isProductiveStatus = Status.NOTSTARTED;
	private static List<Syntax> computed = new ArrayList<Syntax>();
	
	public boolean isProductive() {
	  if (isProductiveStatus == Status.DONE) return isProductive;
	  isProductiveStatus = Status.NOTSTARTED;
	  isProductive = computeIsProductive();
	  for (Syntax s : computed) {
	    if (s.isProductiveStatus == Status.INPROCESS) {
	      s.isProductiveStatus = Status.NOTSTARTED;
	    }
	  }
	  isProductiveStatus = Status.DONE;
	  // System.out.println("Finished " + (isProductive ? "productive" : "unproductive") + " " + this);
	  return isProductive;
	}
	
	private boolean computeIsProductive() {
	  switch (isProductiveStatus) {
	  case NOTSTARTED: 
	    isProductiveStatus = Status.INPROCESS;
	    for (Clause elem : elements) {
	      boolean productive = true;
	      for (Element e : elem.getElements()) {
	        if (e instanceof NonTerminal && !((NonTerminal)e).getType().computeIsProductive()) {
	          // System.out.println("  Found unproductive use of " + e);
	          productive = false;
	          break;
	        }
	      }
	      if (productive) {
	        // System.out.println("  Clause " + elem + " is productive");
	        isProductive = true;
	        isProductiveStatus = Status.DONE;
	        break;
	      }
	    }
	    computed.add(this);
	    return isProductive;
	  case INPROCESS:
	  case DONE:
	    return isProductive;
	  }
	  return false;
	}
	
	private int contextFormCode = -1;
	private ClauseDef terminalCase = null;
	
	public boolean isInContextForm() {
	  if (contextFormCode == -1) {
	    contextFormCode = computeContextForm();
	  }
		return contextFormCode > 0;
	}
	
	/**
	 * If this context is in context form, return a non-null "empty context" case.
	 * Otherwise, return null;
	 * @return terminal case clause
	 */
	public ClauseDef getTerminalCase() {
	  if (isInContextForm()) {
	    return terminalCase;
	  }
	  return null;
	}
	
  /**
   * Determine whether this is a context syntax (a "Gamma").
   * If so, return a positive number (the number of ways variables are bound).
   * Otherwise return 0.
   * @return positive if indeed, otherwise zero.
   */
  private int computeContextForm() {
    // one case must have only terminals
		int terminalCaseCount = 0;
		int contextCaseCount = 0;
		for (Clause c : getClauses()) {
			if (isTerminalCase(c)) {
			  terminalCase = (ClauseDef)c;
				terminalCaseCount++;
			} else if (isContextCase(c))
				contextCaseCount++;
			else return 0;
		}
		boolean isContext = terminalCaseCount == 1 && contextCaseCount > 0; 
		if (isContext)
			debug("Found a context: " + this.getNonTerminal());
		return isContext ? contextCaseCount : 0;
  }

	/** A context case has a recursive reference to the syntax and a variable
	 */
	private boolean isContextCase(Clause c) {
		// look for sub-part of gamma clause, a NonTerminal with same type as this
	  int vars = 0;
	  int recs = 0;
	  
		for (ElemType eType: c.getElemTypes()) {
			if (eType == this) ++recs;
		}
		if (recs != 1) {
			debug("Not found: " + c + " has wrong number of recursive references: " + recs);
			return false;
		}
		
		// look for sub-part of gamma clause that is a variable
		for (Element e : c.getElements()) {
			if (e instanceof Variable) ++vars;
			if (e instanceof Binding) {
			  debug("not found: " + c + " has a binding " + e);
			  return false;
			}
		}
		if (vars != 1) {
		  debug("Not found: " + c + " has wrong number of variables: " + vars);
      return false;
    }
		
		return true;
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
	
	/**
	 * Register this syntax (if in context form) so that it knows what
	 * clause binds the variable.  If we discover a variable being bound
	 * in multiple contexts, we report an error.
	 */
	public void registerVarTypes() {
	  if (isInContextForm()) {
	    for (Clause c : getClauses()) {
	      if (isContextCase(c)) {
	        for (Element e : c.getElements()) {
	          if (e instanceof Variable) {
	            Syntax varType = ((Variable)e).getType();
	            if (varType.context == null) varType.context = (ClauseDef)c;
	            else if (varType.context != c) {
	              ErrorHandler.report(Errors.VARIABLE_HAS_MULTIPLE_CONTEXTS,varType);
	            }
	          }
	        }
	      }
	    }
	  }
	}
	
	/**
	 * Check that if this syntax has variables that it has a registered context.
	 * If it doesn't we report an error.
	 */
	public void checkVarTypeRegistered() {
	  if (variable != null && context == null) {
	    ErrorHandler.report(Errors.VARIABLE_HAS_NO_CONTEXT, this);
	  }
	}
	
	public ClauseDef getContextClause() {
	  return context;
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
		if (term == null) {
			term = new Constant(nonTerminal.getSymbol(), Constant.TYPE); 
		}
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
