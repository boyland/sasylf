package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;

public class DerivationByWeakening extends DerivationWithArgs {
	public DerivationByWeakening(String n, Location l, Clause c) {
		super(n,l,c);
	}

	public String prettyPrintByClause() {
		return " by weakening";
	}

	public void typecheck(Context ctx) {
		super.typecheck(ctx);
		
    if (this.getArgs().size() != 1) {
      ErrorHandler.report(Errors.WRONG_WEAKENING_ARGUMENTS, this);
      return;
    }

    Fact arg = getArgs().get(0);
    // System.out.println("Weakening arg = " + arg);
    Element e = arg.getElement();
    Term adapted = DerivationByAnalysis.adapt(e.asTerm(), e, ctx, false);
    // System.out.println("Weakening arg, adapted = " + adapted);
    Term result = DerivationByAnalysis.adapt(getClause().asTerm(),getClause(),ctx,false);
    // System.out.println("Weakening result: " + result);
    while (adapted instanceof Abstraction) {
      Abstraction ab1 = (Abstraction)adapted;
      if (!(result instanceof Abstraction)) {
        ErrorHandler.report(Errors.BAD_WEAKENING, this); // missing variable binding for ab1.varName in result
        return;
      }
      Abstraction ab2 = (Abstraction)result;
      if (ab1.varName.equals(ab2.varName)) {
        if (!ab1.varType.equals(ab2.varType)) {
          ErrorHandler.report(Errors.BAD_WEAKENING, this); // variable binding for ab1.varName different in result
          return;
        }
        adapted = ab1.getBody();
        result = ab2.getBody();
      } else {
        result = ab2.getBody();
        /*if (result.hasBoundVar(1)) {
          ErrorHandler.report(Errors.BAD_WEAKENING,this); // new variable is used
        }*/
        result = result.incrFreeDeBruijn(-1); // remove variable
      }
    }
    while (result instanceof Abstraction) {
      result = ((Abstraction)result).getBody();
      /*if (result.hasBoundVar(1)) {
        ErrorHandler.report(Errors.BAD_WEAKENING,this); // new variable is used
      }*/
      result = result.incrFreeDeBruijn(-1); // remove variable      
    }
    if (!result.equals(adapted)) {
      ErrorHandler.report(Errors.BAD_WEAKENING, this); // main part of derivation is different
      return;
    }
    // System.out.println("Weakening succeeds!");
    
    // Permit induction on this term if source was a subderivation
    if (ctx.subderivations.contains(arg))
      ctx.subderivations.add(this);
    }
}
