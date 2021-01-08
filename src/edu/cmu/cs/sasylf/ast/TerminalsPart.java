package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

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
}