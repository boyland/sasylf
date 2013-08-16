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
import edu.cmu.cs.sasylf.util.SASyLFError;


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

	/**
	 * Type check this derivation, and then, whether or not it raised an error,
	 * assume it is true and go on.  Here "assume" means add it to the map.
	 * @param ctx context to use.
	 * @return whether everything went without error
	 */
	public final boolean typecheckAndAssume(Context ctx) {
	  boolean result = true;
	  try {
	    this.typecheck(ctx);
	  } catch (SASyLFError error) {
	    result = false;
	  }
	  
	  // If the clause doesn't check, then adding this derivation to
	  // the map will cause internal errors later on.
	  // Perhaps in the future, we want to give it a special error type that will 
	  // allow later things to work, but it's probably simpler to just omit it from the context.
	  if (!clauseChecked) return result;
	  
    this.addToDerivationMap(ctx);
    if (clause instanceof AndClauseUse) {
      String[] names = super.getName().split(",");
      if (names.length == 1) return result;
      List<ClauseUse> clauses = ((AndClauseUse)clause).getClauses();
      if (clauses.size() != names.length) {
        ErrorHandler.recoverableError("unequal number of conjuncts", this);
      }
      for (int i=0; i < names.length; ++i) {
        if (i == clauses.size()) break;
        new DerivationByAssumption(names[i],this.getLocation(),clauses.get(i)).addToDerivationMap(ctx);
      }
    }
    return result;
	}
	
	public void typecheck(Context ctx) {
    clause.typecheck(ctx);

    Element newClause = clause.computeClause(ctx, false);
    if (!(newClause instanceof Clause))
      ErrorHandler.report("Expected a judgment, but found a nonterminal.  Did you forget to name the derivation?", this);

    clause = (Clause) newClause;
    clause.checkBindings(ctx.bindingTypes, this);
    
    clauseChecked = true;
	}

	protected Clause clause;
  private boolean clauseChecked = false;
  
	
	public static void typecheck(Node node, Context ctx, List<Derivation> derivations) {
	  int n = derivations.size();
	  boolean finalOK = false;
	  for (int i=0; i < n; ++i) {
	    Derivation d = derivations.get(i);
	    if (d.clause == null) d.clause = ctx.currentGoalClause;
	    // JTB: Unfortunately we can't do this yet because it may instantiate
	    // outputVars and we don't want this side-effect to happen yet.
	    // else if (i == n-1) do a check match NOW
	    finalOK = d.typecheckAndAssume(ctx);
	  }
	  if (n == 0) {
	    ErrorHandler.report(Errors.NO_DERIVATION, node);
	  }
	  // JTB: TODO: change to use local version:
	  if (finalOK) Theorem.verifyLastDerivation(ctx, ctx.currentGoal, ctx.currentGoalClause, derivations, node);
	}
	
	// JTB: This has DIFFERENT failure modes than Theorem.verifyLastDerivation
	@SuppressWarnings("unused")
  private static void verifyLastDerivation(Context ctx, Term goalTerm, Clause goalClause, List<Derivation> derivations, Node node) {
	  Derivation last = derivations.get(derivations.size()-1);
    checkMatch(last,ctx,goalClause,last.getElement(), Errors.WRONG_RESULT.getText());
	}
	
	public static void checkMatch(Node node, Context ctx, Element match, Element supplied, String errorMsg) {
	  Term matchTerm = DerivationByAnalysis.adapt(match.asTerm(), match, ctx, false);
    Term suppliedTerm = DerivationByAnalysis.adapt(supplied.asTerm(), supplied, ctx, false);
    checkMatch(node,ctx,matchTerm,suppliedTerm,errorMsg);
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
      debug("wrapping sub = " + ctx.adaptationSub);
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
