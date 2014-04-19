package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.term.Facade.App;
import static edu.cmu.cs.sasylf.util.Errors.INVALID_CASE;
import static edu.cmu.cs.sasylf.util.Errors.REUSED_CONTEXT;
import static edu.cmu.cs.sasylf.util.Util.debug;
import static edu.cmu.cs.sasylf.util.Util.verify;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Atom;
import edu.cmu.cs.sasylf.term.EOCUnificationFailed;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Pair;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.term.UnificationIncomplete;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.Util;


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

	public void typecheck(Context parent, Pair<Fact,Integer> isSubderivation) {
	  Context ctx = parent.clone();
		debug("line "+ this.getLocation().getLine(), " case ", ruleName);
		debug("    currentSub = ", ctx.currentSub);
		rule = (Rule) ctx.ruleMap.get(ruleName);
		if (rule == null)
			ErrorHandler.report(Errors.RULE_NOT_FOUND, ruleName, this);
		if (!rule.isInterfaceOK()) return;
		
		if (rule.getPremises().size() != getPremises().size())
			ErrorHandler.report(Errors.RULE_PREMISE_NUMBER, getRuleName(), this);
			//ErrorHandler.report("Expected " + rule.getPremises().size() + " premises for rule " + ruleName + " but " + getPremises().size() + " were given", this);	
		
		ctx.bindingTypes = new HashMap<String, List<ElemType>>(ctx.bindingTypes);
		for (Derivation d : premises) {
			d.typecheck(ctx);
			d.getClause().checkBindings(ctx.bindingTypes, this);
		}

		conclusion.typecheck(ctx);
		conclusion.getClause().checkBindings(ctx.bindingTypes, this);
		
		// make sure we were case-analyzing a derivation, not a nonterminal
		if (ctx.currentCaseAnalysisElement instanceof NonTerminal)
			ErrorHandler.report(Errors.RULE_CASE_SYNTAX, this);

		// look up case analysis for this rule
		Set<Pair<Term,Substitution>> caseResult = ctx.caseTermMap.get(rule);
		if (caseResult == null)
			ErrorHandler.report(Errors.EXTRA_CASE, ": rule " + ruleName + " cannot be used to derive " + ctx.currentCaseAnalysisElement, this, "suggestion: remove it");
		if (caseResult.isEmpty())
			//ErrorHandler.report("Rule " + ruleName + " cannot be used to derive " + ctx.currentCaseAnalysisElement, this);
			ErrorHandler.report(Errors.EXTRA_CASE, this,"suggestion: remove it");


		
		
		
		
		
		
		
		
		
		
		
		
		// make sure rule's conclusion unifies with the thing we're doing case analysis on
		Term concTerm = conclusion.getElement().asTerm();
		concTerm = concTerm.substitute(ctx.currentSub);
		Substitution adaptationSub = new Substitution();
		Term adaptedCaseAnalysis = ctx.currentCaseAnalysisElement.adaptTermTo(ctx.currentCaseAnalysis, concTerm, adaptationSub);
		debug("adapation: ", adaptationSub, "\n\tapplied to ", ctx.currentCaseAnalysis, "\n\tis ", ctx.currentCaseAnalysis.substitute(adaptationSub));
		
		// did we increase the number of lambdas?
		int lambdaDifference =  adaptedCaseAnalysis.countLambdas() - ctx.currentCaseAnalysis.countLambdas();

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
			ClauseUse.readNamesAndTypes((Abstraction)adaptedCaseAnalysis, lambdaDifference, info.varNames, info.varTypes, null);

			// may not be the same as the previous context...
			if (ctx.innermostGamma != null && ctx.innermostGamma.equals(info.nextContext))
				ErrorHandler.report(REUSED_CONTEXT,"May not re-use context name " + ctx.innermostGamma, this);
			// ...or any prior context
			if (ctx.adaptationMap.containsKey(ctx.innermostGamma))
				ErrorHandler.report(REUSED_CONTEXT,"May not re-use context name " + ctx.innermostGamma, this);
			ctx.adaptationMap.put(ctx.innermostGamma, info);
			ctx.innermostGamma = info.nextContext;
			verify(info.nextContext != null, "internal invariant violated");
		}
		debug("concTerm: ", concTerm);
		Term adaptedConcTerm = DerivationByAnalysis.adapt(concTerm, conclusion.getElement(), ctx, false); //concTerm.substitute(adaptationSub);
		debug("adapted concTerm: ", adaptedConcTerm);

		
		// find the conclusion term from the matched case term
		//Term concTerm = ((Application)computedCaseTerm).getArguments().get(((Application)computedCaseTerm).getArguments().size()-1);
		Substitution unifyingSub = null;
		try {
			debug("case unify ", adaptedConcTerm, " with ", adaptedCaseAnalysis);
			unifyingSub = adaptedConcTerm.unify(adaptedCaseAnalysis);
		} catch (EOCUnificationFailed uf) {
			ErrorHandler.report(INVALID_CASE, "Case " + conclusion.getElement() + " is not actually a case of " + ctx.currentCaseAnalysisElement
								+ "\n    Did you re-use a variable (perhaps " + uf.eocTerm + ") which was already in scope?  If so, try using some other variable name in this case.", this);			
		} catch (UnificationIncomplete uf) {
      ErrorHandler.report(INVALID_CASE, "Case too complex for SASyLF to check; consider sending this example to the maintainers", this,
          "SASyLF was trying to unify " + uf.term1 + " and " + uf.term2);
 		} catch (UnificationFailed uf) {
			//uf.printStackTrace();
			debug(this.getLocation(), ": was unifying ", adaptedConcTerm, " and ", adaptedCaseAnalysis);
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
				debug("\tpair.second was ", pairSub);
				pairSub.selectUnavoidable(ctx.inputVars);
				candidate = pair.first.substitute(pairSub);
				
				// Apply the unifying substitution to caseTerm before comparing, in case caseTerm uses a variable it unifies elsewhere inappropriately (see homework8.slf file from Boyland) 
				debug("\tunifying sub = ", unifyingSub);
				caseTerm = caseTerm.substitute(unifyingSub);
				debug("case analysis: does ", caseTerm, " generalize ", candidate);
				debug("\tpair.second is now ", pairSub);
				
				
				Substitution computedSub = candidate.instanceOf(caseTerm);//caseTerm.unify(computedCaseTerm1);
				computedCaseTerm = candidate;
				debug("\told input vars = ", newInputVars);
				debug("\tcomputed sub = ", computedSub);
				Set<FreeVar> unavoidableInputVars = pairSub.selectUnavoidable(newInputVars);
				if (!unavoidableInputVars.isEmpty())
				  debug("\tremoving input vars ", unavoidableInputVars);
				newInputVars.removeAll(unavoidableInputVars);
				// Set<FreeVar> computedSubDomain = computedSub.selectUnavoidable(newInputVars);
				Set<Atom> computedSubDomain = new HashSet<Atom>(computedSub.getMap().keySet());
				computedSubDomain.retainAll(newInputVars);
				if (!computedSubDomain.isEmpty())
					ErrorHandler.report(INVALID_CASE, "Case " + conclusion.getElement() + " is not actually a case of " + ctx.currentCaseAnalysisElement
							+ "\n    The case given requires instantiating the following variable(s) that should be free: " + computedSubDomain, this,
							"SASyLF computes that " + computedSubDomain.iterator().next() + " needs to be " + computedSub.getSubstituted(computedSubDomain.iterator().next()));
				verify(caseResult.remove(pair), "internal invariant broken");
				break;
			} catch (UnificationFailed uf) {
			  debug("candidate ", candidate, " is not an instance of ", caseTerm);
				//uf.printStackTrace();
				continue;
			}/* catch (RuntimeException rt) {
				rt.printStackTrace();
				System.out.println(this.getLocation() + ": was unifying " + caseTerm + " and " + candidate);
			}*/
			
		if (computedCaseTerm == null) {
			// there must have been a candidate, but it didn't unify or wasn't an instance
			String errorDescription = rule.getErrorDescription(candidate, ctx);
			debug("Expected case:\n", errorDescription);
			debug("Your case roundtripped:\n", rule.getErrorDescription(caseTerm, ctx));
			debug("SASyLF generated the LF term: ", candidate);
			debug("You proposed the LF term: ", caseTerm);
			ErrorHandler.report(INVALID_CASE, "The rule case given is invalid; it is most likely too specific in some way and should be generalized", this, "SASyLF considered the LF term " + candidate + " for " + caseTerm);
			// TODO: explain WHY!!!
		}
		
		
		
		
		// check that none of the newInputVars are substituted for
		Set<FreeVar> unavoidable = unifyingSub.selectUnavoidable(newInputVars);
		if (!unavoidable.isEmpty())
			ErrorHandler.report(INVALID_CASE, "Case " + conclusion.getElement() + " is not actually a case of " + ctx.currentCaseAnalysisElement
					+ "\n    The term given requires instantiating the following variable(s) that should be free: " + unavoidable, this);

		
		Util.debug("unifyingSub: ", unifyingSub);
		Util.debug("pairSub: ", pairSub);

		// JTB: I've never seen this code generate any errors.
		// I'm assuming it is extraneous now.
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
		/*for (Atom pairSubKey : pairSub.getMap().keySet()) {
			if (unifyingSub.getMap().containsKey(pairSubKey)) {
				Term ec = pairSub.getSubstituted(pairSubKey);
				Term em = unifyingSub.getSubstituted(pairSubKey);
				Substitution subOfSubs = null;
				try {
					debug("trying ", pairSubKey, ": ", ec, " instanceof ", em);
					subOfSubs = ec.instanceOf(em); // should never fail if checks above succeeded
					Util.tdebug("subOfSubs: ", subOfSubs, " for ", pairSubKey);
				} catch (Exception e) {
          verify(false, "internal invariant violated");
					ErrorHandler.report(INVALID_CASE, "The rule case given is invalid, perhaps due to introducing a fresh variable in the wrong order into a term", this, "SASyLF considered the LF term " + candidate);
				}
				for (Atom subOfSubsKey : subOfSubs.getMap().keySet()) {
					if (ctx.inputVars.contains(subOfSubsKey)) {
						ErrorHandler.report(INVALID_CASE, "When substituting for input variable " + pairSubKey + ", case makes invalid assumptions about the structure of " + subOfSubsKey, this);
					}
				}
			}
		}*/
		
		ClauseUse targetClause = (ClauseUse)ctx.currentCaseAnalysisElement;
    if (targetClause.isRootedInVar()) {
      int n = premises.size();
      for (int i=0; i < n; ++i) {
        Derivation.checkRootMatch(ctx,rule.getPremises().get(i),premises.get(i).getElement(),premises.get(i));
      }
      Derivation.checkRootMatch(ctx,rule.getConclusion(), conclusion.getElement(), conclusion);
    }

		
		// update the current substitution
    ctx.composeSub(unifyingSub);
    
    for (Derivation d : premises) { 
      d.addToDerivationMap(ctx);
    }
    conclusion.addToDerivationMap(ctx);
    
    // check whether the pattern is overly general requires that we first
    // compose the pairSub with the unifyingSub, which sometimes can fail
    // with a non-pattern substitution.  I probably could figure out how to
    // see how to avoid this problem, but it's easier simply to give up trying
    // to check the warning
    
    try {
      pairSub.compose(unifyingSub);
      Util.debug("Unifed pairSub = ", pairSub);
      Set<FreeVar> overlyGeneral = pairSub.selectUnavoidable(ctx.inputVars);
      if (!overlyGeneral.isEmpty()) {
        ErrorHandler.warning("The given pattern is overly general, should restrict " + overlyGeneral, this);
      }
    } catch (UnificationFailed ex) {
      Util.debug("pairSub unification failed ", ex.term1, " = ", ex.term2, 
          " while trying to compose ", pairSub, " with ", unifyingSub);
    }
    
		// update the set of subderivations
		if (isSubderivation != null) {
		  Pair<Fact,Integer> newSub = new Pair<Fact,Integer>(isSubderivation.first,isSubderivation.second+1);
			// add each premise to the list of subderivations
		  for (Fact f : premises) {
		    ctx.subderivations.put(f, newSub);
		  }
		}
		
		

		super.typecheck(ctx, isSubderivation);

		
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

		debug("new term = ", ruleTerm);

		return ruleTerm;
	}

	private Derivation conclusion;
	private List<Derivation> premises;
	private String ruleName;
	private Rule rule;
}

