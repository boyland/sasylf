package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import edu.cmu.cs.sasylf.SubstitutionData;
import edu.cmu.cs.sasylf.util.CopyData;
import edu.cmu.cs.sasylf.util.ErrorReport;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.SASyLFError;

/**
 * A sequence of theorems.
 * Theorem may be mutually recursive only as connected with 'and".
 */
public class TheoremPart implements Part {
	private List<Theorem> theorems;

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

	@Override
	public void substitute(SubstitutionData sd) {
		if (sd.didSubstituteFor(this)) return;
		sd.setSubstitutedFor(this);
		
		for (Theorem theorem : theorems) {
			theorem.substitute(sd);
		}
	}

	@Override
	public TheoremPart copy(CopyData cd) {
		if (cd.containsCopyFor(this)) return (TheoremPart) cd.getCopyFor(this);
		TheoremPart clone;
		try {
			clone = (TheoremPart) super.clone();
		}
		catch(CloneNotSupportedException e) {
			ErrorReport report = new ErrorReport(Errors.INTERNAL_ERROR, "Clone not supported in class: " + getClass(), null, null, true);
			throw new SASyLFError(report);
		}
		cd.addCopyFor(this, clone);
		clone.theorems = new ArrayList<Theorem>();
		for (Theorem theorem : theorems) {
			clone.theorems.add(theorem.copy(cd));
		}
		return clone;
	}

	@Override
	public List<ModuleComponent> argsParams() {
		List<ModuleComponent> theorems = new ArrayList<>();

		for (Theorem theorem : getTheorems()) {
			theorems.add(theorem);
		}
		
		return theorems;
	}

	@Override
	public void collectTopLevelAsModuleComponents(Collection<ModuleComponent> things) {
		theorems.forEach(things::add);
	}
	
}