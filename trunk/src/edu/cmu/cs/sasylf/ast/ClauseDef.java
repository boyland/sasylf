package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Pair;

public class ClauseDef extends Clause {
	public ClauseDef(Clause copy, ClauseType type) {
		this(copy, type, null);
	}
	public ClauseDef(Clause copy, ClauseType type, String cName) {
		super(copy.getLocation());
		setEndLocation(copy.getEndLocation());
		getElements().addAll(copy.getElements());
		this.type = type;
		if (cName != null) {
			consName = cName;
		} else {
			consName = "C";
			for (Element e : getElements()) {
				if (e instanceof Terminal) {
					Terminal t = (Terminal) e;
					//if (Character.isLetter(t.getSymbol().charAt(0))) {
						consName += '_' + t.getSymbol();
					//}
				} 
			}
			consName = uniqueify(consName);
		}
		for (Element e : getElements()) {
		  if (e instanceof Clause) {
        ErrorHandler.report("judgment/syntax must not include parenthesized expressions",copy);
      }
		}
	}

	public String getConstructorName() { return consName; }
	public ClauseType getType() { return type; }
	public int getAssumeIndex() {
	  if (cachedAssumeIndex > -2) return cachedAssumeIndex;
		if (type instanceof Judgment) {
			NonTerminal assumeNT = ((Judgment)type).getAssume();
			return cachedAssumeIndex=getElements().indexOf(assumeNT);
		} else if (type instanceof Syntax) {
		  Syntax s = (Syntax)type;
		  if (s.isInContextForm()) {
		    return cachedAssumeIndex=getElements().indexOf(s.getNonTerminal());
		  }
		}
		return cachedAssumeIndex=-1;
	}
	
	@Override
	public Term getTypeTerm() { return asTerm(); }

	private String consName;
	private ClauseType type;
	public Rule assumptionRule;	
	private int cachedAssumeIndex = -2;

	static private int uniqueint = 0;
	static private Set<String> strings = new HashSet<String>();
	static private String uniqueify(String s) {
		String result = s;
		if (strings.contains(s)) {
			result = s + uniqueint++;
		}
		strings.add(result);
		return result;
	}

	public Constant computeTerm(List<Pair<String, Term>> varBindings) {
		Term typeTerm = type.typeTerm();
		int assumeIndex = getAssumeIndex();
		List<Term> argTypes = new ArrayList<Term>();
		List<String> argNames = new ArrayList<String>();
		
		for (int i = 0; i < getElements().size(); ++i) {
			Element e = getElements().get(i);
			if (! (e instanceof Terminal) && i != assumeIndex 
					&& !(e instanceof Variable)) {
				Term argType = null;
				String argName = "x";
				if (e instanceof Binding) {
					Binding defB = (Binding) e;
					argType = defB.getNonTerminal().getType().typeTerm();
					argName = defB.getNonTerminal().getSymbol();
						
					List<Term> varTypes = new ArrayList<Term>();
					for (Element boundVarElem : defB.getElements()) {
						int varIndex = getIndexOf((Variable)boundVarElem);
						if (varIndex == -1)
							ErrorHandler.report("could not find " + boundVarElem + " in clause " + this, this);
						Variable localVar = (Variable) getElements().get(varIndex);
						varTypes.add(localVar.getType().typeTerm());
					}
					argType = Term.wrapWithLambdas(argType, varTypes);
				} else if (e instanceof NonTerminal){
				  // JTB: The following check is needed for AndClauses which can have multiple
				  // contexts.
				  if (((NonTerminal)e).getType().isInContextForm()) continue;
				  argType = ((NonTerminal)e).getType().typeTerm();
				  argName = ((NonTerminal)e).getSymbol();
				} else if (e instanceof Clause) {
					argType = ((ClauseUse)e).getConstructor().asTerm();
					argName = ((ClauseUse)e).getElemType().toString();
				} else {
					throw new RuntimeException("should be impossible case");
				}
				argTypes.add(argType);
				argNames.add(argName);
			}
		}

		typeTerm = Term.wrapWithLambdas(typeTerm, argTypes, argNames);
		
		return new Constant(consName, typeTerm);
	}

	public int getVariableIndex() {
	  int index = 0;
	  int result = -1;
	  for (Element e : getElements()) {
	    if (e instanceof Variable) {
	      if (result == -1) result = index;
	      else {
	        ErrorHandler.warning("An assumption clause must not have more than one variable", this);
	      }
	    }
	    ++index;
	  }
	  return result;
	}
	
