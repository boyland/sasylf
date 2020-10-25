package edu.cmu.cs.sasylf.ast;


import static edu.cmu.cs.sasylf.util.Errors.VAR_STRUCTURE_KNOWN;
import static edu.cmu.cs.sasylf.util.Util.debug;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.BoundVar;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.SingletonSet;
import edu.cmu.cs.sasylf.util.Util;


public abstract class DerivationByAnalysis extends DerivationWithArgs {
	public DerivationByAnalysis(String n, Location l, Clause c, String derivName) {
		super(n,l,c); 
		super.addArgString(derivName);
	}
	public DerivationByAnalysis(String n, Location l, Clause c, Clause subject) {
		super(n,l,c);
		super.addArgString(subject);
	}
	public DerivationByAnalysis(String n, Location l, Clause c) {
		super(n,l,c);
	}

	public String getTargetDerivationName() { return super.getArgStrings().get(0).getElements().get(0).toString(); }

	protected void computeTargetDerivation(Context ctx) {
		if (targetDerivation == null) {
			super.typecheck(ctx);
			targetDerivation = getArgs().get(0);
			Util.debug("targetDerivation is ", targetDerivation);
		}
	}

	public Fact getTargetDerivation() {
		return targetDerivation;
	}

	public List<Case> getCases() { return cases; }

	public abstract String byPhrase();

	@Override
	public String prettyPrintByClause() {
		return " by " + byPhrase();
	}

	@Override
	public void prettyPrint(PrintWriter out) {
		super.prettyPrint(out);
		for (Case c: cases) {
			c.prettyPrint(out);
		}
		out.println("end " + byPhrase());
	}

