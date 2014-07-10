package edu.cmu.cs.sasylf.term;

import static edu.cmu.cs.sasylf.util.Util.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Relation;
import edu.cmu.cs.sasylf.util.TransitiveRelation;
import edu.cmu.cs.sasylf.util.Util;


public class FreeVar extends Atom {
	public FreeVar(String n, Term t, int s) { super(n); type = t; stamp = s; }
	public FreeVar(String n, Term t) { this(n, t, 0); }
	//public FreeVar(Type t) { this(t, t, 0); }

	private int stamp;
	private Term type;

	public boolean equals(Object obj) {
		return super.equals(obj) && ((FreeVar) obj).stamp == stamp;
	}

  private static ThreadLocal<Integer> freshStamp = new ThreadLocal<Integer>() {
    @Override
    protected Integer initialValue() {
      return 1;
    }
  };

  private static void resetFreshStamp() {
    freshStamp.remove();
  }
  private static int getFreshStampInc() {
    int result = freshStamp.get();
    freshStamp.set(result+1);
    return result;
  }

	public static FreeVar fresh(String s, Term t) {
		FreeVar newV = new FreeVar(s, t, getFreshStampInc());
		return newV;
	}

	public FreeVar freshify() {
		FreeVar newV = new FreeVar(getName(), type, getFreshStampInc());
		return newV;
	}

	public String toString() {
		if (stamp == 0)
			return getName();
		else
			return getName() + "_" + stamp;
	}

	public int getStamp() { return stamp; }
	
	int getOrder() { return 0; }
	boolean isNonPatFreeVarApp(Term other) { return other.isNonPatFreeVarApp(); }

	/** performs a unification, or fails throwing exception, then calls instanceHelper
	 * to continue.  The current substitution is applied lazily.
	 */
	void unifyCase(Term other, Substitution current, Queue<Pair<Term,Term>> worklist) {
		// substitute if applicable
		Term t = current.getMap().get(this);
		if (t != null) {
			// unnecessary (checked in Substitution): verify(!t.equals(this), "substituting equal term!");
			worklist.add(makePair(t, other));
			Term.unifyHelper(current, worklist);
			// t.unifyCase(other, current, worklist); // sometimes broke invariant!
		} else {
			// otherwise add to substitution
			if (!this.equals(other)) {
				if (other instanceof Application && ((Application)other).isPattern()) {
					// other = \x1..xn . this
					FreeVar otherVar = (FreeVar) ((Application)other).getFunction();
					if (current.getMap().get(otherVar) != null) {
						Term newOther = other.substitute(current);//otherVar.apply(((Application)other).getArguments(), 0);
						worklist.add(makePair(this, newOther));
						Term.unifyHelper(current, worklist);						
					} else {
						Term varMatch = this;
						List<Term> otherVarArgTypes = getArgTypes(otherVar.getType(), ((Application)other).getArguments().size());
						varMatch = wrapWithLambdas(varMatch, otherVarArgTypes);
						current.add(otherVar, varMatch);
					}
				} else {
					current.add(this, other);
				}
				//current.add(this, other);
			}

			// continue unifying
			Term.unifyHelper(current, worklist);
		}
	}

	void unifyFlexApp(FreeVar function, List<? extends Term> arguments, Substitution current, Queue<Pair<Term,Term>> worklist) {
		throw new RuntimeException("internal invariant violated");
	}

	void getFreeVariables(Set<FreeVar> s) {
		s.add(this);
	}
	public Term getBaseType() {
		Term baseType = type;
		while (baseType instanceof Abstraction)
			baseType = ((Abstraction) baseType).getBody();
		return baseType;
	}
	@Override
	public Term getType() { return type; }
	public void setType(Term varType) {
		type = varType;
	}

