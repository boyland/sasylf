package edu.cmu.cs.sasylf.term;

import java.util.*;
import java.io.*;


public class Constant extends Atom {
	public static final Term TYPE = new Constant();
	public static final Term UNKNOWN_TYPE = new Constant("UNKNOWN_TYPE", TYPE);
	
	public Constant(String n, Term type) { super(n); this.type = type; }
	@Override
	public Term getType() { return type; }

	private Constant() { super("TYPE"); this.type = this; }
	
	Term type;
	
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
}