	@Override
	public void typecheck(Context ctx) {
		super.typecheck(ctx);
		computeTargetDerivation(ctx);
		Util.debug("On line ",getLocation().getLine()," varFree = ",ctx.varFreeNTmap.keySet());


		Term oldCase = ctx.currentCaseAnalysis;
		Element oldElement = ctx.currentCaseAnalysisElement;
		Term oldGoal = ctx.currentGoal;
		Clause oldGoalClause = ctx.currentGoalClause;
		Map<CanBeCase,Set<Pair<Term,Substitution>>> oldCaseTermMap = ctx.caseTermMap;

		final String targetName = targetDerivation.getName();
		final Element targetElement = targetDerivation.getElement();
		ClauseType caseType = (ClauseType)targetElement.getType();
		final Term targetTerm = ctx.toTerm(targetElement);

		if (caseType.isAbstract()) {
			if (caseType instanceof NotJudgment) {
				ErrorHandler.report("Cannot perform case analysis on 'not' judgments",this);
			} else {
				ErrorHandler.report("Cannot perform case analysis on unspecified type " + caseType, this);
			}
		}

		if (caseType instanceof Judgment) {
			if (targetDerivation instanceof ClauseAssumption) {
				ErrorHandler.report("Cannot perform case analysis on a constructed judgment", targetDerivation);
			}
		} else {
			checkSyntaxAnalysis(ctx, targetName, targetTerm, this);
		}

		try {
			ctx.currentCaseAnalysis = targetTerm;
			debug("setting current case analysis to ", ctx.currentCaseAnalysis);
			ctx.currentCaseAnalysisElement = targetElement;
			ctx.currentGoal = getElement().asTerm().substitute(ctx.currentSub);
			ctx.currentGoalClause = getClause();

			Pair<Fact,Integer> isSubderivation = ctx.subderivations.get(targetDerivation);
			if (isSubderivation != null) debug("found subderivation: ", targetDerivation);

			ctx.caseTermMap = new LinkedHashMap<CanBeCase,Set<Pair<Term,Substitution>>>();

			caseAnalyze(ctx, targetName, targetElement, this, ctx.caseTermMap);

			SASyLFError error = null;
			for (Case c : cases) {
				try {
					c.typecheck(ctx, isSubderivation);
				} catch (SASyLFError ex) {
					error = ex;
				}
			}

			if (this instanceof PartialCaseAnalysis) {
				if (ctx.savedCaseMap == null) ctx.savedCaseMap = new HashMap<String,Map<CanBeCase,Set<Pair<Term,Substitution>>>>();
				ctx.savedCaseMap.put(targetName, ctx.caseTermMap);
				return;
			}
			if (error != null) throw error;

			StringBuilder missingMessages = null;
			StringBuilder missingCaseTexts = null;
			for (Map.Entry<CanBeCase, Set<Pair<Term,Substitution>>> entry : ctx.caseTermMap.entrySet()) {
				if (!entry.getValue().isEmpty()) {
					CanBeCase cbc = entry.getKey();
					for (Pair<Term,Substitution> missing : entry.getValue()) {
						// Pair<Term,Substitution> missing = entry.getValue().iterator().next();  
						String missingCaseText = null;
						// System.out.println("Missing: " + missing);
						Substitution sub = missing.second;
						Substitution revSub = new Substitution();
						//XXX: Shouldn't this be done using selectUnavoidable ?
						for (Map.Entry<FreeVar,Term> e2 : sub.getMap().entrySet()) {
							if (e2.getValue() instanceof FreeVar && !ctx.inputVars.contains(e2.getValue()) && ctx.inputVars.contains(e2.getKey())) {
								// System.out.println("Adding reverse substitution: " + e2.getValue() + " to " + e2.getKey());
								revSub.add((FreeVar)e2.getValue(), e2.getKey());
							}
						}
						Term missingCase = missing.first.substitute(revSub);
						Element targetGamma = null;
						if (ctx.currentCaseAnalysisElement instanceof ClauseUse) {
							ClauseUse target = (ClauseUse)ctx.currentCaseAnalysisElement;
							if (target.getConstructor().getAssumeIndex() >= 0) {
								targetGamma = target.getElements().get(target.getConstructor().getAssumeIndex());
							}
						} else if (ctx.currentCaseAnalysisElement instanceof AssumptionElement) {
							targetGamma = ((AssumptionElement)ctx.currentCaseAnalysisElement).getAssumes();
						} else if (ctx.currentCaseAnalysisElement instanceof NonTerminal) {
							if (!ctx.isVarFree((NonTerminal)ctx.currentCaseAnalysisElement)) {
								targetGamma = ctx.assumedContext;
							}
						}
						if (ctx.currentCaseAnalysis.countLambdas() < missingCase.countLambdas()) {
							if (targetGamma instanceof ClauseUse) {
								targetGamma = ((ClauseUse)targetGamma).getRoot();
							}
							NonTerminal gammaNT = (NonTerminal)targetGamma;
							NonTerminal newGammaNT = null;
							if (ctx.relaxationMap != null) {
								Set<FreeVar> free = ctx.currentCaseAnalysis.getFreeVariables();
								free.retainAll(ctx.relaxationVars);
								if (!free.isEmpty()) {
									for (Map.Entry<NonTerminal, Relaxation> e : ctx.relaxationMap.entrySet()) {
										if (e.getValue().getRelaxationVars().containsAll(free)) {
											newGammaNT = e.getKey();
										}
									}
								}
							}
							if (newGammaNT == null) {
								String newName = gammaNT.getSymbol()+"'";
								int i=0;
								while (ctx.isKnownContext(newGammaNT = new NonTerminal(newName, gammaNT.getLocation()))) {
									newName = gammaNT.getSymbol()+i;
									++i;
								}
							}
							newGammaNT.setType(gammaNT.getType());
							targetGamma = newGammaNT;
						}
						//XXX: The work to get Gamma' here is ignored in TermPrinter for SyntaxCase: 
						// it gets Gamma0 which is more correct, but uglier (it makes sure it is unique), 
						// but this fact shows some disconnect.

						String missingMessage = cbc.getErrorDescription(entry.getValue().iterator().next().first, ctx);
						try {
							TermPrinter termPrinter = new TermPrinter(ctx,targetGamma,this.getLocation());
							missingCaseText = termPrinter.caseToString(missingCase);
						} catch (RuntimeException ex) {
							System.err.println("Couldn't print term: " + missingCase);
							ex.printStackTrace();
							ErrorHandler.report(Errors.MISSING_CASE, missingMessage,getArgStrings().get(0));
						}
						// System.out.println("missing: " + missing);
						if (missingCaseText != null) {
							// System.out.println("Missing Case:");
							// System.out.println(missingCaseText);
						}
						if (missingMessages == null) {
							missingMessages = new StringBuilder();
							missingCaseTexts = new StringBuilder();
						}
						missingMessages.append(missingMessage);
						missingMessages.append('\n');
						missingCaseTexts.append(missingCaseText);
						missingCaseTexts.append('\n');
					}
				}
			}

			if (missingMessages != null) {
				missingMessages.setLength(missingMessages.length()-1);
				int firstNL = missingMessages.indexOf("\n");
				if (firstNL > -1) {
					missingMessages.insert(0, '\n');
				}
				// Util.debug("adaptationSub = ",ctx.adaptationSub);
				ErrorHandler.report(Errors.MISSING_CASE, missingMessages.toString(), getArgStrings().get(0), missingCaseTexts.toString());
			}

		} finally {
			ctx.caseTermMap = oldCaseTermMap;
			ctx.currentCaseAnalysis = oldCase;
			ctx.currentCaseAnalysisElement = oldElement;
			ctx.currentGoal = oldGoal ;
			ctx.currentGoalClause = oldGoalClause;
		}
		// this.addToDerivationMap(ctx);
	}
	
