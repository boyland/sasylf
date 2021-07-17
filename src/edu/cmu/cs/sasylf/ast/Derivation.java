package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Util.debug;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.term.UnificationIncomplete;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.Util;

public abstract class Derivation extends Fact {

	protected Clause clause;
	protected final boolean wasProof; // this derivation was originally "proof by ..."
	private boolean clauseChecked = false;

	private static final int PROOF_SIZE = 5; // size of string "proof"
	
	public Derivation(String n, Location l, Clause c) {
		super(n, l);
		clause = c;
		wasProof = (c == null);
		if (c != null) {
			super.setEndLocation(c.getEndLocation());
		}
	}

	public Clause getClause() { return clause; }
	@Override
	public Clause getElement() { return clause; }

	@Override
	public void prettyPrint(PrintWriter out) {
		if (clause == null) out.print("proof");
		else {
			out.print(getName() + ": ");
			getClause().prettyPrint(out);
		}
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

		return result;
	}


	@Override
	public void addToDerivationMap(Context ctx) {
		super.addToDerivationMap(ctx);
		if (clause instanceof AndClauseUse) {
			String[] names = super.getName().split(",");
			if (names.length == 1) return;
			List<ClauseUse> clauses = ((AndClauseUse)clause).getClauses();
			Util.verify(clauses.size() == names.length, "unequal number of conjuncts");
			Pair<Fact,Integer> derivationInfo = ctx.subderivations.get(this);
			for (int i=0; i < names.length; ++i) {
				if (i == clauses.size()) break;
				Fact fact = new DerivationByAssumption(names[i],this.getLocation(),ContextJudgment.unwrap(clauses.get(i)));
				fact.addToDerivationMap(ctx);
				if (derivationInfo != null) {
					ctx.subderivations.put(fact,derivationInfo);
				}
			}
		}
	}

	@Override
	public void typecheck(Context ctx) {
		ErrorHandler.recordLastSpan(this);
		ctx.checkConsistent(this);
		clause = clause.typecheck(ctx);

		Element newClause = clause.computeClause(ctx, false);
		if (!(newClause instanceof Clause))
			ErrorHandler.error(Errors.DERIVATION_SYNTAX, this);
		else if (!(newClause.getType() instanceof Judgment))
			ErrorHandler.error(Errors.DERIVATION_SYNTAX,this);
		clause = (Clause) newClause;
		clause.checkBindings(ctx.bindingTypes, this);
		clause.checkVariables(Collections.<String>emptySet(), false);

		clauseChecked = true;
	}

	public static void typecheck(Node node, Context ctx, List<Derivation> derivations) {
		int n = derivations.size();
		if (n == 0) {
			ErrorHandler.error(Errors.NO_DERIVATION, node);
		}

		boolean finalOK = false;
		for (int i=0; i < n; ++i) {
			Derivation d = derivations.get(i);
			if (d.clause == null) {
				// we copy over to get the right location for things
				d.clause = ctx.currentGoalClause.clone();
				d.clause.setLocation(d.getLocation());
				d.clause.setEndLocation(d.getLocation().add(PROOF_SIZE));
			}
			finalOK = d.typecheckAndAssume(ctx);
		}
		if (!finalOK) return;

		Derivation last = derivations.get(derivations.size()-1);
		if (last instanceof PartialCaseAnalysis) {
			ErrorHandler.error(Errors.PARTIAL_CASE_ANALYSIS, last, "do\nproof by");
		}
		if (!Derivation.checkMatchWithImplicitCoercions(null, ctx, ctx.currentGoalClause, last.getElement(), "")) {
			ErrorHandler.error(Errors.WRONG_RESULT, last);
		}
	}

