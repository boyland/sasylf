package edu.cmu.cs.sasylf.term;

import static edu.cmu.cs.sasylf.util.Util.debug;
import static edu.cmu.cs.sasylf.util.Util.debug2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Util;

/**
 * A Term has concrete transitive subclasses Constant, Abstraction,
 * Application, BoundVar, and FreeVar.
 */

public abstract class Term {
	// only for free variables
	public final Substitution freshSubstitution(Substitution s) {
		Set<FreeVar> vars = getFreeVariables();
		debug("free vars for freshening: ", vars);
		for (FreeVar v : vars) {
			if (!s.getMap().keySet().contains(v)) {
				FreeVar vfresh = v.freshify();
				debug("freshened ", v, " with ", vfresh);
				s.add(v, vfresh);
			}
		}
		return s;
	}

	/*public final Substitution instanceOfOld(Term t) {
		Set<FreeVar> freeVars = getFreeVariables();
		Substitution subFreeVars = new Substitution();
		Substitution inverseFreeVars = new Substitution();
		for (FreeVar v : freeVars) {
			Constant c = new Constant("Const" + constId++ + "_for_" + v, v.getType());
			subFreeVars.add(v, c);
			inverseFreeVars.add(c,v);
		}
		Term thisWithoutFreeVars = this.substitute(subFreeVars);
		Substitution unifyingSub = thisWithoutFreeVars.unify(t);
		Substitution result = new Substitution();
		for (Atom a : unifyingSub.getMap().keySet()) {
			debug2("instanceOf: mapping atom " + a + " to " + unifyingSub.getMap().get(a) + " under " + inverseFreeVars);
			result.add(a, unifyingSub.getMap().get(a).substitute(inverseFreeVars));
		}
		//Substitution result = unifyingSub.compose(inverseFreeVars);
		
		// verify that domain of substitution has no free variables of this
		/*freeVars.retainAll(result.getMap().keySet());
		if (!freeVars.isEmpty()) {
			FreeVar var = freeVars.iterator().next();
			throw new UnificationFailed("variable " + var + " is not an instance of " + result.getMap().get(var) + " because " + , var, result.getMap().get(var));
		}*/
		/*
		return result;
	}*/
	
	public final Substitution instanceOf(Term t) {
		Set<FreeVar> freeVars = getFreeVariables();
		Substitution unifyingSub = unify(t);
		if (unifyingSub.avoid(freeVars))
			return unifyingSub;
		else
			throw new UnificationFailed("terms unify but the instance relationship does not hold");
	}
	
	//static int constId = 0;
	
	/** FreeVar order 0
	 * Application of FreeVar order 1
	 * others order 2
	 */
	int getOrder() { return 2; }

	/** returns 1 if this is a non-pattern free variable application, otherwise 0 */
	final int oneIfNonPatFreeVarApp() { return isNonPatFreeVarApp() ? 1 : 0; }
	/** returns 1 if this is a non-pattern free variable application, otherwise 0 */
	final int oneIfNonPatFreeVarApp(Term other) { return isNonPatFreeVarApp(other) ? 1 : 0; }
	
	/** true if this is a non-pattern free variable application, otherwise false */
	boolean isNonPatFreeVarApp() { return false; }
	boolean isNonPatFreeVarApp(Term other) { return false; }

	/** puts non-pattern free variable applications last */
	//compare in order of pair elements, put in opposite order of terms
	static class PairComparator implements Comparator<Pair<Term,Term>> {
		public int compare(Pair<Term, Term> t1, Pair<Term, Term> t2) {
			return toPriority(t1) - toPriority(t2);
		}
    /**
     * Compute priority of a pair.
     * 1 point for each non-pattern free application.
     * Previously, max was 1.  But we want to make sure that
     * T2 =?= T22[T23] has higher priority than T1[T2] =?= T1[T22[T23]]
     * Eventually, we may want to count "bad" applications. 
     * @param t1 pair to evaluate
     * @return priority (greater is lower priority...)
     */
    protected int toPriority(Pair<Term, Term> t1) {
      return t1.first.oneIfNonPatFreeVarApp() + t1.second.oneIfNonPatFreeVarApp();
    }
	}
	/** constructs a pair with the terms in order
	 */
	static Pair<Term, Term> makePair(Term t1, Term t2) {
		debug("    pair ", t1, " order ", t1.getOrder());
		debug("    pair ", t2, " order ", t2.getOrder());
		if (t1.getOrder() < t2.getOrder())
			return new Pair<Term,Term>(t1, t2);
		else 
			return new Pair<Term,Term>(t2, t1);
	}
	
