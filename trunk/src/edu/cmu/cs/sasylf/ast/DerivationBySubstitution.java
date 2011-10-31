package edu.cmu.cs.sasylf.ast;

import java.util.*;
import java.io.*;

import edu.cmu.cs.sasylf.term.Term;
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
		
		// TODO verify: substitution is actually legal

		// get terms for arguments
		Term subContext = this.getArgs().get(0).getElement().asTerm();
		Term source = this.getArgs().get(1).getElement().asTerm();
		
		// verify both are judgments
		// verify first has one more assumption than second
		// for now, only support substituting for last assumption
		
		// verify second is a proof that matches the first's assumption
		
		// verify result is the second substituted for (and eliminating) the assumption of the first
	}

}
