package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.term.Facade.Abs;
import static edu.cmu.cs.sasylf.term.Facade.pair;
import static edu.cmu.cs.sasylf.util.Errors.EXPECTED_VARIABLE;
import static edu.cmu.cs.sasylf.util.Errors.MISSING_ASSUMPTION_RULE;
import static edu.cmu.cs.sasylf.util.Util.debug;
import static edu.cmu.cs.sasylf.util.Util.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.grammar.Grammar;
import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.BoundVar;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Util;

public class ClauseUse extends Clause {
	public ClauseUse(Location loc, List<Element> elems, ClauseDef cd) {
		super(loc);
		elements = elems;
		cons = cd;
		root = computeRoot();
		if (!elems.isEmpty()) {
			super.setEndLocation(elems.get(elems.size()-1).getEndLocation());
		}
	}
	/*
	public ClauseUse(Clause copy, Map<List<ElemType>,ClauseDef> parseMap) {
		super(copy.getLocation());
		setEndLocation(copy.getEndLocation());
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
				if (ct instanceof Judgment) {
					Util.verify(false,"A judgment cannot appear inside a clause");
					// ErrorHandler.error("A judgment cannot appear inside a clause", copy);
				}
				ElemType et = (ElemType) ct;
				elemTypes.add(et);
			} else {
				elemTypes.add(e.getElemType());
			}
		}

		ClauseDef cd = parseMap.get(elemTypes);
		if (cd == null)
			ErrorHandler.error("Cannot find a syntax constructor or judgment for expression "+ copy +" with elements " + elemTypes, copy);
		cons = cd;
		root = computeRoot();
	}
	*/

	@Override 
	public boolean equals(Object x) {
		if (!(x instanceof ClauseUse)) return false;
		ClauseUse cu = (ClauseUse)x;
		return (cons == cu.cons && elements.equals(cu.getElements()));
	}

	public ClauseDef getConstructor() { return cons; }

	@Override
	public ClauseType getType() { return cons.getType(); }

	@Override
	public Term getTypeTerm() { return getConstructor().asTerm(); }

	private ClauseDef cons;


	@Override
	public Clause typecheck(Context c) {
		return this; // already done
	}

	@Override
	void checkVariables(Set<String> bound, boolean defining) {
		// we want to handle cases like:
		//   Gamma, x:(fn X => T[X])
		//   Gamma, x':T'' |- (fn x : T => t[x][x']) : T -> T'
		int ai = cons.getAssumeIndex();
		boolean copied = false;
		// System.out.println("(a) Checking " + this + ":" + getClass() + " with ai=" + ai + " defining? " + defining + ", bound = " + bound);
		for (int i=0; i < elements.size(); ++i) {
			if (i == ai || cons.getElements().get(i) instanceof Variable) {
				if (!copied && !defining) bound = new HashSet<String>(bound);
				elements.get(i).checkVariables(bound, true);
			}
		}
		// System.out.println("(b) Checking " + this + ":" + getClass() + " with ai=" + ai + " defining? " + defining + ", bound = " + bound);
		for (int i=0; i < elements.size(); ++i) {
			if (i != ai) {
				elements.get(i).checkVariables(bound, false);
			}
		}
	}
	
	@Override
	protected Element computeClause(Context ctx, boolean inBinding, Grammar g, NonTerminal nt) {
		return this; // already done
	}
	
	@Override
	public Fact asFact(Context ctx, Element assumes) {
		Element localAssumes = null;
		// accept assumes only if we have something that the context can affect.
		if (assumes != null && !ctx.isVarFree(this)) {
			SyntaxDeclaration contextSyntax = (SyntaxDeclaration)assumes.getType();
			Term myType = getType().typeTerm();
			if (contextSyntax.canAppearIn(myType)) {
				localAssumes = assumes;
			}
		}
		return new ClauseAssumption(this,getLocation(),localAssumes);
	}

	/**
	 * Return the assumptions/context for this instance of a judgment.
	 * @return null if no assumption, a non-terminal if no local bindings,
	 * a clause use if assumption has local bindings, or no bindings. 
	 */
	public Element getAssumes() {
		int ai = cons.getAssumeIndex();
		if (ai < 0) return null;
		return getElements().get(ai);
	}
	
