package edu.cmu.cs.sasylf.util;

import java.util.Set;

/**
 * A relation that is always fully maintained as transitive.
 * The algorithm is not efficient.  If it proves too
 * inefficient, consider using https://code.google.com/p/transitivity-utils/
 * @author boyland
 * @param <T>
 */
public class TransitiveRelation<T> extends Relation<T, T> {

	private final boolean reflexive;

	public TransitiveRelation(boolean refl) { 
		reflexive = refl;
	}

	// this code relies on the fact that getAll and getAllReverse
	// return access to internals.

	@Override
	public boolean put(T t1, T t2) {
		if (!super.put(t1, t2)) return false;
		Set<T> before = super.getAllReverse(t1);
		Set<T> after = super.getAll(t2);
		getAll(t1).addAll(after);
		getAllReverse(t2).addAll(before);
		for (T t0 : before) {
			super.put(t0, t2);
			getAll(t0).addAll(after);
		}
		for (T t3 : after) {
			super.put(t1, t3);
			getAllReverse(t3).addAll(before);
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <Ta,Tb> Set<Ta> makeInitialEdgeSet(Tb key) {
		Set<Ta> result = super.makeInitialEdgeSet(key);
		if (reflexive) {
			result.add((Ta)key);
		}
		return result;
	}

	@Override
	public boolean contains(T key, T value) {
		if (reflexive && key != null && key.equals(value)) return true;
		return super.contains(key, value);
	}

}
