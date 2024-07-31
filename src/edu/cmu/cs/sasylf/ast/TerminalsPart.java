package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import edu.cmu.cs.sasylf.CopyData;
import edu.cmu.cs.sasylf.ModuleComponent;
import edu.cmu.cs.sasylf.SubstitutionData;
import edu.cmu.cs.sasylf.util.ErrorReport;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.SASyLFError;

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
			ErrorReport report = new ErrorReport(Errors.INTERNAL_ERROR, "Clone not supported in class: " + getClass(), null, null, true);
			throw new SASyLFError(report);
		}
	}

	@Override
	public List<ModuleComponent> argsParams() {
		return new ArrayList<>();
	}

	@Override
	public void collectTopLevelAsModuleComponents(Collection<ModuleComponent> things) {
		// do nothing
	}
	
}