package edu.cmu.cs.sasylf.ast;


import java.util.*;
import java.io.*;

import edu.cmu.cs.sasylf.term.*;
import edu.cmu.cs.sasylf.util.ErrorHandler;

import static edu.cmu.cs.sasylf.util.Util.*;
import static edu.cmu.cs.sasylf.ast.Errors.*;
import static edu.cmu.cs.sasylf.ast.Errors.*;


public abstract class DerivationByAnalysis extends Derivation {
	public DerivationByAnalysis(String n, Location l, Clause c, String derivName) {
		super(n,l,c); targetDerivationName = derivName;
	}

	public String getTargetDerivationName() { return targetDerivationName; }

	protected void computeTargetDerivation(Context ctx) {
		if (targetDerivation == null) {
			targetDerivation = ctx.derivationMap.get(targetDerivationName);
			debug("targetDerivation is " + targetDerivation);
			if (targetDerivation == null) {
				for (FreeVar v : ctx.inputVars) {
					if (targetDerivationName.equals(v.getName())) {
						if (this instanceof DerivationByInduction)
							ErrorHandler.report("Cannot perform induction over "+ targetDerivationName + " unless you add this variable as a specific \"forall\" clause of the theorem", this);
						else {
							targetDerivation = new SyntaxAssumption(targetDerivationName, getLocation());
							targetDerivation.typecheck(ctx, true);
							return;
						}
					}
				}
				ErrorHandler.report(DERIVATION_NOT_FOUND, "Cannot find a derivation named "+ targetDerivationName, this);
			}
		}
	}
	public Fact getTargetDerivation() {
		return targetDerivation;
	}

	public List<Case> getCases() { return cases; }

	public abstract String byPhrase();

	public void prettyPrint(PrintWriter out) {
		super.prettyPrint(out);
		out.println(" by "+byPhrase()+" on " + targetDerivationName + ":");
		for (Case c: cases) {
			c.prettyPrint(out);
		}
		out.println("end " + byPhrase());
	}

	public void typecheck(Context ctx) {
		computeTargetDerivation(ctx);

		super.typecheck(ctx, false);

		Term oldCase = ctx.currentCaseAnalysis;
		Element oldElement = ctx.currentCaseAnalysisElement;
		Term oldGoal = ctx.currentGoal;
		Clause oldGoalClause = ctx.currentGoalClause;
		Map<CanBeCase,Set<Pair<Term,Substitution>>> oldCaseTermMap = ctx.caseTermMap;
		ctx.currentCaseAnalysis = adapt(targetDerivation.getElement().asTerm(), targetDerivation.getElement(), ctx, true);
		debug("setting current case analysis to " + ctx.currentCaseAnalysis);
		//ctx.currentCaseAnalysis = targetDerivation.getElement().asTerm().substitute(ctx.currentSub);
		ctx.currentCaseAnalysisElement = targetDerivation.getElement();
		ctx.currentGoal = getElement().asTerm().substitute(ctx.currentSub);
		ctx.currentGoalClause = getClause();
		
		boolean isSubderivation = targetDerivation != null
			&& (targetDerivation.equals(ctx.inductionVariable) || ctx.subderivations.contains(targetDerivation));
		if (isSubderivation) debug("found subderivation: " + targetDerivation);
		
		ctx.caseTermMap = new HashMap<CanBeCase,Set<Pair<Term,Substitution>>>();

		if (ctx.currentCaseAnalysisElement instanceof NonTerminal) {
			Syntax syntax = ((NonTerminal)ctx.currentCaseAnalysisElement).getType();
			if (!(ctx.currentCaseAnalysis instanceof FreeVar))
				ErrorHandler.report(VAR_STRUCTURE_KNOWN, "The structure of variable " + ctx.currentCaseAnalysisElement + " is already known and so case analysis is unnecessary (and not currently supported by SASyLF)", this);			

			for (Clause clause : syntax.getClauses()) {
				if (clause.isVarOnlyClause()) {
					continue; // no cases for var clauses
				}
				Term term = ((ClauseDef)clause).getSampleTerm();
				Substitution freshSub = term.freshSubstitution(new Substitution());
				term.substitute(freshSub);
				Set<Pair<Term,Substitution>> set = new HashSet<Pair<Term,Substitution>>();
				set.add(new Pair<Term,Substitution>(term, new Substitution()));
				ctx.caseTermMap.put(clause, set);
			}
			
		} else {
			Judgment judge= (Judgment) ((ClauseUse)ctx.currentCaseAnalysisElement).getConstructor().getType();
			debug("*********** case analyzing line " + getLocation().getLine());
			//debug("    sub = " + ctx.currentSub);
			//debug("    adaptationSub = " + ctx.adaptationSub);
			// see if each rule, in turn, applies
			for (Rule rule : judge.getRules()) {
				Set<Pair<Term,Substitution>> caseResult = rule.caseAnalyze(ctx);
				ctx.caseTermMap.put(rule, caseResult);
			}
		}

		
		for (Case c : cases) {
			c.typecheck(ctx, isSubderivation);
		}
		
		for (Map.Entry<CanBeCase, Set<Pair<Term,Substitution>>> entry : ctx.caseTermMap.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				CanBeCase cbc = entry.getKey();
				ErrorHandler.report(Errors.MISSING_CASE,
									cbc.getErrorDescription(entry.getValue().iterator().next().first, ctx)/* + ": case is " + ctx.currentCaseAnalysis.substitute(sub)
									+ "\n\trule conc was " + arguments.get(lastIdx)*/, this);				
			}
		}
		
