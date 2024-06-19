package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import edu.cmu.cs.sasylf.CloneData;
import edu.cmu.cs.sasylf.SubstitutionData;
import edu.cmu.cs.sasylf.term.Substitution;

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

	public void substitute(String from, String to, SubstitutionData sd) {
		// Do nothing
		// TODO: I'm pretty sure that nothing should be done here
	}

	/**
	 * @deprecated
	 * @return a clone of this part
	 */
	public TerminalsPart clone() {
		try {
			TerminalsPart clone = (TerminalsPart) super.clone();
			HashSet<String> newDeclaredTerminals = new HashSet<>();
			newDeclaredTerminals.addAll(declaredTerminals);
			clone.declaredTerminals = newDeclaredTerminals;
			return clone;
		}
		catch (CloneNotSupportedException e) {
			throw new Error("unexpected error: " + e);
		}
	}

	public TerminalsPart copy(CloneData cd) {
		if (cd.containsCloneFor(this)) return (TerminalsPart) cd.getCloneFor(this);

		try {
			TerminalsPart clone = (TerminalsPart) super.clone();
			cd.addCloneFor(this, clone);
			HashSet<String> newDeclaredTerminals = new HashSet<>();
			newDeclaredTerminals.addAll(declaredTerminals);
			clone.declaredTerminals = newDeclaredTerminals;
			return clone;
		}
		catch (CloneNotSupportedException e) {
			throw new Error("unexpected error: " + e);
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("terminals ");
		for (String t : declaredTerminals) {
			sb.append(t);
			sb.append(" ");
		}
		return sb.toString();
	}
	
}