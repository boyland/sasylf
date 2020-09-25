package edu.cmu.cs.sasylf.module;

import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.util.Span;

/**
 * A container of modules.
 */
public interface ModuleProvider {

	/** Check whether this provider has something that could be seen
	 * as a module with this identifier.
	 * @param id identifier to check, must not be null
	 * @return whether this provider provides this module
	 */
	public boolean has(ModuleId id);

	/**
	 * Get the module for this identifier or report errors and return null.
	 * @param mf module finder to use for further module lookup
	 * @param id module identifier, must not be null
	 * @param location location to use for errors
	 * @return module or null (in which case some errors were reported)
	 */
	public CompUnit get(PathModuleFinder mf, ModuleId id, Span location);
	
	/**
	 * Add a listener that will be informed when there are a module
	 * events.
	 * @param listener must not be null
	 */
	public void addModuleEventListener(ModuleEventListener listener);
	
	// XXX: remove/deleteModuleListener
}