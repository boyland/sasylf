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

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Atom;
import edu.cmu.cs.sasylf.term.BoundVar;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.Util;


public abstract class DerivationByAnalysis extends DerivationWithArgs {
	public DerivationByAnalysis(String n, Location l, Clause c, String derivName) {
		super(n,l,c); 
		Clause cl = new Clause(l);
		cl.getElements().add(new NonTerminal(derivName,l));
		super.getArgStrings().add(cl);
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

	public void prettyPrint(PrintWriter out) {
		super.prettyPrint(out);
		for (Case c: cases) {
			c.prettyPrint(out);
		}
		out.println("end " + byPhrase());
	}

	public void typecheck(Context ctx) {
		super.typecheck(ctx);
    computeTargetDerivation(ctx);

		Term oldCase = ctx.currentCaseAnalysis;
		Element oldElement = ctx.currentCaseAnalysisElement;
		Term oldGoal = ctx.currentGoal;
		Clause oldGoalClause = ctx.currentGoalClause;
		Map<CanBeCase,Set<Pair<Term,Substitution>>> oldCaseTermMap = ctx.caseTermMap;
		
		ClauseType caseType = (ClauseType)targetDerivation.getElement().getType();
		if (caseType.isAbstract()) {
		  if (caseType instanceof NotJudgment) {
		    ErrorHandler.report("Cannot perform case analysis on 'not' judgments",this);
		  } else {
		    ErrorHandler.report("Cannot perform case analysis on parameter structures", this);
		  }
		}
		
		try {
		ctx.currentCaseAnalysis = adapt(targetDerivation.getElement().asTerm(), targetDerivation.getElement(), ctx, true);
		ctx.currentCaseAnalysis = targetDerivation.getElement().asTerm().substitute(ctx.currentSub);
    debug("setting current case analysis to ", ctx.currentCaseAnalysis);
		ctx.currentCaseAnalysisElement = targetDerivation.getElement();
		ctx.currentGoal = getElement().asTerm().substitute(ctx.currentSub);
		ctx.currentGoalClause = getClause();
		
		Pair<Fact,Integer> isSubderivation = ctx.subderivations.get(targetDerivation);
		if (isSubderivation != null) debug("found subderivation: ", targetDerivation);
		
		ctx.caseTermMap = new LinkedHashMap<CanBeCase,Set<Pair<Term,Substitution>>>();
		Map<CanBeCase,Set<Pair<Term,Substitution>>> savedMap = null;
		if (ctx.savedCaseMap != null) savedMap = ctx.savedCaseMap.get(targetDerivation.getName());
		
		// JTB: There are unfortunately many ways to be a syntax case:
		// 1. a nonterminal: e.g. t
		// 2. an assumption: e.g. t assumes Gamma
		// 3. an assumption with a binding: e.g. t[x] assumes (Gamma, x:tau)
		NonTerminal caseNT = null;
		if (ctx.currentCaseAnalysisElement instanceof NonTerminal) {
		  caseNT = (NonTerminal)ctx.currentCaseAnalysisElement;
		} else if (ctx.currentCaseAnalysisElement instanceof AssumptionElement) {
		  // JTB: TODO: Figure out how to avoid the duplicate logic from here and in the next section.
		  AssumptionElement ae = (AssumptionElement)ctx.currentCaseAnalysisElement;
      Element e = ae.getBase();
      if (e instanceof Binding) caseNT = ((Binding)e).getNonTerminal();
      else caseNT = (NonTerminal)e;
		  // now we need to check that the assumptions INCLUDE the current context, if it could influence the NT
      if (ctx.innermostGamma != null &&
          ctx.innermostGamma.getType().canAppearIn(caseNT.getType().typeTerm()) &&
          !ctx.isVarFree(caseNT)) {
        NonTerminal root = ae.getRoot();
        if (root == null || !root.equals(ctx.innermostGamma)) {
          ErrorHandler.report("Case analysis target cannot assume less than context does",this);
        }
      }
		}

		if (savedMap != null) {
		  for (Map.Entry<CanBeCase, Set<Pair<Term,Substitution>>> e : savedMap.entrySet()) {
		    HashSet<Pair<Term, Substitution>> newSet = new HashSet<Pair<Term,Substitution>>();
		    for (Pair<Term,Substitution> p : e.getValue()) {
		      Pair<Term,Substitution> newPair;
		      try {
		        Util.debug("term = ", p.first);
		        Util.debug("sub = ", p.second);
		        Util.debug("current = ", ctx.currentSub);
		        Substitution newSubstitution = new Substitution(p.second);
		        newSubstitution.compose(ctx.currentSub);
		        if (caseNT != null) {
		          FreeVar v = new FreeVar(caseNT.getSymbol(),p.first.getType(new ArrayList<Pair<String,Term>>()));
		          newSubstitution.add(v,p.first);
		        }
		        Util.debug("newSub = ", newSubstitution);
		        if (caseNT != null) newPair = p;
		        else newPair = new Pair<Term,Substitution>(p.first.substitute(newSubstitution),newSubstitution);
		      } catch (UnificationFailed ex) {
		        Util.debug("case no longer feasible.");
		        continue;
		      }
		      newSet.add(newPair);
		    }
		    ctx.caseTermMap.put(e.getKey(), newSet);
		  }
		} else if (caseNT != null) {
			Syntax syntax = caseNT.getType();
			// JTB: TODO: We need to change this check; it's obsolete
			if (!(ctx.currentCaseAnalysis instanceof FreeVar))
				ErrorHandler.report(VAR_STRUCTURE_KNOWN, "The structure of variable " + ctx.currentCaseAnalysisElement + " is already known and so case analysis is unnecessary (and not currently supported by SASyLF)", this);			

			for (Clause clause : syntax.getClauses()) {
			  Set<Pair<Term,Substitution>> set = new HashSet<Pair<Term,Substitution>>();
        ctx.caseTermMap.put(clause, set);
        // and below, we add to the set, as appropriate
        if (clause.isVarOnlyClause()) {
          List<Pair<String,Term>> varBindings = new ArrayList<Pair<String,Term>>();
          NonTerminal root = null;
				  if (ctx.currentCaseAnalysisElement instanceof AssumptionElement) {
				    AssumptionElement ae = (AssumptionElement)ctx.currentCaseAnalysisElement;
				    root = ae.getRoot();
				  } else {
				    // no context, no problem
				    if (ctx.innermostGamma == null) continue;
				    // not dependent on context, no problem
				    if (!ctx.innermostGamma.getType().canAppearIn(syntax.typeTerm())) continue;
				    // if known to be variable free, no problem
				    if (ctx.isVarFree(caseNT)) continue;
				    root = ctx.innermostGamma;
				  }
				  Util.debug("Adding variable cases for ", syntax, " with root = ", root);
				  // XXX: The following loop is dead code: varBindings is still empty!
				  for (Pair<String,Term> pair : varBindings) {
				    if (pair.second.equals(syntax.typeTerm())) {
				      Util.debug("  for ", pair.first);
				      Variable gen = new Variable(pair.first,getLocation());
				      gen.setType(syntax);
				      Term caseTerm = ClauseUse.newWrap(gen.computeTerm(varBindings),varBindings,0);
				      Util.debug("  generated ", caseTerm);
				      set.add(new Pair<Term,Substitution>(caseTerm,new Substitution()));
				    }
				  }
				  if (root != null) {
				    for (Clause c : root.getType().getClauses()) {
				      for (Element e : c.getElements()) {
				        if (e instanceof Variable && ((Variable)e).getType() == syntax) {
				          List<Element> copied = new ArrayList<Element>();
				          int i=0;
				          for (Element e1 : c.getElements()) {
				            if (e1 instanceof NonTerminal) {
				              NonTerminal nt = (NonTerminal)e1;
                      NonTerminal copy = new NonTerminal(nt.getSymbol()+"_"+i, getLocation());
				              copy.setType(nt.getType());
				              copied.add(copy);
				            } else if (e1 instanceof Variable) {
				              Variable v = (Variable)e;
				              Variable copy = new Variable(v.getSymbol()+"_"+i, getLocation());
				              copy.setType(v.getType());
				              copied.add(copy);
				            } else {
				              copied.add(e1);
				            }
				            ++i;
				          }
				          ClauseUse fakeUse = new ClauseUse(getLocation(),copied,(ClauseDef)c);
				          List<Pair<String,Term>> moreBindings = new ArrayList<Pair<String,Term>>();
				          fakeUse.readAssumptions(moreBindings, false);
				          Util.debug("  for ", moreBindings.get(0));
				          // XXX: The following is misleading: varBindings is still empty
				          Term caseTerm = ClauseUse.newWrap(new BoundVar(varBindings.size()+1),varBindings,0);
				          caseTerm = ClauseUse.newDoBindWrap(caseTerm, moreBindings);
				          Util.debug("  generated ", caseTerm);
		              set.add(new Pair<Term,Substitution>(caseTerm,new Substitution()));
				          break;
				        }
				      }
				    }
				  }
				  // Detect bug3.slf
				  //ErrorHandler.recoverableError("A variable is a possible case, but (currently) SASyLF has no way to case variables.",this);
				  continue;
				}
				//System.out.println("clause : " + clause);
				Term term = ((ClauseDef)clause).getSampleTerm();
				//System.out.println("  sample term: " + term);
				Substitution freshSub = term.freshSubstitution(new Substitution());
				term.substitute(freshSub);
        //System.out.println("  sample term after freshification: " + term);
				set.add(new Pair<Term,Substitution>(term, new Substitution()));
			}
		} else {
      debug("*********** case analyzing line ", getLocation().getLine());
      // tdebug("    currentCaseAnalysisElement = " + ctx.currentCaseAnalysisElement);
      // tdebug("    sub = " + ctx.currentSub);
      // tdebug("    adaptationSub = " + ctx.adaptationSub);
			Judgment judge= (Judgment) ((ClauseUse)ctx.currentCaseAnalysisElement).getConstructor().getType();
			// see if each rule, in turn, applies
			for (Rule rule : judge.getRules()) {
			  if (!rule.isInterfaceOK()) continue; // avoid these
				Set<Pair<Term,Substitution>> caseResult = rule.caseAnalyze(ctx, ctx.currentCaseAnalysis, (ClauseUse) ctx.currentCaseAnalysisElement, this);
				// System.out.println("  case Result = " + caseResult);
				ctx.caseTermMap.put(rule, caseResult);
			}
		}

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
		  ctx.savedCaseMap.put(targetDerivation.getName(), ctx.caseTermMap);
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
		      for (Map.Entry<Atom,Term> e2 : sub.getMap().entrySet()) {
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
		          targetGamma = ctx.innermostGamma;
		        }
		      }
		      if (ctx.currentCaseAnalysis.countLambdas() < missingCase.countLambdas()) {
            if (targetGamma instanceof ClauseUse) {
              targetGamma = ((ClauseUse)targetGamma).getRoot();
            }
            NonTerminal gammaNT = (NonTerminal)targetGamma;
            NonTerminal newGammaNT = new NonTerminal(gammaNT.getSymbol()+"'", gammaNT.getLocation());
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
		        ErrorHandler.report(Errors.MISSING_CASE, missingMessage,this);
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
		  ErrorHandler.report(Errors.MISSING_CASE, missingMessages.toString(), this, missingCaseTexts.toString());
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

	/** Adapts this term using the current context
	 * This includes substituting with the current sub
	 * and also adapting the context to include assumptions currently in scope
	 * XXX This works wrong in some cases, where the next method (with originalContext)
	 * XXX succeeds.  This is related to the two ways to check the last derivation
	 * XXX in Theorem.java and Derivation.java.
	 * TODO: generate good and bad regression tests, then combine these two methods together
	 * and merge the "last derivation" checks to do the correct thing.
	 */
	public static Term adapt(Term term, Element element, Context ctx, boolean wrapUnrooted) {

	  if (element instanceof ClauseUse) {
	    return adapt(term,((ClauseUse)element).getRoot(),ctx,element);
	  }
	  
	  if (element instanceof AssumptionElement) {
	    AssumptionElement ae = (AssumptionElement)element;
      return adapt(term, ae.getRoot(), ctx, ae.getBase());
	  }
	  
	  // TODO: generalize this to all terms reference in system
		Util.debug("for element ", element, " term.countLambdas() = ", term.countLambdas());
		Util.debug("adaptation term = ", ctx.matchTermForAdaptation);
		if (ctx.adaptationSub != null) debug("ctx.matchTermForAdaptation.countLambdas() = ", ctx.matchTermForAdaptation.countLambdas());
		try {
		if (ctx.adaptationSub != null && term.countLambdas() < ctx.matchTermForAdaptation.countLambdas() && element instanceof ClauseUse && !ctx.innermostGamma.equals(((ClauseUse)element).getRoot())) {
		  // TODO: This whole section needs to be changed.
		  // JTB: newly added: fix issue #16
		  debug("term before new sub: ", term);
		  term = term.substitute(ctx.currentSub);
      debug("term after new sub: ", term);
      term = ((ClauseUse)element).adaptTermTo(term, ctx.matchTermForAdaptation, ctx.adaptationSub, wrapUnrooted);
      debug("term after adapt: ", term);
      if (((ClauseUse)element).getRoot() == null) {
		    while (term instanceof Abstraction) {
		      Abstraction abs = (Abstraction)term;
		      // Kludge: we assume we always put two things into variables at a time:
		      Abstraction abs2 = (Abstraction)abs.getBody();
		      if (abs2.getBody().hasBoundVar(2)) {
		        debug("uses bound variable wrapped.", term);
		        break;
		      } else {
		        debug("does not use bound variable ", term);
		        term = abs2.getBody();
		      }
		    }
		  }
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
	public static Term adapt(Term term, NonTerminal originalContext, Context ctx, Node errorPoint) {
	  term = term.substitute(ctx.currentSub); // JTB: Added for Issue #16
		NonTerminal targetContext = ctx.innermostGamma;
		Util.debug("adapting from ", originalContext, " to ", targetContext, " on ", term);
		Util.debug("adaptationSub = ", ctx.adaptationSub);
		if (originalContext == null) return term; // no context -- nothing to adapt
		
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
	      // ErrorHandler.warning("Here! with oc = " + originalContext + ", tc = " + targetContext + " and adaptationRoot = " + ctx.adaptationRoot, errorPoint);
				//TODO: make this more principled (e.g. work for more than one adaptation -- see code below)
				Util.debug("adaptation sub = ", ctx.adaptationSub, " applied inside ", ctx.adaptationMap.get(ctx.adaptationRoot).varTypes.size());
				Util.debug("current sub = ", ctx.currentSub);
				if (term instanceof Application) {
	        // System.out.println("term is " + term);
	        // System.out.println("current sub = " + ctx.currentSub);
	        // new RuntimeException("for trace").printStackTrace();
				  ErrorHandler.report("Using variables with a judgment '" + ((Application)term).getFunction() + "' that doesn't assume context", errorPoint);
				}
				Util.debug("before term = ", term);
				term = ((Abstraction)term).subInside(ctx.adaptationSub, ctx.adaptationMap.get(ctx.adaptationRoot).varTypes.size());
				Util.debug("after term = ", term);
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
	private Fact targetDerivation;
}
