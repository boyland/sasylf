package edu.cmu.cs.sasylf.util;

import java.util.AbstractList;

/**
 * A mutable list that holds up to one (non-null) element
 * @param E element type
 */
public class SingletonList<E> extends AbstractList<E> {
	private E element;
	
	/** Create a singleton list with a single (non-null) element.
	 * @param e element for list, must not be null
	 */
	public SingletonList(E e) {
		if (e == null) throw new NullPointerException("cannot initialize with null");
		element = e;
	}
	
	/**
	 * Create an empty list that could add a single element.
	 */
	public SingletonList() {
		element = null;
	}
	
	@Override
	public E get(int index) {
		if (index == 0 && element != null) return element;
		throw new IndexOutOfBoundsException(index + " not in [0," + size());
	}

	@Override
	public int size() {
		if (element != null) return 1;
		return 0;
	}

	@Override
	public E set(int index, E e) {
		if (index < 0 || index >= size()) get(index); // generate error
		if (e == null) throw new NullPointerException("cannot put null into list");
		E result = element;
		element = e;
		return result;
	}

	@Override
	public void add(int index, E e) {
		if (element != null || index != 0) throw new IndexOutOfBoundsException();
		if (e == null) throw new NullPointerException("cannot put null into list");
		element = e;
	}

	@Override
	public E remove(int index) {
		if (element == null || index != 0) throw new IndexOutOfBoundsException();
		E result = element;
		element = null;
		return result;
	}

}
