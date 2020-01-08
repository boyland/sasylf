package edu.cmu.cs.sasylf.module;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;

import edu.cmu.cs.sasylf.ast.Context;
import edu.cmu.cs.sasylf.ast.Node;
import edu.cmu.cs.sasylf.ast.RuleLike;

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
	
	/**
	 * Return true if this module has non-empty requirements.
	 * @return whether has requirements.
	 */
	public boolean isAbstract();
	
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

	/**
	 * Return named entity within this module.
	 * The context is used for versioning.
	 * @return a module, syntax, judgment, rule, theorem or module declared in this module,
	 * or null if nothing with this name.
	 */
	public abstract Object getDeclaration(Context ctx, String name);
}