package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Errors.INVALID_CASE;
import static edu.cmu.cs.sasylf.util.Util.debug;
import static edu.cmu.cs.sasylf.util.Util.verify;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Atom;
import edu.cmu.cs.sasylf.term.Constant;
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
	private final String ruleName;
	private final List<Derivation> premises;
	private final Derivation conclusion;
	private final WhereClause whereClauses;
	
	public RuleCase(Location l, Location l1, Location l2,
			String rn, List<Derivation> ps, Derivation c, WhereClause wcs) {
		super(l, l1, l2);
		conclusion = c;
		premises = ps;
		ruleName = rn;
		whereClauses = wcs;
	}
	public String getRuleName() { return ruleName; }
	public Rule getRule() { return rule; }
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
				ErrorHandler.report("It doesn't appear that a rule case makes sense in this context", this, "SASyLF computes the case analysis is on " + ctx.currentCaseAnalysis);
			}
			for (Rule r : judg.getRules()) {
				if (r.getName().equals(ruleName)) rule = r;
			}
			if (rule == null) {
				ErrorHandler.report(Errors.RULE_NOT_FOUND, ruleName, this);
			}
		}
		if (!rule.isInterfaceOK()) return;

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
			ErrorHandler.report(Errors.RULE_CASE_SYNTAX, this);
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
				ErrorHandler.report("Case should not use named context " + thisRoot, this);
			}
			if (subjectTerm.countLambdas() != patternConc.countLambdas()) {
				Util.debug("caseTerm = ", ctx.currentCaseAnalysis, ", applied = ", appliedTerm);
				ErrorHandler.report("Should not add to context in case analysis",this);
			}
		} else if (subjectRoot.equals(thisRoot)) {
			if (subjectTerm.countLambdas() != patternConc.countLambdas()) {
				Util.debug("caseTerm = ", ctx.currentCaseAnalysis, ", applied = ", appliedTerm);
				ErrorHandler.report("Should not add to context in case analysis",this);
			}
		} else {
			if (thisRoot == null) {
				ErrorHandler.report(Errors.CONTEXT_DISCARDED,this);
			}
			if (!rule.isAssumption()) {
				ErrorHandler.report("Only assumption rules can change context: " + subjectRoot + " -> " + thisRoot, this);
			}
			int diff = patternConc.countLambdas() - subjectTerm.countLambdas();
			if (diff != rule.isAssumptionSize()) {
				Util.debug("diff = ", diff, "assumption size = ", rule.isAssumptionSize());
				ErrorHandler.report("assumption rule should introduce exactly one level of context",this);
			}
			
			// TODO: At this point I'd like to grab the Element that represents the
			// syntax of the new context, the "G', x : T" that can be relaxed to "G".
			ClauseUse container = concClause.getAssumesContaining(thisRoot);
			Util.debug("  relaxation source: " + container);
			verify(container != null,"no use of " + thisRoot + " in " + concClause + "?");


			// we need to make sure the subject pattern has a simple variable where
			// we are going to have a variable because we need this for the relaxation.
			List<FreeVar> relaxVars = new ArrayList<FreeVar>();
			// the following is messy and should be extracted.
			{
				Application bareSubject = (Application)Term.getWrappingAbstractions(subjectTerm, null);
				int j=0;
				ClauseUse ruleConc = (ClauseUse)rule.getConclusion();
				int n = ruleConc.getElements().size();
				int ai = ((ClauseDef)rule.getJudgment().getForm()).getAssumeIndex();
				Substitution canonSub = null;
				for (int i=0; i < n; ++i) {
					if (i == ai) continue;
					Element e = ruleConc.getElements().get(i);
					if (e instanceof Variable) {
						Term t = bareSubject.getArguments().get(j);
						if (t instanceof FreeVar) {
							relaxVars.add((FreeVar)t);
						} else if (!(t instanceof Application) || !(((Application)t).getFunction() instanceof FreeVar)) {
							ErrorHandler.report("Rule " + rule.getName() + " cannot apply since "+ 
									((ClauseUse)ctx.currentCaseAnalysisElement).getElements().get(i) + " cannot be a variable.", this);
						} else {
							Application app = (Application)t;
							FreeVar funcVar = (FreeVar)app.getFunction();
							List<Abstraction> argTypes = new ArrayList<Abstraction>();
							Constant baseType = (Constant)Term.getWrappingAbstractions(funcVar.getType(), argTypes);
							FreeVar newVar = FreeVar.fresh(baseType.toString(),baseType);
							relaxVars.add(newVar);
							if (canonSub == null) canonSub = new Substitution();
							canonSub.add(funcVar, Term.wrapWithLambdas(argTypes, newVar));
						}
						++j;
					} else if (e instanceof NonTerminal) {
						++j;
					}
				}
				if (canonSub != null) {
					Util.debug("Found canonSub = ",canonSub);
					subjectTerm = subjectTerm.substitute(canonSub);
					ctx.composeSub(canonSub);
					canonSub = null;
				}
				relaxVars.add(null); // for the assumption itself
			}
			
			Relaxation former = ctx.relaxationMap == null ? null : ctx.relaxationMap.get(thisRoot);
			if (former != null) {
				if (!former.getValues().equals(relaxVars)) {
					ErrorHandler.report("Context " + thisRoot + " already in use for analyzing " + former.getRelaxationVars(), this);
				}
				// System.out.println("Former Relaxation is " + former);
				adaptedSubjectTerm = former.adapt(subjectTerm);
			} else if (ctx.isKnownContext(thisRoot)) {
				ErrorHandler.report("Context already in use: " + thisRoot, this);
			} else {
				List<Abstraction> newWrappers = new ArrayList<Abstraction>();
				Term.getWrappingAbstractions(patternConc, newWrappers, diff);
				Util.debug("Introducing ",thisRoot,"+",Term.wrappingAbstractionsToString(newWrappers));
				// set up relaxation info
				relax = new Relaxation(container,newWrappers,relaxVars,subjectRoot);
				// System.out.println("Relaxation is " + relax);
				adaptedSubjectTerm = relax.adapt(subjectTerm);
				if (ctx.relaxationMap != null) {
					// maybe we SHOULD have used an existing context!
					for (Map.Entry<NonTerminal, Relaxation> e : ctx.relaxationMap.entrySet()) {
						Relaxation r = e.getValue();
						if (r.getResult().equals(relax.getResult()) &&
								r.getRelaxationVars().equals(relax.getRelaxationVars())) {
							ErrorHandler.warning("Perhaps context " + e.getKey() + " should have been used instead of " + thisRoot, this);
							break;
						}
					}
				}
			}
			
			Util.debug("subject = ", subjectTerm);
			Util.debug("adapted is ", adaptedSubjectTerm);

			conclusionIsUnsound = true; // not always, but safer this way
		}

		// Now create the "unifyingSub"
		// tdebug("Unifying " + patternConc + " " + adaptedSubjectTerm);
		Set<FreeVar> RCCvars = patternConc.getFreeVariables();
		Substitution unifyingSub = null;
		try {
			// JTB: Changed to use selectUnavoidable to prefer user-written variables.
			unifyingSub = adaptedSubjectTerm.unify(patternConc);
			unifyingSub.selectUnavoidable(RCCvars);
		} catch (EOCUnificationFailed uf) {
			ErrorHandler.report(INVALID_CASE, "Case " + conclusion.getElement() + " is not actually a case of " + ctx.currentCaseAnalysisElement
					+ "\n    Did you re-use a variable (perhaps " + uf.eocTerm + ") which was already in scope?  If so, try using some other variable name in this case.", this);     
		} catch (UnificationIncomplete uf) {
			ErrorHandler.report(INVALID_CASE, "Case too complex for SASyLF to check; consider sending this example to the maintainers", this,
					"SASyLF was trying to unify " + uf.term1 + " and " + uf.term2);
		} catch (UnificationFailed uf) {
			//uf.printStackTrace();
			debug(this.getLocation(), ": was unifying ",patternConc, " and ", adaptedSubjectTerm);
			ErrorHandler.report(INVALID_CASE, "Case " + conclusion.getElement() + " is not actually a case of " + ctx.currentCaseAnalysisElement, this, "SASyLF computed the LF term " + adaptedSubjectTerm + " for the conclusion");
		}
		if (adaptedSubjectTerm != subjectTerm) {
			Util.debug("pattern = ",patternConc," adaptedSubject = ",adaptedSubjectTerm);
		}
		//Util.debug("  unifyingSub = ",unifyingSub);

		// look up case analysis for this rule
		Set<Pair<Term,Substitution>> caseResult = ctx.caseTermMap.get(rule);
		if (caseResult == null)
			ErrorHandler.report(Errors.EXTRA_CASE, ": rule " + ruleName + " cannot be used to derive " + ctx.currentCaseAnalysisElement, this, "suggestion: remove it");
		if (caseResult.isEmpty())
			ErrorHandler.report(Errors.EXTRA_CASE, this,"suggestion: remove it");

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
					Util.debug("Candidate = ", candidate);
					Util.debug("caseTerm = ", caseTerm);
					Util.debug("cleaned caseTerm = ", cleanedCaseTerm);
					Util.debug("computedSub = ", computedSub);
					Util.debug("problems = ", problems);
					Util.debug("fishy = " + fishy);
					String explanation = null;
					for (FreeVar v : problems) {
						Term subbed = computedSub.getSubstituted(v);
						Term baseSubbed = Term.getWrappingAbstractions(subbed, null);
						Util.debug("  binding ",v," to ",subbed);
						if (explanation == null && !(v.isGenerated())) {
							if (subbed == baseSubbed) {
								explanation = "Perhaps it constrains " + v + " to be a specific form.";
								// explanation = "Perhaps it uses " + baseSubbed + " where it should use another variable.";
							} else {
								explanation = "Perhaps " + baseSubbed + " should be replaced with something that could depend on the variable(s) in the context.";
							}
						}
					}
					if (explanation == null) {
						// tdebug("problems: " + problems + " in " + computedSub + " after " + pairSub);
						FreeVar first = problems.iterator().next();
						Term subbed = computedSub.getSubstituted(first);
						Term baseSubbed = Term.getWrappingAbstractions(subbed, null);
						if (subbed == baseSubbed) {
							explanation = "Perhaps " + TermPrinter.toString(ctx, subjectRoot, getLocation(), subbed, false) + " should be replaced with a new variable.";
						} else if (!fishy.isEmpty()) {
							explanation = "Perhaps " + fishy.iterator().next() + " should be replaced with something that could depend on the variable(s) in the context.";
						} else {
							explanation = "Perhaps " + baseSubbed + " should be replaced with something that could depend on the variable(s) in the context.";
						}
					}
					// if we ever decide to have mutually compatible patterns
					// without a MGU, we will need to change this error into something
					// more sophisticated
					ErrorHandler.recoverableError(INVALID_CASE, "Case is overly strict. " + explanation, this.getSpan(),
							"SASyLF computes that " + problems.iterator().next() + " needs to be " + computedSub.getSubstituted(problems.iterator().next()));
					generatedError = true;
				} else {
					// now check that all fresh variables are actually new
					// See: bad36, bad54
					String errorString = null;
					for (Atom v : computedSub.getMap().keySet()) {
						if (ctx.inputVars.contains(v)) {
							errorString = "Case reuses " + v + " when it should use a new variable";
							break;
						}
						if (ctx.derivationMap.containsKey(v.toString())) {
							ErrorHandler.warning("Reusing derivation name as a nonterminal: " + v, this.getSpan());
						}
					}
					if (errorString != null) {
						ErrorHandler.recoverableError(INVALID_CASE,  errorString, this.getSpan());
						generatedError = true;
					}
				}
				
				// If no errors so far, see what warnings should be generated:
				if (!generatedError) {
					Set<FreeVar> genVars = computedSub.selectUnavoidable(caseFree);
					Set<FreeVar> subVars = new HashSet<FreeVar>(caseFree);
					subVars.retainAll(ctx.currentSub.getMap().keySet());
					if (!fishy.isEmpty()) {
						FreeVar first = fishy.iterator().next();
						Term result = pairSub.getSubstituted(first);
						ErrorHandler.warning("The given pattern is overly general, should restrict " + fishy, this.getSpan(),
								"SASyLF computes the first restriction as " + first + " -> " + result);
					} else if (!genVars.isEmpty()) {
						ErrorHandler.warning("The given pattern is overly general, should restrict " + genVars, this.getSpan(),
								"SASyLF computes the first restriction as " + computedSub.getSubstituted(genVars.iterator().next()));
					} else if (!subVars.isEmpty()) {
						ErrorHandler.warning("The given pattern uses variables already substituted: " + subVars, this.getSpan());
					}
				}
				
				computedCaseTerm = candidate;
				verify(caseResult.remove(pair), "internal invariant broken");

				break;
			} catch (UnificationIncomplete e) {
				String extraInfo = "\n\tcouldn't unify " + e.term1 + " and " + e.term2;
				ErrorHandler.report(Errors.INVALID_CASE, "SASyLF ran into incompleteness of unification while checking this rule" + extraInfo, this,
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
			// there must have been a candidate, but it didn't unify or wasn't an instance
			String errorDescription = rule.getErrorDescription(candidate, ctx);
			Util.debug("Expected case:\n", errorDescription);
			Util.debug("Your case roundtripped:\n", rule.getErrorDescription(caseTerm, ctx));
			Util.debug("SASyLF generated the LF term: ", candidate);
			Util.debug("You proposed the LF term: ", caseTerm);
			ErrorHandler.report(INVALID_CASE, "The rule case given is invalid; it is most likely too specific in some way and should be generalized", this, "SASyLF considered the LF term " + candidate + " for " + caseTerm);
			// TODO: explain WHY!!!
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
				ErrorHandler.warning("Conclusion in pattern matching of assumption rule cannot be used", conclusion);
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
}

