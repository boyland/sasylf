package edu.cmu.cs.sasylf.term;

import java.util.*;
import java.io.*;

import static edu.cmu.cs.sasylf.util.Util.*;

public class BoundVar extends Term {
	public BoundVar(int index) {
		//verify(index > 0, "de brejn indexes must be positive");
		if (index <= 0)
			debug("warning: de bruijn indexes are generally positive - exceptions only for substitutions that capture vars");
		this.index = index;
	}

	private int index; // de brejn index, must be at least 1

	public int hashCode() { return index; }
	public int getIndex() { return index; }

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof BoundVar)) return false;
		BoundVar bv = (BoundVar) obj;
		return bv.index == index;
	}

	public String toString() {
		return "BoundVar" + index;
	}

	Term incrFreeDeBruijn(int nested, int amount) {
		if (index <= nested && index > 0)
			return this;
		else
			return new BoundVar(index + amount);
	}

	@Override
	public boolean hasBoundVar(int i) {
		return index == i;
	}

	@Override
	public boolean hasBoundVarAbove(int i) {
		return index > i;
	}

	/** Attempts to remove all bound variables above index i and above from the expression.
	 * If we get here it's too late; if the index is in range,
	 * we throw a UnificationFailedException because it can't be done.
	 */
	@Override
	public Term removeBoundVarsAbove(int i) {
		if (index > i)
			throw new UnificationFailed("could not eliminate variable binding");
		return this;
	}

	/** Attempts to remove all bound variables above index i and above from the expression.
	 * If we get here it's too late; if the index is in range,
	 * we throw a UnificationFailedException because it can't be done.
	 */
	@Override
	public void removeBoundVarsAbove(int i, Substitution sub) {
		if (index > i)
			throw new UnificationFailed("could not eliminate variable binding");
	}


	public Term apply(List<? extends Term> arguments, int whichApplied) {
		verify(whichApplied >= arguments.size(), "type invariant broken in term system");
		int argIndex = whichApplied - index;
		if (argIndex >= 0 && argIndex < arguments.size()) {
			Term result = arguments.get(argIndex);
			return result;
		} else
			return this;
	}

	/** performs a unification, or fails throwing exception, then calls instanceHelper
	 * to continue.  The current substitution is applied lazily.
	 */
	void unifyCase(Term other, Substitution current, Queue<Pair<Term,Term>> worklist) {
		// other term must be equal to me, otherwise fail
		if (equals(other))
			Term.unifyHelper(current, worklist);
		else
			throw new UnificationFailed("Atoms differ: " + this + " and " + other, this, other);
	}
	
	/** 
	 * we are unifying y with e xn...x1
	 * 
	 * solution: e = \yn. ... \y1 . yi
	 * 
	 * where y == xi
	 */
	void unifyFlexApp(FreeVar function, List<? extends Term> arguments, Substitution current, Queue<Pair<Term,Term>> worklist) {
		Application errorApp =  new Application(function, arguments);
		// TODO: should enforce that args are in proper order of binding, and that they include *all* the free "bound" vars in the unified thing
		for (Term t : arguments)
			if (!(t instanceof BoundVar))
				throw new UnificationFailed("not implemented: non-pattern unification case after delay: " + errorApp + " and " + this, errorApp, this);

		// compute i
		int i = 1;
		for (; i <= arguments.size(); ++i) {
			if (this.equals(arguments.get(arguments.size()-i)))
				break;
		}
		if (i == arguments.size()+1)
			throw new UnificationFailed("cannot unify " + this + " with expression " + errorApp + " in which var is not free", errorApp, this);
		
		Term wrappedBVar = new BoundVar(i);
		
		Term varType = function.getType();
		//List<Term> argTypes = getArgTypes(varType, arguments.size());
		List<Term> argTypes = getArgTypes(varType);
		//wrappedThis = wrappedThis.incrFreeDeBruijn(argTypes.size()); - don't do this, want to implicitly capture vars
		wrappedBVar = wrapWithLambdas(wrappedBVar, argTypes);
		current.add(function, wrappedBVar);

		// continue unifying
		unifyHelper(current, worklist);
	}


	public Term getType(List<Pair<String, Term>> varBindings) {
		int indexToUse = varBindings.size() - index;
		
		if (indexToUse < 0 || indexToUse >= varBindings.size())
			return Constant.UNKNOWN_TYPE; // we're typechecking with missing binding info; just return a default type
		
		return varBindings.get(indexToUse).second;
	}
}
