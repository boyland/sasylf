package org.sasylf.util;

public class Cell<T> {

	private T value;

	public Cell(T initial) {
		value = initial;
	}

	public T get() {
		return value;
	}

	public void set(T next) {
		value = next;
	}
}
