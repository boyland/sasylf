package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Errors.INVALID_CASE;
import static edu.cmu.cs.sasylf.util.Util.debug;
import static edu.cmu.cs.sasylf.util.Util.verify;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
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
import edu.cmu.cs.sasylf.util.Pair;
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
		
		RuleLike x = ctx.ruleMap.get(ruleName);
		if (x == null) {
		  ErrorHandler.report(Errors.RULE_NOT_FOUND, ruleName, this);
		} else if (!(x instanceof Rule)) {
		  ErrorHandler.report(Errors.THEOREM_NOT_RULE, ruleName, this);
		}
		rule = (Rule)x;
		if (!rule.isInterfaceOK()) return;
		
		for (Derivation d : premises) {
			d.typecheck(ctx);
			d.getClause().checkBindings(ctx.bindingTypes, this);
		}

		conclusion.typecheck(ctx);
		conclusion.getClause().checkBindings(ctx.bindingTypes, this);
		ClauseUse concClause = (ClauseUse)conclusion.getClause();
    NonTerminal thisRoot = concClause.getRoot();
		
    // make sure we were case-analyzing a derivation, not a nonterminal
    if (ctx.currentCaseAnalysisElement instanceof NonTerminal)
      ErrorHandler.report(Errors.RULE_CASE_SYNTAX, this);
    Term subjectTerm = ctx.currentCaseAnalysisElement.asTerm().substitute(ctx.currentSub);
    NonTerminal subjectRoot = ctx.currentCaseAnalysisElement.getRoot();
    int numPremises = premises.size();
    Substitution adaptSub = new Substitution();
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

		// find all free variables NOT bound in the conclusion:
		Set<FreeVar> newVars = new HashSet<FreeVar>();
		for (int i=0; i < numPremises; ++i) {
		  newVars.addAll(appliedTerm.getArguments().get(i).getFreeVariables());
		}
		newVars.removeAll(appliedTerm.getArguments().get(numPremises).getFreeVariables());
		// and then make sure that they are not already in use:
		for (FreeVar v : newVars) {
		  if (ctx.currentSub.getSubstituted(v) != null || ctx.inputVars.contains(v)) {
		    ErrorHandler.report(Errors.INVALID_CASE, "Case should not reuse binding for " + v, this);
		  }
		  if (ctx.derivationMap.containsKey(v.toString())) {
		    ErrorHandler.warning("Reusing derivation name as a nonterminal: " + v, this);
		  }
		}
		
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
		  if (ctx.isKnownContext(thisRoot)) {
		    ErrorHandler.report("Context already in use: " + thisRoot, this);
		  }
		  int diff = patternConc.countLambdas() - subjectTerm.countLambdas();
		  if (diff != rule.isAssumptionSize()) {
		    Util.debug("diff = ", diff, "assumption size = ", rule.isAssumptionSize());
		    ErrorHandler.report("assumption rule should introduce exactly one level of context",this);
		  }
		  
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
		    }
		    relaxVars.add(null); // for the assumption itself
		  }
		  
		  List<Abstraction> newWrappers = new ArrayList<Abstraction>();
		  Term.getWrappingAbstractions(patternConc, newWrappers, diff);
		  Util.debug("Introducing ",thisRoot,"+",Term.wrappingAbstractionsToString(newWrappers));
		  
		  adaptedSubjectTerm = ClauseUse.wrapWithOuterLambdas(subjectTerm, patternConc, diff, adaptSub);
		  Util.debug("subject = ", subjectTerm)
;     Util.debug("adapted is ", adaptedSubjectTerm);
		  Util.debug("adaptSub = ", adaptSub);
		  
		  // set up relaxation info
		  relax = new Relaxation(newWrappers,relaxVars,subjectRoot);
		  conclusionIsUnsound = true; // not always, but safer this way
		}

		// Now create the "unifyingSub"
		Substitution unifyingSub = null;
		try {
		  unifyingSub = patternConc.unify(adaptedSubjectTerm);
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
		  Util.debug("sub = ",unifyingSub);
		}
		
		// look up case analysis for this rule
		Set<Pair<Term,Substitution>> caseResult = ctx.caseTermMap.get(rule);
		if (caseResult == null)
			ErrorHandler.report(Errors.EXTRA_CASE, ": rule " + ruleName + " cannot be used to derive " + ctx.currentCaseAnalysisElement, this, "suggestion: remove it");
		if (caseResult.isEmpty())
			ErrorHandler.report(Errors.EXTRA_CASE, this,"suggestion: remove it");

		Util.debug("caseResult = ",caseResult);
		
		// find the computed case that matches the given rule
		Term caseTerm = Term.wrapWithLambdas(addedContext,canonRuleApp(appliedTerm));
		Term computedCaseTerm = null;
		Term candidate = null;
		Substitution pairSub = null;
		for (Pair<Term,Substitution> pair : caseResult)
		  try {
				pairSub = new Substitution(pair.second);
				debug("\tpair.second was ", pairSub);
				// Set<FreeVar> boundInputVars = pairSub.selectUnavoidable(ctx.inputVars);
				candidate = pair.first; // .substitute(pairSub); // reorganized and so much re-sub.

				Util.debug("case analysis: does ", caseTerm, " generalize ", candidate);
				Util.debug("\tpair.second is now ", pairSub);

				Set<FreeVar> patternFree = candidate.getFreeVariables();
				//  JTB: I'm not sure why we did all this...
				//Set<FreeVar> inputVars = new HashSet<FreeVar>(ctx.inputVars);
				//inputVars.removeAll(boundInputVars);
				//Util.debug("removing ",boundInputVars);
				//patternFree.addAll(inputVars);
				Util.debug("patternFree = ",patternFree);
				
				Substitution computedSub = caseTerm.unify(candidate);
				Set<FreeVar> problems = computedSub.selectUnavoidable(patternFree);
				if (!problems.isEmpty()) {
				  Util.debug("Candidate = ", candidate);
				  Util.debug("caseTerm = ", caseTerm);
				  Util.debug("computedSUb = ", computedSub);
				  Util.debug("patternFree = ", patternFree);
				  // if we ever decide to have mutually compatible patterns
				  // without a MGU, we will need to change this error into something
				  // more sophisticated
				  ErrorHandler.recoverableError(INVALID_CASE, "Case " + conclusion.getElement() + " is not actually a case of " + ctx.currentCaseAnalysisElement
				      + "\n    The case given requires instantiating the following variable(s) that should be free: " + problems, this,
				      "SASyLF computes that " + problems.iterator().next() + " needs to be " + computedSub.getSubstituted(problems.iterator().next()));
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
		
		
		Util.debug("unifyingSub: ", unifyingSub);
		Util.debug("pairSub: ", pairSub);

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
    
    // check whether the pattern is overly general requires that we first
    // compose the pairSub with the unifyingSub, which sometimes can fail
    // with a non-pattern substitution.  I probably could figure out how to
    // see how to avoid this problem, but it's easier simply to give up trying
    // to check the warning
    
    try {
      pairSub.compose(unifyingSub);
      Util.debug("Unifed pairSub = ", pairSub);
      Set<FreeVar> overlyGeneral = pairSub.selectUnavoidable(ctx.inputVars);
      overlyGeneral.removeAll(adaptSub.getMap().keySet());
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
	
	private Derivation conclusion;
	private List<Derivation> premises;
	private String ruleName;
	private Rule rule;
}

