package edu.cmu.cs.sasylf.ast;

import java.util.*;
import java.io.*;

import edu.cmu.cs.sasylf.term.*;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.SASyLFError;
import static edu.cmu.cs.sasylf.term.Facade.App;
import static edu.cmu.cs.sasylf.util.Util.*;
import static edu.cmu.cs.sasylf.ast.Errors.*;


public class RuleCase extends Case {
	public RuleCase(Location l, String rn, List<Derivation> ps, Derivation c) {
		super(l);
		conclusion = c;
		premises = ps;
		ruleName = rn;
	}

	public String getRuleName() { return ruleName; }
	public Rule getRule() { return rule; }
	public List<Derivation> getPremises() { return premises; }
	public Derivation getConclusion() { return conclusion; }

	public void prettyPrint(PrintWriter out) {
		out.println("case rule\n");
		for (Derivation d : premises) {
			out.print("premise ");
			d.prettyPrint(out);
			out.println();
		}
		out.print("--------------------- ");
		out.println(ruleName);
		conclusion.prettyPrint(out);
		out.println("\n\nis\n");

		super.prettyPrint(out);
	}

	public void typecheck(Context ctx, boolean isSubderivation) {
		debug("line "+ this.getLocation().getLine() + " case " + ruleName);
		debug("    currentSub = "+ ctx.currentSub);
		rule = (Rule) ctx.ruleMap.get(ruleName);
		if (rule == null)
			ErrorHandler.report(Errors.RULE_NOT_FOUND, ruleName, this);

		if (rule.getPremises().size() != getPremises().size())
			ErrorHandler.report(Errors.RULE_PREMISE_NUMBER, getRuleName(), this);
			//ErrorHandler.report("Expected " + rule.getPremises().size() + " premises for rule " + ruleName + " but " + getPremises().size() + " were given", this);	
		
		Map<String, List<ElemType>> oldBindingTypes = ctx.bindingTypes;
		ctx.bindingTypes = new HashMap<String, List<ElemType>>(ctx.bindingTypes);
		for (Derivation d : premises) {
			d.typecheck(ctx);
			d.getClause().checkBindings(ctx.bindingTypes, this);
		}

		conclusion.typecheck(ctx);
		conclusion.getClause().checkBindings(ctx.bindingTypes, this);
		
		// make sure we were case-analyzing a derivation, not a nonterminal
		if (ctx.currentCaseAnalysisElement instanceof NonTerminal)
			ErrorHandler.report("When case-analyzing a non-terminal, must use syntax cases, not rule cases", this);

		// look up case analysis for this rule
		Set<Pair<Term,Substitution>> caseResult = ctx.caseTermMap.get(rule);
		if (caseResult == null)
			ErrorHandler.report(Errors.EXTRA_CASE, ": rule " + ruleName + " cannot be used to derive " + ctx.currentCaseAnalysisElement, this);
		if (caseResult.isEmpty())
			//ErrorHandler.report("Rule " + ruleName + " cannot be used to derive " + ctx.currentCaseAnalysisElement, this);
			ErrorHandler.report(Errors.EXTRA_CASE, this);


		
		
		
		
		
		
		
		
		
		
		
		
		// make sure rule's conclusion unifies with the thing we're doing case analysis on
		Term concTerm = conclusion.getElement().asTerm();
		concTerm = concTerm.substitute(ctx.currentSub);
		Substitution adaptationSub = new Substitution();
		Term adaptedCaseAnalysis = ctx.currentCaseAnalysisElement.adaptTermTo(ctx.currentCaseAnalysis, concTerm, adaptationSub);
		debug("adapation: " + adaptationSub + "\n\tapplied to " + ctx.currentCaseAnalysis + "\n\tis " + ctx.currentCaseAnalysis.substitute(adaptationSub));
		
		// did we increase the number of lambdas?
		int lambdaDifference =  adaptedCaseAnalysis.countLambdas() - ctx.currentCaseAnalysis.countLambdas();
		Substitution oldAdaptationSub = ctx.adaptationSub;
		Map<NonTerminal, AdaptationInfo> oldAdaptationMap = new HashMap<NonTerminal,AdaptationInfo>(ctx.adaptationMap);
		NonTerminal oldInnermostGamma = ctx.innermostGamma;
		Term oldMatchTerm = ctx.matchTermForAdaptation;
		
		try {

		if (lambdaDifference > 0) {
			if (ctx.adaptationSub != null)
				if (ctx.matchTermForAdaptation.countLambdas() == adaptedCaseAnalysis.countLambdas()) {
					// we're just re-doing the same adaptation we did before
					// TODO: more principled approach is to adapt the ctx.currentCaseAnalysis in DerivationByAnalysis before we even get here
					adaptationSub = new Substitution(ctx.adaptationSub);
					adaptedCaseAnalysis = ctx.currentCaseAnalysisElement.adaptTermTo(ctx.currentCaseAnalysis, concTerm, adaptationSub);
				} else {
					ErrorHandler.report("Sorry, more than one nested variable rule case analysis is not yet supported", this);
				}
			ctx.adaptationSub = new Substitution(adaptationSub);
			
			// decrement the free bound vars in adaptationSub by the difference
			adaptationSub.incrFreeDeBruijn(-lambdaDifference);
			
			// store the adapted term to keep track of how we unrolled the context
			ctx.matchTermForAdaptation = adaptedCaseAnalysis;
			AdaptationInfo info = new AdaptationInfo(((ClauseUse)conclusion.getClause()).getRoot());
			ClauseUse.readNamesAndTypes((Abstraction)adaptedCaseAnalysis, lambdaDifference, info.varNames, info.varTypes);

			// may not be the same as the previous context...
			if (ctx.innermostGamma.equals(info.nextContext))
				ErrorHandler.report(REUSED_CONTEXT,"May not re-use context name " + ctx.innermostGamma, this);
			// ...or any prior context
			if (ctx.adaptationMap.containsKey(ctx.innermostGamma))
				ErrorHandler.report(REUSED_CONTEXT,"May not re-use context name " + ctx.innermostGamma, this);
			ctx.adaptationMap.put(ctx.innermostGamma, info);
			ctx.innermostGamma = info.nextContext;
			verify(info.nextContext != null, "internal invariant violated");
		}
		debug("concTerm: " + concTerm);
		Term adaptedConcTerm = DerivationByAnalysis.adapt(concTerm, conclusion.getElement(), ctx, false); //concTerm.substitute(adaptationSub);
		debug("adapted concTerm: " + adaptedConcTerm);

		
		// find the conclusion term from the matched case term
		//Term concTerm = ((Application)computedCaseTerm).getArguments().get(((Application)computedCaseTerm).getArguments().size()-1);
		Substitution unifyingSub = null;
		try {
			debug("case unify " + adaptedConcTerm + " with " + adaptedCaseAnalysis);
			unifyingSub = adaptedConcTerm.unify(adaptedCaseAnalysis);
		} catch (EOCUnificationFailed uf) {
			ErrorHandler.report(INVALID_CASE, "Case " + conclusion.getElement() + " is not actually a case of " + ctx.currentCaseAnalysisElement
								+ "\n    Did you re-use a variable (perhaps " + uf.eocTerm + ") which was already in scope?  If so, try using some other variable name in this case.", this);			
		} catch (UnificationFailed uf) {
			//uf.printStackTrace();
			debug(this.getLocation() + ": was unifying " + adaptedConcTerm + " and " + adaptedCaseAnalysis);
			ErrorHandler.report(INVALID_CASE, "Case " + conclusion.getElement() + " is not actually a case of " + ctx.currentCaseAnalysisElement, this, "SASyLF computed the LF term " + adaptedCaseAnalysis + " for the conclusion");
		} catch (SASyLFError error) {
			throw error;
		} catch (RuntimeException rt) {
			//rt.printStackTrace();
			System.err.println(this.getLocation() + ": was unifying " + adaptedConcTerm + " and " + adaptedCaseAnalysis);
			throw rt;
		}

		
		
		
		
		
		
		
		
		
		// find the computed case that matches the given rule
		Term caseTerm = computeTerm(ctx);
		Term computedCaseTerm = null;
		Term candidate = null;
		Substitution pairSub = null;
		Set<FreeVar> newInputVars = new HashSet<FreeVar>(ctx.inputVars);
		for (Pair<Term,Substitution> pair : caseResult)
			try {
				pairSub = new Substitution(pair.second);
				debug("\tpair.second was " + pairSub);
				pairSub.selectUnavoidable(ctx.inputVars);
				candidate = pair.first.substitute(pairSub);
				
				// Apply the unifying substitution to caseTerm before comparing, in case caseTerm uses a variable it unifies elsewhere inappropriately (see homework8.slf file from Boyland) 
				debug("\tunifying sub = " + unifyingSub);
				caseTerm = caseTerm.substitute(unifyingSub);
				debug("case analysis: does " + caseTerm + " generalize " + candidate);
				debug("\tpair.second is now " + pairSub);
				
				
				Substitution computedSub = candidate.instanceOf(caseTerm);//caseTerm.unify(computedCaseTerm1);
				computedCaseTerm = candidate;
				debug("\told input vars = " + newInputVars);
				debug("\tcomputed sub = " + computedSub);
				Set<FreeVar> unavoidableInputVars = pairSub.selectUnavoidable(newInputVars);
				if (!unavoidableInputVars.isEmpty())
					debug("\tremoving input vars " + unavoidableInputVars);
				newInputVars.removeAll(unavoidableInputVars);
				Set<Term> computedSubDomain = new HashSet<Term>(computedSub.getMap().keySet());
				computedSubDomain.retainAll(newInputVars);
				if (!computedSubDomain.isEmpty())
					ErrorHandler.report(INVALID_CASE, "Case " + conclusion.getElement() + " is not actually a case of " + ctx.currentCaseAnalysisElement
							+ "\n    The term given requires instantiating the following variable(s) that should be free: " + computedSubDomain, this);
				verify(caseResult.remove(pair), "internal invariant broken");
				break;
			} catch (UnificationFailed uf) {
			  debug("candidate " + candidate + " is not an instance of " + caseTerm);
				//uf.printStackTrace();
				continue;
			}/* catch (RuntimeException rt) {
				rt.printStackTrace();
				System.out.println(this.getLocation() + ": was unifying " + caseTerm + " and " + candidate);
			}*/
			
		if (computedCaseTerm == null) {
			// there must have been a candidate, but it didn't unify or wasn't an instance
			String errorDescription = rule.getErrorDescription(candidate, ctx);
			debug("Expected case:\n" + errorDescription);
			debug("Your case roundtripped:\n" + rule.getErrorDescription(caseTerm, ctx));
			debug("SASyLF generated the LF term: " + candidate);
			debug("You proposed the LF term: " + caseTerm);
			ErrorHandler.report(INVALID_CASE, "The rule case given is invalid; it is most likely too specific in some way and should be generalized", this, "SASyLF considered the LF term " + candidate);
			// TODO: explain WHY!!!
		}
		
		
		
		
		// check that none of the newInputVars are substituted for
		Set<FreeVar> unavoidable = unifyingSub.selectUnavoidable(newInputVars);
		if (!unavoidable.isEmpty())
			ErrorHandler.report(INVALID_CASE, "Case " + conclusion.getElement() + " is not actually a case of " + ctx.currentCaseAnalysisElement
					+ "\n    The term given requires instantiating the following variable(s) that should be free: " + unavoidable, this);


		/*
		 * How to fix HW1Bogus issue
		 * 
		 * 1) compute computedCaseTerm, pairSub = unify with case analysis, substitute with pairSub
		 * 2) extract caseTerm, unifyingSub = unify with case analysis, substitute with unifyingSub
		 * 3) ensure computedCaseTerm instanceof caseTerm
		 * 4) for all LHS E that pairSub and unifyingSub map to Ec and Em:
		 *     5) subOfSubs = Ec instanceof Em
		 *     6) if LHS of subOfSubs in inputVar then ERROR
		 */
		
		debug("unifyingSub: " + unifyingSub);
		debug("pairSub: " + pairSub);

		for (Atom pairSubKey : pairSub.getMap().keySet()) {
			if (unifyingSub.getMap().containsKey(pairSubKey)) {
				Term ec = pairSub.getSubstituted(pairSubKey);
				Term em = unifyingSub.getSubstituted(pairSubKey);
				Substitution subOfSubs = null;
				try {
					debug("trying " + pairSubKey + ": " + ec + " instanceof " + em);
					subOfSubs = ec.instanceOf(em); // should never fail if checks above succeeded
					debug("subOfSubs: " + subOfSubs + " for " + pairSubKey);
				} catch (Exception e) {
					ErrorHandler.report(INVALID_CASE, "The rule case given is invalid, perhaps due to introducing a fresh variable in the wrong order into a term", this, "SASyLF considered the LF term " + candidate);
					//verify(false, "internal invariant violated");
				}
				for (Atom subOfSubsKey : subOfSubs.getMap().keySet()) {
					if (ctx.inputVars.contains(subOfSubsKey)) {
						ErrorHandler.report(INVALID_CASE, "When substituting for input variable " + pairSubKey + ", case makes invalid assumptions about the structure of " + subOfSubsKey, this);
					}
				}
			}
		}
		
		
		// update the current substitution
		Substitution oldSub = new Substitution(ctx.currentSub);
		// CHANGED
		//ctx.currentSub.compose(adaptationSub);  // modifies in place
		// TODO: may want to replace adaptationSub with adaptation info in Context
		debug("composing " + ctx.currentSub + " with " + unifyingSub);
		debug("old sub: " + oldSub);
		ctx.currentSub.compose(unifyingSub);  // modifies in place
		debug("result: " + ctx.currentSub);
		
		// update the set of free variables
		Set<FreeVar> oldInputVars = ctx.inputVars;
		ctx.inputVars = newInputVars;
		Set<FreeVar> addedInputVars = adaptedConcTerm.getFreeVariables();
		
		for (Derivation d : premises) {
			Term premiseTerm = d.getElement().asTerm();
			premiseTerm = premiseTerm.substitute(ctx.currentSub);
			addedInputVars.addAll(premiseTerm.getFreeVariables());
		}
		
		addedInputVars.removeAll(newInputVars);
		if (!addedInputVars.isEmpty())
			debug("\tadding new input vars " + addedInputVars);
		ctx.inputVars.addAll(addedInputVars);

		// update the set of subderivations
		List<Fact> oldSubderivations = new ArrayList<Fact>(ctx.subderivations);
		if (isSubderivation) {
			// add each premise to the list of subderivations
			ctx.subderivations.addAll(premises);
		}
		
		try {

		super.typecheck(ctx, isSubderivation);

		} finally {
		ctx.currentSub = oldSub;
		ctx.inputVars = oldInputVars;
		ctx.subderivations = oldSubderivations;
		}
		
		} finally {
			
		// restore the current substitution and input vars
		ctx.adaptationSub = oldAdaptationSub;
		ctx.adaptationMap = oldAdaptationMap;
		ctx.innermostGamma = oldInnermostGamma;
		ctx.bindingTypes = oldBindingTypes;
		ctx.matchTermForAdaptation = oldMatchTerm;
}
	}

	/** Computes a term representing the current rule case, with actual premises and conclusion */
	private Term computeTerm(Context ctx) {
		Term concTerm = conclusion.getElement().asTerm();
		concTerm = DerivationByAnalysis.adapt(concTerm, conclusion.getElement(), ctx, false);
		
		List<Term> args = new ArrayList<Term>();
		for (int i = 0; i < getPremises().size(); ++i) {
			Term argTerm = getPremises().get(i).getElement().asTerm();
			argTerm = DerivationByAnalysis.adapt(argTerm,getPremises().get(i).getElement(), ctx, false);
			args.add(argTerm);
		}
		args.add(concTerm);
		Term ruleTerm = App(getRule().getRuleAppConstant(), args);

		debug("new term = " + ruleTerm);

		return ruleTerm;
	}

	private Derivation conclusion;
	private List<Derivation> premises;
	private String ruleName;
	private Rule rule;
}

