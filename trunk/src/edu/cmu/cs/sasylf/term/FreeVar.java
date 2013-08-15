package edu.cmu.cs.sasylf.term;

import static edu.cmu.cs.sasylf.util.Util.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import edu.cmu.cs.sasylf.util.Relation;


public class FreeVar extends Atom {
	public FreeVar(String n, Term t, int s) { super(n); type = t; stamp = s; }
	public FreeVar(String n, Term t) { this(n, t, 0); }
	//public FreeVar(Type t) { this(t, t, 0); }

	private int stamp;
	private Term type;

	public boolean equals(Object obj) {
		return super.equals(obj) && ((FreeVar) obj).stamp == stamp;
	}

	static int freshStamp = 1;

	public static FreeVar fresh(String s, Term t) {
		FreeVar newV = new FreeVar(s, t, freshStamp++);
		return newV;
	}

	public FreeVar freshify() {
		FreeVar newV = new FreeVar(getName(), type, freshStamp++);
		return newV;
	}

	public String toString() {
		if (stamp == 0)
			return getName();
		else
			return getName() + "_" + stamp;
	}

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
		sub.add(this, appTerm);
	}

	@Override
	public void bindInFreeVars(List<Term> typeTerms, Substitution sub, int idx) {
		Term earlierSub = sub.getSubstituted(this);
		if (earlierSub != null || typeTerms.size() == 0)
			return;

		// compute the new type
		Term baseType = getBaseType();
		Term newVarType = type;
		List<Term> bVarList = new ArrayList<Term>();
		int currIdx = idx+typeTerms.size(); // idx-1 because we will increment it before executing main body of loop
		for (Term typeTerm : typeTerms) {
			currIdx--;
			if (!canAppearIn(typeTerm, baseType))
				continue;
			newVarType = Abstraction.make("extendedTypeArg", typeTerm, newVarType);
			bVarList.add(new BoundVar(currIdx));
		}
		
		if (bVarList.size() == 0)
			return;

		// create a fresh free variable
		FreeVar newVar = this.freshify();
		newVar.type = newVarType;		

		// add new bound variables and fill in substitution
		Term appTerm = Facade.App(newVar, bVarList);
		sub.add(this, appTerm);
	}

	@Override
	public void bindInFreeVars(Term typeTerm, Substitution sub, int i) {
		Term earlierSub = sub.getSubstituted(this);
		if (earlierSub != null)
			return;

		Term baseType = getBaseType();
		
		if (!canAppearIn(typeTerm, baseType))
			return;
		
		FreeVar newVar = this.freshify();
		newVar.type = Abstraction.make("extendedTypeArg", typeTerm, type);			
		Term appTerm = new Application(newVar, new BoundVar(i));
		sub.add(this, appTerm);
	}

	/** Binds the ith bound variable in all free variables.
	 * Modifies the substitution to reflect changes.
	 * We return a fresh free variable with a type that has one more argument,
	 * and then we apply it to the appropriate bound variable.
	 * 
	 * BUT--we only bind the fresh variable if typeTerm can appear in the base type of this FreeVar.
	 */
	@Override
	@Deprecated
	public Term oldBindInFreeVars(int i, Term typeTerm, Substitution sub) {
		Term earlierSub = sub.getSubstituted(this);
		if (earlierSub != null)
			return earlierSub;
		
		Term baseType = getBaseType();
		
		if (!canAppearIn(typeTerm, baseType))
			return this;
		
		FreeVar newVar = this.freshify();
		newVar.type = Abstraction.make("extendedTypeArg", typeTerm, type);
		Term appTerm = new Application(newVar, new BoundVar(i));
		sub.add(this, appTerm);
		return appTerm;
	}
	
	public static boolean canAppearIn(Term term1, Term term2) {
		debug("testing if " + term1 + " can appear in " + term2);
		return appearsIn.contains(term1, term2);
		// hardcode a result for now
		/*if (term2.toString().equals("loc"))
			return false;
		if (term2.toString().equals("tau"))
			return false;
		return true;*/
	}
	
	public static void setAppearsIn(Term term1, Term term2) {
		boolean changed = appearsIn.put(term1, term2);
		if (changed)
			enforceTransitivity(term1, term2);
	}
	
	/*private static void enforceTransitivity1(Term term1, Term term2) {
		Set<Pair<Term, Term>> worklist = new HashSet<Pair<Term, Term>>();
		worklist.add(new Pair<Term, Term>(term1, term2));
		while (!worklist.isEmpty()) {
			Pair<Term, Term> pair = worklist.iterator().next();
			verify(worklist.remove(pair), "internal invariant violated");
			Term t1 = pair.first;
			Term t2 = pair.second;
			// if t1 appearsIn t2 and t2 appearsIn t3 then t1 appearsIn t3
			Set<Term> t3set = appearsIn.getAll(t2);
			for (Term t3 : t3set) {
				boolean changed = appearsIn.put(t1, t3);
				if (changed) {
					
				}
			}
		}
		
	}*/

	private static void enforceTransitivity(Term t1, Term t2) {
		debug("added " + t1 + " in " + t2);
		// if t1 appearsIn t2 and t2 appearsIn t3 then t1 appearsIn t3
		Set<Term> t3set = appearsIn.getAll(t2);
		for (Term t3 : t3set) {
			if (!canAppearIn(t1, t3))
				debug("transitivity: " + t1 + " in " + t2 + " in " + t3);
			setAppearsIn(t1, t3);
		}
		// if t0 appearsIn t1 and t1 appearsIn t2 then t0 appearsIn t2
		Set<Term> t0set = appearsIn.getAllReverse(t1);
		for (Term t0 : t0set) {
			if (!canAppearIn(t0, t2))
				debug("transitivityBack: " + t0 + " in " + t1 + " in " + t2);
			setAppearsIn(t0, t2);
		}
	}

	private static Relation<Term,Term> appearsIn = new Relation<Term,Term>();

	public static void reinit() {
		appearsIn = new Relation<Term,Term>();
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
			debug("converted to eta long - from " + this + " to " + newT);
			return newT;
		} else {
			return this;
		}
	}
}
