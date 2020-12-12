package edu.cmu.cs.sasylf.term;

import static edu.cmu.cs.sasylf.util.Util.debug;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
	
	private boolean wellFormed() {
		if (varMap == null) {
			if (unmodifiableMap == null || unmodifiableMap.isEmpty()) return true;
			return Util.report("immutable cache of empty substitution is wrong: " + unmodifiableMap);
		}
		Set<FreeVar> freeSet = new HashSet<>();
		for (Map.Entry<FreeVar, Term> e : varMap.entrySet()) {
			e.getValue().getFreeVariables(freeSet);
			freeSet.retainAll(varMap.keySet());
			if (!freeSet.isEmpty()) return Util.report("badly formed substitution: both maps " + freeSet + " and uses in binding " + e.getKey() + " -> " + e.getValue());
		}
		if (unmodifiableMap != null) {
			if (!unmodifiableMap.equals(varMap)) return Util.report("Immutable cahce is wrong " + unmodifiableMap);
		}
		return true;
	}
	
	public Substitution() { 
		assert wellFormed() : "Invariant failed in constructor";
	}

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
		assert wellFormed() : "Invariant failed in copy constructor";
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

		assert wellFormed() : "Invariant broken in selecUnavoidable";
		return result;
	}

	/** 
	 * Add a binding to this substitution.  The binding is substituted to follow
	 * the current substitution, thus
	 * guaranteeing compositionality, ensures no recursion in substitution (including mutual recursion)
	 * but allows substituting X for X (this leaves the map unchanged). If the variable
	 * already has a binding, the two values are unified, which might produce a
	 * unification exception, which must be caught.
	 * <p>
	 * This method cannot be used to 'rotate' a substitution
	 * unless the original binding is removed first.
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
			assert wellFormed() : "Invariant fails on recursive call to compose";
			compose(unifier);
		}

		// add the new entry to the map
		varMap.put(var, tSubstituted);
		assert wellFormed() : "Invariant failed at end of 'add'";
	}

	/**
	 * Remove this variable from the map,
	 * returning the old substitution
	 * @param v variable to check, should not be null
	 * @return former mapping for this variable.
	 */
	public Term remove(FreeVar v) {
		Term result = varMap.remove(v);
		assert wellFormed() : "Invariant broken in remove";
		return result;
	}

	public void removeAll(Collection<FreeVar> col) {
		assert wellFormed() : "Invariant broken in removeAll";
		varMap.keySet().removeAll(col);
	}
	
	public void retainAll(Collection<FreeVar> col) {
		assert wellFormed() : "Invariant broken in retainAll";
		varMap.keySet().retainAll(col);
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
	
	/**
	 * Check whether this substitution can be successfully added to this.
	 * There are two reasons why substitution might not be permitted:
	 * <ol>
	 * <li> The second substitution re-introduces a variable the first one substituted, 
	 * but does not successfully map it back to itself.</li>
	 * <li> The second substitution maps an existing variable to a different thing than the first.
	 * </ol>
	 * @param other another composition, must not be null
	 * @param doit start performing the substitution, by removing mappings.
	 * If a problem happens, we throw an exception
	 * @throws IllegalArgumentException if doit and the result is false.
	 * NB: this object is unchanged when the exception is thrown
	 * @return false 
	 */
	protected boolean checkComposition(Substitution other, boolean doIt) throws IllegalArgumentException {
		Set<FreeVar> common = new HashSet<>(varMap.keySet());
		common.retainAll(other.varMap.keySet());
		for (FreeVar fv : common) {
			Term t1 = varMap.get(fv).substitute(other);
			Term t2 = other.varMap.get(fv);
			if (!t1.equals(t2)) {
				Util.debug( t1, " is not equal to ", t2, " when trying ", this, ".compose(", other, ")");
				if (!doIt) return false;
				throw new IllegalArgumentException(this + " cannot compose " + other + " because of inconsistency for " + fv);				
			}
		}
		Set<FreeVar> free = other.getFreeVariables();
		for (Iterator<FreeVar> it = free.iterator(); it.hasNext(); ) {
			FreeVar fv = it.next();
			if (varMap.containsKey(fv)) {
				Term newTerm = varMap.get(fv).substitute(other);
				if (fv != newTerm.getEtaEquivFreeVar()) {
					Util.debug(newTerm, " is not ", fv, " when trying ", this, ".compose(", other, ")");
					if (!doIt) return false;
					throw new IllegalArgumentException(this + " cannot compose " + other + " because of occurs on " + fv);
				}
			} else it.remove();
		}
		if (doIt && !free.isEmpty()) varMap.keySet().removeAll(free);
		return true;
	}
	
	/**
	 * Return true if the parameter can be successfully composed with this one.
	 * It returns false if the substitution would cause an occurs-check problem
	 * or an inconsistency.
	 * @param other another substitution.  If null, this method returns false.
	 * @return whether this substitution received this substitution in composition.
	 */
	public boolean canCompose(Substitution other) {
		if (other == null) return false;
		if (other == this) return true;
		return checkComposition(other,false);
	}
	
	/** Modifies this substitution to incorporate the existing
	 * substitution plus the other substitution.  This method supports "rotating"
	 * a substitution (e.g. changing A -> B to B -> A), but not "reintroducing" 
	 * a variable that was mapped already to something other than a variable.
	 * @see #merge(Substitution)
	 */
	public void compose(Substitution other) {
		if (other == this) return; // NOP
		
		checkComposition(other,true);
		
		for (Map.Entry<FreeVar,Term> e : varMap.entrySet()) {
			e.setValue(e.getValue().substitute(other));
		}
		
		for (FreeVar v : other.varMap.keySet()) {
			if (!varMap.containsKey(v)) {
				varMap.put(v, other.varMap.get(v));
			}
		}

		assert wellFormed() : "composition broken invariant";
	}
	
	/**
	 * Combine the argument substitution with this one.
	 * If they both map the same variable, we unify their results,
	 * which might cause a unification error (incomplete or failure).
	 * This method cannot be used to (reliably) perform a "rotation" of a variable.
	 * @param other another substitution, must be null
	 */
	public void merge(Substitution other) throws UnificationFailed {
		if (other == this) return; // NOP
		for (FreeVar v : other.varMap.keySet()) {
			add(v, other.varMap.get(v));
		}
	}

	public final void incrFreeDeBruijn(int amount) {
		for (FreeVar v: varMap.keySet()) {
			varMap.put(v, varMap.get(v).incrFreeDeBruijn(amount));
		}
		assert wellFormed() : "Invariant broken in incrFreeDeBruijn";
	}
	
	public Map<FreeVar, Term> getMap() {
		if (unmodifiableMap == null)
			unmodifiableMap = Collections.unmodifiableMap(varMap);
		assert wellFormed() : "invariant broken in getMap()";
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
