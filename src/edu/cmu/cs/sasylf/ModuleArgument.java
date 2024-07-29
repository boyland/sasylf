package edu.cmu.cs.sasylf;

public interface ModuleArgument {

  /**
   * Returns true if and only if this ModuleArgument object can be applied to
   * the given parameter.
   * 
   * Raises an exception and returns false if and only if the argument is not
   * applicable to the given parameter.
   * @param param
   * @return true if and only if this ModuleArgument object can be applied to, false otherwise
   */
  public boolean matchesParam(ModuleArgument param);

}