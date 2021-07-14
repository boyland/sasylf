package edu.cmu.cs.sasylf.ast;


import static edu.cmu.cs.sasylf.util.Util.debug;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.BoundVar;
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
				ErrorHandler.error(Errors.CASE_SUBJECT_NOT,this);
			} else {
				ErrorHandler.error(Errors.CASE_SUBJECT_ABSTRACT,caseType.getName(), this);
			}
		}

		if (caseType instanceof Judgment) {
			Util.verify(!(targetDerivation instanceof ClauseAssumption), 
					"DerivationWithArgs is supposed to stop this sort of thing!");
		} else {
			// see bad79.slf: exploit2
			if (targetElement instanceof AssumptionElement) {
				NonTerminal contextRoot = ctx.assumedContext;
				NonTerminal root = targetElement.getRoot();
				if (contextRoot != null && !contextRoot.equals(root)) {
					Util.debug("targetTerm = ", targetTerm, ", var free = ", ctx.varFreeNTmap);
					if (!ctx.isVarFree(targetElement)) {
						ErrorHandler.recoverableError(Errors.CASE_SUBJECT_ROOT_INTERNAL, contextRoot.toString(), targetElement);
					}
				}
			}
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
			
			generateMissingCaseError(ctx, targetElement, targetTerm, Errors.MISSING_CASE,
					getArgStrings().get(0), ctx.caseTermMap);

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
						ErrorHandler.error(Errors.CASE_SUBJECT_BINDING_IMPURE, source);
					}
				}
			}
		}
		if (fv == null) {
			ErrorHandler.error(Errors.CASE_SUBJECT_KNOWN, source, "SASyLF computed it as " + targetTerm);
		} else if (ctx.isRelaxationVar(fv)) {
			ErrorHandler.error(Errors.CASE_SUBJECT_RELAX, source);
		} else if (!ctx.inputVars.contains(fv)) {
			ErrorHandler.error(Errors.CASE_SUBJECT_UNKNOWN, ctx.inputVars.toString(), source);
		}
	}
	
	/// Case analysis methods:
	// These are used also by inversion, and so are factored out.
	
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
					Util.debug("Saved case:\nterm = ", p.first);
					Util.debug("sub = ", p.second);
					Util.debug("current = ", ctx.currentSub);
					Substitution newSubstitution = new Substitution(p.second);
					try {
						newSubstitution.merge(ctx.currentSub); // need merge, not compose
						if (!ctx.canCompose(newSubstitution)) {
							Util.debug("case no longer feasible (relaxation): ");
							continue;
						}
						Util.debug("newSub = ", newSubstitution);
						newPair = new Pair<Term,Substitution>(p.first.substitute(newSubstitution),newSubstitution);
					} catch (UnificationIncomplete ex) {
						Util.debug("case cannot be checked: ", newSubstitution, "\n cannot merge with ", ctx.currentSub);
						if (e.getKey() instanceof Rule && !((Rule)e.getKey()).isAssumption()) {
							Util.debug("  Trying again de novo");
							// let's try doing a case analysis de novo
							Term targetTerm = ctx.toTerm(targetElement);
							newSet = e.getKey().caseAnalyze(ctx, targetTerm, targetElement, source);
							continue;
						}
						ErrorHandler.error(Errors.CASE_UNIFICATION_INCOMPLETE, e.getKey().getName(), source, "SASyLF tried to unify " + ex.term1 + " and " + ex.term2);
						continue;
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

	/**
	 * Compute the (remaining) size of a case analysis
	 * @param map the case analysis map
	 * @return the number of elements in all the sets
	 */
	public static int caseAnalysisSize(Map<CanBeCase,Set<Pair<Term,Substitution>>> map) {
		int result = 0;
		for (Set<Pair<Term,Substitution>> s : map.values()) {
			result += s.size();
		}
		return result;
	}

	/**
	 * Generate an error report if there are missing cases (if the map contains any cases still).
	 * @param ctx
	 * @param targetElement
	 * @param targetTerm
	 * @param errorClass error to report
	 * @param errorClause location to report error on
	 * @param caseMap map of remaining cases
	 */
	public static void generateMissingCaseError(Context ctx,
			final Element targetElement, final Term targetTerm,
			final Errors errorClass, final Node errorClause,
			final Map<CanBeCase, Set<Pair<Term, Substitution>>> caseMap) {
		StringBuilder missingMessages = null;
		StringBuilder missingCaseTexts = null;
		for (Map.Entry<CanBeCase, Set<Pair<Term,Substitution>>> entry : caseMap.entrySet()) {
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
					if (targetElement instanceof ClauseUse) {
						ClauseUse target = (ClauseUse)targetElement;
						if (target.getConstructor().getAssumeIndex() >= 0) {
							targetGamma = target.getElements().get(target.getConstructor().getAssumeIndex());
						}
					} else if (targetElement instanceof AssumptionElement) {
						targetGamma = ((AssumptionElement)targetElement).getAssumes();
					} else if (targetElement instanceof NonTerminal) {
						if (!ctx.isVarFree((NonTerminal)targetElement)) {
							targetGamma = ctx.assumedContext;
						}
					}
					if (targetTerm.countLambdas() < missingCase.countLambdas()) {
						if (targetGamma instanceof ClauseUse) {
							targetGamma = ((ClauseUse)targetGamma).getRoot();
						}
						NonTerminal gammaNT = (NonTerminal)targetGamma;
						NonTerminal newGammaNT = null;
						if (ctx.relaxationMap != null) {
							Set<FreeVar> free = targetTerm.getFreeVariables();
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
						TermPrinter termPrinter = new TermPrinter(ctx,targetGamma,errorClause.getLocation());
						missingCaseText = termPrinter.caseToString(missingCase);
					} catch (RuntimeException ex) {
						System.err.println("Couldn't print term: " + missingCase);
						ex.printStackTrace();
						ErrorHandler.error(errorClass, missingMessage,errorClause);
					}
					// System.out.println("missing: " + missing);
					// System.out.println("   case: " + missingCase);
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
			ErrorHandler.error(errorClass, missingMessages.toString(), errorClause, missingCaseTexts.toString());
		}
	}

	private List<Case> cases = new ArrayList<Case>();
	private Fact targetDerivation;
	
	@Override
	public void collectQualNames(Consumer<QualName> consumer) {
		for (Case c : cases) {
			c.collectQualNames(consumer);
		}
	}
}