	/**
	 * Return the clause use that has an instance of the given nonterminal.
	 * We only look in "Assumes" locations.
	 * @param nt an assumed nonterminal
	 * @return clause that contains the nonterminal, or null if it can't be found
	 */
	public ClauseUse getAssumesContaining(NonTerminal nt) {
		int ai = cons.getAssumeIndex();
		if (ai < 0) return null;
		Element e = getElements().get(ai);
		if (e.equals(nt)) return this;
		if (e instanceof ClauseUse) return ((ClauseUse)e).getAssumesContaining(nt);
		return null;
	}

	/** True iff assumptions environment is rooted in a variable */
	private NonTerminal root;
	//private boolean hasBindings;
	@Override
	public NonTerminal getRoot() { return root; }
	public boolean isRootedInVar() { return root != null; }
	/** Whether there are variable bindings -- not in use currently */
	// public boolean hasBindings() { return hasBindings; }

	private NonTerminal computeRoot() {
		int ai = cons.getAssumeIndex();
		if (ai < 0) {
			return null;
		}
		if (ai >= getElements().size()) {
			System.out.println("Cannot find ai(" + ai + ") in " + getElements());
		}
		Element e = getElements().get(ai);
		if (e == null) {
			System.out.println("null root in " + getElements() + " at " + ai);
		}
		return computeRootHelper(e);
	}

	private NonTerminal computeRootHelper(Element e) {
		if (e instanceof NonTerminal) return (NonTerminal)e;
		if (e instanceof Terminal) return null;
		if (e instanceof Variable) return null; // syntax binder
		if (e instanceof ClauseUse) {
			for (Element ep : ((ClauseUse) e).getElements()) {
				if (ep.getType().equals(e.getType())) {
					//XXX will need to revisit (of course) if we permit context concatenation
					//hasBindings = true;
					return computeRootHelper(ep);
				}
			}
			return null;
		}
		ErrorHandler.error(Errors.INTERNAL_ERROR, "Internal Error: no root in clause", e);
		throw new RuntimeException("Internal Error");
	}

	@Override
	public ElemType getElemType() {
		ClauseType ct = getConstructor().getType();
		if (ct instanceof SyntaxDeclaration)
			return (SyntaxDeclaration) ct;
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
		Term cnst = cons.asTerm();
		List<Term> args = new ArrayList<Term>();
		// System.out.println("converting term " + this + " with assumed vars " + varBindings);
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
					// 1. varBindings must be added in the order they are bound
					//    earlier (outer) bindings come earlier
					// but when we generate Abs terms, we need to bind later things first.
					// Hence the list reversal of vars.
					// NB: The order of variables in the Binding of the ClauseDef is definitive.
					for (Element boundVarElem : defB.getElements()) {
						int varIndex = cons.getIndexOf((Variable)boundVarElem);
						if (varIndex == -1)
							debug("could not find ", boundVarElem, " in clause ", cons,
									"\n    context is ", this);
						Element varElement = getElements().get(varIndex);
						if (!(varElement instanceof Variable))
							ErrorHandler.error(EXPECTED_VARIABLE, varElement);
						Variable localVar = (Variable) varElement;
						String localVarName = localVar.getSymbol();
						vars.add(localVar);
						newVarBindings.add(new Pair<String, Term>(localVarName, localVar.getType().typeTerm()));
					}
					//newVarBindings.addAll(varBindings); // TODO: infinite loop in unification if we do this BEFORE
					t = e.computeTerm(newVarBindings);
					Collections.reverse(vars); // to get proper order.
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

		return (args.size() > 0) ? Facade.App(cnst, args) : cnst;
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
		// System.out.println("converted " + this + " to " + t);
		return t;
	}

	public boolean hasVariables() {
		int ai = cons.getAssumeIndex();
		if (ai < 0) return false;
		Element e = getElements().get(ai);
		return !(e instanceof NonTerminal);
	}

	/**
	 * Called when checking a syntax case.
	 * @param assumes TODO
	 * @return list of nonterminals in something we are case analyzing
	 */
	public List<SyntaxAssumption> getNonTerminals(Context ctx, Element assumes) {
		int assumeIndex = getConstructor().getAssumeIndex();
		List<SyntaxAssumption> facts = new ArrayList<SyntaxAssumption>();
		// JTB: TODO: This method is poorly named,
		// and it looks at assumeIndex which will never be defined for (normal) syntax.
		if (assumeIndex != -1) {
			throw new RuntimeException("assumeIndex on a syntax clause? " + this);
		}
		for (int i = 0; i < getElements().size(); ++i) {
			Element e = getElements().get(i);
			if (e instanceof Terminal) continue;
			if (e instanceof Variable) continue;
			if (e instanceof Clause) {
				throw new RuntimeException("syntax cases shouldn't have nested syntax: " + this);
			}
			Fact f;
			if (e instanceof Binding) {
				Binding b = (Binding)e;
				Element context = assumes;
				for (Element sube : b.getElements()) {
					context = ((Variable)sube).genContext(context, ctx);
				}
				f = e.asFact(ctx, context);
			} else {
				f = e.asFact(ctx, assumes);
			}
			facts.add((SyntaxAssumption)f);
		}
		return facts;
	}

