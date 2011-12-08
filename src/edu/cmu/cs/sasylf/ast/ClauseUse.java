package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.ast.Errors.EXPECTED_VARIABLE;
import static edu.cmu.cs.sasylf.ast.Errors.MISSING_ASSUMPTION_RULE;
import static edu.cmu.cs.sasylf.term.Facade.Abs;
import static edu.cmu.cs.sasylf.term.Facade.pair;
import static edu.cmu.cs.sasylf.util.Util.debug;
import static edu.cmu.cs.sasylf.util.Util.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.sasylf.grammar.Grammar;
import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.term.Pair;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;

public class ClauseUse extends Clause {
	public ClauseUse(Location loc, List<Element> elems, ClauseDef cd) {
		super(loc);
		elements = elems;
		cons = cd;
	}
	public ClauseUse(Clause copy, Map<List<ElemType>,ClauseDef> parseMap) {
		super(copy.getLocation());
		getElements().addAll(copy.getElements());

		List<ElemType> elemTypes = new ArrayList<ElemType>();
		for (int i = 0; i < getElements().size(); ++i) {
			Element e = getElements().get(i);
			if (e instanceof Clause) {
				Clause c = (Clause) e;
				ClauseUse cu = new ClauseUse(c, parseMap);
				getElements().set(i, cu);

				// must be an ElemType because can't be a judgment here
				ClauseType ct = cu.getConstructor().getType();
				if (ct instanceof Judgment)
					ErrorHandler.report("A judgment cannot appear inside a clause", copy);
				ElemType et = (ElemType) ct;
				elemTypes.add(et);
			} else {
				elemTypes.add(e.getElemType());
			}
		}

		ClauseDef cd = parseMap.get(elemTypes);
		if (cd == null)
			ErrorHandler.report("Cannot find a syntax constructor or judgment for expression "+ copy +" with elements " + elemTypes, copy);
		cons = cd;
	}

	@Override 
	public boolean equals(Object x) {
	  if (!(x instanceof ClauseUse)) return false;
	  ClauseUse cu = (ClauseUse)x;
	  return (cons == cu.cons && elements.equals(cu.getElements()));
	}
	
	public ClauseDef getConstructor() { return cons; }

	@Override
	public Term getTypeTerm() { return getConstructor().asTerm(); }

	private ClauseDef cons;
	
	
	@Override
  public Element computeClause(Context ctx, boolean inBinding, Grammar g) {
    return this; // already done
  }

  /** True iff assumptions environment is rooted in a variable */
	private NonTerminal root;
	public NonTerminal getRoot() { return root; }
	public boolean isRootedInVar() { return root != null; }

	@Override
	public ElemType getElemType() {
		ClauseType ct = getConstructor().getType();
		if (ct instanceof Syntax)
			return (Syntax) ct;
		else
			// not applicable--this is a judgment and does not have an ElemType
			throw new RuntimeException("should only call getElemTypes on syntax def clauses which don't have sub-clauses; can't call getElemType() on a Clause");
	}

	/* Computes a term for this ClauseUse, with no abstractions.
	 * Same as computeTerm but without abstractions.
	 */
	public Term getBaseTerm() {
		/* Note duplicated code in computeTerm() */
		int assumeIndex = cons.getAssumeIndex();
		List<Pair<String, Term>> varBindings = new ArrayList<Pair<String, Term>>();
		if (assumeIndex != -1) {
			root = getElements().get(assumeIndex).readAssumptions(varBindings, false);
		}

		return computeBasicTerm(varBindings, false);
	}

