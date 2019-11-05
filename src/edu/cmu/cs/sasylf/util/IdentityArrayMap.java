package edu.cmu.cs.sasylf.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A limited version of map which maps arrays of objects mapped
 * using system identity to some value.
 * @author boyland
 */
public class IdentityArrayMap<T> {
	private static class HashEntry<T> implements Map.Entry<Object[], T>{
		private Object[] key;
		// private int hashCode;
		private T value;
		
		HashEntry(Object[] k, int h, T val) {
			key = k;
			// hashCode = h;
			value = val;
		}
		
		@Override
		public Object[] getKey() {
			return key;
		}

		@Override
		public T getValue() {
			return value;
		}

		@Override
		public T setValue(T value) {
			T result = this.value;
			this.value = value;
			return result;
		}
	}
	
	// for now: don't worry about efficiency
	private Map<Integer,List<HashEntry<T>>> impl = new HashMap<Integer,List<HashEntry<T>>>();
	
	private static final int MULT_AMOUNT = 37;
	
	private static int hash(Object[] array) {
		if (array == null) return 0;
		int h = 0;
		for (Object o : array) {
			h *= MULT_AMOUNT;
			h += System.identityHashCode(o);
		}
		return h;
	}
	
	private static boolean equals(Object[] a, Object[] b) {
		if (a == null) return a == b;
		if (b == null) return false;
		if (a.length != b.length) return false;
		int n = a.length;
		for (int i=0; i < n; ++i) {
			if (a[i] != b[i]) return false;
		}
		return true;
	}
	
	/**
	 * Return the key associated with this array of values.
	 * @param array key to look up
	 * @return value associated (if any) or null
	 */
	public T get(Object[] array) {
		int h = hash(array);
		List<HashEntry<T>> l = impl.get(h);
		if (l == null) return null;
		for (HashEntry<T> e : l) {
			if (equals(array,e.getKey())) return e.getValue();
		}
		return null;
	}
	
	/**
	 * Add or update an entry for the given key.
	 * @param array array of key values
	 * @param val value to map to
	 * @return old value (if any) or null
	 */
	public T put(Object[] array, T val) {
		int h = hash(array);
		List<HashEntry<T>> l = impl.get(h);
		if (l == null) {
			l = new ArrayList<HashEntry<T>>();
			impl.put(h,l);
		}
		for (HashEntry<T> e : l) {
			if (equals(array,e.getKey())) {
				T result = e.getValue();
				e.setValue(val);
				return result;
			}
		}
		HashEntry<T> he = new HashEntry<T>(array.clone(),h,val);
		l.add(he);
		return null;
	}
}
