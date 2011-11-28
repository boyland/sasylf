package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Util.*;

import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.util.ErrorHandler;


public abstract class Derivation extends Fact {
	public Derivation(String n, Location l, Clause c) {
		super(n, l); clause = c;
	}

	public Clause getClause() { return clause; }
	public Clause getElement() { return clause; }

	public void prettyPrint(PrintWriter out) {
		out.print(getName() + ": ");
		getClause().prettyPrint(out);
	}

	public void typecheck(Context ctx) {
		this.typecheck(ctx, true);
	}
	
	@Override
	public void typecheck(Context ctx, boolean addToMap) {
		clause.typecheck(ctx);

		Element newClause = clause.computeClause(ctx, false);
		if (!(newClause instanceof Clause))
			ErrorHandler.report("Expected a judgment, but found a nonterminal.  Did you forget to name the derivation?", this);

		clause = (Clause) newClause;
		clause.checkBindings(ctx.bindingTypes, this);
		//clause = new ClauseUse(clause, ctx.parseMap);
		if (addToMap) {
			this.addToDerivationMap(ctx);
			if (clause instanceof AndClauseUse) {
			  String[] names = super.getName().split(",");
			  if (names.length == 1) return;
			  List<ClauseUse> clauses = ((AndClauseUse)clause).getClauses();
			  if (clauses.size() != names.length) {
			    ErrorHandler.report("unequal number of conjuncts", this);
			  }
			  for (int i=0; i < names.length; ++i) {
			    new DerivationByAssumption(names[i],this.getLocation(),clauses.get(i)).addToDerivationMap(ctx);
			  }
			}
		}
	}

	protected Clause clause;
	
	public static void typecheck(Node node, Context ctx, List<Derivation> derivations) {
	  int n = derivations.size();
	  for (int i=0; i < n; ++i) {
	    Derivation d = derivations.get(i);
	    if (d.clause == null) d.clause = ctx.currentGoalClause;
	    // JTB: Unfortunately we can't do this yet because it may instantiate
	    // outputVars and we don't want this side-effect to happen yet.
	    // else if (i == n-1) do a check match NOW
	    d.typecheck(ctx);
	  }
	  // JTB: TODO: change to use checkMatch:
	  Theorem.verifyLastDerivation(ctx, ctx.currentGoal, ctx.currentGoalClause, derivations, node);
	}
	
	public static void checkMatch(Node node, Context ctx, Element match, Element supplied, String errorMsg) {
	  checkMatch(node,ctx,DerivationByAnalysis.adapt(match.asTerm(), match, ctx, false),
	      DerivationByAnalysis.adapt(supplied.asTerm(), supplied, ctx, false),errorMsg);
	}
	
	/**
	 * Check that the supplied term satisfies the requirements of the match term,
	 * possibly changing the values of output variables.
	 * @param node location of matching, for errors
	 * @param ctx context, will be modified
	 * @param matchTerm required term
	 * @param suppliedTerm given term
	 * @param errorMsg message to label in case match doesn't work
	 */
	public static void checkMatch(Node node, Context ctx, Term matchTerm, Term suppliedTerm, String errorMsg) {
    try {
      debug("match = " + matchTerm + ", supplied = " + suppliedTerm);
      debug("current sub = " + ctx.currentSub);
      debug("current inputVars = " + ctx.inputVars);
      Substitution instanceSub = suppliedTerm.instanceOf(matchTerm);
      debug("instance sub = " + instanceSub);
      // must not require instantiating free variables
      if (!instanceSub.avoid(ctx.inputVars)) {
        Set<FreeVar> unavoidable = instanceSub.selectUnavoidable(ctx.inputVars);
        ErrorHandler.report(errorMsg,node,"  could not avoid vars " + unavoidable);
      }
      debug("Adding to ctx: " + instanceSub);
      ctx.currentSub.compose(instanceSub);
      // we need to update input vars with new variables:
      for (FreeVar v : matchTerm.getFreeVariables()) {
        if (!ctx.inputVars.contains(v)) {
          debug("Adding new free variable: " + v);
          ctx.inputVars.add(v);
        }
      }
    } catch (UnificationFailed e) {
      ErrorHandler.report(errorMsg, node, "\twas checking " + suppliedTerm + " instance of " + matchTerm);
    }

	}
}
