package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Errors.DERIVATION_NOT_FOUND;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Util;

public class DerivationByOrInversion extends DerivationWithArgs {
	
	private final String inputName;
	private final WhereClause whereClauses;

	public DerivationByOrInversion(String n, Location start, Location end, Clause c,
			String relation, WhereClause wcs) {
		super(n, start, c);
		setEndLocation(end); // overwrite end location to include entire justification
		inputName = relation;
		whereClauses = wcs;
	}

	@Override
	public String prettyPrintByClause() {
		return " by inversion of or on " + inputName;
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
		if (!(judge instanceof OrJudgment)) {
			ErrorHandler.report("inversion of 'or' can only be applied to a 'or' clause", this,
					"The judgment is " + judge);
		}
		if (!whereClauses.isEmpty()) {
			ErrorHandler.recoverableError("Inversion of 'or' never requires 'where' clauses", this);
		}

		// Do a mini-case analysis, and see if we find result in premises

		Term targetTerm = ctx.toTerm(targetClause);
		Set<Pair<Term,Substitution>> caseResult = new HashSet<Pair<Term,Substitution>>();
		
		Rule theRule = null;
		
		// see if each rule, in turn, applies
		for (Rule rule : judge.getRules()) {
			if (!rule.isInterfaceOK()) continue; // avoid these
			if (ctx.savedCaseMap != null && ctx.savedCaseMap.containsKey(inputName)) {
				for (Pair<Term,Substitution> p : ctx.savedCaseMap.get(inputName).get(rule)) {
					Pair<Term,Substitution> newPair;
					try {
						Util.debug("for 'or'");
						Util.debug("  term = ", p.first);
						Util.debug("  sub = ", p.second);
						Util.debug("  current = ", ctx.currentSub);
						Substitution newSubstitution = new Substitution(p.second);
						newSubstitution.compose(ctx.currentSub);
						Util.debug("  newSub = ", newSubstitution);
						newPair = new Pair<Term,Substitution>(p.first.substitute(newSubstitution),newSubstitution);
					} catch (UnificationFailed ex) {
						// XXX: WIll this actually ever happen?
						Util.debug("case no longer feasible.");
						continue;
					}
					caseResult.add(newPair);
					theRule = rule;
				}
			} else {
				final Set<Pair<Term, Substitution>> cases = rule.caseAnalyze(ctx, targetTerm, targetClause, this);
				if (!cases.isEmpty()) {
					caseResult.addAll(cases);
					theRule = rule;
				}
			}
		}
		
		if (caseResult.isEmpty()) {
			ErrorHandler.report("No more possibilities.  Suggest use 'contradiction' instead of 'inversion of or'", this);
		}
		if (caseResult.size() > 1) {
			ErrorHandler.report("Multiple cases remain.  Suggest case analysis.", this);
		}
		Pair<Term,Substitution> pair = caseResult.iterator().next();
		
		Util.verify(theRule != null, "How did we get here?");
		
		Term result = ctx.toTerm(getClause()); // this is the output of the derivation
		pair.second.avoid(ctx.inputVars);
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
			NonTerminal gamma = theRule.getConclusion().getRoot();
			for (int i=0; i < pieces.size(); ++i) {
				if (gamma.equals(theRule.getPremises().get(i).getRoot())) {
					Util.debug("adding wrappers to ",pieces.get(i));
					pieces.set(i,Term.wrapWithLambdas(outer, pieces.get(i)));
				}
			}
		}
		
		List<ClauseUse> clauses;
		List<String> names;
		if (this.getClause() instanceof AndClauseUse) {
			clauses = ((AndClauseUse)this.getClause()).getClauses();
			names = Arrays.asList(super.getName().split(","));
			if (clauses.size() == 0) {
				ErrorHandler.warning("'use Inversion of or' doesn't yield any information",	this);
				return;
			} 
		} else {
			clauses = Collections.singletonList((ClauseUse)this.getClause());
			names = Collections.singletonList(super.getName());
		}

		if (pieces.size() != clauses.size()) {
			ErrorHandler.report("inversion yields " + pieces.size() + " but only accepting " + clauses.size(), this);
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

			// If the derivation has no implicit context, we
			// skip the context check
			if (targetClause.isRootedInVar()) {
				checkRootMatch(ctx,theRule.getPremises().get(i),clauses.get(i),this);
			}
		}

		// Permit induction on this term if source was a subderivation
		if (ctx.subderivations.containsKey(targetDerivation)) {
			Pair<Fact, Integer> p = ctx.subderivations.get(targetDerivation);
			Pair<Fact, Integer> newPair = new Pair<Fact,Integer>(p.first,p.second+1);
			ctx.subderivations.put(this,newPair);
		} 
	}
}