	public int getIndexOf(Variable boundVar) {
		return getElements().indexOf(boundVar);
	}
	
	/** Computes a sample term for use in case analysis.
	 * Consists of the clause constant applied to fresh variables.
	 */
	public Term getSampleTerm() {
		Constant constant = (Constant)asTerm();
		Term typeTerm = constant.getType();
		List<Term> arguments = new ArrayList<Term>();
		while (typeTerm instanceof Abstraction) {
			Abstraction abs = (Abstraction)typeTerm;
			Term var = Facade.FreshVar(abs.varName, abs.varType);
			arguments.add(var);
			typeTerm = abs.getBody();
		}
		return constant.apply(arguments, 0);
	}

	/** All top-level vars should also be present inside bindings
	 * Only variables that appear at top level should be present inside bindings
	 * @param isContext 
	 */
	public void checkVarUse(boolean isContext) {
		Set<Variable> topVars = new HashSet<Variable>(), boundVars = new HashSet<Variable>();
		for (int i = 0; i < elements.size(); ++i) {
			Element e = elements.get(i);
			if (e instanceof Variable)
				topVars.add((Variable) e);
			if (e instanceof Binding) {
				for (Element boundE : ((Binding)e).getElements()) {
					if (boundE instanceof Variable)
						boundVars.add((Variable) boundE);
					else
						ErrorHandler.report(Errors.BAD_SYNTAX_BINDING, boundE);
				}
			}
		}
		if (!topVars.equals(boundVars)) {
			if (!topVars.containsAll(boundVars)) {
				// a variable in a binding was not bound outside
				boundVars.removeAll(topVars);
				ErrorHandler.report("Variable(s) " + boundVars + " were used in a binding but never declared", this);
			} else {
				// a variable declared at the top was not used in a binding
				topVars.removeAll(boundVars);
				if (!isContext)
					ErrorHandler.report(Errors.UNBOUND_VAR_USE, "Variable(s) " + topVars + " were used at the top level of this syntax or judgment form.  SASyLF assumes you are declaring this variable, but the variable is not bound in any expression.", this);
			}
		}
	}
	
	@Override
	public void prettyPrint(PrintWriter out, PrintContext ctx) {
	  if (ctx == null) {
	    super.prettyPrint(out, null);
	    return;
	  }
		//System.err.println("clausedef.prettyPrint for " + (ctx == null ? "" : ctx.term));
		Term origT = ctx.term;
		Term t = origT;
		List<String> origBoundVars = ctx.boundVars;
		int origBoundVarCount = ctx.boundVarCount;
		Set<String> ctxVars = null;
		if (getAssumeIndex() != -1 && t instanceof Abstraction) {
			ctxVars = new HashSet<String>();
			ctx.boundVars = new ArrayList<String>(ctx.boundVars);
			while (t instanceof Abstraction) {
				String varName = "<aVar" + ctx.boundVarCount + ">";
				ctx.boundVars.add(varName);
				ctxVars.add(varName);
				t = ((Abstraction)t).getBody();
				if (t instanceof Abstraction) {
					t = ((Abstraction)t).getBody();					
					ctx.boundVars.add("<aVar" + ctx.boundVarCount + "assumption>");
				}
				ctx.boundVarCount++;
			}
		}
		boolean prev = false;
		Iterator<? extends Term> termIter = null;
		if (t instanceof Application) {
			List<? extends Term> elemTerms = ((Application)t).getArguments();
			termIter = elemTerms.iterator();
		}
		for (int i = 0; i < elements.size(); ++i) {
			Element e = elements.get(i);
			if (prev)
				out.print(' ');
			if (e instanceof Clause)
				out.print('(');
			t = null;
			if (!(e instanceof Terminal) && !(e instanceof Variable) && termIter != null && termIter.hasNext() && i != getAssumeIndex())
				t = termIter.next();
			//System.err.println("type " + e.getClass().getName() + " for " + e + " with term " + t);
			if (i == getAssumeIndex()) {
				out.print(ctx.contextVarName);
				if (origT instanceof Abstraction)
					out.print("<expanded with vars:"+ ctxVars +">");
			}
			else
				e.prettyPrint(out, t == null? null : new PrintContext(t, ctx));
			if (e instanceof Clause)
				out.print(')');
			prev = true;
		}
		
		if (origBoundVars != null) {
			ctx.boundVars = origBoundVars;
			ctx.boundVarCount = origBoundVarCount;
		}
	}
}