		/*if (!(ctx.currentCaseAnalysisElement instanceof NonTerminal) && !ctx.caseTermMap.isEmpty()) {
			Rule rule = ctx.caseTermMap.keySet().iterator().next();
			ErrorHandler.report(Errors.MISSING_CASE,
								rule.getName()/* + ": case is " + ctx.currentCaseAnalysis.substitute(sub)
								+ "\n\trule conc was " + arguments.get(lastIdx)* /, this);
		}*/

		ctx.caseTermMap = oldCaseTermMap;
		ctx.currentCaseAnalysis = oldCase;
		ctx.currentCaseAnalysisElement = oldElement;
		ctx.currentGoal = oldGoal ;
		ctx.currentGoalClause = oldGoalClause;
		this.addToDerivationMap(ctx);
	}

	/** Adapts this term using the current context
	 * This includes substituting with the current sub
	 * and also adapting the context to include assumptions currently in scope
	 */
	public static Term adapt(Term term, Element element, Context ctx, boolean wrapUnrooted) {
		// TODO: generalize this to all terms reference in system
		debug("for element " + element + " term.countLambdas() = "+term.countLambdas());
		if (ctx.adaptationSub != null) debug("ctx.matchTermForAdaptation.countLambdas() = "+ctx.matchTermForAdaptation.countLambdas());
		try {
		if (ctx.adaptationSub != null && term.countLambdas() < ctx.matchTermForAdaptation.countLambdas() && element instanceof ClauseUse && !ctx.innermostGamma.equals(((ClauseUse)element).getRoot())) {
			term = ((ClauseUse)element).adaptTermTo(term, ctx.matchTermForAdaptation, ctx.adaptationSub, wrapUnrooted);
		}
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		return term.substitute(ctx.currentSub);
	}

	/** Adapts this term using the current context
	 * This includes substituting with the current sub
	 * and also adapting the context to include assumptions currently in scope
	 */
	public static Term adapt(Term term, NonTerminal originalContext, Context ctx) {
		NonTerminal targetContext = ctx.innermostGamma;
		debug("adapting from " + originalContext + " to " + targetContext + " on " + term);
		
		if (originalContext != null && !originalContext.equals(targetContext)) {
			List<Term> varTypes = new ArrayList<Term>();
			List<String> varNames = new ArrayList<String>();
			
			while (!originalContext.equals(targetContext)) {
				AdaptationInfo info = ctx.adaptationMap.get(originalContext);
				if (info == null)
					ErrorHandler.report(Errors.UNKNOWN_CONTEXT,"The context variable " + originalContext + " is undefined", originalContext);
				varNames.addAll(info.varNames);
				varTypes.addAll(info.varTypes);
				originalContext = info.nextContext;
			}

			term = ClauseUse.doWrap(term, varNames, varTypes, ctx.adaptationSub == null? new Substitution() : ctx.adaptationSub);
		}
		else if (targetContext != null && !targetContext.equals(ctx.adaptationRoot)) {
			Set<FreeVar> varSet = term.getFreeVariables();
			varSet.retainAll(ctx.adaptationSub.getMap().keySet());
			if (!varSet.isEmpty()) {
				//TODO: make this more principled (e.g. work for more than one adaptation -- see code below)
				debug("adaptation sub = " + ctx.adaptationSub + " applied inside " + ctx.adaptationMap.get(ctx.adaptationRoot).varTypes.size());
				debug("current sub = " + ctx.currentSub);
				term = ((Abstraction)term).subInside(ctx.adaptationSub, ctx.adaptationMap.get(ctx.adaptationRoot).varTypes.size());
				debug("term = " + term);
			}
			
			/*NonTerminal checkContext = ctx.adaptationRoot;
			while (!checkContext.equals(targetContext)) {
				AdaptationInfo info = ctx.adaptationMap.get(checkContext);
				if (info == null)
					ErrorHandler.report(Errors.UNKNOWN_CONTEXT,"The context variable " + originalContext + " is undefined", originalContext);
				
				checkContext = info.nextContext;
			}*/
		}
		
		return term.substitute(ctx.currentSub);
	}

	private List<Case> cases = new ArrayList<Case>();
	private String targetDerivationName;
	private Fact targetDerivation;
}
