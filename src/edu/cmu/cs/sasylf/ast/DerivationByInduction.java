package edu.cmu.cs.sasylf.ast;

import java.util.*;
import java.io.*;

import edu.cmu.cs.sasylf.util.ErrorHandler;
import static edu.cmu.cs.sasylf.util.Util.*;

public class DerivationByInduction extends DerivationByAnalysis {
	public DerivationByInduction(String n, Location l, Clause c, String derivName) {
		super(n,l,c, derivName);
	}

	public String byPhrase() { return "induction"; }

	public void typecheck(Context ctx) {
		verify(ctx.inductionVariable == null, "can't nest inductions!");
		computeTargetDerivation(ctx);
		ctx.inductionVariable = getTargetDerivation();
		debug("induction variable " + ctx.inductionVariable + " of type " + ctx.inductionVariable.getClass());

		if (!(getTargetDerivation() instanceof DerivationByAssumption)
				&& !(getTargetDerivation() instanceof SyntaxAssumption))
			ErrorHandler.report("Fact "+ getTargetDerivationName() + " must be one of the assumed facts in the forall clause of this theorem", this);
		
		if (getTargetDerivation() instanceof SyntaxAssumption && !((SyntaxAssumption)getTargetDerivation()).isTheoremArg())
			ErrorHandler.report("Nonterminal "+ getTargetDerivationName() + " must be an explicit forall clause argument of this theorem", this);

		// find which argument of the theorem this is
		ctx.inductionPosition = -1;
		for (int i = 0; i < ctx.currentTheorem.getForalls().size(); ++i) {
			if (getTargetDerivation().equals(ctx.currentTheorem.getForalls().get(i)))
				ctx.inductionPosition = i;
		}
		if (ctx.inductionPosition == -1)
			ErrorHandler.report("Fact "+ getTargetDerivationName() + " must be one of the assumed facts in the forall clause of this theorem", this);
		
		super.typecheck(ctx);
	}
}