	/**
	 * Check that the supplied derivation matches what is required.
	 * In the process, permit implicit coercions:
	 * <ol>
	 * <li> An empty 'or' clause can satisfy anything
	 * <li> A single derivation can satisfy an 'or' clause including it
	 * <li> An 'or' clause can satisfy another 'or' clause if everything it has
	 *      is included.
	 * </ol>
	 * <p>
	 * The error messages generated (optionally) assume that
	 * someone is justifying a local clause with something from elsewhere,
	 * e.g., as with DerivationByPrevious.  If this assumption is not valid,
	 * then this method should be called with "false" to indicate to indicate
	 * that the client will generate its own error message.
	 * @param errorPoint location in program where error should be reported.  Should be null if no printing of errors desired.
	 * @param ctx global information, must not be null
	 * @param match requirement
	 * @param supplied what is provided
	 * @param info extra information (language neutral) about the coercion (default empty)
	 * @throws SASyLFError if there is an error and errorMsg is not null.
	 * @return whether the justification is valid, false if there is an error.
	 */
	public static boolean checkMatchWithImplicitCoercions(Node errorPoint, Context ctx, Element match, Element supplied, String info) {
		// XXX: It's not clear whether unwrapping is needed
		if (match.getType() instanceof ContextJudgment) {
			Util.debug("Found context judgment in match!");
			// match = ContextJudgment.unwrap((ClauseUse)match);
		}
		if (supplied.getType() instanceof ContextJudgment) {
			Util.debug("Found context judgment in supplied!");
			// supplied = ContextJudgment.unwrap((ClauseUse)supplied);
		}
		if (supplied instanceof OrClauseUse) {
			for (ClauseUse provided : ((OrClauseUse)supplied).getClauses()) {
				provided = ContextJudgment.unwrap(provided);
				boolean result = checkMatchWithImplicitCoercions(errorPoint,ctx,match,provided,info);
				if (result == false) return false;
			}
			return true;
		}
		if (match instanceof OrClauseUse) {
			for (ClauseUse required : ((OrClauseUse)match).getClauses()) {
				required = ContextJudgment.unwrap(required);
				boolean result = checkMatchWithImplicitCoercions(null,ctx,required,supplied,info);
				if (result == true) return true;
			}
			if (errorPoint == null) return false;
			ErrorHandler.error(Errors.DERIVATION_OR,info, errorPoint);
		}
		if (match instanceof AndClauseUse) {
			if (!(supplied instanceof AndClauseUse)) {
				if (errorPoint == null) return false;
				ErrorHandler.error(Errors.DERIVATION_AND,info, errorPoint);
			}
			List<ClauseUse> matchList = ((AndClauseUse)match).getClauses();
			List<ClauseUse> suppliedList = ((AndClauseUse)supplied).getClauses();
			if (matchList.size() != suppliedList.size()) {
				if (errorPoint == null) return false;
				ErrorHandler.error(Errors.DERIVATION_AND_NEQ, matchList.size() + " != " + suppliedList.size() + " " + info, errorPoint);
			}
			for (int i=0; i < matchList.size(); ++i) {
				String newMsg = "#" + (i+1) + " " + info;
				if (!checkMatchWithImplicitCoercions(errorPoint,ctx,matchList.get(i),suppliedList.get(i),newMsg)) return false;
			}
			return true;
		} else if (supplied instanceof AndClauseUse) {
			if (errorPoint == null) return false;
			ErrorHandler.error(Errors.DERIVATION_NOT_AND,info, errorPoint);
		}
		
		if (checkRelax(ctx,match,supplied)) return true;
		return checkMatch(errorPoint,ctx,match,supplied,info+supplied);
	}

	public static boolean checkRelax(Context ctx, Element match, Element supplied) {
		if (ctx.relaxationMap == null) return false;
		NonTerminal srcRoot = supplied.getRoot();
		NonTerminal trgRoot = match.getRoot();
		if (srcRoot == null || trgRoot == null) return false;
		Term source = supplied.asTerm().substitute(ctx.currentSub);
		Term target = match.asTerm().substitute(ctx.currentSub);
		while (!srcRoot.equals(trgRoot)) {
			Relaxation r = ctx.relaxationMap.get(srcRoot);
			if (r == null) return false;
			Util.debug(supplied.getLocation().getLine()," ********* Found a relaxation ",r,", when currentSub = ",ctx.currentSub);
			Term newSource = r.relax(source);
			if (newSource == null) return false;
			source = newSource;
			srcRoot = r.getResult();
		}
		if (checkMatch(null,ctx, target, source, "")) {
			Util.debug("could relax ",source," to ",target);
			return true;
		} else {
			Util.debug("couldn't relax ",source," to ",target);
			return false;
		}
	}

	public static boolean checkMatch(Node errorPoint, Context ctx, Element match, Element supplied, String info) {
		Term matchTerm = match.asTerm().substitute(ctx.currentSub); // DerivationByAnalysis.adapt(match.asTerm(), match, ctx, false);
		Term suppliedTerm = supplied.asTerm().substitute(ctx.currentSub); // DerivationByAnalysis.adapt(supplied.asTerm(), supplied, ctx, false);
		return checkMatch(errorPoint,ctx,matchTerm,suppliedTerm,info) &&
				checkRootMatch(ctx, supplied, match, errorPoint);
	}

