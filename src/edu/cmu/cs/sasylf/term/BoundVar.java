package edu.cmu.cs.sasylf.term;

import java.util.List;
import java.util.Queue;
import java.util.Set;

import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Util;

public class BoundVar extends Atom {
	public BoundVar(int index) {
		super(null);
		//verify(index > 0, "de brejn indexes must be positive");
		if (index <= 0) {
			Util.debug("warning: de bruijn indexes are generally positive - exceptions only for substitutions that capture vars");
			// new Throwable("for trace").printStackTrace();
		}
		this.index = index;
	}

	private int index; // de brejn index, must be at least 1

	@Override
	public int hashCode() { return index; }
	public int getIndex() { return index; }

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof BoundVar)) return false;
		BoundVar bv = (BoundVar) obj;
		return bv.index == index;
	}

	@Override
	public String toString() {
		return "BoundVar" + index;
	}

	@Override
	public Term incrFreeDeBruijn(int nested, int amount) {
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
	public void removeBoundVarsAbove(int i, Substitution sub) {
		if (index > i)
			throw new UnificationFailed("could not eliminate variable binding");
	}


	@Override
	public Term apply(List<? extends Term> arguments, int whichApplied) {
		if (whichApplied < arguments.size()) {
			return super.apply(arguments, whichApplied);
		}
		// Util.verify(whichApplied >= arguments.size(), "type invariant broken in term system");
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
	@Override
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
	@Override
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

	@Override
	protected boolean selectUnusablePositions(int bound,
			Set<Pair<FreeVar, Integer>> unsable) {
		return index <= bound;
	}

	@Override
	public Term getType(List<Pair<String, Term>> varBindings) {
		int indexToUse = varBindings.size() - index;

		if (indexToUse < 0 || indexToUse >= varBindings.size())
			return Constant.UNKNOWN_TYPE; // we're typechecking with missing binding info; just return a default type

		return varBindings.get(indexToUse).second;
	}
	
	@Override
	public Term getType() {
		return Constant.UNKNOWN_TYPE;
	}
	
}