	// computes the main part of the term - without nested abstractions
	// inAssumption means we are computing a fake term for an assumption clause
	private Term computeBasicTerm(List<Pair<String, Term>> varBindings, boolean inAssumption) {
		int assumeIndex = cons.getAssumeIndex();
		Constant cnst = (Constant) cons.computeTerm(varBindings);
		List<Term> args = new ArrayList<Term>();
		debug("converting term " + this + " with assumed vars " + varBindings);
		for (int i = 0; i < getElements().size(); ++i) {
			Element e = getElements().get(i);
			if (! (e instanceof Terminal) && i != assumeIndex 
					&& !(cons.getElements().get(i) instanceof Variable)) {
				Element defE = cons.getElements().get(i);
				if (defE instanceof NonTerminal && ((NonTerminal)defE).getType().isInContextForm()) continue;
				Term t = null;
				if (defE instanceof Binding) {
					Binding defB = (Binding) defE;
					List<Variable> vars = new ArrayList<Variable>();
					List<Pair<String, Term>> newVarBindings = new ArrayList<Pair<String,Term>>(varBindings);
					// must add in opposite order because of the way de Bruijn works 
					for (int j = defB.getElements().size()-1; j >=0; --j) {
						Element boundVarElem = defB.getElements().get(j);
						int varIndex = cons.getIndexOf((Variable)boundVarElem);
						if (varIndex == -1)
							debug("could not find " + boundVarElem + " in clause " + cons
									+ "\n    context is " + this);
						Element varElement = getElements().get(varIndex);
						if (!(varElement instanceof Variable))
							ErrorHandler.report(EXPECTED_VARIABLE, "Expected variable matching " + boundVarElem + " but found the non-variable " + varElement, varElement);
						Variable localVar = (Variable) varElement;
						String localVarName = localVar.getSymbol();
						vars.add(localVar);
						newVarBindings.add(new Pair<String, Term>(localVarName, localVar.getType().typeTerm()));
					}
					//newVarBindings.addAll(varBindings); // TODO: infinite loop in unification if we do this BEFORE
					t = e.computeTerm(newVarBindings);
					for (Variable v : vars) {
						t = Abs(v.getSymbol(), v.getType().typeTerm(), t);
					}
				} else {
					if (inAssumption && e instanceof ClauseUse && ((ClauseUse)e).getConstructor().getType().equals(cons.getType()))
						t = Facade.FreshVar("AssumptionVar", Constant.UNKNOWN_TYPE);
					else
						t = e.computeTerm(varBindings);					
				}
				args.add(t);
			}
		}
		
		return (args.size() > 0) ? new Application(cnst, args) : cnst;
	}
	
	@Override
	public Term computeTerm(List<Pair<String, Term>> varBindings) {
		// ignore terminals
		// ignore variables
		// pass through by default
		// if defE was a binding:
		//	get position of each var in binding
		//	look up local name for var using position, add local names to varBindings
		//	compute child value with new var bindings
		//	wrap child in abstraction

		/* Note duplicated code in getBaseTerm() */
		int assumeIndex = cons.getAssumeIndex();
		int initialBindingsSize = varBindings.size();
		if (assumeIndex != -1) {
			verify(varBindings.size() == 0, "assume rule with nonempty var bindings");
			root = getElements().get(assumeIndex).readAssumptions(varBindings, true);
		}

		Term t = computeBasicTerm(varBindings, false);
		
		if (assumeIndex != -1) {
		  t = newWrap(t,varBindings,initialBindingsSize);
		}
		debug("    conversion result is " + t);
		//System.out.println("converted " + this + " to " + t);
		return t;
	}
	
	/**
	 * Called when checking a syntax case.
	 * @return list of nonterminals in something we are case analyzing
	 */
	public List<Fact> getNonTerminals() {
		int assumeIndex = getConstructor().getAssumeIndex();
		List<Fact> facts = new ArrayList<Fact>();
		// JTB: TODO: This method is poorly named,
		// and it looks at assumeIndex which will never be define for syntax.
		if (assumeIndex != -1) {
		  System.out.println("assumeIndex = " + assumeIndex);
		}
		for (int i = 0; i < getElements().size(); ++i) {
			Element e = getElements().get(i);
			if (! (e instanceof Terminal) && i != assumeIndex 
					&& !(e instanceof Variable)) {
				if (e instanceof Binding) {
          facts.add(new BindingAssumption((Binding)e,new Clause(e.getLocation())));					
				} else if (e instanceof NonTerminal){
					facts.add(new SyntaxAssumption((NonTerminal)e,new Clause(e.getLocation())));
				} else if (e instanceof Clause) {
					throw new RuntimeException("should be impossible case");
				} else {
					throw new RuntimeException("should be impossible case");
				}
			}
		}
		return facts;
	}

