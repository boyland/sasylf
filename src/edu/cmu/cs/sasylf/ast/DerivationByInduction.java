package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.reduction.InductionSchema;
import edu.cmu.cs.sasylf.reduction.StructuralInduction;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Util;

public class DerivationByInduction extends DerivationByAnalysis {
	public DerivationByInduction(String n, Location l, Clause c, String derivName) {
		super(n,l,c, derivName);
	}
	public DerivationByInduction(String n, Location l, Clause c) {
	  super(n,l,c);
	}

	public String byPhrase() { return "induction"; }

	public void typecheck(Context ctx) {
    if (!ctx.currentTheorem.getDerivations().contains(this)) {
      ErrorHandler.report("Induction can only be declared at top level of a proof.\nSuggest 'use induction by " + getArgStrings() + "'", this);
    }
	  InductionSchema is = InductionSchema.create(ctx.currentTheorem, getArgStrings(), this);

	  if (is != null && !ctx.currentTheorem.getInductionSchema().equals(is)) {
      ErrorHandler.report(Errors.INDUCTION_REPEAT,this,"induction\ncase analysis");	    
	  }

		// special case: handle "use induction by"
		if (getClause() instanceof AndClauseUse && ((AndClauseUse)getClause()).getClauses().isEmpty()) {
		  Util.debug("use induction detected.");
		  return;
		}
		
		if (is != null && !(is instanceof StructuralInduction)) {
		  ErrorHandler.report("'induction' combined with a case analysis can accept only one structural induction argument.", this);
		}
		super.typecheck(ctx);
	}
}
