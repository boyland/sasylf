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
	protected String suspectOutputVarError = null; // an output variable was set.

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
			if (clauses.size() != names.length) {
				ErrorHandler.recoverableError("unequal number of conjuncts", this);
			}
			Pair<Fact,Integer> derivationInfo = ctx.subderivations.get(this);
			for (int i=0; i < names.length; ++i) {
				if (i == clauses.size()) break;
				Fact fact = new DerivationByAssumption(names[i],this.getLocation(),clauses.get(i));
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
		clause.typecheck(ctx);

		Element newClause = clause.computeClause(ctx, false);
		if (!(newClause instanceof Clause))
			ErrorHandler.report(Errors.DERIVATION_SYNTAX, this);
		else if (!(newClause.getType() instanceof Judgment))
			ErrorHandler.report(Errors.DERIVATION_SYNTAX,this);
		clause = (Clause) newClause;
		clause.checkBindings(ctx.bindingTypes, this);
		clause.checkVariables(Collections.<String>emptySet(), false);

		if (!ctx.outputVars.isEmpty()){
			Set<FreeVar> vs = new HashSet<FreeVar>(ctx.outputVars);
			vs.retainAll(ctx.inputVars);
			if (!vs.isEmpty()) {
				suspectOutputVarError = vs.toString();
				// System.out.println(this.getLocation().getLine() + ": suspect prematurely bound " + suspectOutputVarError);
			}
		}
		
		clauseChecked = true;
	}

	public static void typecheck(Node node, Context ctx, List<Derivation> derivations) {
		int n = derivations.size();
		if (n == 0) {
			ErrorHandler.report(Errors.NO_DERIVATION, node);
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
			ErrorHandler.report(Errors.PARTIAL_CASE_ANALYSIS, last, "do\nproof by");
		}
		Derivation.checkMatchWithImplicitCoercions(last, ctx, ctx.currentGoalClause, last.getElement(), Errors.WRONG_RESULT.getText());	  
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
	 * @param node location in program where error should be reported.  May be null if errorMsg is null.
	 * @param ctx global information, must not be null
	 * @param match requirement
	 * @param supplied what is provided
	 * @param errorMsg error message to print if there is no match.  If null, no error printing.
	 *                 Just return null.
	 * @throws SASyLFError if there is an error and errorMsg is not null.
	 * @return
	 */
	public static boolean checkMatchWithImplicitCoercions(Node node, Context ctx, Element match, Element supplied, String errorMsg) {
		if (supplied instanceof OrClauseUse) {
			for (ClauseUse provided : ((OrClauseUse)supplied).getClauses()) {
				boolean result = checkMatchWithImplicitCoercions(node,ctx,match,provided,errorMsg);
				if (result == false) return false;
			}
			return true;
		}
		if (match instanceof OrClauseUse) {
			for (ClauseUse required : ((OrClauseUse)match).getClauses()) {
				boolean result = checkMatchWithImplicitCoercions(node,ctx,required,supplied,null);
				if (result == true) return true;
			}
			if (errorMsg == null) return false;
			ErrorHandler.report(errorMsg + "\nNone of the possibilities matched.", node);
		}
		if (match instanceof AndClauseUse) {
			if (!(supplied instanceof AndClauseUse)) {
				if (errorMsg == null) return false;
				ErrorHandler.report(errorMsg + "\nExpected multiple clauses.", node);
			}
			List<ClauseUse> matchList = ((AndClauseUse)match).getClauses();
			List<ClauseUse> suppliedList = ((AndClauseUse)supplied).getClauses();
			if (matchList.size() != suppliedList.size()) {
				if (errorMsg == null) return false;
				ErrorHandler.report(errorMsg + "\nMismatch because expected " + matchList.size() + " conjuncts but got " + suppliedList.size(), node);
			}
			for (int i=0; i < matchList.size(); ++i) {
				String newMsg = errorMsg;
				if (newMsg != null) {
					newMsg += " (conjunct #" + (i+1) + ")";
				}
				if (!checkMatchWithImplicitCoercions(node,ctx,matchList.get(i),suppliedList.get(i),newMsg)) return false;
			}
			return true;
		} else if (supplied instanceof AndClauseUse) {
			if (errorMsg == null) return false;
			ErrorHandler.report(errorMsg + "\nUnexpected multiple clauses.", node);
		}
		
		if (checkRelax(ctx,match,supplied)) return true;
		return checkMatch(node,ctx,match,supplied,errorMsg);
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
		if (checkMatch(null,ctx, target, source, null)) {
			Util.debug("could relax ",source," to ",target);
			return true;
		} else {
			Util.debug("couldn't relax ",source," to ",target);
			return false;
		}
	}

	public static boolean checkMatch(Node node, Context ctx, Element match, Element supplied, String errorMsg) {
		Term matchTerm = match.asTerm().substitute(ctx.currentSub); // DerivationByAnalysis.adapt(match.asTerm(), match, ctx, false);
		Term suppliedTerm = supplied.asTerm().substitute(ctx.currentSub); // DerivationByAnalysis.adapt(supplied.asTerm(), supplied, ctx, false);
		return checkMatch(node,ctx,matchTerm,suppliedTerm,errorMsg) &&
				checkRootMatch(ctx, supplied, match, errorMsg == null ? null : node);
	}

	/**
	 * Check that the supplied term satisfies the requirements of the match term,
	 * possibly changing the values of output variables.
	 * @param node location of matching, for errors
	 * @param ctx context, will be modified
	 * @param matchTerm required term
	 * @param suppliedTerm given term
	 * @param errorMsg message to label in case match doesn't work.  If null, return false instead
	 * of giving an error message.
	 */
	public static boolean checkMatch(Node node, Context ctx, Term matchTerm, Term suppliedTerm, String errorMsg) {
		try {
			debug("check match = ", matchTerm, ", supplied = ", suppliedTerm);
			debug("  current sub = ", ctx.currentSub);
			debug("  current inputVars = ", ctx.inputVars);
			Substitution instanceSub = suppliedTerm.instanceOf(matchTerm);
			debug("  instance sub = ", instanceSub);
			// try to use the provided variable in preference
			instanceSub.avoid(suppliedTerm.getFreeVariables());
			// must not require instantiating free variables
			if (!instanceSub.avoid(ctx.inputVars)) {
				Set<FreeVar> unavoidable = instanceSub.selectUnavoidable(ctx.inputVars);
				return report(errorMsg," restricts " + unavoidable,node,"  could not avoid vars ", unavoidable);
			}
			debug("Adding to ctx: ", instanceSub);
			ctx.composeSub(instanceSub);
		} catch (UnificationFailed e) {
			return report(errorMsg, null, node, "\twas checking ",suppliedTerm," instance of ",matchTerm,": ", e.getMessage());
		}
		return true;
	}

	/**
	 * Report an error for this node (if given a non-null error message)
	 * @param errorMsg error message, or null to just return false
	 * @param node node to report error on, must not be null
	 * @param extraInfo extra LF info to put into error report
	 * @return false always
	 */
	protected static boolean report(String errorMsg, String addendum, Node node, Object... extraInfo) {
		if (errorMsg == null) return false;
		if (addendum != null) errorMsg += addendum;
		if (node instanceof Derivation && ((Derivation)node).suspectOutputVarError != null) {
			errorMsg += "\nPerhaps these output variables were set prematurely: " + ((Derivation)node).suspectOutputVarError;
		}
		StringBuilder sb = new StringBuilder();
		for (Object o : extraInfo) {
			sb.append(o == null ? "<null>" : o.toString());
		}
		ErrorHandler.report(errorMsg, node, sb.toString());
		return false;
	}
	
	/**
	 * Give an error for this node if copying from the source to the target
	 * involves changing the variable context.
	 * @param kind name action of this node
	 * @param srcClause clause being used
	 * @param trgClause clause being defined
	 * @param errorPoint TODO
	 */
	public static boolean checkRootMatch(String kind, ClauseUse srcClause, ClauseUse trgClause, Node errorPoint) {
		if (srcClause.getRoot() == null) {
			if (trgClause.getRoot() != null) {
				if (errorPoint == null) return false;
				ErrorHandler.report(kind+" cannot be used to weaken to variable context", errorPoint);
			}
		} else if (trgClause.getRoot() == null) {
			if (errorPoint == null) return false;
			ErrorHandler.report(Errors.CONTEXT_DISCARDED, errorPoint);
		} else if (!srcClause.getRoot().equals(trgClause.getRoot())) {
			if (errorPoint == null) return false;
			ErrorHandler.report(kind+" cannot be used to change variable context",errorPoint);
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
	 */
	public static boolean checkRootMatch(Context ctx, Element source, Element dest, Node errorPoint) {
		/*if (source instanceof ClauseUse && dest instanceof ClauseUse) {
	    ClauseUse src = (ClauseUse) source;
	    ClauseUse dst = (ClauseUse) dest;*/
		if (source.getRoot() != null && dest.getRoot() == null) {
			if (errorPoint == null) return false;
			ErrorHandler.report(Errors.CONTEXT_DISCARDED, errorPoint);
		}
		//}
		return true;
	}
}