	/** For ClauseUse, checks that this is an assumption list and adds
	 * assumptions to varBindings and assumedVars.  For varBindings we
	 * only add actual variables, but for assumed vars we also add a variable
	 * for the derivation represented by the hypothetical judgment.
	 * 
	 * TODO: not sure, may need to add it to both
	 * 
	 * For other elements, does nothing.
	 * 
	 * Returns non-null if the innermost assumption is a NonTerminal (and returns that NonTerminal).
	 */
	@Override
	NonTerminal readAssumptions(List<Pair<String, Term>> varBindings, boolean includeAssumptionTerm) {
		// should have zero to one recursive ClauseUse of the same type - call recursively
		boolean foundClause = false;
		for (Element e : getElements()) {
			if (e instanceof ClauseUse && ((ClauseUse)e).getConstructor().getType().equals(cons.getType())) {
				if (foundClause)
					ErrorHandler.report("An assumption case must not have more than one nested list of assumptions", cons);
				foundClause = true;
				root = e.readAssumptions(varBindings, includeAssumptionTerm);
			} else if (e instanceof NonTerminal && ((NonTerminal)e).getType().equals(cons.getType())) {
				if (foundClause)
					ErrorHandler.report("An assumption case must not have more than one nested list of assumptions", cons);
				foundClause = true;
				root = (NonTerminal) e;
			}
		}

		int varIndex = cons.getVariableIndex();

		// Previously: this code would look for variables in the elements, but a variable
		// appearing was not necessary in a binding occurrence, e.g.  X <: X2
		// where X is being bound but X2 is a USE of an existing variable.
		// So we can't just loop through the USE looking for variables, we need to go to the place
		// where the clause definition says variables should appear.
    if (varIndex >= 0) {
      Element e = getElements().get(varIndex);
      if (!(e instanceof Variable)) {
        ErrorHandler.report(Errors.EXPECTED_VARIABLE,this," (found " + e + ")");
      } else {
				Variable v = (Variable) e;
				
				/* Here we implement hypothetical judgments
				 * Look up the var rule for the ClauseDef and get its conclusion
				 * Transform the conclusion into a term (w/o abstractions)
				 * Cut out the outermost abstraction in the term (so the var should bind to v now)
				 * Hopefully, that's our term!
				 */
				String derivSym = "INTERNAL_DERIV_" + v.getSymbol();
				if (cons.assumptionRule == null) {
					ErrorHandler.report(MISSING_ASSUMPTION_RULE, "There's no rule for using an assumption of the form " + cons, cons);
				}
				ClauseUse varRuleConc = (ClauseUse) cons.assumptionRule.getConclusion();
				Term derivTerm = includeAssumptionTerm ? varRuleConc.getBaseTerm() : null;

				// Adapt derivTerm to the particular form of the assumption clause used here
				Term myClauseTerm = computeBasicTerm(varBindings, true);
				ClauseUse varRuleConcAssumption = (ClauseUse) varRuleConc.getElements().get(varRuleConc.cons.getAssumeIndex());
				Term ruleClauseTerm = varRuleConcAssumption.getBaseTerm(); // need to call computeBasicTerm instead with true arg?
				Substitution ruleFreshSub = new Substitution();
				ruleClauseTerm.freshSubstitution(ruleFreshSub);
				ruleClauseTerm = ruleClauseTerm.substitute(ruleFreshSub);
				Substitution bindingSub = new Substitution();
				List<Term> varTypes = new ArrayList<Term>();
				for (Pair<String, Term> p : varBindings)
					varTypes.add(p.second);
				debug("varTypes = "+ varTypes);
				ruleClauseTerm.bindInFreeVars(varTypes, bindingSub, 1);
				ruleClauseTerm = ruleClauseTerm.substitute(bindingSub);
				debug("unifying terms " + myClauseTerm + " and " + ruleClauseTerm + " from " + this + " and " + varRuleConcAssumption);
				Substitution adaptationSub = myClauseTerm.unifyAllowingBVs(ruleClauseTerm);
				// transform adaptationSub to adapt from ruleClauseTerm to myClauseTerm
				adaptationSub.avoid(myClauseTerm.getFreeVariables());
				debug("adaptationSub = "+ adaptationSub);
				if (derivTerm != null) {
					debug("\torig   derivTerm = " + derivTerm);
					derivTerm.freshSubstitution(ruleFreshSub);
					derivTerm = derivTerm.substitute(ruleFreshSub);
					bindingSub.incrFreeDeBruijn(2);
					derivTerm = derivTerm.substitute(bindingSub);
					debug("\tmiddle derivTerm = " + derivTerm);
					derivTerm = derivTerm.substitute(adaptationSub);
					derivTerm = derivTerm.incrFreeDeBruijn(-1);
				}
				debug("\tresult derivTerm = " + derivTerm);
				
				varBindings.add(pair(v.getSymbol(), (Term) v.getType().typeTerm()));
				/* v2 */ varBindings.add(pair(derivSym, derivTerm));
      }
		}
		
		return root;
	}
	
