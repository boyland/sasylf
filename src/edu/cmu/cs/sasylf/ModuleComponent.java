package edu.cmu.cs.sasylf;

import java.util.Map;
import java.util.Optional;

import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.ast.Judgment;
import edu.cmu.cs.sasylf.ast.ModulePart;
import edu.cmu.cs.sasylf.ast.Syntax;

/**
 * Represents an argument or parameter to a CompUnit.
 * <br/><br/>
 * This interface is implemented by Syntax, Judgment, and Theorem.
 */
public interface ModuleComponent {

  /**
   * Returns true if and only if this ModuleArgument object can be applied to
   * the given parameter.
   * 
   * Raises an exception and returns false if and only if the argument is not
   * applicable to the given parameter.
   * @param param
   * @return true if and only if this ModuleArgument object can be applied to, false otherwise
   */
  public Optional<SubstitutionData> matchesParam( // TODO: This could be removed from the interface
    ModuleComponent paramModArg,
		ModulePart mp,
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

  /**
   * Provides this ModuleArgument object to the given compilation unit.
   * Returns true if and only if this ModuleArgument object is applicable to the next parameter in the compilation unit.
   * @param cu the compilation unit to provide this ModuleArgument object to
   * @param mp the module part in which this application is taking place
   * @param paramToArgSyntax the map of syntax parameters to syntax arguments
   * @param paramToArgJudgment the map of judgment parameters to judgment arguments
   * @return true if and only if this ModuleArgument object is applicable to the next parameter in the compilation unit
   */
  public boolean provideTo(CompUnit cu, ModulePart mp, Map<Syntax, Syntax> paramToArgSyntax, Map<Judgment, Judgment> paramToArgJudgment);

}