	// TODO: deprecate - I think no-one should call this
	// XXX: ClauseUse calls it.
	public final Substitution unifyAllowingBVs(Term t) {
		Substitution current = new Substitution();
		Queue<Pair<Term,Term>> worklist = new PriorityQueue<Pair<Term,Term>>(11, new PairComparator());
		worklist.add(makePair(this, t));
		debugCount = 0;
		unifyHelper(current, worklist);
		
		return current;
	}
	
	/** Sets up worklist and calls unifyHelper
	 */
	public final Substitution unify(Term t) {
	  Substitution current;
	  try {
	    current = unifyAllowingBVs(t);
	  } catch (UnificationIncomplete ex) {
	    Util.debug("Was trying to unify ",this," and ", t);
	    throw ex;
	  }
		// a free variable in the input to unify() should not, in its substitution result, have any free bound variables
		Set<FreeVar> freeVars = getFreeVariables();
		freeVars.addAll(t.getFreeVariables());
    Set<Pair<FreeVar,Integer>> unusable = new HashSet<Pair<FreeVar,Integer>>();
		for (FreeVar v : freeVars) {
			Term substituted = current.getSubstituted(v);
			if (substituted != null && !substituted.selectUnusablePositions(0, unusable)) {
				debug("Could not eliminate bound variables from substitution ", substituted, " for var ", v);
				throw new UnificationFailed("illegal variable binding in result: " + substituted + " for " + v + "\n" + current);
			}
		}
		// The following code is suspect; it isn't clear that it is sound.
		if (!unusable.isEmpty()) {
		  // restructure set as map:
		  Map<FreeVar,Set<Integer>> map = new HashMap<FreeVar,Set<Integer>>();
		  for (Pair<FreeVar,Integer> p : unusable) {
		    if (!map.containsKey(p.first)) {
		      map.put(p.first, new HashSet<Integer>());
		    }
		    map.get(p.first).add(p.second);
		  }
		  for (Map.Entry<FreeVar, Set<Integer>> replace : map.entrySet()) {
		    Util.debug("need to replace: ",replace);
		    FreeVar v = replace.getKey();
		    Term type = v.getType();
		    List<Term> oldTypes = new ArrayList<Term>();
		    List<String> oldNames = new ArrayList<String>();
		    List<Term> newTypes = new ArrayList<Term>();
		    List<String> newNames = new ArrayList<String>();
		    int i=0;
		    while (type instanceof Abstraction) {
		      Abstraction f = (Abstraction)type;
		      oldTypes.add(f.varType);
		      oldNames.add(f.varName);
		      if (replace.getValue().contains(i)) {
	          //XXX we assume types on other parameters don't depend on removed positions
		        type = f.getBody().incrFreeDeBruijn(-1);
		      } else {
		        newTypes.add(f.varType);
		        newNames.add(f.varName);
		        type = f.getBody();
		      }
		      ++i;
		    }
		    Term newType = wrapWithLambdas(type, newTypes, newNames);
        FreeVar newV = FreeVar.fresh(v.getName(), newType);
        Term replacement;
        if (newTypes.isEmpty()) replacement = newV;
        else {
          List<Term> newArgs = new ArrayList<Term>();
          int n = newTypes.size();
          for (int j=0; j < n; ++j) {
            newArgs.add(new BoundVar(n-j));
          }
          replacement = Facade.App(newV, newArgs);
        }
        current.add(v, wrapWithLambdas(replacement, oldTypes, oldNames));
		  }
		}

		return current;
	}

	static int debugCount = 0;
	
	/** picks first pair and calls unifyCase
	 */
	static final void unifyHelper(Substitution current, Queue<Pair<Term,Term>> worklist) {
		if (debugCount++ == 30)
			debug2("in loop");
		Pair<Term,Term> p = worklist.poll();
		if (p != null) {
			if (!typesCompatible(p.first.getType(new ArrayList<Pair<String,Term>>()), p.second.getType(new ArrayList<Pair<String,Term>>()))) {
				debug("tried to unify ", p.first.substitute(current), " with ", p.second.substitute(current)," but types didn't match:");
				debug("\ttypes were ", p.first.getType(new ArrayList<Pair<String,Term>>()), " and ", p.second.getType(new ArrayList<Pair<String,Term>>()));
				throw new UnificationFailed("unifying things whose types don't match");
			}
			debug("subtask: unify ", p.first.substitute(current), " with ", p.second.substitute(current));
			debug("    raw ", p.first, " with ", p.second);
			debug("    substitution: ", current);
			debug("    worklist: ", worklist);
			p.first.unifyCase(p.second, current, worklist);
		}
	}
	
