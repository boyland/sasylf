package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.ast.Errors.JUDGMENT_EXPECTED;
import static edu.cmu.cs.sasylf.ast.Errors.WRONG_JUDGMENT;
import static edu.cmu.cs.sasylf.term.Facade.App;
import static edu.cmu.cs.sasylf.util.Util.debug;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Pair;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.Util;

public class Rule extends RuleLike implements CanBeCase {
	public Rule(Location loc, String n, List<Clause> l, Clause c) { super(n, loc); premises=l; conclusion=c; }
	public List<Clause> getPremises() { return premises; }
	public Clause getConclusion() { return conclusion; }
	
	/** rules should have no existential variables
	 */
    public Set<FreeVar> getExistentialVars() {
    	return new HashSet<FreeVar>();
    }

    @Override
    public String getKind() {
      return "rule";
    }
    
	private List<Clause> premises;
	private Clause conclusion;
	private boolean isAssumpt = false; // default: false

	public void prettyPrint(PrintWriter out) {
		for (Clause c : premises) {
			out.print("premise ");
			c.prettyPrint(out);
			out.println();
		}

		out.print("--------------------- ");
		out.println(getName());
		conclusion.prettyPrint(out);
		out.println("\n");
	}

	public void typecheck(Context ctx, Judgment judge) {
	  judgment = judge;
		Map<String, List<ElemType>> bindingTypes = new HashMap<String, List<ElemType>>();
		conclusion.typecheck(ctx);
		ClauseUse myConc = (ClauseUse) conclusion.computeClause(ctx, false);
		if (!(myConc.getConstructor().getType() instanceof Judgment))
			ErrorHandler.report(JUDGMENT_EXPECTED, "Rule conclusion must be a judgment form, not just syntax", myConc);

		if (!(myConc.getConstructor().getType() == judge))
			ErrorHandler.report(WRONG_JUDGMENT, "Rule conclusion was expected to be a " + judge.getName() + " judgment but instead had the form of the " + ((Judgment)myConc.getConstructor().getType()).getName() + " judgment", myConc);
		
		myConc.checkBindings(bindingTypes, this);
		conclusion = myConc;
		//conclusion = new ClauseUse(conclusion, ctx.parseMap);

		if (ctx.ruleMap.containsKey(getName())) {
		  if (ctx.ruleMap.get(getName()) != this) {
		    ErrorHandler.recoverableError(Errors.RULE_LIKE_REDECLARED, this);
		  }
		} else ctx.ruleMap.put(getName(), this);
		
    try {
      computeAssumption(ctx);
      myConc.checkVariables(new HashSet<String>(), false);
    } catch (SASyLFError ex) {
      // continue
    }
    
    for (int i = 0; i < premises.size(); ++i) {
			Clause c = premises.get(i);
			c.typecheck(ctx);
			ClauseUse premiseClause = (ClauseUse) c.computeClause(ctx, false);
			if (!(premiseClause.getConstructor().getType() instanceof Judgment)) {
				ErrorHandler.report(JUDGMENT_EXPECTED, "Rule premise must be a judgment form, not just syntax", premiseClause);
			}
			premiseClause.checkBindings(bindingTypes, this);
			premises.set(i, premiseClause);
			premiseClause.checkVariables(new HashSet<String>(), false);
			//premises.set(i, new ClauseUse(c, ctx.parseMap));
			NonTerminal nt = premiseClause.getRoot();
			if (nt != null) {
			  if (!nt.equals(myConc.getRoot())) {
			    ErrorHandler.recoverableError(Errors.PREMISE_CONTEXT_MISMATCH, c);
			  }
			}
		}
		
    if (judge.getAssume() != null && !isAssumpt) { // bad15
      NonTerminal nt = myConc.getRoot();
      if (nt == null) {
        ErrorHandler.report(Errors.EMPTY_CONCLUSION_CONTEXT, conclusion);
      } else if (myConc.hasVariables()) {
        ErrorHandler.report(Errors.VAR_CONCLUSION_CONTEXT, conclusion);
      }
    }
    
    ruleIsOk = true;
	}
	
