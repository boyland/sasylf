package edu.cmu.cs.sasylf.reduction;

import java.util.List;

import edu.cmu.cs.sasylf.ast.Context;
import edu.cmu.cs.sasylf.ast.Fact;
import edu.cmu.cs.sasylf.ast.Node;
import edu.cmu.cs.sasylf.ast.SyntaxAssumption;
import edu.cmu.cs.sasylf.ast.Theorem;
import edu.cmu.cs.sasylf.term.Pair;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;

/**
 * Structural induction on a term or a derivation
 */
public class StructuralInduction extends InductionSchema {

  /**
   * Number of formal parameter to perform induction on.
   * @param a zero-based index of argument to do induction on
   */
  public StructuralInduction(int a) {
    argNum = a;
  }

  public static StructuralInduction create(Theorem thm, String name, Node errorPoint) {    
    List<Fact> foralls = thm.getForalls();
    for (int i=0; i < foralls.size(); ++i) {
      Fact f = foralls.get(i);
      if (f.getName().equals(name)) {
        return new StructuralInduction(i);
      }
    }
    if (errorPoint != null) {
      ErrorHandler.recoverableError(Errors.INDUCTION_NOT_INPUT, "Induction item should be a theorem input",errorPoint);
    }
    return null;
  }
  
  @Override
  public boolean matches(InductionSchema s, Node errorPoint, boolean equality) {
    if (s instanceof StructuralInduction) {
      return !equality || argNum == ((StructuralInduction)s).argNum;
    }
    if (errorPoint != null) {
      ErrorHandler.recoverableError("Expected induction by " + s + ", but found " + describe(), errorPoint);
    }
    return false;
  }

  @Override
  public Reduction reduces(Context ctx, InductionSchema s, List<Fact> args, Node errorPoint) {
    assert matches(s,null, false) : "reduces called with bad schema " + s;
    assert args != null;
    Fact source = getSubject(ctx.currentTheorem.getForalls());
    Fact subject = ((StructuralInduction)s).getSubject(args);
    
    // check equality first
    if (source.equals(subject)) return Reduction.EQUAL;

    // syntax handled differently than derivations
    if (subject instanceof SyntaxAssumption) {
      Term inductionTerm = source.getElement().asTerm();
      Term inductiveTerm = subject.getElement().asTerm();
      Term inductionSub = inductionTerm.substitute(ctx.currentSub).stripUnusedLambdas();
      Term inductiveSub = inductiveTerm.substitute(ctx.currentSub).stripUnusedLambdas();
      // System.out.println("Is " + inductiveSub + " subterm of " + inductionSub + "?");
      if (inductionSub.equals(inductiveSub)) return Reduction.EQUAL;
      if (inductionSub.containsProper(inductiveSub)) return Reduction.LESS;
      if (errorPoint != null) {
        ErrorHandler.recoverableError(Errors.NOT_SUBDERIVATION, ", " + subject + " not inside of " + source.getName(), errorPoint);
      }
      return Reduction.NONE;
    }

    Pair<Fact,Integer> p = ctx.subderivations.get(subject);
    if (p == null) {
      if (errorPoint != null) {
        ErrorHandler.recoverableError("Not a subderivation: " + subject, errorPoint);
      }
    } else if (p.first != source) {
      if (errorPoint != null) {
        ErrorHandler.recoverableError("Not a subderivation of " + source + " : " + subject, errorPoint);
      }
    } else {
      return p.second == 0 ? Reduction.EQUAL : Reduction.LESS;
    }
    
    return Reduction.NONE;
  }

  @Override
  public String describe() {
    return "structure on arg #" + argNum;
  }
  
  @Override
  public int hashCode() {
    return argNum;
  }
  
  public Fact getSubject(List<Fact> args) {
    return args.get(argNum);
  }

  private final int argNum;
}