	/**
	 * Collect a set of argument positions (zero-based) for free variable applications
	 * which cannot be used if the result must not have bound variables above given bound.
	 * If a bound variable above the bound is not a parameter of a free variable application,
	 * return false. <p> This implementation simply returns true, which is not sound in general.
	 * @param bound highest legal deBruijn index
	 * @param unusable set of unusable positions to be mutated
	 * @return whether all illegal bound variables can be avoided
	 */
	protected boolean selectUnusablePositions(int bound, Set<Pair<FreeVar,Integer>> unusable) {
	  return true;
	}

	public boolean typeEquals(Term otherType) {
		return 	this == Constant.UNKNOWN_TYPE
				|| otherType == Constant.UNKNOWN_TYPE
				|| this.equals(otherType);
	}
	
	/*protected static boolean typesEqual(Term type, Term type2) {
		return type.typeEquals(type2);
		//return type == Constant.UNKNOWN_TYPE || type2 == Constant.UNKNOWN_TYPE || type.equals(type2);
	}*/

	/** true if there's hope these types might ever be unified */
	// TODO: clean this up, it's a bit of a hack (though seems likely it's serviceable)
	protected static boolean typesCompatible(Term type, Term type2) {
		if (type == Constant.UNKNOWN_TYPE || type2 == Constant.UNKNOWN_TYPE)
			return true;
		if (type instanceof Abstraction && type.countLambdas() == type2.countLambdas())
			return true;
		return type.equals(type2);
	}

	/**
	 * Return true if this bound var is used in this term.
	 * @param i index of bound variable to check
	 * @return true bound variable is used.
	 */
	public boolean hasBoundVar(int i) {
		return false;
	}
	
	public boolean hasBoundVarAbove(int i) {
		return false;
	}
	
	// does not check for free "bound variables"
	public final Set<FreeVar> getFreeVariables() {
		Set<FreeVar> s = new HashSet<FreeVar>();
		getFreeVariables(s);
		return s;
	}

	public final Term substitute(Substitution s) { return substitute(s, 0); }

	/************ must override the functions below if default behavior does not apply *************/

	Term substitute(Substitution s, int varIncrAmount) { return this; }

	/** performs a unification, or fails throwing exception, then calls instanceHelper
	 * to continue.  The current substitution is applied lazily.
	 */
	abstract void unifyCase(Term other, Substitution current, Queue<Pair<Term,Term>> worklist);
	
	/** case for Constant given here
	 *  FlexVar case should be impossible
	 *  Application, Abstraction, BoundVar cases separate
	 */
	void unifyFlexApp(FreeVar function, List<? extends Term> arguments, Substitution current, Queue<Pair<Term,Term>> worklist) {
		// code below is OK if arguments are bound variables
		//throw new UnificationFailed("flex-flex case against constant or BoundVar or Abstraction not supported", function, this);

		/* case: C = E x1...xn
		 * 
		 * set E = \x1...\xn . C
		 */ 

		Application errorApp =  new Application(function, arguments);
		// TODO: should enforce that args are in proper order of binding, and that they include *all* the free "bound" vars in the unified thing
		for (Term t : arguments)
			if (!(t instanceof BoundVar))
				throw new UnificationIncomplete("not implemented: non-pattern unification case after delay: " + errorApp + " and " + this, errorApp, this);

		Term wrappedThis = this;
		Term varType = function.getType();
		//List<Term> argTypes = getArgTypes(varType, arguments.size());
		List<Term> argTypes = getArgTypes(varType);
		//wrappedThis = wrappedThis.incrFreeDeBruijn(argTypes.size()); - don't do this, want to implicitly capture vars
		wrappedThis = wrapWithLambdas(wrappedThis, argTypes);
		current.add(function, wrappedThis);

		// continue unifying
		unifyHelper(current, worklist);
	}

	public static Term wrapWithLambdas(Term termToWrap, List<Term> argTypes) {
		for (int i = argTypes.size()-1; i >= 0; --i) {
			termToWrap = Abstraction.make("x", argTypes.get(i), termToWrap);
		}
		return termToWrap;
	}

