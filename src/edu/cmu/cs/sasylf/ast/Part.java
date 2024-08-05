package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import edu.cmu.cs.sasylf.SubstitutionData;
import edu.cmu.cs.sasylf.util.CopyData;

/**
 * A part of a compilation unit/module.
 */
public interface Part extends Cloneable {

	/**
	 * Type check the elements in this chunk.
	 * @param ctx context to use, must not be null
	 */
	public abstract void typecheck(Context ctx);

	public abstract void prettyPrint(PrintWriter out);

	/**
	 * Export all top-level declarations in this chunk
	 * @param things collection to place in, must not be null.
	 */
	public abstract void collectTopLevel(Collection<? super Node> things);

	/**
	 * Export all top-level declarations in this chunk that as ModuleComponent objects
	 * @param things collection to place in, must not be null.
	 */
	public abstract void collectTopLevelAsModuleComponents(Collection<ModuleComponent> things);

	/**
	 * Export all rule-likes into a provided map
	 * @param map destination, must not be null.
	 */
	public abstract void collectRuleLike(Map<String,? super RuleLike> map);
	
	public abstract void collectQualNames(Consumer<QualName> consumer);

	/**
	 * Substitute inside of this Part according to <code> sd </code>
	 * @param sd substitution data
	 */
	public abstract void substitute(SubstitutionData sd);
	
	/**
	 * Creates and returns a deep copy of this Part
	 * @param cd clone data
	 * @return a deep copy of this Part
	 */
	public abstract Part copy(CopyData cd);

	/**
	 * Returns the components inside of this part that could be used as arguments/parameters
	 * @return the components inside of this part that could be used as arguments/parameters
	 */
	public List<ModuleComponent> argsParams();

}