package edu.cmu.cs.sasylf.term;

import java.util.Queue;

import edu.cmu.cs.sasylf.CloneData;
import edu.cmu.cs.sasylf.SubstitutionData;
import edu.cmu.cs.sasylf.util.Pair;


public class Constant extends Atom {
	public static final Constant TYPE = new Constant();
	public static final Constant UNKNOWN_TYPE = new Constant("UNKNOWN_TYPE", TYPE);

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

	public void substitute (SubstitutionData sd) {
		if (sd.didSubstituteFor(this)) return;
		sd.setSubstitutedFor(this);

		if (type != null) {
			type.substitute(sd);
		}

	}


	@Override
	public Constant copy(CloneData cd) {
		if (cd.containsCloneFor(this)) return (Constant) cd.getCloneFor(this);

		Constant clone;

		try {
			clone = (Constant) super.clone();
		}
		catch(CloneNotSupportedException e) {
			System.out.println("Clone not supported for Constant");
			System.exit(1);
			return null;
		}
		
		cd.addCloneFor(this, clone);

		/*
			Term type;
		*/

		if (clone.type != null) {
			clone.type = clone.type.copy(cd);
		}

		return clone;
	}
}