  public static Term wrapWithLambdas(Term termToWrap, List<Term> argTypes, List<String> argNames) {
    for (int i = argTypes.size()-1; i >= 0; --i) {
      termToWrap = Abstraction.make(argNames.get(i), argTypes.get(i), termToWrap);
    }
    return termToWrap;
  }
  
	public static List<Term> getArgTypes(Term varType, int count) {
		List<Term> argTypes = new ArrayList<Term>();
		debug("getting ", count, " args from ", varType);
		for (int i = 0; i < count; ++i) {
			argTypes.add(((Abstraction)varType).varType);
			varType = ((Abstraction)varType).getBody();
		}
		return argTypes;
	}

	public static List<Term> getArgTypes(Term varType) {
		List<Term> argTypes = new ArrayList<Term>();
		debug("getting all args from ", varType);
		while (varType instanceof Abstraction) {
			argTypes.add(((Abstraction)varType).varType);
			varType = ((Abstraction)varType).getBody();
		}
		return argTypes;
	}
	
	public static Term wrapWithLambdas(List<Abstraction> abs, Term t) {
	  return wrapWithLambdas(abs,t,abs.size());
	}
	
  public static Term wrapWithLambdas(List<Abstraction> abs, Term t, int n) {
    for (int i=n-1; i >= 0; --i) {
      Abstraction a = abs.get(i);
      t = Abstraction.make(a.varName, a.varType, t);
    }
    return t;
  }

  public static Term getWrappingAbstractions(Term t, List<Abstraction> abs) {
	  while (t instanceof Abstraction) {
	    Abstraction a = (Abstraction)t;
	    abs.add(a);
	    t = a.getBody();
	  }
	  return t;
	}

  public static Term getWrappingAbstractions(Term t, List<Abstraction> abs, int n) {
    while (n > 0) {
      Abstraction a = (Abstraction)t;
      abs.add(a);
      t = a.getBody();
      --n;
    }
    return t;
  }

  public static String wrappingAbstractionsToString(List<Abstraction> abs) {
    StringBuilder sb = null;
    for (Abstraction a : abs) {
      if (sb == null) sb = new StringBuilder("{");
      else sb.append(", ");
      sb.append(a.varName);
      sb.append(":");
      sb.append(a.varType);
    }
    if (sb == null) return "{}";
    else sb.append("}");
    return sb.toString();
  }
  
  /**
	 * Compute the type family for this term.
	 * Ignore dependencies; ignore arguments -- just the type family.
	 * @return type family of this term
	 */
	public Constant getTypeFamily() {
	  List<Pair<String,Term>> argTypes = new ArrayList<Pair<String,Term>>();
	  Term type = getType(argTypes);
	  return type.baseTypeFamily();
	}

  /**
   * Compute the family for this type.
   * @return type family for this type
   */
  public Constant baseTypeFamily() {
    Term type = this;
    while (type instanceof Abstraction) {
	    type = ((Abstraction)type).getBody();
	  }
	  if (type instanceof Application) {
	    type = ((Application)type).getFunction();
	  }
	  if (type instanceof Constant) {
	    return (Constant)type;
	  }
	  return (Constant) Constant.UNKNOWN_TYPE;
  }
	
	public abstract Term apply(List<? extends Term> arguments, int whichApplied);

	final Term subForBoundVars(Map<Integer, Term> adjustmentMap) {
		// computed greatest variable index
		int maxInt = 0;
		for (int i : adjustmentMap.keySet()) {
			if (i > maxInt)
				maxInt = i;
		}
		
		// wrap this term in that many lambdas
		Term wrappedTerm = this;
		for (int i = 0; i < maxInt; ++i) {
			wrappedTerm = Facade.Abs(Constant.UNKNOWN_TYPE, wrappedTerm);
		}
		
		// compute argument list
		List<Term> argList = new ArrayList<Term>();
		for (int i = 0; i < maxInt; ++i) {
			if (adjustmentMap.get(i) != null)
				argList.add(adjustmentMap.get(i));
			else
				argList.add(Facade.BVar(i+1));
		}
		
		Term result = wrappedTerm.apply(argList, 0);
		debug("\tadjusting ", this, " to ", result);
		return result;
	}


	/**
	 * Increments free DeBruijn variables by amount.  nested is used to track what is free and what is bound
	 * as you go inside abstractions. 
	 * 
	 * Term implements the default case, returning the identical term, used for FreeVar or Constant */
	Term incrFreeDeBruijn(int nested, int amount) { return this; }