	/** For ClauseUse, checks that this is an assumption list and adds
	 * assumptions to varBindings and assumedVars.  For varBindings we
	 * only add actual variables, but for assumed vars we also add a variable
	 * for the derivation represented by the hypothetical judgment.
	 * For other elements, does nothing.
	 * 
	 * Returns non-null if the innermost assumption is a NonTerminal (and returns that NonTerminal).
	 */
	@Override
	NonTerminal readAssumptions(List<Pair<String, Term>> varBindings, boolean includeAssumptionTerm) {
		// should have zero to one recursive ClauseUse of the same type - call recursively
		// XXX: If we add context append, will need to change this code.
		boolean foundClause = false;
		for (Element e : getElements()) {
			if (e instanceof ClauseUse && ((ClauseUse)e).getConstructor().getType().equals(cons.getType())) {
				Util.verify(!foundClause, "branching in assumption");
				foundClause = true;
				root = e.readAssumptions(varBindings, includeAssumptionTerm);
			} else if (e instanceof NonTerminal && ((NonTerminal)e).getType().equals(cons.getType())) {
				Util.verify(!foundClause, "branching in assumption");
				foundClause = true;
				root = (NonTerminal) e;
			}
		}

		int varIndex = cons.getVariableIndex();

		// TODO: rewrite this code to handle non-variable assumptions too.  (If we add them)

		// Previously: this code would look for variables in the elements, but a variable
		// appearing was not necessary in a binding occurrence, e.g.  X <: X2
		// where X is being bound but X2 is a USE of an existing variable.
		// So we can't just loop through the USE looking for variables, we need to go to the place
		// where the clause definition says variables should appear.
		if (varIndex >= 0) {
			Element e = getElements().get(varIndex);
			if (!(e instanceof Variable)) {
				ErrorHandler.error(Errors.EXPECTED_VARIABLE,e);
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
					ErrorHandler.error(MISSING_ASSUMPTION_RULE, "" + cons, this);
				}
				ClauseUse varRuleConc = (ClauseUse) cons.assumptionRule.getConclusion();
				Term derivTerm = includeAssumptionTerm ? varRuleConc.getBaseTerm() : null;

				Util.debug("getting derivTerm for " + this);
				// Adapt derivTerm to the particular form of the assumption clause used here
				Term myClauseTerm = computeBasicTerm(varBindings, true);
				Util.debug("myClauseTerm = ",myClauseTerm);
				ClauseUse varRuleConcAssumption = (ClauseUse) varRuleConc.getElements().get(varRuleConc.cons.getAssumeIndex());
				Term ruleClauseTerm = varRuleConcAssumption.getBaseTerm(); // need to call computeBasicTerm instead with true arg?
				Substitution ruleFreshSub = new Substitution();
				ruleClauseTerm.freshSubstitution(ruleFreshSub);
				ruleClauseTerm = ruleClauseTerm.substitute(ruleFreshSub);
				Substitution bindingSub = new Substitution();
				List<Term> varTypes = new ArrayList<Term>();
				for (Pair<String, Term> p : varBindings)
					varTypes.add(p.second);
				Util.debug("varTypes = ", varTypes);
				Util.debug("ruleClauseTerm = ",ruleClauseTerm);
				ruleClauseTerm.bindInFreeVars(varTypes, bindingSub);
				ruleClauseTerm = ruleClauseTerm.substitute(bindingSub);
				Util.debug("unifying terms ", myClauseTerm, " and ", ruleClauseTerm, " from ", this, " and ", varRuleConcAssumption);
				Substitution adaptationSub = myClauseTerm.unify(ruleClauseTerm);
				// transform adaptationSub to adapt from ruleClauseTerm to myClauseTerm
				adaptationSub.avoid(myClauseTerm.getFreeVariables());
				Util.debug("adaptationSub = ", adaptationSub);
				Util.debug("\tand bindingSub = ", bindingSub);
				if (derivTerm != null) {
					Util.debug("\torig   derivTerm = ", derivTerm);
					derivTerm.freshSubstitution(ruleFreshSub);
					derivTerm = derivTerm.substitute(ruleFreshSub);
					bindingSub.incrFreeDeBruijn(1); //XXX: This is unclear (JTB)
					derivTerm = derivTerm.substitute(bindingSub);
					Util.debug("\tmiddle derivTerm = ", derivTerm);
					derivTerm = derivTerm.substitute(adaptationSub);
					// derivTerm = derivTerm.incrFreeDeBruijn(-1);  <-- KLUDGE no longer needed since this method corrected
				}
				Util.debug("\tresult derivTerm = ", derivTerm);

				varBindings.add(pair(v.getSymbol(), (Term) v.getType().typeTerm()));
				if (derivTerm != null) {
					Util.verify(includeAssumptionTerm, "assumoption term wasn't supposed to exist!");
					/* v2 */ varBindings.add(pair(derivSym, derivTerm));
				}
			}
		}

