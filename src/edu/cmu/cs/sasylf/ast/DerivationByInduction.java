package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.reduction.InductionSchema;
import edu.cmu.cs.sasylf.reduction.StructuralInduction;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Util;

public class DerivationByInduction extends DerivationByAnalysis {
	public DerivationByInduction(String n, Location l, Clause c, String derivName) {
		super(n,l,c, derivName);
	}
	public DerivationByInduction(String n, Location l, Clause c, Clause subject) {
		super(n,l,c, subject);
	}
	public DerivationByInduction(String n, Location l, Clause c) {
		super(n,l,c);
	}

	@Override
	public String byPhrase() { return "induction"; }

	@Override
	public void typecheck(Context ctx) {
		if (!ctx.currentTheorem.getDerivations().contains(this)) {
			ErrorHandler.error(Errors.INDUCTION_NESTED, this);
		}
		InductionSchema is = InductionSchema.create(ctx.currentTheorem, getArgStrings(), true);

		if (is != null && !ctx.currentTheorem.getInductionSchema().equals(is)) {
			ErrorHandler.error(Errors.INDUCTION_REPEAT,this,"induction\ncase analysis");	    
		}

		// special case: handle "use induction by"
		if (getCases().isEmpty() &&
				getClause() instanceof AndClauseUse && 
				((AndClauseUse)getClause()).getClauses().isEmpty()) {
			Util.debug("use induction detected: " + is);
			return;
		}

		if (is != null && !(is instanceof StructuralInduction)) {
			ErrorHandler.error(Errors.CASE_SUBJECT_MULTIPLE, this);
		}
		super.typecheck(ctx);
	}
}
