package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Util.debug;
import static edu.cmu.cs.sasylf.util.Util.verify;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Atom;
import edu.cmu.cs.sasylf.term.EOCUnificationFailed;
import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.term.UnificationIncomplete;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Util;


public class RuleCase extends Case {
	
	private Rule rule;
	private final QualName ruleName;
	private final List<Derivation> premises;
	private final Derivation conclusion;
	private final WhereClause whereClauses;
	
	public RuleCase(Location l, Location l1, Location l2,
			QualName rn, List<Derivation> ps, Derivation c, WhereClause wcs) {
		super(l, l1, l2);
		conclusion = c;
		premises = ps;
		ruleName = rn;
		whereClauses = wcs;
	}
	public String getRuleName() { return ruleName.toString(); }
	public CanBeCase getRule() { return rule; }
	public List<Derivation> getPremises() { return premises; }
	public Derivation getConclusion() { return conclusion; }

	@Override
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

	@Override
	public void typecheck(Context parent, Pair<Fact,Integer> isSubderivation) {
		Context ctx = parent.clone();
		debug("line "+ this.getLocation().getLine(), " case ", ruleName);
		debug("    currentSub = ", ctx.currentSub);
		if (rule == null) {
			Judgment judg = ctx.getJudgment(ctx.currentCaseAnalysis.baseTypeFamily());
			if (judg == null) {
				if (ctx.currentCaseAnalysisElement.getType() instanceof SyntaxDeclaration) {
					ErrorHandler.error(Errors.RULE_CASE_SYNTAX, this, "SASyLF computes the case analysis is on " + ctx.currentCaseAnalysis);
				} else {
					ErrorHandler.error(Errors.INTERNAL_ERROR, ": Got an instance of a secret judgment? " + ctx.currentCaseAnalysisElement, this);
				}
			}
			String stringName = ruleName.toString();
			if (ruleName.getLastSegment().equals(stringName)) { //XXX change to checkSource
				for (Rule r : judg.getRules()) {
					if (r.getName().equals(stringName)) rule = r;
				}
				if (rule == null) {
					ErrorHandler.error(Errors.RULE_NOT_FOUND, stringName, this);
				}
				ruleName.resolveAsNamed(rule);
			} else {
				Object resolution = ruleName.resolve(ctx);
				if (!(resolution instanceof Rule)) {
					ErrorHandler.error(Errors.RULE_EXPECTED, QualName.classify(resolution) + " " + ruleName, ruleName);
				}
				for (Rule r : judg.getRules()) {
					if (r == resolution) rule = r;
				}
				if (rule == null) {
					ErrorHandler.error(Errors.RULE_WRONG_JUDGMENT, judg.getName(), ruleName);
				} else {
					ErrorHandler.warning(Errors.RULE_NO_QUALIFICATION, ruleName);
				}
			}
		}
		if (!rule.isInterfaceOK()) return;
		
		// TODO: check caseResult here!

		for (Derivation d : premises) {
			d.typecheck(ctx);
			d.getClause().checkBindings(ctx.bindingTypes, this);
		}

		conclusion.typecheck(ctx);
		conclusion.getClause().checkBindings(ctx.bindingTypes, this);
		ClauseUse concClause = (ClauseUse)conclusion.getClause();
		NonTerminal thisRoot = concClause.getRoot();

		whereClauses.typecheck(ctx);
		
		// make sure we were case-analyzing a derivation, not a nonterminal
		if (ctx.currentCaseAnalysisElement instanceof NonTerminal)
			ErrorHandler.error(Errors.RULE_CASE_SYNTAX, this);
		Term subjectTerm = ctx.currentCaseAnalysisElement.asTerm().substitute(ctx.currentSub);
		Term rcc = conclusion.getClause().asTerm();
		NonTerminal subjectRoot = ctx.getCurrentCaseAnalysisRoot();
		int numPremises = premises.size();
		Term adaptedSubjectTerm = subjectTerm;
		// Technically the conclusion is unsound if we are using
		// the assumption rule and unrolling the context.
		// When     Gamma', x': T, x : T |- x' : T'
		// is used to match Gamma, x : T |- t : T'
		// then this implies T is bound only in Gamma', when it may actually
		// have other dependencies.
		boolean conclusionIsUnsound = false;

		List<Abstraction> addedContext = new ArrayList<Abstraction>();
		Application appliedTerm = rule.checkApplication(ctx, premises, conclusion, addedContext, this, true);
		Term patternConc = Term.wrapWithLambdas(addedContext, appliedTerm.getArguments().get(numPremises));

		// We used to find all free variables NOT bound in the conclusion
		// but it wasn't general enough, so this is handled in the main checker now.

		Relaxation relax = null;

		// Check context changes.  The root and number of "lambdas".
		if (subjectRoot == null) {
			if (thisRoot != null) {
				ErrorHandler.recoverableError(Errors.CASE_CONTEXT_ADDED, thisRoot.toString(), this.getSpan());
			}
			if (subjectTerm.countLambdas() != patternConc.countLambdas()) {
				Util.debug("caseTerm = ", ctx.currentCaseAnalysis, ", applied = ", appliedTerm);
				ErrorHandler.error(Errors.CASE_CONTEXT_CHANGED, this);
			}
		} else if (subjectRoot.equals(thisRoot)) {
			int diff = patternConc.countLambdas() - subjectTerm.countLambdas();
			if (diff != 0) {
				if (rule.isAssumption()) {
					ErrorHandler.error(Errors.REUSED_CONTEXT, this);
				}
				Util.debug("caseTerm = ", ctx.currentCaseAnalysis, ", applied = ", appliedTerm);
				ErrorHandler.error(Errors.CASE_CONTEXT_CHANGED, this);
			}
		} else if (thisRoot == null) {
			ErrorHandler.recoverableError(Errors.CONTEXT_DISCARDED, subjectRoot.toString(), this.getSpan());			
		} else { // neither null, but not equal
			if (!rule.isAssumption()) {
				ErrorHandler.error(Errors.CASE_CONTEXT_CHANGED_EX, subjectRoot.toString(), this);
			}
			int diff = patternConc.countLambdas() - subjectTerm.countLambdas();
			if (diff != rule.isAssumptionSize()) {
				Util.debug("diff = ", diff, "assumption size = ", rule.isAssumptionSize());
				ErrorHandler.error(Errors.CASE_ASSUMPTION_SINGLE, this);
			}

			relax = Relaxation.computeRelaxation(ctx, (ClauseUse)ctx.currentCaseAnalysisElement, subjectRoot,
					subjectTerm, thisRoot, patternConc, rule, this);
			
			subjectTerm = subjectTerm.substitute(ctx.currentSub);
			adaptedSubjectTerm = relax.adapt(subjectTerm);
			Util.debug("In case of assumption rule case at ",getLocation()," relaxation = ",relax);
			Util.debug("subject = ", subjectTerm);
			Util.debug("adapted is ", adaptedSubjectTerm);
			// NB: We just created an adaptedSubjectTerm,
			// which was also done in caseAnalyze.
			// Two ways of doing the same thing in *very* different ways
			// is probably a bad idea.

			conclusionIsUnsound = true; // not always, but safer this way
		}

		// Now create the "unifyingSub"
		Util.debug("Unifying " + patternConc + " ?\n" + adaptedSubjectTerm);
		Set<FreeVar> RCCvars = patternConc.getFreeVariables();
		Substitution unifyingSub = null;
		try {
			// JTB: Changed to use selectUnavoidable to prefer user-written variables.
			unifyingSub = adaptedSubjectTerm.unify(patternConc);
			unifyingSub.selectUnavoidable(RCCvars);
		} catch (EOCUnificationFailed uf) {
			ErrorHandler.error(Errors.CASE_OCCUR, uf.eocTerm.toString(), this);     
		} catch (UnificationIncomplete uf) {
			Term app = uf.term1;
			// try to find a FV[...]
			if (app instanceof Application && !(((Application)app).getFunction() instanceof FreeVar)) {
				app = uf.term2;
			}
			TermPrinter tp = new TermPrinter(ctx, subjectRoot, getLocation(), false);
			String problem = tp.toString(app, false);
			ErrorHandler.error(Errors.CASE_INCOMPLETE, problem, this,
					"SASyLF was trying to unify " + uf.term1 + " and " + uf.term2);
		} catch (UnificationFailed uf) {
			TermPrinter tp = new TermPrinter(ctx,subjectRoot,getLocation(),false);
			Element e1, e2;
			try {
				e1 = tp.asElement(uf.term1);
				e2 = tp.asElement(uf.term2);
			} catch (RuntimeException ex) {
				e1 = ctx.currentCaseAnalysisElement;
				e2 = conclusion.getElement();
			}
			Util.debug(this.getLocation(), ": was unifying ",patternConc, " and ", adaptedSubjectTerm, " with error ", uf);
			ErrorHandler.error(Errors.CASE_MISMATCH, e1 + " =?= " + e2, conclusion, "SASyLF computed the LF term " + adaptedSubjectTerm + " for the conclusion");
		}
		if (adaptedSubjectTerm != subjectTerm) {
			Util.debug("pattern = ",patternConc," adaptedSubject = ",adaptedSubjectTerm);
		}
		Util.debug("  unifyingSub = ",unifyingSub);

		// look up case analysis for this rule
		Set<Pair<Term,Substitution>> caseResult = ctx.caseTermMap.get(rule);
		if (caseResult == null) {
			ErrorHandler.error(Errors.CASE_UNNECESSARY, this, "suggestion: remove it");
		} else if (caseResult.isEmpty()) {
			ErrorHandler.error(Errors.CASE_REDUNDANT, this, "suggestion: remove it");
		}
		
		//Util.debug("caseResult = ",caseResult);

		// find the computed case that matches the given rule
		Term caseTerm = Term.wrapWithLambdas(addedContext,canonRuleApp(appliedTerm));
		Set<FreeVar> caseFree = caseTerm.getFreeVariables();
		Term computedCaseTerm = null;
		Term candidate = null;
		Substitution pairSub = null;
		for (Pair<Term,Substitution> pair : caseResult)
			try {
				boolean generatedError = false;
				pairSub = new Substitution(pair.second);
				//debug("\tpair.first was ", pair.first);
				//debug("\tpair.second was ", pairSub);
				
				// The case creation already SU(ctx.inputVars),
				// but we need to SU(RCCvars) because we want to bias the 
				// substitution to the variables the user chose, not SASyLF
				// in case there is freedom.
				Set<FreeVar> fishy = pairSub.selectUnavoidable(RCCvars);
				// fishy variables indicate that at least a warning should be generated and perhaps an error
				candidate = pair.first.substitute(pairSub); // reorganized and so must re-sub.
					
				//Util.tdebug("case analysis: does ", caseTerm, " generalize ", candidate);
				//Util.tdebug("\tpair.second is now ", pairSub);

				Set<FreeVar> candidateFree = candidate.getFreeVariables();
				//tdebug("case free = " + caseFree);
				//tdebug("candidate free = " + candidateFree);
				
				// for correctness: we first apply the current substitution and then pairSub
				// to the case term.  If everything works, we will warn if these substitutions had any effect.

				Term cleanedCaseTerm = caseTerm.substitute(ctx.currentSub).substitute(pairSub);
				// Util.debug("cleaned case term = " + cleanedCaseTerm);
				Substitution computedSub = cleanedCaseTerm.unify(candidate);
				
				// tdebug("computedSub = " + computedSub);
				Set<FreeVar> problems = computedSub.selectUnavoidable(candidateFree);
				if (!problems.isEmpty()) {
					final TermPrinter tp = new TermPrinter(ctx, subjectRoot, getLocation(),false);
					Util.debug("Candidate = ", candidate);
					Util.debug("caseTerm = ", caseTerm);
					Util.debug("cleaned caseTerm = ", cleanedCaseTerm);
					Util.debug("computedSub = ", computedSub);
					Util.debug("problems = ", problems);
					Util.debug("fishy = " + fishy);
					Errors errorClass = null;
					String explanation = null;
					Set<FreeVar> userFree = cleanedCaseTerm.getFreeVariables();
					computedSub.avoid(userFree);
					for (FreeVar v : problems) {
						if (!v.isGenerated()) {
							errorClass = Errors.CASE_STRICT_RESTRICTS;
							explanation = v.toString();
							break;
						}
					}
					if (errorClass == null) {
						Util.debug("problems: ", problems, " in ", computedSub, " after ", pairSub);
						FreeVar first = problems.iterator().next();
						Term subbed = computedSub.getSubstituted(first);
						Term baseSubbed = Term.getWrappingAbstractions(subbed, null);
						if (subbed == baseSubbed) {
							errorClass = Errors.CASE_STRICT_NEED_VAR;
							explanation = tp.toString(subbed,false);
						} else {
							Element elem = tp.asElement(subbed);
							if (elem instanceof AssumptionElement) elem = ((AssumptionElement)elem).getBase();
							errorClass = Errors.CASE_STRICT_NEED_DEPEND;
							explanation = elem.toString();
						}
					}
					Substitution restricted = new Substitution(computedSub);
					restricted.retainAll(problems);
					ErrorHandler.recoverableError(errorClass, explanation, this.getSpan(),
							"SASyLF computes that " + problems + " should not be substituted in " + restricted);
					generatedError = true;
				} else {
					// now check that all fresh variables are actually new
					// See: bad36, bad54
					for (Atom v : computedSub.getMap().keySet()) {
						if (ctx.inputVars.contains(v)) {
							Util.debug("pair.fst = ", pair.first, ",pairSub = ", pairSub, ", cleanedCaseTerm = ", cleanedCaseTerm, ", computedSub = ",computedSub);
							ErrorHandler.recoverableError(Errors.CASE_STRICT_NEED_VAR, v.toString(), this.getSpan());
							generatedError = true;
							break;
						}
						if (ctx.derivationMap.containsKey(v.toString())) {
							ErrorHandler.warning(Errors.DERIVATION_NAME_REUSED, ": " + v, this.getSpan());
						}
					}
				}
				
				// If no errors so far, see what warnings should be generated:
				if (!generatedError) {
					Set<FreeVar> genVars = computedSub.selectUnavoidable(caseFree);
					if (!fishy.isEmpty()) {
						FreeVar first = fishy.iterator().next();
						Term result = pairSub.getSubstituted(first);
						ErrorHandler.warning(Errors.RULE_CASE_TOO_GENERAL, "" + fishy, this.getSpan(),
								"SASyLF computes the first restriction as " + first + " -> " + result);
					} else if (!genVars.isEmpty()) {
						ErrorHandler.warning(Errors.RULE_CASE_TOO_GENERAL, genVars.toString(), this.getSpan(),
								"SASyLF computes the first restriction as " + computedSub.getSubstituted(genVars.iterator().next()));
					}
				}
				
				computedCaseTerm = candidate;
				verify(caseResult.remove(pair), "internal invariant broken");

				break;
			} catch (UnificationIncomplete e) {
				ErrorHandler.error(Errors.CASE_UNIFICATION_INCOMPLETE, this,
						"(was checking " + candidate + " instance of " + caseTerm + ",\n got exception " + e);      
				return; // tell Java we're gone.

			} catch (UnificationFailed uf) {
				Util.debug("candidate ", candidate, " is not an instance of ", caseTerm);
				//uf.printStackTrace();
				continue;
			}/* catch (RuntimeException rt) {
				rt.printStackTrace();
				System.out.println(this.getLocation() + ": was unifying " + caseTerm + " and " + candidate);
			}*/

		if (computedCaseTerm == null) {
			// There must have been a candidate, but it didn't unify or wasn't an instance
			// It is redundant because it matches the subject, but no case is needed for it.
			ErrorHandler.error(Errors.CASE_REDUNDANT, this, "SASyLF considered the LF term " + candidate + " for " + caseTerm);
		}

		//Util.debug("unifyingSub: ", unifyingSub);
		//Util.debug("pairSub: ", pairSub);

		// can't be done before because it would interfere with checking tests.
		if (relax != null) {
			ctx.addRelaxation(thisRoot, relax);
		}

		// update the current substitution
		ctx.composeSub(unifyingSub);

		for (Derivation d : premises) { 
			d.addToDerivationMap(ctx);
		}
		if (conclusionIsUnsound) {
			if (!conclusion.getName().equals("_")) {
				ErrorHandler.warning(Errors.CASE_CONCLUSION_UNSOUND, conclusion.getName(), conclusion, null);
			}
		} else {
			conclusion.addToDerivationMap(ctx);
		}

		// tdebug("current sub = " + ctx.currentSub);

		// update the set of subderivations
		if (isSubderivation != null) {
			Pair<Fact,Integer> newSub = new Pair<Fact,Integer>(isSubderivation.first,isSubderivation.second+1);
			// add each premise to the list of subderivations
			for (Fact f : premises) {
				ctx.subderivations.put(f, newSub);
			}
		}

		// verify user-written where clauses
		whereClauses.checkWhereClauses(ctx, adaptedSubjectTerm, rcc, null, conclusion);

		super.typecheck(ctx, isSubderivation);
	}
	
	/**
	 * We take a rule application and if (as is the case with the assumption rule)
	 * the conclusion has a context, we move that context outside.  This is because
	 * The syntax for an assumption rule doesn't reflect how it can be pattern matched:
	 * <pre>
	 *     Gamma, x:T |- x:T
	 * </pre>
	 * can be matched by
	 * <pre>
	 *     Gamma, x:T, x':T' |- x:T
	 * </pre>
	 * So we need the whole context to be outside of the application.    
	 * @param app
	 * @return
	 */
	private Term canonRuleApp(Application app) {
		if (app.getArguments().size() == 1 && app.getArguments().get(0) instanceof Abstraction) {
			List<Abstraction> abs = new ArrayList<Abstraction>();
			Term bare = Term.getWrappingAbstractions(app.getArguments().get(0), abs);
			app = Facade.App(app.getFunction(), bare);
			return Term.wrapWithLambdas(abs, app);
		}
		return app;
	}
	
	@Override
	public void collectQualNames(Consumer<QualName> consumer) {
		ruleName.visit(consumer);
		super.collectQualNames(consumer);
	}
	
	
}

