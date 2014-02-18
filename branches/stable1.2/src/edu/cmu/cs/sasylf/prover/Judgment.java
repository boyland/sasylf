/** 
 * prettyPrint() and related methods written by Matthew Rodriguez, 2008.
 */

package edu.cmu.cs.sasylf.prover;

import java.util.List;

import edu.cmu.cs.sasylf.ast.Clause;
import edu.cmu.cs.sasylf.ast.ClauseDef;
import edu.cmu.cs.sasylf.ast.Element;
import edu.cmu.cs.sasylf.ast.NonTerminal;
import edu.cmu.cs.sasylf.ast.Syntax;
import edu.cmu.cs.sasylf.ast.Terminal;
import edu.cmu.cs.sasylf.term.*;

public class Judgment {
	private Term term;
	private edu.cmu.cs.sasylf.ast.Judgment judgmentType;
	
	public Term getTerm() { return term; }
	public edu.cmu.cs.sasylf.ast.Judgment getJudgmentType() { return judgmentType; }
	
	private Substitution sub;
	
	public Judgment(Term term, edu.cmu.cs.sasylf.ast.Judgment type) {
		this.term = term;
		this.judgmentType = type;
	}
	
	public String toString() {
		return term.toString();
	}
	public int hashCode() {
		return term.hashCode();
	}
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Judgment)) return false;
		Judgment j = (Judgment) obj;
		return term.equals(j.term);
	}
	
	/** Prints this judgment in a form that can be understood by the tool.
	 * @param substitution The substitution to be used.
	 * @return a pretty string
	 */
	public String prettyPrint(Substitution substitution) {
		String s = "";
		sub = substitution;
		//Gets a list of elements in this judgment.
		List<Element> list = judgmentType.getForm().getElements();
		int index = 0;
		//Prints each element. 
		for(Element e: list) {
			s += printElement(e, (Application)term, index);
			if(e instanceof NonTerminal) {
				index++;
			}
		}
		return s;
	}

	/** Pretty prints an application object.
	 * @param app the application object
	 * @param syntax the syntax object to be used for the app's form
	 */
	private String printApplication(Application app, Syntax syntax) {
		String s = "(";
		List<Clause> clauses = syntax.getClauses();
		for(Clause c: clauses) {
			ClauseDef cd = (ClauseDef)c;
			//Searches for the ClauseDef that provides the right form for the app
			if(app.getFunction().toString().equals(cd.getConstructorName())) {
				int index = 0;
				for(Element e: cd.getElements()) {
					//If it is a terminal symbol, just print the symbol
					if(e instanceof Terminal) {
						s += ((Terminal)e).getSymbol();
					//If it's a nonterminal symbol, printElement the thing
					} else if(e instanceof NonTerminal) {
						s += printElement(e, app, index);
						index++;
					}
					s += " ";
				}
			}
		}
		return s.trim() + ")";
	}
	
	/** Pretty prints an element
	 * @param e the element to be pretty printed
	 * @param a the application used to determine the Term of the element, if it's a nonterminal
	 * @param index the index that this element is at in a
	 */
	private String printElement(Element e, Application a, int index) {
		String s = "";
		//If it's a terminal, just print it with some nice white space around it
		if(e instanceof Terminal) {
			Terminal t = (Terminal)e;
			s += " " + t.getSymbol() + " ";
		//If it's a nonterminal, use the Argument to determine its Term, and printTerm it
		} else if(e instanceof NonTerminal) {
			NonTerminal nt = (NonTerminal)e;
			List<? extends Term> args = a.getArguments();
			Term t = args.get(index);
			Syntax syntax = nt.getType();
			s += printTerm(t, syntax);
		}
		return s;
	}
	
	/** Pretty prints a Term
	 * @param t the term to be pretty printed
	 * @param syntax the syntax used to determine the pretty symbol of this term
	 */
	private String printTerm(Term t, Syntax syntax) {
		String s = "";
		//If it's a constant, look for the pretty symbol in the syntax.
		if(t instanceof Constant) {
			Constant cn = (Constant)t;
			List<Clause> clauses = syntax.getClauses();
			for(Clause c: clauses) {
				ClauseDef cd = (ClauseDef)c;
				if(cn.getName().equals(cd.getConstructorName())){
					for(Element e: cd.getElements()) {
						s += ((Terminal)e).getSymbol();
					}
				}
			}
		//If it's a freevar, check to see if it's a substitution
		} else if (t instanceof FreeVar) {
			FreeVar v = (FreeVar)t;
			Term t2 = sub.getSubstituted(v);
			//If there is a substitution for it, printTerm the substitution
			if(t2 != null) {
				s += printTerm(t2, syntax);
			//If there is no substitution for it, it truly is a free variable. Print it.
			} else {
				s += v.getName();
			}
		//If it's an application, printApplication it
		} else if (t instanceof Application) {
			s += printApplication((Application)t, syntax);
		}
		return s;
	}
	
}
