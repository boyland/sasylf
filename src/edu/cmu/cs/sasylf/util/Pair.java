package edu.cmu.cs.sasylf.util;

public class Pair<S,T> {
	public Pair(S f, T s) { first = f; second = s; }

	public S first;
	public T second;

	@Override
	public int hashCode() { return first.hashCode() + second.hashCode(); }

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Pair)) return false;
		Pair<?,?> p = (Pair<?,?>) obj;
		return first.equals(p.first) && second.equals(p.second);
	}

	@Override
	public String toString() {
		return "Pair[" + first + ", " + second + "]";
	}
	
	public static <S,T> Pair<S,T> create(S v1, T v2) {
		return new Pair<>(v1,v2);
	}
}
