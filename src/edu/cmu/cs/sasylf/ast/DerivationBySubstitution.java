package edu.cmu.cs.sasylf.ast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.BoundVar;
import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.Util;


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
		
		if (!(arg0 instanceof ClauseUse)) {
		  ErrorHandler.report("First argument of substitution must be a judgment instance.",this);
		}
		if (!(arg1 instanceof ClauseUse)) {
		  ErrorHandler.report("Second argument of substitution must be a judgment instance.", this);
		}
		
    Term subContext = DerivationByAnalysis.adapt(arg0.asTerm(),arg0,ctx,false);
		Term source = DerivationByAnalysis.adapt(arg1.asTerm(),arg0,ctx,false);
		
		Util.debug("subContext = " + subContext);
		Util.debug("source = " + source);
		
		Term result = doSubst(ctx, subContext, source, new ArrayDeque<Term>());
		
    Util.debug("result = " + result);
		
    // verify result is the second substituted for (and eliminating) the assumption of the first
    Term claimedResult = DerivationByAnalysis.adapt(getClause().asTerm(),getClause(),ctx,false);
    //System.out.println("claimed = " + claimedResult);

    if (!result.equals(claimedResult)) {
      ErrorHandler.report(Errors.BAD_RULE_APPLICATION, "The claimed fact is not justified by applying substitution",this,
          "  (got " + result + " instead)");
    } 
    
    checkRootMatch(ctx,getArgs().get(0).getElement(),this.getElement(),this);

    // TODO: Is it ever safe to do induction after a substitution?
	}

  /**
   * @param func An abstraction we try to call with the arg
   * @param arg A (possibly dependent) term to use as argument
   * @param varTypes types of bound variables in scope, pushed on. 
   * @return result term
   * @throws SASyLFError if an error is found
   */
	private Term doSubst(Context ctx, Term func, Term arg, Deque<Term> varTypes) {
	  if (func instanceof Abstraction) {
	    Abstraction f = (Abstraction)func;
	    if (arg instanceof Abstraction) {
	      Abstraction a = (Abstraction)arg;
	      if (!f.varType.equals(a.varType)) {
	        ErrorHandler.report("Context of first argument to substitution must have as prefix that of second", this);
	      }
	      varTypes.push(f.varType);
	      Term result = doSubst(ctx, f.getBody(),a.getBody(),varTypes);
	      varTypes.pop();
	      return Abstraction.make(f.varName, f.varType, result);
	    } else if (f.varType.equals(arg)){
	      ErrorHandler.warning("Internal warning: Using a new feature?", this);
	      if (f.getBody().hasBoundVar(1)) {
	        ErrorHandler.report("Internal error: uses context derivation?", this);
	      }
	      return f.getBody().incrFreeDeBruijn(-1);
	    } else if (f.getBody() instanceof Abstraction) {
	      Term tyV = f.varType;
	      for (Term ty : varTypes) {
	        tyV = Facade.Abs(ty, tyV);
	      }
        FreeVar v = FreeVar.fresh("subst", tyV);
        int n = varTypes.size();
        Term app = v;
        if (n > 0) {
          List<Term> va = new ArrayList<Term>();
          for (int i=0; i < n; ++i) {
            va.add(new BoundVar(n-i));
          }
          app = Facade.App(v, va);
        } 
	      Term fv = f.apply(Collections.singletonList(app), 0);
	      Abstraction f2 = (Abstraction)fv;
	      Util.debug("About to unify " + f2.varType + " and " + arg);
	      Substitution sub;
        try {
          sub = f2.varType.unify(arg);
        } catch (UnificationFailed e) {
          ErrorHandler.report(Errors.SUBSTITUTION_FAILED, this, "  (" + f2.varType + " != " + arg + ")");
          return null;
        }
	      Set<FreeVar> unavoided = sub.selectUnavoidable(ctx.inputVars);
	      if (!unavoided.isEmpty()) {
	        ErrorHandler.report("Substitution would constrain variables, including " + unavoided.iterator().next(), this);
	      }
	      Term result = f2.getBody();
	      if (result.hasBoundVar(1)) {
	        ErrorHandler.report("Internal error: uses context derivation?", this);
	      } 
	      return result.incrFreeDeBruijn(-1).substitute(sub);
	    } else {
	      ErrorHandler.report("Cannot determine where substitution would happen",  this);
	    }
	  }
	  return null;
	}

}
