package edu.cmu.cs.sasylf.ast;

import java.util.LinkedList;
import java.util.List;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.BoundVar;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;

public class DerivationByExchange extends DerivationWithArgs {
	public DerivationByExchange(String n, Location l, Clause c) {
		super(n,l,c);
	}

	public String prettyPrintByClause() {
		return " by exchange";
	}

	public void typecheck(Context ctx) {
		super.typecheck(ctx);

    if (this.getArgs().size() != 1) {
      ErrorHandler.report(Errors.WRONG_EXCHANGE_ARGUMENTS, this);
      return;
    }

    Fact arg = getArgs().get(0);
    //System.out.println("Exchange arg = " + arg);
    Element e = arg.getElement();
    Term adapted = DerivationByAnalysis.adapt(e.asTerm(), e, ctx, false);
    //System.out.println("Exchange arg, adapted = " + adapted);
    Term result = DerivationByAnalysis.adapt(getClause().asTerm(),getClause(),ctx,false);
    //System.out.println("Exchange result: " + result);
    
    if (!checkExchange(result,adapted)) {
      ErrorHandler.report(Errors.BAD_EXCHANGE, this);
    }

		if (ctx.subderivations.contains(arg))
			ctx.subderivations.add(this);
	}
	
	private static boolean checkExchange(Term t1, Term t2) {
	  // NB: If the following line is uncommented,
	  // exchange will be (safely) allowed in more situations
	  // but the cases where it works will be harder to explain.
	  // if (t1.equals(t2)) return true;
	  if (t1 instanceof Abstraction) {
	    Abstraction ab1 = (Abstraction)t1;
	    List<Term> remaining = new LinkedList<Term>();
	    //System.out.println("Checking " + t2);
	    //System.out.println(" against " + ab1);
	    Term body2 = removeMatching(t2,remaining,ab1.varName,ab1.varType);
	    if (body2 == null) return false;
	    //System.out.println("Proceeding with exchanged body: " + body2);
	    return checkExchange(ab1.getBody(),body2);
	  } else return t1.equals(t2);
	}
	
	private static Term removeMatching(Term t, List<Term> rem, String name, Term type) {
	  if (t instanceof Abstraction) {
	    Abstraction ab = (Abstraction)t;
	    if (ab.varName.equals(name)) {
	      if (ab.varType.equals(type)) {
	        rem.add(new BoundVar(rem.size()+1));
	        return ab.getBody().apply(rem, rem.size());
	      }
	      // System.out.println("Wrong type");
	      return null;
	    } else {
	      rem.add(0, new BoundVar(rem.size()+1));
	      Term result = removeMatching(ab.getBody(),rem,name,type.incrFreeDeBruijn(1));
	      if (result == null) return null;
	      return Abstraction.make(ab.varName,ab.varType,result);
	    }
	  } else return null;
	}
}
