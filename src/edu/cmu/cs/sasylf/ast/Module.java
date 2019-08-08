package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;

/**
 * The compile-time representation of a SASyLF module.
 * Commonly a compilation unit, but may also be an instance of
 * a compilation unit with requirements.
 */
public interface Module {

	/**
	 * Return the module name of this compilation unit (may be null).
	 * @return module name of this compilation unit.
	 */
	public abstract String getName();

	public abstract void prettyPrint(PrintWriter out);

	/**
	 * Type check this compilation unit in the default module context.
	 * @return
	 */
	public abstract boolean typecheck();

	/**
	 * Put all top-level declarations is this compilation unit (module)
	 * into the collection
	 * @param things collection to add to.
	 */
	public abstract void collectTopLevel(Collection<? super Node> things);

	/**
	 * Get all the things that can be called (rules and theorems) in a map
	 * where it can be searched by the UI.
	 * @param map map to add to
	 */
	public abstract void collectRuleLike(Map<String, ? super RuleLike> map);

}