	/** If environment is a variable, add lambdas to term to make it match matchTerm.
	 * Modifies the substitution to reflect changes.
	 * Also changes free variables so they bind new the new bound variables in them
	 */
	@Override
	public Term adaptTermTo(Term term, Term matchTerm, Substitution sub) {
		return adaptTermTo(term, matchTerm, sub, false);
	}

	public Term adaptTermTo(Term term, Term matchTerm, Substitution sub, boolean wrapUnrooted) {
		Term result = wrapWithOuterLambdas(term, matchTerm, getAdaptationNumber(term, matchTerm, wrapUnrooted), sub, wrapUnrooted);
		debug("adapation of " + term + " to " + result + " with sub " + sub + "\n\tsub applied is: " + term.substitute(sub));
		return result;
	}

	/** If environment is a variable, return the number of lambdas that must be added to term to make it match matchTerm */
	public int getAdaptationNumber(Term term, Term matchTerm, boolean wrapUnrooted) {
		if (isRootedInVar() || wrapUnrooted) {
			int termLambdas = term.countLambdas();
			int matchLambdas = matchTerm.countLambdas();
			if (termLambdas >= matchLambdas)
				return 0;
			return matchLambdas - termLambdas;
		} else
			return 0;
	}

	/** Wraps term in i lambdas that match the types of matchTerm.
	 * Also changes free variables so they bind new the new bound variables in them
	 * This version is for clients who don't need to track the resulting substitution.
	 */
	public Term wrapWithOuterLambdas(Term term, Term matchTerm, int i) {
		return wrapWithOuterLambdas(term, matchTerm, i, new Substitution(), false);
	}

	/** Wraps term in i lambdas that match the types of matchTerm.
	 * Also changes free variables so they bind new the new bound variables in them
	 * Modifies the substitution to reflect changes.
	 */
	public Term wrapWithOuterLambdas(Term term, Term matchTerm, int i, Substitution sub, boolean wrapUnrooted) {
		if (i == 0 || (!isRootedInVar() && !wrapUnrooted))
			return term;
		return wrapWithOuterLambdas(term, matchTerm, i, sub);
	}
	