	private void computeAssumption(Context ctx) {
		ClauseUse concClauseUse = (ClauseUse)this.getConclusion();
		ClauseDef concClauseDef = concClauseUse.getConstructor();
    Judgment parent = (Judgment)concClauseDef.getType();
		// must have no premises, parent must assume something
		if (premises.size() > 0 || parent.getAssume() == null)
			return; // default false
		
		// must have Gamma, E(x) for Gamma in conclusion
		int assumeIndex = ((ClauseDef)parent.getForm()).getAssumeIndex();
		Element assumeElement = getConclusion().getElements().get(assumeIndex);
		if (!(assumeElement instanceof ClauseUse))
			return; // default false

		// look for sub-part of gamma clause, a NonTerminal with same type as gamma
		ClauseUse assumeClauseUse = (ClauseUse) assumeElement;
		ClauseDef assumeClauseDef = assumeClauseUse.getConstructor();
    Syntax gammaType = (Syntax) assumeClauseDef.getType();
		if (assumeClauseDef == gammaType.getTerminalCase()) return; // error given elsewhere
		int n = assumeClauseDef.getElements().size();
		
		isAssumpt = true;
		// now we generate errors if there is a problem

    if (assumeClauseDef.assumptionRule != null &&
        assumeClauseDef.assumptionRule != this) // idempotency
      // note: caseAnalyze makes this same assumption, incrementing de Bruijn by 2
      ErrorHandler.report("Multiple uses of the same assumption not supported", this);
    assumeClauseDef.assumptionRule = this;

		// now check that assume clause does not bind two NTs together,
		// or use a clause for a NT.
		Set<Element> defined = new HashSet<Element>();
		for (int i=0; i < n; ++i) {
		  Element u = assumeClauseUse.getElements().get(i);
		  Element d = assumeClauseDef.getElements().get(i);
		  if (d.getType() == gammaType) continue;
		  if (defined.contains(u)) {
		    ErrorHandler.report("Found duplicate instance of " + u, assumeClauseUse);
		  }
		  if (d instanceof Variable) {
		    if (!(u instanceof Variable)) {
		      ErrorHandler.report("Expected variable in assumption rule, found " + u, assumeClauseUse);
		    }
		    defined.add(u);
		  } else if (d instanceof NonTerminal) {
		    if (!(u instanceof NonTerminal)) {
		      ErrorHandler.report("Expected name in assumption rule, found " + u, assumeClauseUse);
		    }
		    defined.add(u);
		  }
		}

		// now check that every NT or Var in the rest of the clause is represented in "unique" Set.
		Set<Element> used = new HashSet<Element>();
		// perhaps we need a "getFreeVars" method
		Deque<Element> worklist = new ArrayDeque<Element>();
		for (Element e : concClauseUse.getElements()) {
		  if (e != assumeClauseUse) worklist.add(e);
		}
		while (!worklist.isEmpty()) {
		  Element e = worklist.remove();
		  if (e instanceof NonTerminal || e instanceof Variable) used.add(e);
		  if (e instanceof Clause) {
		    worklist.addAll(((Clause) e).getElements());
		  }
		}
		
		Set<Element> intersection = new HashSet<Element>(defined);
		intersection.retainAll(used);
		defined.removeAll(intersection);
		used.removeAll(intersection);
		
		// If something is used but not defined, it means the translation to LF
		// doesn't know what to use for the nonterminal when forming the internal derivation
		if (!used.isEmpty()) {
		  ErrorHandler.report("assumption rule gives no way to bind " + used, concClauseUse);
		}
		
		// If a nonterminal is defined but not used, it means that we don't know what to choose
		// for the nonterminal when converting back to LF.  It also means the nonterminal
		// in the surface syntax is arbitrary which is confusing.
		// A variable can be unused because the LF has that in the main parameter; 
		// it isn't needed in the internal derivation, but this violates assumptions made
		// internally, and it wouldn't make sense to have a variable AND a derivation
		// that doesn't use it.  Might as well have two separate things, once this
		// is supported.
		for (Element e : defined) {
		  // if (e instanceof Variable) continue;
		  ErrorHandler.report("assumption rule doesn't use " + e, concClauseUse);
		}
	}
	
	public boolean isAssumption() {
		return isAssumpt ;
	}
	
