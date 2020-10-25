package edu.cmu.cs.sasylf.util;

import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A mutable set of no more than one (non-null) element.
 * @param T element type
 */
public class SingletonSet<T> extends AbstractSet<T> {

	private T element;

	/**
	 * Create an empty set that can have one element (at most) added.
	 */
	public SingletonSet() {
		element = null;
	}
	
	/**
	 * Create a singleton set with a single (non-null) element
	 * @param elem element at add, must not be null
	 */
	public SingletonSet(T elem) {
		if (elem == null) throw new NullPointerException("set element cannot be null");
		element = elem;
	}
	
	@Override
	public Iterator<T> iterator() {
		return new MyIterator();
	}

	@Override
	public int size() {
		return element == null ? 0 : 1;
	}
	
	@Override
	public boolean add(T elem) {
		if (elem == null) throw new NullPointerException("cannot add null to set");
		if (element == null) {
			element = elem;
			return true;
		}
		if (element.equals(elem)) return false;
		throw new IllegalStateException("Cannot add a second element to a singleton set!");
	}
	
	private class MyIterator implements Iterator<T> {
		private boolean hasNext = element != null;
		
		@Override
		public void remove() {
			if (hasNext || element == null) throw new IllegalStateException("nothing to remove");
			element = null;
		}

		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public T next() {
			if (!hasNext) throw new NoSuchElementException("no more");
			if (element == null) throw new ConcurrentModificationException("stale");
			hasNext = false;
			return element;
		}
	}
	
	/**
	 * Create a (mutable) singleton set.
	 * @param element initial element, must not be null
	 * @return new singleton set
	 */
	public static <T> SingletonSet<T> create(T element) {
		return new SingletonSet<T>(element);
	}
}
