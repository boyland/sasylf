package edu.cmu.cs.sasylf.ast;


import static edu.cmu.cs.sasylf.util.Errors.VAR_STRUCTURE_KNOWN;
import static edu.cmu.cs.sasylf.util.Util.debug;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
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
import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
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
	public DerivationByAnalysis(String n, Location l, Clause c, Clause subject) {
		super(n,l,c);
		super.getArgStrings().add(subject);
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

		ClauseType caseType = (ClauseType)targetDerivation.getElement().getType();
		if (caseType.isAbstract()) {
			if (caseType instanceof NotJudgment) {
				ErrorHandler.report("Cannot perform case analysis on 'not' judgments",this);
			} else {
				ErrorHandler.report("Cannot perform case analysis on parameter structures", this);
			}
		}

		if (caseType instanceof Judgment) {
			if (targetDerivation instanceof ClauseAssumption) {
				ErrorHandler.report("Cannot perform case analysis on a constructed judgment", targetDerivation);
			}
		}

		try {
			ctx.currentCaseAnalysis = ctx.toTerm(targetDerivation.getElement());
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
			NonTerminal caseNTRoot = null;
			if (ctx.currentCaseAnalysisElement instanceof NonTerminal) {
				caseNT = (NonTerminal)ctx.currentCaseAnalysisElement;
				caseNTRoot = ctx.getContext(caseNT);
			} else if (ctx.currentCaseAnalysisElement instanceof AssumptionElement) {
				// JTB: TODO: Figure out how to avoid the duplicate logic from here and in the next section.
				AssumptionElement ae = (AssumptionElement)ctx.currentCaseAnalysisElement;
				Element e = ae.getBase();
				if (e instanceof Binding) {  
					Binding b = (Binding)e;
					Set<Variable> args = new HashSet<Variable>();
					// savedCase presumes that we only case the pure binder.
					for (Element arg : b.getElements()) {
						if (!(arg instanceof Variable) ||
								!args.add((Variable)arg)) {
							ErrorHandler.report(VAR_STRUCTURE_KNOWN, "Case analysis is only permitted on pure binders, with unique variable arguments", this);
						}
					}
					caseNT = b.getNonTerminal();
				}
				else if (e instanceof NonTerminal) caseNT = (NonTerminal)e;
				else ErrorHandler.report(VAR_STRUCTURE_KNOWN, "The structure of " + targetDerivation+" is already known",this);
				if (!ctx.canRelaxTo(ctx.getContext(caseNT), ae.getRoot())) {
					ErrorHandler.report("Case analysis target cannot assume less than context does",this);        
				}
				caseNTRoot = ae.getRoot();
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
				if (!ctx.inputVars.contains(caseNT.computeTerm(null))) {
					ErrorHandler.report("Undeclared syntax: " + caseNT +(ctx.inputVars.isEmpty() ? "":", perhaps you meant one of " + ctx.inputVars), targetDerivation);
				}
				Syntax syntax = caseNT.getType();
				for (Clause clause : syntax.getClauses()) {
					Set<Pair<Term,Substitution>> set = new HashSet<Pair<Term,Substitution>>();
					Substitution emptySubstitution = new Substitution();
					ctx.caseTermMap.put(clause, set);
					// and below, we add to the set, as appropriate
					List<Abstraction> context = new ArrayList<Abstraction>();
					Term.getWrappingAbstractions(ctx.currentCaseAnalysis, context);
					if (clause.isVarOnlyClause()) {
						// Special case (1): any of the variables in the context that are relevant.
						int n = context.size();
						for (int i=0; i < n; ++i) {
							Abstraction a = context.get(i);
							if (a.getArgType().equals(syntax.typeTerm())) {
								Term term = Term.wrapWithLambdas(context, new BoundVar(n-i));
								set.add(new Pair<Term,Substitution>(term,emptySubstitution));
							}
						}
						// Special case (2): we have a new variable
						if (caseNTRoot != null) {
							ClauseDef contextClause = syntax.getContextClause();
							Rule assumptionRule = contextClause.assumptionRule;
							List<Abstraction> newContext = new ArrayList<Abstraction>();
							if (assumptionRule != null) {
								Application ruleFresh = assumptionRule.getFreshAdaptedRuleTerm(Collections.<Abstraction>emptyList(), null);
								Term.getWrappingAbstractions(ruleFresh.getArguments().get(0),newContext);
							} else {
								newContext.add((Abstraction) Facade.Abs(syntax.typeTerm(), new BoundVar(1)));
							}
							newContext.addAll(context);
							Term term = Term.wrapWithLambdas(newContext,  new BoundVar(newContext.size()));
							Util.debug("adding pattern ",term);
							set.add(new Pair<Term,Substitution>(term,emptySubstitution));
						}
						/*
          if (targetDerivation instanceof BindingAssumption) {
            System.out.println("Found a binding: " + targetDerivation);
          }
          List<Pair<String,Term>> varBindings = new ArrayList<Pair<String,Term>>();
          NonTerminal root = null;
				  if (ctx.currentCaseAnalysisElement instanceof AssumptionElement) {
				    AssumptionElement ae = (AssumptionElement)ctx.currentCaseAnalysisElement;
				    root = ae.getRoot();
				  } else {
				    // no context, no problem
				    if (ctx.assumedContext == null) continue;
				    // not dependent on context, no problem
				    if (!ctx.assumedContext.getType().canAppearIn(syntax.typeTerm())) continue;
				    // if known to be variable free, no problem
				    if (ctx.isVarFree(caseNT)) continue;
				    root = ctx.assumedContext;
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
				  }*/
						// Detect bug3.slf
						//ErrorHandler.recoverableError("A variable is a possible case, but (currently) SASyLF has no way to case variables.",this);
						continue;
					}
					//System.out.println("clause : " + clause);
					Term term = ((ClauseDef)clause).getSampleTerm();
					//System.out.println("  sample term: " + term);
					Substitution freshSub = term.freshSubstitution(new Substitution());
					term = term.substitute(freshSub);
					// adaptation!
					term = ctx.adapt(term, context, true);

					Util.debug("------Unify?");
					Util.debug("term = ",term);
					Util.debug("subj = ",ctx.currentCaseAnalysis);
					Substitution checkSub;
					try {
						checkSub = term.unify(ctx.currentCaseAnalysis);
					} catch (UnificationFailed ex) {
						Util.debug("error = ",ex.getMessage());
						ErrorHandler.report(Errors.INTERNAL_ERROR, "Unification should not fail",this);
						return;
					}
					Util.debug("checking checkSub = ",checkSub);
					if (!ctx.canCompose(checkSub)) {
						Util.debug("can't compose.");
						continue;
					}
					//System.out.println("  sample term after freshification: " + term);
					set.add(new Pair<Term,Substitution>(term, new Substitution()));
				}
			} else {
				debug("*********** case analyzing line ", getLocation().getLine());
				// tdebug("    currentCaseAnalysisElement = " + ctx.currentCaseAnalysisElement);
				// tdebug("    sub = " + ctx.currentSub);
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
								targetGamma = ctx.assumedContext;
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

	private List<Case> cases = new ArrayList<Case>();
	private Fact targetDerivation;
}
