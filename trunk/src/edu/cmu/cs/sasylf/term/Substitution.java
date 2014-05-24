package edu.cmu.cs.sasylf.term;

import static edu.cmu.cs.sasylf.util.Util.debug;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.util.Util;

/**
 * A mutable class of term substitutions, substuting arbitrary terms for variables.
 * An earlier design permitted substitution of constants too, but this is being
 * changed.
 */
public class Substitution {
	public Substitution() { }

	/** var may not be free in term
	 */
	public Substitution(Term term, FreeVar var) {
		add(var, term);
	}
	public Substitution(List<? extends Term> terms, List<? extends Atom> vars) {
		if (terms.size() != vars.size())
			throw new RuntimeException("implementation error");

		for (int i = 0; i < terms.size(); ++i) {
			add(vars.get(i), terms.get(i));
		}
	}

	/** Copy constructor */
	public Substitution(Substitution other) {
		varMap.putAll(other.varMap);
	}

	public boolean isUnifier(Term t1, Term t2) {
		t1 = t1.substitute(this);
		t2 = t2.substitute(this);
		return t1.equals(t2);
	}
	
	/** returns true if could avoid all atoms */
	public boolean avoid(Set<? extends Atom> atoms) {
		return selectUnavoidable(atoms).isEmpty();
	}

	/** Modifies the substitution to avoid each of these vars if possible.
	 * Returns the subset of vars that can't be avoided. */
	public <T extends Atom> Set<T> selectUnavoidable(Set<T> vars) {
		Set<T> result = new HashSet<T>();
		
		for (T v : vars) {
			Term t = varMap.get(v);
			if (t != null) {
				// see if there's an equivalent free variable
				FreeVar fv = t.getEtaEquivFreeVar();
				
				if (fv == null) {
				  Substitution revSub = new Substitution();
				  fv = t.getEtaPermutedEquivFreeVar((FreeVar)v, revSub);
				  if (fv != null && !vars.contains(fv)) {
				    Util.debug("Found permuted var: ", v);
				    varMap.remove(v);
				    this.compose(revSub);
				  } else {
				    // can't avoid
				    result.add(v);
				    Util.debug("could not avoid ", v, " because it is equal to non-FreeVar expression ", t);
				  }
				} else if (vars.contains(fv)) {
					// can't avoid
					result.add(v);
					Util.debug("could not avoid ", v," because it is equal to another thing we must avoid, ", fv);
				} else {
					// switch a and t
					varMap.remove(v);
					add(fv,v);
				}
			}
		}
		
		return result;
	}
	
	/** guarantees compositionality, ensures no recursion in substitution (including mutual recursion)
	 * but allows substituting X for X (this leaves the map unchanged). If the variable
   * already has a binding, the two values are unified, which might produce a
   * unification exception, which must be caught.
   * @throws EOCUnificationFailed occurrence check failed (var bound to something including itself)
   * @throws UnificationFailed two binding for the variable failed to unify.
   */
	public void add(Atom atom, Term t) {
		debug("substituting ", t, " for ", atom, " adding to ", this);
		if (atom instanceof Constant) throw new RuntimeException("InternaError: substituting constant " + atom);
		FreeVar var = (FreeVar)atom;
		
		// perform the substitution on t
		Term tSubstituted;
		if (varMap.isEmpty())
			tSubstituted = t;
		else
			tSubstituted = t.substitute(this);
		
		FreeVar subFreeVar = tSubstituted.getEtaEquivFreeVar();
		
		// return if this is the empty substitution
		if (tSubstituted.equals(var) || (subFreeVar != null && subFreeVar.equals(var)))
			return;

		debug("tSubstituted is ", tSubstituted);
		
		// ensure var is not free in tSubstituted
		Set<FreeVar> freeVars = tSubstituted.getFreeVariables();
		if(freeVars.contains(var))
			throw new EOCUnificationFailed("Extended Occurs Check failed: " + var + " is free in " + tSubstituted, var);

		// perform substitution on the existing variables
    if (!varMap.isEmpty()) {
      Substitution newSub = new Substitution(tSubstituted, var);
      for (Atom v : varMap.keySet()) {
        varMap.put(v, varMap.get(v).substitute(newSub));
      }
    }
		if (varMap.containsKey(var)) {
		  Term oldTerm = varMap.get(var);
		  Substitution unifier = oldTerm.unify(tSubstituted);
		  tSubstituted = tSubstituted.substitute(unifier);
		  compose(unifier);
		}
		
		// add the new entry to the map
		varMap.put(var, tSubstituted);
	}

	public Term remove(Atom v) {
	  return varMap.remove(v);
	}
	
	public void removeAll(Collection<? extends Atom> col) {
	  varMap.entrySet().removeAll(col);
	}
	
	/**
	 * Return what this variable is substituted with according to this substitution.
	 * @param var variable to look up.
	 * @return null if no substitution
	 */
	public Term getSubstituted(Atom var) {
		return varMap.get(var);
	}

	/** Modifies this substitution to incorporate the existing
	 * substitution plus the other substitution.
	 */
	public void compose(Substitution other) {
		for (Atom v : other.varMap.keySet()) {
			//if (varMap.containsKey(v))
			//throw new RuntimeException("bad composition: " + this + " and " + other);
			add(v, other.varMap.get(v));
		}

		//return this;
	}

	public final void incrFreeDeBruijn(int amount) {
		for (Atom v: varMap.keySet()) {
			varMap.put(v, varMap.get(v).incrFreeDeBruijn(amount));
		}
	}

	private Map<Atom, Term> varMap = new HashMap<Atom, Term>();
	private Map<Atom, Term> unmodifiableMap;

	public Map<Atom, Term> getMap() {
		if (unmodifiableMap == null)
			unmodifiableMap = Collections.unmodifiableMap(varMap);
		return unmodifiableMap;
	}

	public int hashCode() { return varMap.hashCode(); }

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Substitution)) return false;
		Substitution s = (Substitution) obj;
		return varMap.equals(s.varMap);
	}

	public boolean containsAll(Substitution other) {
	  return varMap.entrySet().containsAll(other.varMap.entrySet());
	}
	
	public String toString() {
		return getMap().toString();
	}

}
