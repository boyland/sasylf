package edu.cmu.cs.sasylf.term;

import java.util.List;


public abstract class Atom extends Term {
	protected Atom(String name) { this.name = name; }

	public final String getName() { return name; }
	@Override
	public final Term getType(List<Pair<String, Term>> varBindings) { return getType(); }
	public abstract Term getType();
	private String name;

	public int hashCode() { return name.hashCode(); }

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Atom)) return false;
		if (obj.getClass() != this.getClass()) return false;
		Atom a = (Atom) obj;
		return name.equals(a.name);
	}

	public Term apply(List<? extends Term> arguments, int whichApplied) {
		if (whichApplied < arguments.size())
			return new Application(this, arguments.subList(whichApplied,arguments.size()));
		else
			return this;
	}

	Term substitute(Substitution s, int varIncrAmount) {
		Term t = s.getSubstituted(this);
		if (t != null)
			return t.incrFreeDeBruijn(varIncrAmount);
		else
			return this;
	}

	public String toString() {
		return name;
	}
}
