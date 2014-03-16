package edu.cmu.cs.sasylf.ast;


import static edu.cmu.cs.sasylf.ast.Errors.DERIVATION_NOT_FOUND;
import static edu.cmu.cs.sasylf.ast.Errors.VAR_STRUCTURE_KNOWN;
import static edu.cmu.cs.sasylf.util.Util.debug;

import java.io.PrintWriter;
import java.util.ArrayList;
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
import edu.cmu.cs.sasylf.term.Pair;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;


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
						  // XXX: JTB: Isn't this redundant?  We already have the thing in the map,
						  // or we wouldn't have found it?
							targetDerivation = new SyntaxAssumption(targetDerivationName, getLocation());
							targetDerivation.typecheck(ctx);
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

		super.typecheck(ctx);

		Term oldCase = ctx.currentCaseAnalysis;
		Element oldElement = ctx.currentCaseAnalysisElement;
		Term oldGoal = ctx.currentGoal;
		Clause oldGoalClause = ctx.currentGoalClause;
		Map<CanBeCase,Set<Pair<Term,Substitution>>> oldCaseTermMap = ctx.caseTermMap;
		
		try {
		ctx.currentCaseAnalysis = adapt(targetDerivation.getElement().asTerm(), targetDerivation.getElement(), ctx, true);
		debug("setting current case analysis to " + ctx.currentCaseAnalysis);
		//ctx.currentCaseAnalysis = targetDerivation.getElement().asTerm().substitute(ctx.currentSub);
		ctx.currentCaseAnalysisElement = targetDerivation.getElement();
		ctx.currentGoal = getElement().asTerm().substitute(ctx.currentSub);
		ctx.currentGoalClause = getClause();
		
		boolean isSubderivation = targetDerivation != null
			&& (targetDerivation.equals(ctx.inductionVariable) || ctx.subderivations.contains(targetDerivation));
		if (isSubderivation) debug("found subderivation: " + targetDerivation);
		
		ctx.caseTermMap = new LinkedHashMap<CanBeCase,Set<Pair<Term,Substitution>>>();
		
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
          !ctx.varfreeNTs.contains(caseNT)) {
        Clause assumes = ae.getAssumes();
        NonTerminal root = null;
        if (assumes instanceof ClauseUse) {
          root = ((ClauseUse)assumes).getRoot();
        } else {
          root = (NonTerminal)assumes.getElements().get(0);
        } 
        if (root == null || !root.equals(ctx.innermostGamma)) {
          ErrorHandler.report("Case analysis target cannot assume less than context does",this);
        }
      }
		}

		if (caseNT != null) {
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
				    Clause assumes =  ae.getAssumes();
		        if (assumes instanceof ClauseUse) {
		          ((ClauseUse)assumes).readAssumptions(varBindings, true);
		          root = ((ClauseUse)assumes).getRoot();
		        } else {
		          root = (NonTerminal)assumes.getElements().get(0);
		        } 
				  } else {
				    // no context, no problem
				    if (ctx.innermostGamma == null) continue;
				    // not dependent on context, no problem
				    if (!ctx.innermostGamma.getType().canAppearIn(syntax.typeTerm())) continue;
				    // if known to be variable free, no problem
				    if (ctx.varfreeNTs.contains(caseNT)) continue;
				    root = ctx.innermostGamma;
				  }
				  debug("Adding variable cases for " + syntax);
				  for (Pair<String,Term> pair : varBindings) {
				    if (pair.second.equals(syntax.typeTerm())) {
				      debug("  for " + pair.first);
				      Variable gen = new Variable(pair.first,getLocation());
				      gen.setType(syntax);
				      Term caseTerm = ClauseUse.newWrap(gen.computeTerm(varBindings),varBindings,0);
				      debug("  generated " + caseTerm);
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
				          fakeUse.readAssumptions(moreBindings, true);
				          debug("  for " + moreBindings.get(0));
				          Term caseTerm = ClauseUse.newWrap(new BoundVar(varBindings.size()+2),varBindings,0);
				          caseTerm = ClauseUse.newDoBindWrap(caseTerm, moreBindings);
				          debug("  generated " + caseTerm);
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
      debug("*********** case analyzing line " + getLocation().getLine());
      // tdebug("    currentCaseAnalysisElement = " + ctx.currentCaseAnalysisElement);
      // tdebug("    sub = " + ctx.currentSub);
      // tdebug("    adaptationSub = " + ctx.adaptationSub);
			Judgment judge= (Judgment) ((ClauseUse)ctx.currentCaseAnalysisElement).getConstructor().getType();
			// see if each rule, in turn, applies
			for (Rule rule : judge.getRules()) {
			  if (!rule.isInterfaceOK()) continue; // avoid these
				Set<Pair<Term,Substitution>> caseResult = rule.caseAnalyze(ctx);
				// System.out.println("  case Result = " + caseResult);
				ctx.caseTermMap.put(rule, caseResult);
			}
		}

		
		for (Case c : cases) {
			c.typecheck(ctx, isSubderivation);
		}
		
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
		      for (Map.Entry<Atom,Term> e2 : sub.getMap().entrySet()) {
		        if (e2.getValue() instanceof FreeVar && !ctx.inputVars.contains(e2.getValue())) {
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
		        if (!ctx.varfreeNTs.contains(ctx.currentCaseAnalysisElement)) {
		          targetGamma = ctx.innermostGamma;
		        }
		      }
		      if (cbc instanceof Rule && ((Rule)cbc).isAssumption()) {
		        NonTerminal gammaNT = ((ClauseUse)ctx.currentCaseAnalysisElement).getRoot();
		        NonTerminal newGammaNT = new NonTerminal(gammaNT.getSymbol()+"'", gammaNT.getLocation());
		        newGammaNT.setType(gammaNT.getType());
		        targetGamma = newGammaNT;
		      } else if (cbc instanceof Clause && ((Clause)cbc).isVarOnlyClause()) {
		        //XXX If we neglect to do this, we generate
		        // a reuse of Gamma, which our PM system does NOT complain about.
		        if (targetGamma instanceof ClauseUse) {
		          targetGamma = ((ClauseUse)targetGamma).getRoot();
		        }
		        NonTerminal gammaNT = (NonTerminal)targetGamma;
		        NonTerminal newGammaNT = new NonTerminal(gammaNT.getSymbol()+"'", gammaNT.getLocation());
		        newGammaNT.setType(gammaNT.getType());
		        targetGamma = newGammaNT;
		      }

		      String missingMessage = cbc.getErrorDescription(entry.getValue().iterator().next().first, ctx);
		      try {
		        TermPrinter termPrinter = new TermPrinter(ctx,targetGamma,this.getLocation());
		        missingCaseText = termPrinter.caseToString(missingCase);
		      } catch (RuntimeException ex) {
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
	 */
	public static Term adapt(Term term, Element element, Context ctx, boolean wrapUnrooted) {
		// TODO: generalize this to all terms reference in system
		debug("for element " + element + " term.countLambdas() = "+term.countLambdas());
		if (ctx.adaptationSub != null) debug("ctx.matchTermForAdaptation.countLambdas() = "+ctx.matchTermForAdaptation.countLambdas());
		try {
		if (ctx.adaptationSub != null && term.countLambdas() < ctx.matchTermForAdaptation.countLambdas() && element instanceof ClauseUse && !ctx.innermostGamma.equals(((ClauseUse)element).getRoot())) {
		  // TODO: This whole section needs to be changed.
		  // JTB: newly added: fix issue #16
		  debug("term before new sub: " + term);
		  term = term.substitute(ctx.currentSub);
      debug("term after new sub: " + term);
      term = ((ClauseUse)element).adaptTermTo(term, ctx.matchTermForAdaptation, ctx.adaptationSub, wrapUnrooted);
      debug("term after adapt: " + term);
      if (((ClauseUse)element).getRoot() == null) {
		    while (term instanceof Abstraction) {
		      Abstraction abs = (Abstraction)term;
		      // Kludge: we assume we always put two things into variables at a time:
		      Abstraction abs2 = (Abstraction)abs.getBody();
		      if (abs2.getBody().hasBoundVar(2)) {
		        debug("uses bound variable wrapped." + term);
		        break;
		      } else {
		        debug("does not use bound variable " + term);
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
				if (term instanceof Application) {
	        // System.out.println("term is " + term);
	        // System.out.println("current sub = " + ctx.currentSub);
	        // new RuntimeException("for trace").printStackTrace();
				  ErrorHandler.report("Using variables with a judgment '" + ((Application)term).getFunction() + "' that doesn't assume context", errorPoint);
				}
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