	/** Returns a fresh term for the rule and a substitution that matches the term.
	 * sub will be null if no case analysis is possible
	 * @param term2 TODO
	 * @param clauseUse TODO
	 */
	public Set<Pair<Term,Substitution>> caseAnalyze(Context ctx, Term term, ClauseUse clause) {
    Set<Pair<Term,Substitution>> result = new HashSet<Pair<Term,Substitution>>();

    // Special case: if the variable is known to be var-free, we can't match this rule
    // XXX: This will need to change if we permit variable free assumptions!
    // Rewrite to not use a special case here, but rather where we try to put variables
    // into a varFree NTS.
		if (isAssumption()) {
		  int n=conclusion.getElements().size();
		  for (int i=0; i < n; ++i) {
		    if (conclusion.getElements().get(i) instanceof Variable &&
		        ctx.varfreeNTs.contains(clause.getElements().get(i))) {
		      Util.debug("no vars in ", clause);
		      return result;
		    }
		  }
		}
		
		// compute term for rule
		Term ruleTerm = this.getFreshRuleAppTerm(term, new Substitution(), null);
		Util.debug("\tfor rule ", getName(), " computed rule term ", ruleTerm);

		/*List<? extends Term > args = ((Application)ruleTerm).getArguments();
		Term concTerm = args.get(args.size()-1);
		int delta = clause.getAdaptationNumber(term, concTerm);
		if (delta > 0) {
			term = ((ClauseUse)conclusion).adaptTermTo(term, concTerm, new Substitution());
		}*/

		// compute term to check against rule
		List<Term> termArgs = this.getFreeVarArgs(term);
		termArgs.add(term);
		Term appliedTerm = App(this.getRuleAppConstant(), termArgs);
	
		// see if the rule applies
		Pair<Term, Substitution> pair = checkRuleApplication(term, ruleTerm, appliedTerm);
		if (pair != null) {
			debug("\tadding ", pair.first);
			result.add(pair);
		}
		
		if (isAssumption()) {
		  
			// see how deep the assumptions are in term
			int assumptionDepth = term.countLambdas();
			// System.out.println("In assumption, with depth = " + assumptionDepth);

			/*int assumeIndex = ((ClauseUse)ctx.currentCaseAnalysisElement).getConstructor().getAssumeIndex();
			List<Variable> assumedVars = new ArrayList<Variable>();
			List<Pair<String, Term>> varBindings = new ArrayList<Pair<String, Term>>();
			Element gammaClause =((ClauseUse)ctx.currentCaseAnalysisElement).getElements().get(assumeIndex);
			gammaClause.readAssumptions(varBindings, assumedVars);*/
			
			if (assumptionDepth > 0) {				
				// TODO: Here, we need to specialize the assumption rule for each element (except the last) that is visible in Gamma
				// TODO: we also need to consider inserting the new variable in multiple places if subordination means it's different
				if (assumptionDepth > 2) {
					ErrorHandler.report("Sorry, haven't yet implemented case analysis when there is more than one variable in scope", this);
				}
				
				// Also specialize the assumption rule for the next element in Gamma that is not currently visible
				/* Invariant: appliedTerm is of the form ruleConstructor [fn x => J(x)]
				 * where J is an instance of a judgment form.
				 * Goal: produce a new term of the form [fn x => fn y => J(x)]
				 */
				Util.debug("applied term is ", appliedTerm);
								
				// adapt the rule term
				Abstraction ruleConcTerm = (Abstraction)((Application)ruleTerm).getArguments().get(0);
				/* v2 */ Abstraction ruleConcTermInner = (Abstraction)ruleConcTerm.getBody();
				Term ruleConcBodyTerm = /* v2 */ruleConcTermInner /* v1 ruleConcTerm*/.getBody();
				// increment de Bruijn
				Term ruleTerm2 = ruleConcBodyTerm.incrFreeDeBruijn(/* v2 */ 2 /* v1 1 */);
				// add the top two variables from term
				Abstraction termAsAbstraction = (Abstraction) term;
				/* v2 */ Abstraction termAsAbstraction2 = (Abstraction) termAsAbstraction.getBody();
				/* v2 */ ruleTerm2 =  Facade.Abs(termAsAbstraction2.varName, termAsAbstraction2.varType, ruleTerm2);
				ruleTerm2 =  Facade.Abs(termAsAbstraction.varName, termAsAbstraction.varType, ruleTerm2);
				// add back the rule variables
				/* v2 */ ruleTerm2 =  Facade.Abs(ruleConcTermInner.varName, ruleConcTermInner.varType, ruleTerm2);
				ruleTerm2 =  Facade.Abs(ruleConcTerm.varName, ruleConcTerm.varType, ruleTerm2);
				// put it in a rule
				ruleTerm2 = Facade.App(((Application)ruleTerm).getFunction(), ruleTerm2);
				
				// adapt the applied term
				Substitution sub = new Substitution();
				term.bindInFreeVars(ruleConcTerm.varType, sub);
				Term appliedTerm2 = term.substitute(sub);
				/* v2 */appliedTerm2 = appliedTerm2.incrFreeDeBruijn(1);
				/* v2 */appliedTerm2 =  Facade.Abs(ruleConcTermInner.varName, ruleConcTermInner.varType, appliedTerm2);
				appliedTerm2 = Facade.Abs(ruleConcTerm.varName, ruleConcTerm.varType, appliedTerm2);
				appliedTerm2 = Facade.App(((Application)appliedTerm).getFunction(), appliedTerm2);

				//now try it out
				Util.debug("found a term with assumptions!\n\truleTerm2 = ", ruleTerm2, "\n\tappliedTerm2 = ", appliedTerm2);
				Util.debug("\n\truleTerm = ", ruleTerm, "\n\tappliedTerm = ", appliedTerm);
				pair = checkRuleApplication(term, ruleTerm2, appliedTerm2);
				if (pair != null) {
				  debug("\tadded result!");
					result.add(pair);
				}
			} else {
				// what if assumptionDepth == 0 but rule has assumptions?

				List<? extends Term > args = ((Application)ruleTerm).getArguments();
				Term concTerm = args.get(args.size()-1);
				int delta = clause.getAdaptationNumber(term, concTerm, false);
				if (delta > 0) {
					if (ctx.matchTermForAdaptation != null) {
						Substitution adaptationSub = ctx.adaptationSub == null ? new Substitution() : new Substitution(ctx.adaptationSub);
						term = ((ClauseUse)conclusion).adaptTermTo(term, ctx.matchTermForAdaptation, adaptationSub);
					} else
						term = ((ClauseUse)conclusion).adaptTermTo(term, concTerm, new Substitution());
				}

				// compute term to check against rule
				termArgs = this.getFreeVarArgs(term);
				termArgs.add(term);
				appliedTerm = App(this.getRuleAppConstant(), termArgs);
			
				// see if the rule applies
				pair = checkRuleApplication(term, ruleTerm, appliedTerm);
				if (pair != null) {
				  debug("\tadding ", pair.first);
					result.add(pair);
				}
				
			}
		}
		
		return result;
	}
	
