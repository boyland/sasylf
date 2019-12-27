package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Errors.DERIVATION_NOT_FOUND;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Util;

public class DerivationByInversion extends DerivationWithArgs {
	
	private final QualName ruleName;
	private final String inputName;
	private final WhereClause whereClauses;

	public DerivationByInversion(String n, Location start, Location end, Clause c,
			QualName rule, String relation, WhereClause wcs) {
		super(n, start, c);
		setEndLocation(end); // overwrite end location to include entire justification
		ruleName = rule;
		inputName = relation;
		whereClauses = wcs;
	}

	@Override
	public String prettyPrintByClause() {
		return " by inversion of " + ruleName + " on " + inputName;
	}

	@Override
	public void typecheck(Context ctx) {    
		super.typecheck(ctx);

		Fact targetDerivation = ctx.derivationMap.get(inputName);
		if (targetDerivation == null) {
			ErrorHandler.report(DERIVATION_NOT_FOUND, "Cannot find a derivation named "+ inputName, this);
			return;
		}
		if (!(targetDerivation.getElement() instanceof ClauseUse)) {
			ErrorHandler.report(Errors.INVERSION_REQUIRES_CLAUSE,this);
		}
		ClauseUse targetClause = (ClauseUse)targetDerivation.getElement();
		if (!(targetClause.getType() instanceof Judgment)) {
			ErrorHandler.report(Errors.INVERSION_REQUIRES_CLAUSE,this);
		}
		Judgment judge = (Judgment)targetClause.getType();
		Object resolution = ruleName.resolve(ctx);
		if (resolution == null) return; // error already signaled
		if (!(resolution instanceof Rule)) {
			ErrorHandler.report(Errors.RULE_NOT_FOUND, ruleName.toString(), this);
			return;
		}
		if (((Rule)resolution).getJudgment().typeTerm() != judge.typeTerm()) {
			ErrorHandler.report(Errors.EXTRA_CASE, ": rule " + ruleName + " cannot be used to derive " + targetClause, this);
		}

		whereClauses.typecheck(ctx);
		
		// Do a mini-case analysis, and see if we find result in premises

		Term targetTerm = ctx.toTerm(targetClause);

		// build a sigma_u (substitution imposed by this inversion)
		// starting with the current sigma
		// sigma_u mappings from CAS variables will need where clauses
		Substitution su = new Substitution(ctx.currentSub);
		
		boolean found_rulel = false;

		// see if each rule, in turn, applies
		for (Rule rule : judge.getRules()) {
			if (!rule.isInterfaceOK()) continue; // avoid these
			Set<Pair<Term,Substitution>> caseResult;
			if (ctx.savedCaseMap != null && ctx.savedCaseMap.containsKey(inputName)) {
				caseResult = new HashSet<Pair<Term,Substitution>>();
				for (Pair<Term,Substitution> p : ctx.savedCaseMap.get(inputName).get(rule)) {
					// TODO: refactor this with DerivationByAnalysis
					Pair<Term,Substitution> newPair;
					try {
						Util.debug("term = ", p.first);
						Util.debug("sub = ", p.second);
						Util.debug("current = ", ctx.currentSub);
						Substitution newSubstitution = new Substitution(p.second);
						newSubstitution.compose(ctx.currentSub);
						Util.debug("newSub = ", newSubstitution);
						newPair = new Pair<Term,Substitution>(p.first.substitute(newSubstitution),newSubstitution);
					} catch (UnificationFailed ex) {
						Util.debug("case no longer feasible.");
						continue;
					}
					caseResult.add(newPair);
				}
			} else {
				caseResult = rule.caseAnalyze(ctx, targetTerm, targetClause, this);
			}
			if (caseResult.isEmpty()) continue;
			Iterator<Pair<Term, Substitution>> iterator = caseResult.iterator();
			if (rule == resolution) {
				Util.debug("before inversion: targetTerm = " + targetTerm);
				Util.debug("inversion: before, sub = " + ctx.currentSub);
				Util.debug("inversion: caseResult = ", caseResult);
				Pair<Term,Substitution> pair = iterator.next();
				if (iterator.hasNext()) {
					ErrorHandler.report("Cannot use inversion: two or more instance of '" + ruleName + "' apply.", this);
				}
				Term result = ctx.toTerm(getClause()); // this is the output of the derivation
				result = result.substitute(pair.second);
				Util.debug("  after adapt/subst, result = ", result);
				Util.debug("inversion gets substitution ",pair.second);
				ctx.composeSub(pair.second); // ctx sub changes from inversion with internal "fresh" rule
				List<Abstraction> outer = new ArrayList<Abstraction>();
				Application ruleInstance = (Application)Term.getWrappingAbstractions(pair.first,outer);
				List<Term> pieces = new ArrayList<Term>(ruleInstance.getArguments());
				pieces.remove(pieces.size()-1);
				if (outer.size() > 1) {
					NonTerminal gamma = rule.getConclusion().getRoot();
					for (int i=0; i < pieces.size(); ++i) {
						if (gamma.equals(rule.getPremises().get(i).getRoot())) {
							Util.debug("adding wrappers to ",pieces.get(i));
							pieces.set(i,Term.wrapWithLambdas(outer, pieces.get(i)));
						}
					}
				}
				// If there are multiple clauses, or if 
				if (pieces.size() <= 1 || this.getClause() instanceof AndClauseUse) {
					List<ClauseUse> clauses;
					List<String> names;
					if (this.getClause() instanceof AndClauseUse) {
						clauses = ((AndClauseUse)this.getClause()).getClauses();
						names = Arrays.asList(super.getName().split(","));
					} else {
						clauses = Collections.singletonList((ClauseUse)this.getClause());
						names = Collections.singletonList(super.getName());
					}
					if (pieces.size() != clauses.size()) {
						// If clauses.size() == 0, we are "use inversion" which can
						// ignore all results.
						if (clauses.size() > 0) { 
							ErrorHandler.report("inversion yields " + pieces.size() + " but only accepting " + clauses.size(), this);
						}
					}
					for (int i=0; i < clauses.size(); ++i) {
						ClauseUse cu = clauses.get(i);
						Term mt = ctx.toTerm(cu);
						Term piece = pieces.get(i).substitute(ctx.currentSub);
						// ctx sub changes as a result of each checkMatch,
						// with user-supplied RHS's (as opposed to "fresh" version)
						if (!Derivation.checkMatch(cu, ctx, mt, piece, null)) {
							String replaceContext = names.get(i) + ":... " + (i +1 < clauses.size() ? "and" : "by"); 
							String justified = TermPrinter.toString(ctx,targetClause.getAssumes(), cu.getLocation(),piece,true);
							ErrorHandler.report(Errors.OTHER_JUSTIFIED,": " + justified, cu, replaceContext + "\n" + justified);
						}
						
						// continue building up sigma_u from the user-written premises
						// avoid mapping user-written variables
						su.compose(ctx.currentSub);
						su.avoid(mt.getFreeVariables());
						
						// If the derivation has no implicit context, we
						// skip the context check
						if (targetClause.isRootedInVar()) {
							checkRootMatch(ctx,rule.getPremises().get(i),clauses.get(i),this);
						}
					}
				} else {
					// backward compatibility: just look for result in the pieces
					if (!pieces.contains(result)) {
						ErrorHandler.report(Errors.INVERSION_NOT_FOUND, this,
								"\t SASyLF did not find " + result + " in " + pieces);
					}
					if (targetClause.isRootedInVar()) {
						int i = pieces.indexOf(result);
						checkRootMatch(ctx,rule.getPremises().get(i),this.getElement(),this);
					}
				}
				// Because we do the unifications step by step, we can't just "avoid"
				// on the substitutions as they come, but instead do this at the end:
				Term userResult = getClause().asTerm(); 
				ctx.avoidIfPossible(userResult.getFreeVariables());
				su.avoid(userResult.getFreeVariables());
				found_rulel = true;
			} else {
				ErrorHandler.report(Errors.MISSING_CASE,
						rule.getErrorDescription(iterator.next().first, ctx), this);       
			}
		}
		if (!found_rulel) {
			ErrorHandler.report(Errors.EXTRA_CASE, ": rule " + ruleName + " cannot be used to derive " + ctx.currentCaseAnalysisElement, this);
		}

		// add to sigma_u new mappings from CAS variables to generated terms
		// (for which the user has not provided their own terms in premises)
		for (FreeVar v : targetTerm.getFreeVariables()) {
			if (su.getSubstituted(v) == null) { // favor user-defined mappings
				Term mapped = ctx.currentSub.getSubstituted(v);
				if (mapped != null)
					su.add(v, mapped);
			}
		}
		
		// verify user-written where clauses
		whereClauses.checkWhereClauses(ctx, targetTerm, null, su, this);

		// Permit induction on this term if source was a subderivation
		if (ctx.subderivations.containsKey(targetDerivation)) {
			Pair<Fact, Integer> pair = ctx.subderivations.get(targetDerivation);
			Pair<Fact, Integer> newPair = new Pair<Fact,Integer>(pair.first,pair.second+1);
			ctx.subderivations.put(this,newPair);
		} 
	}
}
