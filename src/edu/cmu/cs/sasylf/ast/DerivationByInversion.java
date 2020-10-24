package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Errors.VAR_STRUCTURE_KNOWN;

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
	
	public static final QualName OR = new QualName(new Location("",0,0), "or");
	
	private final QualName ruleName;
	private final WhereClause whereClauses;

	public DerivationByInversion(String n, Location start, Location end, Clause c,
			QualName rule, String relation, WhereClause wcs) {
		super(n, start, c);
		setEndLocation(end); // overwrite end location to include entire justification
		ruleName = rule;
		addArgString(relation);
		whereClauses = wcs;
	}

	@Override
	public String prettyPrintByClause() {
		if (ruleName == null) return " by inversion" ;
		return " by inversion of " + ruleName;
	}

	public String getTargetDerivationName() { return super.getArgStrings().get(0).getElements().get(0).toString(); }

	@Override
	public void typecheck(Context ctx) {    
		super.typecheck(ctx);

		Fact targetDerivation = super.getArgs().get(0);
		String inputName = getTargetDerivationName();
		
		final Element targetElement = targetDerivation.getElement();
		final Term targetTerm = ctx.toTerm(targetElement);
		
		if (targetElement.getType() instanceof SyntaxDeclaration) {
			if (ruleName != null) {
				ErrorHandler.report("inversion on syntax doesn't use rules; just write 'inversion on " + inputName + "'", this);
			}
			FreeVar fv = targetTerm.getEtaEquivFreeVar();
			if (fv == null) {
				ErrorHandler.report(VAR_STRUCTURE_KNOWN, "The structure of " + targetDerivation+" is already known",this);
			} else if (ctx.isRelaxationVar(fv)) {
				ErrorHandler.report(VAR_STRUCTURE_KNOWN, "Case analysis cannot be done on this variable which is already known to be a bound variable", this);
			} else if (!ctx.inputVars.contains(fv)) {
				ErrorHandler.report("Undeclared syntax: " + targetDerivation +(ctx.inputVars.isEmpty() ? "":", perhaps you meant one of " + ctx.inputVars), targetDerivation);
			}
			ErrorHandler.report("Inversion on syntax not yet implemented", this);
		}
		if (!(targetElement instanceof ClauseUse)) {
			ErrorHandler.report(Errors.INVERSION_REQUIRES_CLAUSE,this);
		}
		ClauseUse targetClause = (ClauseUse)targetElement;
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

		Substitution userSub = whereClauses.typecheck(ctx);
		Set<FreeVar> userSubFree = userSub.getFreeVariables();
		
		// Do a mini-case analysis, and see if we find result in premises

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
						Util.debug("for rule = ",rule.getName());
						Util.debug("  term = ", p.first);
						Util.debug("  sub = ", p.second);
						Util.debug("  current = ", ctx.currentSub);
						Substitution newSubstitution = new Substitution(p.second);
						newSubstitution.compose(ctx.currentSub);
						Util.debug("  newSub = ", newSubstitution);
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
				pair.second.avoid(ctx.inputVars);
				pair.second.avoid(userSubFree);
				pair.second.avoid(result.getFreeVariables());
				pair.first = pair.first.substitute(pair.second);
				result = result.substitute(pair.second);
				Util.debug("  after adapt/subst, result = ", result);
				Util.debug("  case = ",pair.first);
				Util.debug("inversion gets substitution ",pair.second);
				ctx.composeSub(pair.second);
				
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
						Term mt = cu.asTerm(); // want user-level variables
						Term piece = pieces.get(i).substitute(ctx.currentSub);
						// ctx sub changes as a result of each checkMatch,
						// in the wrong way since we are applying it in the "opposite" way
						if (!Derivation.checkMatch(cu, ctx, mt, piece, null)) {
							String replaceContext = names.get(i) + ":... " + (i +1 < clauses.size() ? "and" : "by"); 
							String justified = TermPrinter.toString(ctx,targetClause.getAssumes(), cu.getLocation(),piece,true);
							ErrorHandler.report(Errors.OTHER_JUSTIFIED,": " + justified, cu, replaceContext + "\n" + justified);
						}
						// avoid mapping user-written variables
						ctx.avoidIfPossible(mt.getFreeVariables());
						
						// continue building up sigma_u from the user-written premises
						su.compose(ctx.currentSub);
						
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

				found_rulel = true;
			} else {
				ErrorHandler.report(Errors.MISSING_CASE,
						rule.getErrorDescription(iterator.next().first, ctx), this);       
			}
		}
		if (!found_rulel) {
			ErrorHandler.report(Errors.EXTRA_CASE, ": rule " + ruleName + " cannot be used to derive " + targetClause, this);
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
