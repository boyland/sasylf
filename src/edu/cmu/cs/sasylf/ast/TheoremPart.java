package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import edu.cmu.cs.sasylf.CloneData;
import edu.cmu.cs.sasylf.SubstitutionData;
import edu.cmu.cs.sasylf.util.SASyLFError;

/**
 * A sequence of theorems.
 * Theorem may be mutually recursive only as connected with 'and".
 */
public class TheoremPart implements Part {
	List<Theorem> theorems;

	public List<Theorem> getTheorems() {
		return theorems;
	}
	
	/**
	 * Create a part from a list of theorems.
	 * @param theos theorem or lemma declarations (must not be null).
	 */
	public TheoremPart(List<Theorem> theos) {
		theorems = new ArrayList<Theorem>(theos);
	}
	
	@Override
	public void prettyPrint(PrintWriter out) {
		for (Theorem t: theorems) {
			t.prettyPrint(out);
		}
	}

	@Override
	public void typecheck(Context ctx) {	
		for (Theorem t: theorems) {
			try {
				t.typecheck(ctx);
			} catch (SASyLFError e) {
				// already reported, swallow the exception
			}
		}
	}
	
	@Override
	public void collectTopLevel(Collection<? super Node> things) {
		for (Theorem th : theorems) {
			things.add(th);
		}
	}
	
	@Override
	public void collectRuleLike(Map<String,? super RuleLike> map) {
		for (Theorem t : theorems) {
			map.put(t.getName(), t);
		}
	}

	@Override
	public void collectQualNames(Consumer<QualName> consumer) {
		for (Theorem theorem : theorems) {
			theorem.collectQualNames(consumer);
		}
	}

	public void substitute(String from, String to, SubstitutionData sd) {
		if (sd.didSubstituteFor(this)) return;
		sd.setSubstitutedFor(this);

		for (Theorem theorem : theorems) {
			theorem.substitute(from, to, sd);
		}
	}

	public TheoremPart copy(CloneData cd) {
		if (cd.containsCloneFor(this)) return (TheoremPart) cd.getCloneFor(this);
		TheoremPart clone;
		try {
			clone = (TheoremPart) super.clone();
			cd.addCloneFor(this, clone);
		
			List<Theorem> newTheorems = new ArrayList<Theorem>();
			for (Theorem theorem : theorems) {
				newTheorems.add(theorem.copy(cd));
			}
			clone.theorems = newTheorems;
			return clone;
		}
		catch (CloneNotSupportedException e) {
			System.out.println("Clone not supported in TheoremPart");
			System.exit(1);
			return null;
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Theorem t: theorems) {
			sb.append(t.toString());
			sb.append("\n");
		}
		return sb.toString();
	}
	
}