	/**
	 * Check that the supplied term satisfies the requirements of the match term,
	 * possibly changing the values of output variables.
	 * @param errorPoint location for errors; should be null if only a boolean result wanted
	 * @param ctx context, will be modified
	 * @param matchTerm required term
	 * @param suppliedTerm given term
	 * @param info language neutral description of suppliedTerm
	 * of giving an error message.
	 * @return true if match succeeded
	 */
	public static boolean checkMatch(Node errorPoint, Context ctx, Term matchTerm, Term suppliedTerm, String info) {
		try {
			debug("check match = ", matchTerm, ", supplied = ", suppliedTerm);
			debug("  current sub = ", ctx.currentSub);
			debug("  current inputVars = ", ctx.inputVars);
			Substitution instanceSub = suppliedTerm.unify(matchTerm);
			debug("  instance sub = ", instanceSub);
			// try to use the provided variable in preference
			instanceSub.avoid(suppliedTerm.getFreeVariables());
			// must not require instantiating free variables
			if (!instanceSub.avoid(ctx.inputVars)) {
				if (errorPoint != null) {
					Set<FreeVar> unavoidable = instanceSub.selectUnavoidable(ctx.inputVars);
					Set<FreeVar> output = new HashSet<>(ctx.outputVars);
					output.retainAll(unavoidable);
					if (output.isEmpty()) {
						ErrorHandler.error(Errors.DERIVATION_RESTRICTS, unavoidable.toString(), errorPoint);
					} else {
						FreeVar sample = output.iterator().next();
						ErrorHandler.error(Errors.DERIVATION_RESTRICTS_OUTPUT, sample.toString(), errorPoint, "  restricts " + unavoidable);
					}
				}
				return false;
			}
			debug("Checking adding to ctx: ", instanceSub);
			// NB: ctx.canCompose can't check this (see comment)
			if (!ctx.currentSub.canCompose(instanceSub)) {
				if (errorPoint != null) {
					ErrorHandler.error(Errors.DERIVATION_MISMATCH, info, errorPoint,
							"\twas trying to add " + instanceSub + " to context " + ctx.currentSub);
				}
				return false;
			}
			debug("Adding to ctx: ", instanceSub);
			ctx.composeSub(instanceSub);
		} catch (UnificationIncomplete e) {
			if (errorPoint != null) {
				ErrorHandler.error(Errors.DERIVATION_MISMATCH_INCOMPLETE, errorPoint, 
						"\twas checking " + suppliedTerm + " instance of " + matchTerm);
			}
			return false;
			
		} catch (UnificationFailed e) {
			if (errorPoint != null) {
				ErrorHandler.error(Errors.DERIVATION_MISMATCH, info, errorPoint, 
						"\twas checking " + suppliedTerm + " instance of " + matchTerm);
			}
			return false;
		}
		return true;
	}
	
	/**
	 * Give an error for this node if copying from the source to the target
	 * involves changing the variable context.
	 * @param kind name action of this node
	 * @param srcClause clause being used
	 * @param trgClause clause being defined
	 * @param errorPoint location to generate errors for, may be null
	 * @return true if the match succeeds
	 */
	public static boolean checkRootMatch(String kind, ClauseUse srcClause, ClauseUse trgClause, Node errorPoint) {
		if (srcClause.getRoot() == null) {
			if (trgClause.getRoot() != null) {
				if (errorPoint == null) return false;
				ErrorHandler.error(Errors.CONTEXT_INTRODUCED, kind, errorPoint);
			}
		} else if (trgClause.getRoot() == null) {
			if (errorPoint == null) return false;
			ErrorHandler.error(Errors.CONTEXT_DISCARDED, srcClause.getRoot().toString(), errorPoint);
		} else if (!srcClause.getRoot().equals(trgClause.getRoot())) {
			if (errorPoint == null) return false;
			ErrorHandler.error(Errors.CONTEXT_CHANGED, kind, errorPoint);
		}
		return true;
	}

	/**
	 * Type checking cannot check whether an implicit context is discarded.
	 * This methods checks this situation.
	 * @param ctx global definitions (not used)
	 * @param source flow start of a derivation
	 * @param dest flow termination of a derivation
	 * @param errorPoint point where to mention an error.
	 * @return true if context not discarded
	 */
	public static boolean checkRootMatch(Context ctx, Element source, Element dest, Node errorPoint) {
		if (source.getRoot() != null && dest.getRoot() == null) {
			if (errorPoint == null) return false;
			ErrorHandler.error(Errors.CONTEXT_DISCARDED, source.getRoot().toString(), errorPoint);
		}
		// can't check any more because of relaxation
		return true;
	}
}
