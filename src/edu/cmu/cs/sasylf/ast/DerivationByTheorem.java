package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;

public class DerivationByTheorem extends DerivationByIHRule {

  public DerivationByTheorem(String n, Location l, Clause c, String name, String kind) {
    super(n, l, c);
    theoremName = name;
    theoremKind = kind;
  }

  @Override
  public String getRuleName() { return theoremName; }
  
  public RuleLike getRule(Context ctx) {
    if (theorem == null) {
      // make sure we can find the rule
      theorem = ctx.ruleMap.get(theoremName);
      if (theorem == null) {
        theorem = ctx.recursiveTheorems.get(theoremName);
        if (theorem == null)
          ErrorHandler.report(Errors.THEOREM_NOT_FOUND, theoremName, this);
      }
      if (!theorem.isInterfaceOK()) {
        ErrorHandler.report(Errors.RULE_BAD, theoremName, this);
      }
      if (!(theorem instanceof Theorem)) {
        if (theoremKind.length() == 0) {
          String kind = "rule";
          ErrorHandler.recoverableError(Errors.THEOREM_KIND_MISSING, kind, this, "by\nby "+kind);
        } else {
          ErrorHandler.recoverableError(Errors.RULE_NOT_THEOREM, theoremName, this, theoremKind +"\nrule");
        }
      } else { 
        String kind = ((Theorem)theorem).getKind();
        if (theoremKind.length() == 0) {
          ErrorHandler.recoverableError(Errors.THEOREM_KIND_MISSING, kind, this, "by\nby "+kind);
        } else if (!kind.equals(theoremKind)) {
          ErrorHandler.recoverableError(Errors.THEOREM_KIND_WRONG, theoremKind + " " + theoremName, this, theoremKind+"\n"+kind);
        }
      }
    }
    return theorem;
  }

  @Override
  public void typecheck(Context ctx) {
    super.typecheck(ctx);
    
    if (theorem instanceof Theorem) {
      Theorem self = ctx.currentTheorem;
      Theorem other = (Theorem)theorem;
      if (self.getGroupLeader() == other.getGroupLeader()) {
        Fact inductiveArg = getArgs().get(other.getInductionIndex());
        if (!ctx.subderivations.contains(inductiveArg)) {
          Fact inductionVariable = self.getForalls().get(self.getInductionIndex());
          if (inductiveArg.equals(inductionVariable)) {
            if (self.getGroupIndex() <= other.getGroupIndex()) {
              ErrorHandler.report(Errors.MUTUAL_NOT_EARLIER, this);
            }
          } else if (inductiveArg instanceof SyntaxAssumption) {
            Term inductionTerm = inductionVariable.getElement().asTerm();
            Term inductiveTerm = inductiveArg.getElement().asTerm();
            Term inductionSub = inductionTerm.substitute(ctx.currentSub);
            Term inductiveSub = inductiveTerm.substitute(ctx.currentSub);
            // System.out.println("Is " + inductiveSub + " subterm of " + inductionSub + "?");
            if (!inductionSub.containsProper(inductiveSub)) {
              ErrorHandler.report(Errors.NOT_SUBDERIVATION, this);
            }
          } else {
            ErrorHandler.report(Errors.MUTUAL_NOT_SUBDERIVATION, this);
          }
        }
      }
      /*if (self.getAssumes() != null && !self.getAssumes().equals(other.getAssumes())) {
        ErrorHandler.warning("Possible loss of assume: " + self.getAssumes() + " != " + other.getAssumes(), this);
      }*/
    }
  }
  
  public String prettyPrintByClause() {
    return " by " + theoremKind + " " + theoremName;
  }

  private String theoremKind;
  private String theoremName;
  private RuleLike theorem;

}
