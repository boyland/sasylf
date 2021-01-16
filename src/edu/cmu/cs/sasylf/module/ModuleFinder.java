package edu.cmu.cs.sasylf.module;

import edu.cmu.cs.sasylf.Proof;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.Span;

/**
 * Object representing a path for finding a module 
 * given a name or a package and a name.
 */
public interface ModuleFinder {

	public static final String[] EMPTY_PACKAGE = new String[0];

	/**
	 * Check to see if this module finder has a candidate module with the given name.
	 * Attempting to find it may cause errors (e.g. parse errors), but this method
	 * will simply return true (without errors)
	 * @param id module id to check, must not be null
	 * @return whether something representing itself as a module is present.
	 */
	public abstract boolean hasCandidate(ModuleId id);
	
	/**
	 * Find a module given just a name.
	 * The implementation may look in the current package,
	 * as well as in any generally imported package.
	 * @param name must not be null
	 * @param location where to report errors
	 * @return compilation unit for module
	 * @throws SASyLFError if none found, or if conflicting modules found
	 */
	public abstract Module findModule(String name, Span location);

	/**
	 * Find a module in a given package.
	 * @param id must not be null
	 * @param location where to report errors
	 * @return compilation unit for module
	 * @throws SASyLFError if none found, or if conflicting modules found
	 */
	public abstract Module findModule(ModuleId id, Span location);

	/**
	 * Find a module in a given package and return the results
	 * of reading it (errors etc).
	 * @param id must not be null
	 * @param location where to report errors in finding the module
	 * @return Proof object for the module
	 */
	public abstract Proof findProof(ModuleId id, Span location);
	
	/**
	 * Set the current package for use in later {@link #findModule(String, Span)} calls.
	 * @param pName must not be null
	 */
	public abstract void setCurrentPackage(String[] pName);
}
