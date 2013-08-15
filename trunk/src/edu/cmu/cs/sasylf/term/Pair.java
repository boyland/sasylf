package edu.cmu.cs.sasylf.term;

public class Pair<S,T> {
	public Pair(S f, T s) { first = f; second = s; }

	public S first;
	public T second;

	public int hashCode() { return first.hashCode() + second.hashCode(); }

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Pair)) return false;
		Pair<?,?> p = (Pair<?,?>) obj;
		return first.equals(p.first) && second.equals(p.second);
	}

	public String toString() {
		return "Pair[" + first + ", " + second + "]";
	}
}
