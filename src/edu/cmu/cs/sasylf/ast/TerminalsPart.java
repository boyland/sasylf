package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import edu.cmu.cs.sasylf.CopyData;
import edu.cmu.cs.sasylf.SubstitutionData;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.UpdatableErrorReport;

/**
 * Declared terminals/
 */
public class TerminalsPart implements Part {
	private Set<String> declaredTerminals;
	
	/**
	 * Create a part for terminals
	 * @param terms terminals (must not be null)
	 */
	public TerminalsPart(Set<String> terms) {
		declaredTerminals = new HashSet<String>(terms);
	}
	
	/**
	 * @param out
	 */
	@Override
	public void prettyPrint(PrintWriter out) {
		out.print("terminals ");
		for (String t : declaredTerminals) {
			out.print(t);
		}
		out.println();
	}

	@Override
	public void typecheck(Context ctx) {
		ctx.termSet.addAll(declaredTerminals);
	}
	
	@Override
	public void collectTopLevel(Collection<? super Node> things) {
		// do nothing
	}
	
	@Override
	public void collectRuleLike(Map<String,? super RuleLike> map) {
		// do nothing
	}

	@Override
	public void collectQualNames(Consumer<QualName> consumer) {
		// Do nothing
	}

	@Override
	public void substitute(SubstitutionData sd) {
		/*
		 * Do nothing, because there is nothing we could possibly need
		 * to substitute inside of a terminals declaration.
		 */
	}

	@Override
	public TerminalsPart copy(CopyData cd) {
		if (cd.containsCopyFor(this)) return (TerminalsPart) cd.getCopyFor(this);

		try {
			TerminalsPart clone = (TerminalsPart) super.clone();
			cd.addCopyFor(this, clone);
			HashSet<String> newDeclaredTerminals = new HashSet<>();
			newDeclaredTerminals.addAll(declaredTerminals);
			clone.declaredTerminals = newDeclaredTerminals;
			return clone;
		}
		catch (CloneNotSupportedException e) {
			UpdatableErrorReport report = new UpdatableErrorReport(Errors.INTERNAL_ERROR, "Clone not supported in class: " + getClass(), null);
			throw new SASyLFError(report);
		}
	}
	
}