package edu.cmu.cs.sasylf.term;

import java.util.Queue;

import edu.cmu.cs.sasylf.CopyData;
import edu.cmu.cs.sasylf.SubstitutionData;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.UpdatableErrorReport;


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
		if (equals(other)) Term.unifyHelper(current, worklist);
		else throw new UnificationFailed("Atoms differ: " + this + " and " + other, this, other);	
	}

	@Override
	public void substitute (SubstitutionData sd) {
		if (sd.didSubstituteFor(this)) return;
		super.substitute(sd);
		sd.setSubstitutedFor(this);

		if (type != null) {
			type.substitute(sd);
		}
	}

	@Override
	public Constant copy(CopyData cd) {
		if (cd.containsCopyFor(this)) return (Constant) cd.getCopyFor(this);

		Constant clone = (Constant) super.clone();
		
		cd.addCopyFor(this, clone);

		if (clone.type != null) {
			clone.type = clone.type.copy(cd);
		}

		return clone;
	}
}
