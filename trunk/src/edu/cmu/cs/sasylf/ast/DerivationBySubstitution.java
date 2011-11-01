package edu.cmu.cs.sasylf.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Atom;
import edu.cmu.cs.sasylf.term.BoundVar;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.util.ErrorHandler;


public class DerivationBySubstitution extends DerivationWithArgs {
	public DerivationBySubstitution(String n, Location l, Clause c) {
		super(n,l,c);
	}
	
	public String prettyPrintByClause() {
		return " by substitution";
	}

	@Override
	public void typecheck(Context ctx) {
		super.typecheck(ctx);
		
		if (this.getArgs().size() != 2) {
			ErrorHandler.report(Errors.WRONG_SUBSTITUTION_ARGUMENTS, this);
		}

		// get terms for arguments
		Element arg0 = this.getArgs().get(0).getElement();
		Element arg1 = this.getArgs().get(1).getElement();
		
    Term subContext = DerivationByAnalysis.adapt(arg0.asTerm(),arg0,ctx,false);
		Term source = DerivationByAnalysis.adapt(arg1.asTerm(),arg0,ctx,false);
		
		// System.out.println("subContext = " + subContext);
		// System.out.println("source = " + source);
		
		// for now, only support substituting for last assumption
		
		if (!(subContext instanceof Abstraction) ||
		    !(((Abstraction)subContext).getBody() instanceof Abstraction)) {
		  ErrorHandler.report(Errors.SUBSTITUTION_NO_CONTEXT, this, "  (no context on " + arg0 + ")");
		  return;
		}
		
		Term type = ((Abstraction)subContext).varType;
		FreeVar fv = FreeVar.fresh("subst",type);
		
		// verify second is a proof that matches the first's assumption
		List<Term> subs = new ArrayList<Term>();
		subs.add(fv);
		
		Abstraction result1 = (Abstraction)subContext.apply(subs,0);
    // System.out.println("result1 = " + result1);
		
    Term result = doSubst(result1, source, fv);
    if (result == null) return; // error found
		
    // System.out.println("result = " + result);
		
    // verify result is the second substituted for (and eliminating) the assumption of the first
    Term claimedResult = DerivationByAnalysis.adapt(getClause().asTerm(),getClause(),ctx,false);
    //System.out.println("claimed = " + claimedResult);

    if (!result.equals(claimedResult)) {
      ErrorHandler.report(Errors.BAD_RULE_APPLICATION, "The claimed fact is not justified by applying substitution",this,
          "  (got " + result + " instead)");
    } else {
      // Permit induction on this term if source was a subderivation
      if (ctx.subderivations.contains(getArgs().get(0)))
        ctx.subderivations.add(this);
    }
	}

  /**
   * @param func An abstraction we try to call with the arg
   * @param arg A (possibly dependent) term to use as argument
   * @param fv free variable that may be bound to permit unification to suuceed
   * @return result term, or null if error found
   */
  private Term doSubst(Abstraction func, Term arg, FreeVar fv) {
    // System.out.println("trying with " + arg);
    if (arg instanceof Abstraction) {
      // I have to unpack the body because otherwise fv can't be bound to a variable.
      // There's probably a better way, but I can't figure it out.
      Abstraction darg = (Abstraction)arg;
      FreeVar temp = FreeVar.fresh(darg.varName,darg.varType);
      List<Term> temps = new ArrayList<Term>();
      temps.add(temp);
      Term body = darg.apply(temps, 0);
      Term result = doSubst(func,body,fv);
      if (result == null) return result;
      Substitution sub = new Substitution(new BoundVar(1),temp);
      return Abstraction.make(darg.varName, darg.varType, result.substitute(sub));
    } else {
      Substitution sub;
      try {
        sub = func.varType.unify(arg);
      } catch (UnificationFailed e) {
        ErrorHandler.report(Errors.SUBSTITUTION_FAILED, this, "  (" + func.varType + " != " + arg + ")");
        return null;
      }
      // System.out.println("sub = " + sub);

      // We check that no variables have been forced together.
      for (Map.Entry<Atom,Term> e : sub.getMap().entrySet()) {
        if (e.getKey() == fv) continue;
        ErrorHandler.report(Errors.BAD_RULE_APPLICATION, "The claimed fact is not justified by applying substitution (binds free variable " + e.getKey() + ")",this,
            "\t(not general enough, in particular " + e.getKey() + " cannot be assumed to be " + e.getValue() + ")");
        return null;
      }

      assert !func.getBody().hasBoundVar(1) : "internal derivation used?";

      Term result = func.getBody().incrFreeDeBruijn(-1).substitute(sub);
      return result;
    }
  }

}