  /**
   * Wrap a term with lambdas to match the match term, up to i variables.
   * We substitute variables that could depend on the term.
   * @param term term to wrap
   * @param matchTerm term to get variables from
   * @param i number of variables to get
   * @param sub substitution used and modified, to hold substitution with paranmeterized variables
   * @return wrapped term
   */
  public static Term wrapWithOuterLambdas(Term term, Term matchTerm, int i,
      Substitution sub) {
    if (i == 0) return term;
    Abstraction absMatchTerm = (Abstraction) matchTerm;
		List<Term> varTypes = new ArrayList<Term>();
		List<String> varNames = new ArrayList<String>();
		
		readNamesAndTypes(absMatchTerm, i, varNames, varTypes);

		return doWrap(term, varNames, varTypes, sub);
  }
  
	/**
	 * Reads the names and types of the i lambdas on the outside of absMatchTerm, and adds them to varNames and varTypes
	 */
	public static void readNamesAndTypes(Abstraction absMatchTerm, int i, List<String> varNames, List<Term> varTypes) {
		// determine how to wrap
		for (int j = 0; j < i; ++j) {
			varTypes.add(absMatchTerm.varType);
			varNames.add(absMatchTerm.varName);
			if (j < i-1)
				absMatchTerm = (Abstraction) absMatchTerm.getBody();
		}
	}

	/**
	 * Wrap a generated term in bindings from the list
	 * @param t term to wrap
	 * @param varBindings bindings to use
	 * @param from skip this many from the front of the list
	 * @return wrapped term
	 */
	public static Term newWrap(Term t, List<Pair<String,Term>> varBindings, int from) {
    for (int i = varBindings.size()-1; i >= from; --i) {
      t = Abs(varBindings.get(i).first, varBindings.get(i).second, t);
    }
    return t;
	}
	
	public static Term newDoBindWrap(Term term, List<Pair<String,Term>> varBindings) {
	  List<String> varNames = new ArrayList<String>();
	  List<Term> varTypes = new ArrayList<Term>();
	  Substitution sub = new Substitution();
	  for (Pair<String,Term> pair : varBindings) {
	    varNames.add(pair.first);
	    varTypes.add(pair.second);
	  }
	  return doWrap(term,varNames,varTypes,sub);
	}
	
	/** Wraps term in i lambdas that have variable names and types given by varNames and varTypes.
	 * Also changes free variables so they bind new the new bound variables in them
	 * Modifies the substitution to reflect changes.
	 */
	public static Term doWrap(Term term, List<String> varNames, List<Term> varTypes, Substitution sub) {

		// bind in free vars
		debug("before binding in free vars: " + term.substitute(sub) + " with types " + varTypes);
		term.bindInFreeVars(varTypes, sub, 1);
		term = term.substitute(sub);
		/*for (int j = i-1; j >= 0; --j) {
			term.bindInFreeVars(varTypes.get(j), sub, i-j);
			term = term.substitute(sub);
		}*/
		debug("after binding in free vars: " + term + " with sub " + sub);

		// do the wrapping
		for (int j = varNames.size()-1; j >= 0; --j) {
			term = Abstraction.make(varNames.get(j), varTypes.get(j), term);
		}

		return term;
	}

	/** Wraps term in i lambdas that match the types of matchTerm.
	 * Also changes free variables so they bind new the new bound variables in them
	 * Modifies the substitution to reflect changes.
	 */
	public Term oldWrapWithOuterLambdas(Term term, Term matchTerm, int i, Substitution sub) {
		if (i == 0 || !isRootedInVar())
			return term;
		Abstraction absMatchTerm = (Abstraction) matchTerm;
		term = oldWrapWithOuterLambdas(term, absMatchTerm.getBody(), i-1, sub);
		debug("before binding in free vars: " + term.substitute(sub));
		term.bindInFreeVars(absMatchTerm.varType, sub);
		term = term.substitute(sub);
		debug("after binding in free vars: " + term + " with sub " + sub);
		return Abstraction.make(absMatchTerm.varName, absMatchTerm.varType, term);
	}
	
}
