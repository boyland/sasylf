package edu.cmu.cs.sasylf.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
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
		final ElementType caseType = targetElement.getType();
		
		if (caseType instanceof SyntaxDeclaration) {
			if (ruleName != null) {
				ErrorHandler.report("inversion on syntax doesn't use rules; just write 'inversion on " + inputName + "'", this);
			}
			if (!(this.getClause() instanceof AndClauseUse) ||
				!((AndClauseUse)this.getClause()).getClauses().isEmpty()) {
				ErrorHandler.report("cannot prove anything with inversion on syntax.  Suggest 'use inversion'", this);
			}			
			DerivationByAnalysis.checkSyntaxAnalysis(ctx, inputName, targetTerm, this);
		}
		
		Object resolution = null;
		
		if (ruleName == OR) {
			if (!(caseType instanceof OrJudgment)) {
				ErrorHandler.report("inversion of 'or' can only be applied to a 'or' clause", this,
						"The judgment is " + caseType);
			}
		} else if (ruleName != null) {
			resolution = ruleName.resolve(ctx);
			if (resolution == null) return; // error already signaled
			if (!(resolution instanceof Rule)) {
				ErrorHandler.report(Errors.RULE_NOT_FOUND, ruleName.toString(), this);
				return;
			}
			if (((Rule)resolution).getJudgment().typeTerm() != caseType.typeTerm()) {
				ErrorHandler.report(Errors.EXTRA_CASE, ": rule " + ruleName + " cannot be used to derive " + targetElement, this);
			}
			if (!((Rule)resolution).isInterfaceOK()) {
				ErrorHandler.report("Rule " + ruleName + " cannot be used for inversion until it is fixex.", this);
			}
		}

		Substitution userSub = whereClauses.typecheck(ctx);
		Set<FreeVar> userSubFree = userSub.getFreeVariables();
		
		// Do a mini-case analysis, and see if we find result in premises
		Map<CanBeCase,Set<Pair<Term,Substitution>>> caseMap = new HashMap<CanBeCase,Set<Pair<Term,Substitution>>>();		
		DerivationByAnalysis.caseAnalyze(ctx, inputName, targetElement, this, caseMap);
		
		final int caseSize = DerivationByAnalysis.caseAnalysisSize(caseMap);
		if (caseSize == 0) {
			ErrorHandler.report("The target of inversion is actually not possible.  Suggest using 'by contradiction on' instead of inversion.", this);
		}
		if (caseSize > 1) {
			DerivationByAnalysis.generateMissingCaseError(ctx, targetElement, targetTerm, Errors.TOO_MANY_CASES, this, caseMap);
		}
		
		CanBeCase only = null;
		for (Map.Entry<CanBeCase, Set<Pair<Term,Substitution>>> e : caseMap.entrySet()) {
			if (e.getValue().isEmpty()) continue;
			only = e.getKey();
			break;
		}
		
		if (resolution != null && resolution != only) {
			Rule otherRule = (Rule)only;			
			ErrorHandler.report(Errors.WRONG_RULE, 
					"Wrong rule named; should be "+otherRule.getName(), this, 
					"by rule " + ruleName + "\nby rule " + otherRule.getName());
		}

		// build a sigma_u (substitution imposed by this inversion)
		// starting with the current sigma
		// sigma_u mappings from CAS variables will need where clauses
		Substitution su = new Substitution(ctx.currentSub);
		
		Set<Pair<Term,Substitution>> caseResult = caseMap.get(only);
		Util.verify(caseResult.size() == 1, "Computered as such!");
		Pair<Term,Substitution> pair = caseResult.iterator().next();
		Util.debug("Inversion on " + this.getLocation());
		Util.debug("before inversion: targetTerm = " + targetTerm);
		Util.debug("inversion: before, sub = " + ctx.currentSub);
		Util.debug("inversion: caseResult = ", caseResult);
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

		if (only instanceof Rule) {
			Rule rule = (Rule)only;
			ClauseUse targetClause = (ClauseUse)targetElement;
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
					Substitution changed = ctx.avoidIfPossible(mt.getFreeVariables());

					// continue building up sigma_u from the user-written premises
					su.compose(changed); 

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
		}

		// add to sigma_u new mappings from CAS variables to generated terms
		// (for which the user has not provided their own terms in premises)
		for (FreeVar v : targetTerm.getFreeVariables()) {
			if (su.getSubstituted(v) == null) { // favor user-defined mappings
				Term mapped = ctx.currentSub.getSubstituted(v);
				if (mapped != null) {
					su.add(v, mapped);
				}
			}
		}
		
		// verify user-written where clauses
		whereClauses.checkWhereClauses(ctx, targetTerm, null, su, this);

		// Permit induction on this term if source was a subderivation
		if (ctx.subderivations.containsKey(targetDerivation)) {
			Pair<Fact, Integer> p = ctx.subderivations.get(targetDerivation);
			Pair<Fact, Integer> newPair = new Pair<Fact,Integer>(p.first,p.second+1);
			ctx.subderivations.put(this,newPair);
		} 
	}
	
	@Override
	public void collectQualNames(Consumer<QualName> consumer) {
		ruleName.visit(consumer);
	}
}
