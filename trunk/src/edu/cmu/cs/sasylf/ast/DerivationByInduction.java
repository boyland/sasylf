package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Util.debug;
import edu.cmu.cs.sasylf.util.ErrorHandler;

public class DerivationByInduction extends DerivationByAnalysis {
	public DerivationByInduction(String n, Location l, Clause c, String derivName) {
		super(n,l,c, derivName);
	}

	public String byPhrase() { return "induction"; }

	public void typecheck(Context ctx) {
	  if (ctx.inductionVariable != null) {
	    ErrorHandler.report(Errors.INDUCTION_REPEAT,this,"induction\ncase analysis");
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

		super.typecheck(ctx);
	}
}
