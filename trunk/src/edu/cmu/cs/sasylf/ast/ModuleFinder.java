package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.util.Span;

/**
 * Object representing a path for finding a module 
 * given a name or a package and a name.
 */
public interface ModuleFinder {

  public static final String[] EMPTY_PACKAGE = new String[0];

  /**
   * Find a module given just a name.
   * The implementation make look in the current package,
   * as well as in any generally imported package.
   * @param name must not be null
   * @param location where to report errors
   * @return compilation unit for module
   * @throws SASyLFError if none found, or if conflicting modules found
   */
  public abstract CompUnit findModule(String name, Span location);
  
  /**
   * Find a module in a given package.
   * @param id must not be null
   * @param location where to report errors
   * @return compilation unit for module
   * @throws SASyLFError if none found, or if conflicting modules found
   */
  public abstract CompUnit findModule(ModuleId id, Span location);

  /**
   * Set the current package for use in later {@link #findModule(String, Span)} calls.
   * @param pName must not be null
   */
  public abstract void setCurrentPackage(String[] pName);
}