	/** Produces a substitution that will bind an outer bound variable in all free variables.
	 * If typeTerm can appear in the base type of this FreeVar, we substitute this free variable
	 * with a fresh free variable with a type that has one more argument, bound to a bound variable
	 * with an index of 1.
	 */
	@Override
	public void bindInFreeVars(Term typeTerm, Substitution sub) {
		Term earlierSub = sub.getSubstituted(this);
		if (earlierSub != null)
			return;

		Term baseType = getBaseType();
		
		if (!canAppearIn(typeTerm, baseType))
			return;
		
		/*FreeVar newVar = null;
		Term earlierSub = sub.getSubstituted(this);
		if (earlierSub != null) {
			newVar = (FreeVar)((Application)earlierSub).getFunction();
		} else {*/
			FreeVar newVar = this.freshify();
			newVar.type = Abstraction.make("extendedTypeArg", typeTerm, type);			
		//}
		Term appTerm = new Application(newVar, new BoundVar(1));
    Util.debug("newVar2 ",newVar," for ",this, " : ", type, ", has type ",newVar.type);
		sub.add(this, appTerm);
	}

	@Override
	public void bindInFreeVars(List<Term> typeTerms, Substitution sub) {
		Term earlierSub = sub.getSubstituted(this);
		int n = typeTerms.size();
    if (earlierSub != null || n == 0)
			return;

		// compute the new type
		Term baseType = getBaseType();
		Term newVarType = type;
		List<Term> bVarList = new ArrayList<Term>();
		for (int i=n-1; i >= 0; --i) {
		  Term typeTerm = typeTerms.get(i);
      if (!canAppearIn(typeTerm.baseTypeFamily(), baseType))
        continue;
      newVarType = Abstraction.make("extendedTypeArg", typeTerm, newVarType);
      bVarList.add(new BoundVar(n-i));
		}
		
		if (bVarList.size() == 0)
			return;

		Collections.reverse(bVarList);
		
		// create a fresh free variable
		FreeVar newVar = this.freshify();
		newVar.type = newVarType;		
    Util.debug("newVar2 ",newVar," for ",this, " : ", type, ", has type ",newVar.type);

		// add new bound variables and fill in substitution
		Term appTerm = Facade.App(newVar, bVarList);
		List<Pair<String,Term>> varBindings = new ArrayList<Pair<String,Term>>();
		for (Term ty : typeTerms) {
		  varBindings.add(new Pair<String,Term>("_",ty));
		}
		Util.verify(type.equals(appTerm.getType(varBindings)), "replacement has wrong type");
		sub.add(this, appTerm);
	}
	
	public static boolean canAppearIn(Term term1, Term term2) {
		debug("testing if ", term1, " can appear in ", term2);
		return getAppearsIn().contains(term1, term2);
	}
	
	public static void setAppearsIn(Term term1, Term term2) {
		getAppearsIn().put(term1, term2);
	}

	public static Relation<Term,Term> getAppearsIn() {
    return appearsIn.get();
  }
  private static void resetAppearsIn() {
    FreeVar.appearsIn.remove();
  }

  private static ThreadLocal<Relation<Term,Term>> appearsIn = new ThreadLocal<Relation<Term,Term>>() {
    @Override
    protected Relation<Term, Term> initialValue() {
      return new TransitiveRelation<Term>(false);
    }    
  };

  public static void printSubordination() {
    Relation<Term,Term> rel = appearsIn.get();
    for (Pair<Term,Term> p : rel) {
      System.out.println(p.first + " < " + p.second);
    }
  }
  
	public static void reinit() {
	  resetFreshStamp();
		resetAppearsIn();
	}
	
	public static void computeAppearsInClosure() {
		// already done while adding!
		// TODO: eliminate this function
	}
	
	@Override
	public FreeVar getEtaEquivFreeVar() {
		return this;
	}

	/** converts (locally) to eta-long form.
	 * Here, we implement the conversion if necessary.
	 */
	@Override
	public Term toEtaLong() {
		int numLambdas = getType().countLambdas();
		if (numLambdas > 0) {
			List<Term> args = new ArrayList<Term>();
			List<Term> types = new ArrayList<Term>();
			Term myType = getType();
			for (int i = numLambdas; i > 0; --i) {
				args.add(new BoundVar(i));
				Abstraction abs = (Abstraction) myType;
				types.add(abs.varType);
				myType = abs.getBody();
			}
			Term newT = this.apply(args, 0);
			for (int i = numLambdas-1; i >= 0; --i)
				newT = Facade.Abs(types.get(i), newT);
			debug("converted to eta long - from ", this, " to ", newT);
			return newT;
		} else {
			return this;
		}
	}
}
