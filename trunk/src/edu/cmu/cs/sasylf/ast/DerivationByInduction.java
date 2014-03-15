package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Util.debug;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Util;

public class DerivationByInduction extends DerivationByAnalysis {
	public DerivationByInduction(String n, Location l, Clause c, String derivName) {
		super(n,l,c, derivName);
	}

	public String byPhrase() { return "induction"; }

	public void typecheck(Context ctx) {
	  if (ctx.inductionVariable != null) { 
	    // XXX: should be OK to repeat the same thing
	    ErrorHandler.report(Errors.INDUCTION_REPEAT,this,"induction\ncase analysis");
	  }
	  if (!ctx.currentTheorem.getDerivations().contains(this)) {
	    ErrorHandler.report("Induction can only be declared at top level of a proof.\nSuggest 'use induction by " + getArgStrings() + "'", this);
	  }
    computeTargetDerivation(ctx);
		ctx.inductionVariable = getTargetDerivation();
		debug("induction variable " + ctx.inductionVariable + " of type " + ctx.inductionVariable.getClass());

		if (!(getTargetDerivation() instanceof DerivationByAssumption)
				&& !(getTargetDerivation() instanceof NonTerminalAssumption))
			ErrorHandler.report(Errors.INDUCTION_NOT_INPUT,"Fact "+ getTargetDerivationName() + " must be one of the assumed facts in the forall clause of this theorem", this);
		
		if (getTargetDerivation() instanceof NonTerminalAssumption && !((NonTerminalAssumption)getTargetDerivation()).isTheoremArg())
			ErrorHandler.report(Errors.INDUCTION_NOT_INPUT,"Nonterminal "+ getTargetDerivationName() + " must be an explicit forall clause argument of this theorem", this);

		// find which argument of the theorem this is
		ctx.inductionPosition = -1;
		for (int i = 0; i < ctx.currentTheorem.getForalls().size(); ++i) {
			if (getTargetDerivation().equals(ctx.currentTheorem.getForalls().get(i)))
				ctx.inductionPosition = i;
		}
		if (ctx.inductionPosition == -1)
			ErrorHandler.report(Errors.INDUCTION_NOT_INPUT,"Fact "+ getTargetDerivationName() + " must be one of the assumed facts in the forall clause of this theorem", this);

		// special case: handle "use induction by"
		if (getClause() instanceof AndClauseUse && ((AndClauseUse)getClause()).getClauses().isEmpty()) {
		  Util.debug("use induction detected.");
		  return;
		}
		
		if (getArgStrings().size() > 1) {
		  ErrorHandler.report("'induction' combined with a case analysis can accept only one argument.", this);
		}
		super.typecheck(ctx);
	}
}