	/**
	 * Increments free DeBruijn variables by amount.
	 */
	public final Term incrFreeDeBruijn(int amount) { return incrFreeDeBruijn(0, amount); }

	/**
	 * Add any free variables to the given set.
	 * @param s set to add to, never null
	 */
	void getFreeVariables(Set<FreeVar> s) {}

	/** returns the number of enclosing Abstractions in the term */
	public int countLambdas() {
		return 0;
	}

	public abstract Term getType(List<Pair<String, Term>> varBindings);

	/** Produces a substitution that will bind an outer bound variable in all free variables.
	 * In Term we implement the default case (which does nothing) for Constant and BoundVar.
	 */
	public void bindInFreeVars(Term typeTerm, Substitution sub) {}
	public void bindInFreeVars(Term typeTerm, Substitution sub, int i) {}
	// NB: The idx is always 1.  We actually don't need it paradoxically even for
	// Abstraction because the substitution will be applied to the outside.
	public void bindInFreeVars(List<Term> typeTerms, Substitution sub, int idx) {}
	
	/** Binds the ith bound variable in all free variables, where the variable type is possible.
	 * Modifies the substitution to reflect changes.
	 * In Term we implement the default case (which does nothing) for Constant and BoundVar.
	 */
	@Deprecated
	public Term oldBindInFreeVars(int i, Term typeTerm, Substitution sub) {
		return this;
	}

	/** Attempts to remove all bound variables above index i and above from the expression.
	 * If this is impossible a UnificationFailedException is thrown.
	 * In Term we implement the default case (which does nothing) for Constant and FreeVar
	 */
	@Deprecated
	public Term removeBoundVarsAbove(int i) {
		return this;
	}

	/** Attempts to remove all bound variables above index i and above from the expression.
	 * Modifies the input substitution to effect the change.
	 * If this is impossible a UnificationFailedException is thrown.
	 * In Term we implement the default case (which does nothing) for Constant and FreeVar
	 */
	public void removeBoundVarsAbove(int i, Substitution sub) {
	}

	/** Returns an eta-equivalent free variable, if possible.
	 *  Returns null if the term is not eta-equivalent to a free variable.
	 *  Here, we implement the default--not equivalent.  Only Abstraction and FreeVar override.
	 */
	public FreeVar getEtaEquivFreeVar() {
		return null;
	}

	/**
	 * Return an eta-equivalent free variable with permutation, if possible.
	 * Returns null if there is not free variable with a non-empty permutation of
	 * arguments.  If result is not null, substitution is modified to specify
	 * the reverse permutation.  Only Abstraction overrides.
	 */
	public FreeVar getEtaPermutedEquivFreeVar(FreeVar src, Substitution reverseSub) {
	  return null;
	}
	
	/** converts (locally) to eta-long form.
	 * Here, we implement the default--return this.  Only FreeVar overrides.
	 */
	public Term toEtaLong() {
		return this;
	}

	/**
	 * Remove any unused lambda bindings around the syntax term.
	 * We only remove outer level binders.
	 * Unused means unused except in the types of other unused lambda bindings. 
	 * We currently do this before checking reduction.  
	 * It should only be applied to fully bound terms,
	 * because it checks multiple variables at once.
	 * This method is a NOP for everything except abstractions.
	 * @return term unchanged or with fewer abstraction layers.
	 */
	public final Term stripUnusedLambdas() {
	  Term result = this;
	  Term t = this;
	  for (;;) {
	    if (!t.hasBoundVarAbove(0)) result = t;
	    if (!(t instanceof Abstraction)) break;
	    Abstraction a = (Abstraction)t;
	    t = a.getBody();
	  }
	  return result;
	}
	
	// reduce() is unnecessary - terms are always in normal form

	/**
	 * Check if the argument term is a subterm of this one.
	 * @param other term to look for inside of this term
	 * @return whether the other term is a strict subterm of this one.
	 */
	public boolean containsProper(Term other) {
	  return false;
	}
	
	/**
	 * Check whether the argument term is a (possibly improper) subterm of this one.
	 * @param other term to look for inside of this term
	 * @return whether the other term is a (possibly improper) subterm of this one.
	 */
	public boolean contains(Term other) {
	  Util.debug(this, " >?= ", other);
	  return this.equals(other) || containsProper(other);
	}
}
