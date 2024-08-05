package edu.cmu.cs.sasylf.ast;

import java.util.Map;
import java.util.Optional;

import edu.cmu.cs.sasylf.util.CopyData;

/**
 * Represents an argument or parameter to a CompUnit.
 * <br/><br/>
 * This interface is implemented by Syntax, Judgment, and Theorem.
 */
public interface ModuleComponent {

  /**
   * Returns a nonempty Optional if and only if this ModuleArgument object can be applied to
   * the given parameter. Otherwise, returns an empty Optional.
   * <br/><br/>
   * If the Optional is nonempty, it contains the SubstitutionData object that should
   * be used to apply this ModuleArgument object to the given parameter.
   * 
   * applicable to the given parameter.
   * @param param
   * @return true if and only if this ModuleArgument object can be applied to, false otherwise
   */
  public Optional<SubstitutionData> matchesParam(
    ModuleComponent paramModArg,
		ModulePart mp,
    // TODO: examine the following two maps
		Map<Syntax, Syntax> paramToArgSyntax,
		Map<Judgment, Judgment> paramToArgJudgment
  );

  /**
   * Returns the name of this ModuleArgument object.
   * @return the name of this ModuleArgument object
   */
  public String getName();

  /**
   * Returns the kind of this ModuleArgument object.
   * <br/><br/>
   * Kind is either "syntax", "judgment", or "theorem".
   * @return the kind of this ModuleArgument object
   */
  public String getKind();

  /**
   * Creates and returns a copy of this ModuleArgument object.
   * @param cd the CopyData object to be used in the copy
   * @return a copy of this ModuleArgument object
   */
  public ModuleComponent copy(CopyData cd);

}