package edu.cmu.cs.sasylf.ast;

import java.util.*;

import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;


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

		Term argTerm;
		if (this.getArgStrings().size() == 1) {
		  argTerm = getAdaptedArg(ctx, 0);
		} else {
		  Clause cl = this.getClause();
		  if (!(cl instanceof AndClauseUse)) {
		    ErrorHandler.report("Claimed fact is not a conjunction, remove extra arguments", this);
		  }
		  List<ClauseUse> results = ((AndClauseUse)cl).getClauses();
		  int n = results.size();
      if (n != getArgStrings().size()) {
		    ErrorHandler.report("Wrong number of facts for conjuction, expected " + n, this);
		  }
		  List<ClauseUse> sourceClauses = new ArrayList<ClauseUse>();
		  for (Fact f : this.getArgs()) {
		    if (f instanceof ClauseAssumption) {
		      Element e = ((ClauseAssumption)f).getElement();
		      if (e instanceof ClauseUse) {
		        sourceClauses.add((ClauseUse)e);
		        continue;
		      }
		    } else if (f instanceof Derivation) {
		      sourceClauses.add((ClauseUse)f.getElement());
		      continue;
		    }
		    ErrorHandler.report("can only conjoin derivations, not syntax: " + f.getName(),this);
		  }
		  for (int i=0; i < n; ++i) {
		    ClauseUse source = sourceClauses.get(i);
		    ClauseUse result = results.get(i);
		    Derivation.checkMatch(this,ctx,result,source,"Claimed conjunct #" + (i+1) + " is not equivalent to previous");
		  }
		  return;
		}
		Term derivTerm = DerivationByAnalysis.adapt(getElement().asTerm(), getElement(), ctx, false);
		
		Derivation.checkMatch(this, ctx, derivTerm, argTerm, "Derivation " + getElement() + " is not equivalent to the previous derivations");
		/*
		if (!argTerm.equals(derivTerm)) {
			// TODO: could be looser than this
			ErrorHandler.report(NOT_EQUIVALENT, "Derivation " + getElement() + " is not equivalent to the previous derivation " + getArgStrings().get(0), this,
			    "\t this term is " + derivTerm + "\n\tprevious term is " + argTerm);			
		}*/
	}
}