	/** Checks if this rule applies to term, assuming ruleTerm is the term for the rule
	 * and appliedTerm is the rule term built up from term.  
	 */
	private Pair<Term, Substitution> checkRuleApplication(Term term,
			Term ruleTerm, Term appliedTerm) {
		Substitution sub = null;
		try {
			sub = ruleTerm.unify(appliedTerm);
			Util.debug("found sub ", sub, " for case analyzing ", term, " with rule ", getName());
		} catch (UnificationFailed e) {
			Util.debug("unification failed on ", ruleTerm, " and ", appliedTerm);
			sub = null;
		}
		if (sub == null)
			return null;
		else
			return new Pair<Term,Substitution>(ruleTerm.substitute(sub), sub);
	}
	
	@Override
	public String getErrorDescription(Term t, Context ctx) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.print("rule " + this.getName());
		
		// TODO: comment this back in to get explicit error messages
		/*if (t != null && t instanceof Application) {
			List<? extends Term> elemTerms = ((Application)t).getArguments();
			pw.println(":");
			//pw.println(t);
			PrintContext pctx = null;
			for(int i = 0; i < elemTerms.size()-1; ++i) {
				t = elemTerms.get(i);
				pctx = (pctx == null) ? new PrintContext(t, ctx.inputVars, ctx.innermostGamma) : new PrintContext(t, pctx);
				((ClauseUse)getPremises().get(i)).getConstructor().prettyPrint(pw, pctx);
				pw.println();
			}
			pw.println("--------------------");
			t = elemTerms.get(elemTerms.size()-1);
			pctx = (pctx == null) ? new PrintContext(t, ctx.inputVars, ctx.innermostGamma) : new PrintContext(t, pctx);
			((ClauseUse)getConclusion()).getConstructor().prettyPrint(pw, pctx);
		}*/
		return sw.toString();
	}
	
	@Override
	public int countLambdas(Term t) {
	  return ((Application)t).getArguments().get(premises.size()).countLambdas();
	}

	/*public void resolveClauses(Map<List<ElemType>,ClauseDef> parseMap) {
	for (int i = 0; i < premises.size(); ++i) {
	    Clause c = premises.get(i);
	}
    }*/
	
	@Override
	public boolean isInterfaceOK() {
	  return ruleIsOk;
	}
	
	@Override
	public NonTerminal getAssumes() {
	  return getJudgment().getAssume();
	}
	
	public Judgment getJudgment() {
	  if (judgment == null) throw new InternalError("judgment not yet set!");
	  return judgment;
	}
	
	private Judgment judgment;
	private boolean ruleIsOk = false;
}