		return root;
	}

	/** If environment is a variable, add lambdas to term to make it match matchTerm.
	 * Modifies the substitution to reflect changes.
	 * Also changes free variables so they bind new the new bound variables in them
	 * @deprecated
	 */
	@Deprecated
	@Override
	public Term adaptTermTo(Term term, Term matchTerm, Substitution sub) {
		return adaptTermTo(term, matchTerm, sub, false);
	}

	/**
	 * @deprecated
	 * @param term
	 * @param matchTerm
	 * @param sub
	 * @param wrapUnrooted
	 * @return
	 */
	@Deprecated
	public Term adaptTermTo(Term term, Term matchTerm, Substitution sub, boolean wrapUnrooted) {
		Term result = wrapWithOuterLambdas(term, matchTerm, getAdaptationNumber(term, matchTerm, wrapUnrooted), sub, wrapUnrooted);
		debug("adapation of ", term, " to ", result, " with sub ", sub, "\n\tsub applied is: ", term.substitute(sub));
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

		readNamesAndTypes(absMatchTerm, i, varNames, varTypes, null); // pass in null, because doWrap checks

		return doWrap(term, varNames, varTypes, sub);
	}

	/**
	 * Reads the names and types of the i lambdas on the outside of absMatchTerm, and adds them to varNames and varTypes.
	 * XXX: This code is a mess: we have two ways to avoid non-subordinate types, and clients make use of different ones.
	 * @param base TODO
	 */
	public static void readNamesAndTypes(Abstraction absMatchTerm, int i, List<String> varNames, List<Term> varTypes, Term base) {
		// determine how to wrap
		Constant ty = base == null ? null : base.getTypeFamily();
		for (int j = 0; j < i; ++j) {
			if (ty == null || FreeVar.canAppearIn(absMatchTerm.varType.baseTypeFamily(), ty)) {
				varTypes.add(absMatchTerm.varType);
				varNames.add(absMatchTerm.varName);
			} else {
				Util.debug("Skipping dependency on variable ", absMatchTerm.varName, " for ", base);
			}
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
		debug("before binding in free vars: ", term, " with types ", varTypes);
		term.bindInFreeVars(varTypes, sub);
		term = term.substitute(sub);
		/*for (int j = i-1; j >= 0; --j) {
			term.bindInFreeVars(varTypes.get(j), sub, i-j);
			term = term.substitute(sub);
		}*/
		debug("after binding in free vars: ", term, " with sub ", sub);

		// do the wrapping
		Constant typeFamily = term.getTypeFamily();
		for (int j = varNames.size()-1; j >= 0; --j) {
			Constant varFamily = varTypes.get(j).baseTypeFamily();
			if (FreeVar.canAppearIn(varFamily, typeFamily)) {
				term = Abstraction.make(varNames.get(j), varTypes.get(j), term);
			} else if (term.equals(new BoundVar(1))) {
				term = Abstraction.make(varNames.get(j), varTypes.get(j), term);
				typeFamily = varFamily;
			} else {
				Util.debug("Skipping ", varNames.get(j), " in ", term);
				term = term.incrFreeDeBruijn(-1);
				Util.debug("  term is now ", term);
			}
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
		debug("before binding in free vars: ", term.substitute(sub));
		term.bindInFreeVars(absMatchTerm.varType, sub);
		term = term.substitute(sub);
		debug("after binding in free vars: ", term, " with sub ", sub);
		return Abstraction.make(absMatchTerm.varName, absMatchTerm.varType, term);
	}

}