	/**
	 * Check a syntax analysis target: it must be a free variable without added constraints.
	 * @param ctx context
	 * @param targetName name of term being analyzed (for error messages)
	 * @param targetTerm term being analyzed
	 * @param source error location
	 */
	public static void checkSyntaxAnalysis(Context ctx, final String targetName,
			final Term targetTerm, Node source) {
		FreeVar fv = null;
		Term bare = Term.getWrappingAbstractions(targetTerm, null);
		if (bare instanceof FreeVar) fv = (FreeVar)bare;
		else if (bare instanceof Application) {
			Application app = (Application)bare;
			if (app.getFunction() instanceof FreeVar) {
				fv = (FreeVar)app.getFunction();
				Set<Term> args = new HashSet<Term>();
				for (Term t : app.getArguments()) {
					if (!(t instanceof BoundVar) || !args.add(t)) {
						ErrorHandler.report(VAR_STRUCTURE_KNOWN, "Case analysis is only permitted on pure binders, with unique variable arguments", source);
					}
				}
			}
		}
		if (fv == null) {
			ErrorHandler.report(VAR_STRUCTURE_KNOWN, "The structure of " + targetName+" is already known",source,
					"SASyLF computed it as " + targetTerm);
		} else if (ctx.isRelaxationVar(fv)) {
			ErrorHandler.report(VAR_STRUCTURE_KNOWN, "Case analysis cannot be done on this variable which is already known to be a bound variable", source);
		} else if (!ctx.inputVars.contains(fv)) {
			ErrorHandler.report("Undeclared syntax: " + targetName +(ctx.inputVars.isEmpty() ? "":", perhaps you meant one of " + ctx.inputVars), source);
		}
	}
	
	/** Case analyze a target in the current context and store the
	 * results in the map passed in.  This code first checks the saved map,
	 * and otherwise does a new case analysis.
	 * @param ctx context
	 * @param targetName name of the target, used to look up in saved case map
	 * @param targetElement element being cased
	 * @param source node to use for errors, or debugging
	 * @param map map into which to place cases
	 */
	public static void caseAnalyze(Context ctx, final String targetName,
			final Element targetElement, Node source,
			Map<CanBeCase, Set<Pair<Term, Substitution>>> map) {
		Map<CanBeCase,Set<Pair<Term,Substitution>>> savedMap = null;
		if (ctx.savedCaseMap != null) savedMap = ctx.savedCaseMap.get(targetName);
		if (savedMap != null) {
			// System.out.println("\n*** Checking saved cases.\n");
			for (Map.Entry<CanBeCase, Set<Pair<Term,Substitution>>> e : savedMap.entrySet()) {
				Set<Pair<Term, Substitution>> newSet = new SingletonSet<Pair<Term,Substitution>>();
				for (Pair<Term,Substitution> p : e.getValue()) {
					Pair<Term,Substitution> newPair;
					try {
						Util.debug("Saved case:\nterm = ", p.first);
						Util.debug("sub = ", p.second);
						Util.debug("current = ", ctx.currentSub);
						Substitution newSubstitution = new Substitution(p.second);
						newSubstitution.compose(ctx.currentSub);
						if (!ctx.canCompose(newSubstitution)) {
							Util.debug("case no longer feasible (relaxation): ");
							continue;
						}
						Util.debug("newSub = ", newSubstitution);
						newPair = new Pair<Term,Substitution>(p.first.substitute(newSubstitution),newSubstitution);
					} catch (UnificationFailed ex) {
						Util.debug("case no longer feasible.");
						continue;
					}
					if (newSet.size() == 1) newSet = new HashSet<Pair<Term,Substitution>>(newSet);
					newSet.add(newPair);
				}
				map.put(e.getKey(), newSet);
			}

		} else {
			debug("*********** case analyzing line ", source.getLocation().getLine());
			final ClauseType ctype = (ClauseType)targetElement.getType();
			ctype.analyze(ctx, targetElement, source, map);
		}
	}

	private List<Case> cases = new ArrayList<Case>();
	private Fact targetDerivation;
}
