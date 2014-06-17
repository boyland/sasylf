package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;

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
    
    if (!(e instanceof ClauseUse)) {
      ErrorHandler.report("Do not weaken syntax using 'weakening'\nWeakening is implicit for syntax", this);
    }
    
    NonTerminal srcRoot = e.getRoot();
    NonTerminal trgRoot = getClause().getRoot();
    Term source = ctx.toTerm(e);
    Term result = ctx.toTerm(getClause());

    // perform relaxation first
    while (srcRoot != null && !srcRoot.equals(trgRoot)) {
      Relaxation r;
      if (ctx.relaxationMap == null || (r = ctx.relaxationMap.get(srcRoot)) == null) {
        ErrorHandler.report("No way known to relax " + srcRoot + " to " + trgRoot, this);    
        return;
      }
      Term relaxed = r.relax(source);
      if (relaxed == null) {
        ErrorHandler.report("Can only relax " + srcRoot + " to " + trgRoot + " if one uses the exact same same variable and assumptions", this);
      }
      source = relaxed;
      srcRoot = r.getResult();
    }
    
    while (source instanceof Abstraction) {
      Abstraction ab1 = (Abstraction)source;
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
        source = ab1.getBody();
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
    if (!result.equals(source)) {
      ErrorHandler.report(Errors.BAD_WEAKENING, this); // main part of derivation is different
      return;
    }
    
    checkRootMatch(ctx,e,this.getElement(),this);
    
    // System.out.println("Weakening succeeds!");
    
    // Permit induction on this term if source was a subderivation
    if (ctx.subderivations.containsKey(arg))
      ctx.subderivations.put(this,ctx.subderivations.get(arg));
    }
}
