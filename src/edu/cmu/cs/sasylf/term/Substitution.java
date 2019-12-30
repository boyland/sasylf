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
 * A mutable class of term substitutions, substituting arbitrary terms for variables.
 * An earlier design permitted substitution of constants too, but this is being
 * changed.
 */
public class Substitution {
	
	private Map<FreeVar, Term> varMap = new HashMap<FreeVar, Term>();
	private Map<FreeVar, Term> unmodifiableMap;
	
	public Substitution() { }

	/** var may not be free in term
	 */
	public Substitution(Term term, FreeVar var) {
		add(var, term);
	}
	public Substitution(List<FreeVar> vars, List<? extends Term> terms) {
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

	/**
	 * Return true if the substitution is the NOP.
	 * @return true is there are no variables to map.
	 */
	public boolean isEmpty() {
		return varMap.isEmpty();
	}
	
	public boolean isUnifier(Term t1, Term t2) {
		t1 = t1.substitute(this);
		t2 = t2.substitute(this);
		return t1.equals(t2);
	}

	/** 
	 * Returns true if could avoid mapping all given variables.<br>
	 * NB: This may permanently modify this substitution.
	 */
	public boolean avoid(Set<FreeVar> vars) {
		return selectUnavoidable(vars).isEmpty();
	}

	/** Modifies the substitution to avoid each of these vars if possible.
	 * Returns the subset of vars that can't be avoided. */
	public Set<FreeVar> selectUnavoidable(Set<FreeVar> vars) {
		Set<FreeVar> result = new HashSet<FreeVar>();

		for (FreeVar v : vars) {
			Term t = varMap.get(v);
			if (t != null) {
				// see if there's an equivalent free variable (perhaps permuted)
				Substitution revSub = new Substitution();
				FreeVar fv = t.getEtaPermutedEquivFreeVar(v, revSub);

				if (fv == null) {
					// can't avoid
					result.add(v);
					Util.debug("could not avoid ", v, " because it is equal to non-FreeVar expression ", t);					
				} else if (vars.contains(fv)) {
					// can't avoid
					result.add(v);
					Util.debug("could not avoid ", v," because it is equal to another thing we must avoid, ", fv);
				} else {
					// switch a and t
					varMap.remove(v);
					compose(revSub);
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
	public void add(FreeVar var, Term t) {
		debug("substituting ", t, " for ", var, " adding to ", this);

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
			for (FreeVar v : varMap.keySet()) {
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

	/**
	 * Remove this variable from the map,
	 * returning the old substitution
	 * @param v variable to check, should not be null
	 * @return former mapping for this variable.
	 */
	public Term remove(FreeVar v) {
		return varMap.remove(v);
	}

	public void removeAll(Collection<FreeVar> col) {
		varMap.keySet().removeAll(col);
	}
	
	/**
	 * Return the domain as an unmodifiable set.
	 * @return set of variables mapped y this substitution
	 */
	public Set<FreeVar> getDomain() {
		return Collections.unmodifiableSet(varMap.keySet());
	}

	/**
	 * Return what this variable is substituted with according to this substitution.
	 * @param var variable to look up.
	 * @return null if no substitution
	 */
	public Term getSubstituted(FreeVar var) {
		return varMap.get(var);
	}

	/**
	 * Return free variables in the range of the substitution.
	 */
	public Set<FreeVar> getFreeVariables() {
		Set<FreeVar> result = new HashSet<FreeVar>();
		for (Map.Entry<FreeVar, Term> e : varMap.entrySet()) {
			e.getValue().getFreeVariables(result);
		}
		return result;
	}
	
	/** Modifies this substitution to incorporate the existing
	 * substitution plus the other substitution.
	 */
	public void compose(Substitution other) {
		for (FreeVar v : other.varMap.keySet()) {
			//if (varMap.containsKey(v))
			//throw new RuntimeException("bad composition: " + this + " and " + other);
			add(v, other.varMap.get(v));
		}

		//return this;
	}

	public final void incrFreeDeBruijn(int amount) {
		for (FreeVar v: varMap.keySet()) {
			varMap.put(v, varMap.get(v).incrFreeDeBruijn(amount));
		}
	}
	
	public Map<FreeVar, Term> getMap() {
		if (unmodifiableMap == null)
			unmodifiableMap = Collections.unmodifiableMap(varMap);
		return unmodifiableMap;
	}

	@Override
	public int hashCode() { return varMap.hashCode(); }

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Substitution)) return false;
		Substitution s = (Substitution) obj;
		return varMap.equals(s.varMap);
	}

	public boolean containsAll(Substitution other) {
		return varMap.entrySet().containsAll(other.varMap.entrySet());
	}

	@Override
	public String toString() {
		if (varMap.isEmpty())
			return "{}";
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		for (FreeVar v : varMap.keySet())
			sb.append(v + " -> " + varMap.get(v) + ", ");
		return sb.substring(0, sb.length() - 2) + "}";
	}

}
