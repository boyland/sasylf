package edu.cmu.cs.sasylf.ast;

import java.util.*;
import java.io.*;

import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import static edu.cmu.cs.sasylf.ast.Errors.*;


public class DerivationByPrevious extends DerivationWithArgs {
	public DerivationByPrevious(String n, Location l, Clause c) {
		super(n,l,c);
	}
	public String prettyPrintByClause() {
		return " by ";
	}


	// verify: that this is the same as the previous one, except with any new subs
	@Override
	public void typecheck(Context ctx) {
		super.typecheck(ctx);

		Term argTerm = getAdaptedArg(ctx, 0);
		Term derivTerm = getElement().asTerm().substitute(ctx.currentSub);
		
		if (!argTerm.equals(derivTerm)) {
			// TODO: could be looser than this
			ErrorHandler.report(NOT_EQUIVALENT, "Derivation " + getElement() + " is not equivalent to the previous derivation " + getArgStrings().get(0), this,
			    "\t this term is " + derivTerm + "\n\tprevious term is " + argTerm);			
		}
	}
}