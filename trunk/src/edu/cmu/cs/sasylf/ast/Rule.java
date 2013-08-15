package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.ast.Errors.JUDGMENT_EXPECTED;
import static edu.cmu.cs.sasylf.ast.Errors.WRONG_JUDGMENT;
import static edu.cmu.cs.sasylf.term.Facade.App;
import static edu.cmu.cs.sasylf.util.Util.debug;

import java.io.PrintWriter;
import java.io.StringWriter;
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

public class Rule extends RuleLike implements CanBeCase {
	public Rule(Location loc, String n, List<Clause> l, Clause c) { super(n, loc); premises=l; conclusion=c; }
	public List<Clause> getPremises() { return premises; }
	public Clause getConclusion() { return conclusion; }
	
	/** rules should have no existential variables
	 */
    public Set<FreeVar> getExistentialVars() {
    	return new HashSet<FreeVar>();
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

		for (int i = 0; i < premises.size(); ++i) {
			Clause c = premises.get(i);
			c.typecheck(ctx);
			ClauseUse premiseClause = (ClauseUse) c.computeClause(ctx, false);
			if (!(premiseClause.getConstructor().getType() instanceof Judgment))
				ErrorHandler.report(JUDGMENT_EXPECTED, "Rule premise must be a judgment form, not just syntax", premiseClause);
			premiseClause.checkBindings(bindingTypes, this);
			premises.set(i, premiseClause);
			//premises.set(i, new ClauseUse(c, ctx.parseMap));
		}
		ctx.ruleMap.put(getName(), this);
		computeAssumption(ctx);
	}
	
	private void computeAssumption(Context ctx) {
		ClauseUse concClauseUse = (ClauseUse)this.getConclusion();
		Judgment parent = (Judgment)concClauseUse.getConstructor().getType();
		// must have no premises, parent must assume something
		if (premises.size() > 0 || parent.getAssume() == null)
			return; // default false
		
		// must have Gamma, E(x) for Gamma in conclusion
		int assumeIndex = ((ClauseDef)parent.getForm()).getAssumeIndex();
		Element assumeElement = getConclusion().getElements().get(assumeIndex);
		if (!(assumeElement instanceof Clause))
			return; // default false
		// look for sub-part of gamma clause, a NonTerminal with same type as gamma
		ClauseUse assumeClause = (ClauseUse) assumeElement;
		Syntax gammaType = (Syntax) assumeClause.getConstructor().getType();
		boolean found = false;
		for (ElemType eType: assumeClause.getElemTypes()) {
			if (eType == gammaType)
				found = true;
		}
		if (!found)
			return; // default false
		
		// must have x in body
		// look for sub-part of gamma clause that is a variable
		Element varElem = null;
		for (Element e : assumeClause.getElements()) {
			if (e instanceof Variable)
				varElem = e;
		}
		if (varElem == null)
			return; // default false
		boolean foundVar = false;
		for (Element e : getConclusion().getElements()) {
			if (e.equals(varElem))
				foundVar = true;
		}
		if (foundVar) {
			isAssumpt = true;
			if (assumeClause.getConstructor().assumptionRule != null)
				// note: caseAnalyze makes this same assumption, incrementing de Bruijn by 2
				ErrorHandler.report("Multiple uses of the same assumption not supported", this);
			assumeClause.getConstructor().assumptionRule = this;
			
			// should not have more nonterminals in the body than we have in the assumption clause
			//Set<NonTerminal> bodyNonTerminals = new HashSet<NonTerminal>();
			//Set<NonTerminal> assumptionNonTerminals = new HashSet<NonTerminal>();
			int numBodyNonTerminals = 0;
			int numAssumptionNonTerminals = 0;
			for (Element e : assumeClause.getElements()) {
				if (e instanceof NonTerminal)
					numAssumptionNonTerminals++;
			}
			for (Element e : getConclusion().getElements()) {
				if (e instanceof NonTerminal)
					numBodyNonTerminals++;
			}
			if (numBodyNonTerminals>numAssumptionNonTerminals)
				ErrorHandler.report("In a variable rule, no nonterminal should be mentioned in the main part of the rule unless it is mentioned in the context assumption", this);
			// TODO: should check that the sets are the same
		}
	}
	
	public boolean isAssumption() {
		return isAssumpt ;
	}
	
	/** Returns a fresh term for the rule and a substitution that matches the term.
	 * sub will be null if no case analysis is possible
	 */
	public Set<Pair<Term,Substitution>> caseAnalyze(Context ctx) {
		Term term = ctx.currentCaseAnalysis;
		ClauseUse clause = (ClauseUse) ctx.currentCaseAnalysisElement;
		Set<Pair<Term,Substitution>> result = new HashSet<Pair<Term,Substitution>>();
		
		// compute term for rule
		Term ruleTerm = this.getFreshRuleAppTerm(term, new Substitution(), null);
		debug("\tfor rule " + getName() + " computed rule term " + ruleTerm);

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
			debug("\tadding " + pair.first);
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
				debug("applied term is " + appliedTerm);
								
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
				debug("found a term with assumptions!\n\truleTerm2 = " + ruleTerm2 + "\n\tappliedTerm2 = " + appliedTerm2);
				debug("\n\truleTerm = " + ruleTerm + "\n\tappliedTerm = " + appliedTerm);
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
						debug("adaptationSub = " + adaptationSub);
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
					debug("\tadding " + pair.first);
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
		Term fixedRuleTerm = null;
		try {
			//sub = ruleTerm.unify(appliedTerm);
			sub = ruleTerm.unifyAllowingBVs(appliedTerm);

			debug("found sub " + sub + " for case analyzing " + term + " with rule " + getName());
			// a free variable in term should not, in its substitution result, have any free bound variables
			// TODO: really should build up substitution, rather than just replacing each one piecemeal
			// this version could be buggy.
			Set<FreeVar> freeVars = term.getFreeVariables();
			Substitution removeBVSub = new Substitution();
			for (FreeVar v : freeVars) {
				Term substituted = sub.getSubstituted(v);
				if (substituted != null && substituted.hasBoundVarAbove(0)) {
					// try to remove it
					//Term newSubstituted1 = substituted.removeBoundVarsAbove(0);
					substituted.removeBoundVarsAbove(0, removeBVSub);
					Term newSubstituted = substituted.substitute(removeBVSub);
					debug("got new substitution: " + newSubstituted);
					sub.add(v, newSubstituted);
					//throw new UnificationFailed("illegal variable binding in result: " + substituted + " for " + v + "\n" + sub);
				}
			}
			fixedRuleTerm = appliedTerm.substitute(sub).substitute(removeBVSub);
			if (!ruleTerm.equals(fixedRuleTerm)) {
				debug("computed rule term is " + ruleTerm + "\n\tfixed to " + fixedRuleTerm + "\n\tsub is " + sub);
				sub.compose(removeBVSub);
			}
			//System.err.println("rule unified");
		} catch (UnificationFailed e) {
			//System.err.println("rule did not unify");
			// did not unify, leave sub null
			debug("unification failed on " + ruleTerm + " and " + appliedTerm);
			//e.printStackTrace();
			sub = null;
		}
		if (sub == null)
			return null;
		else
			return new Pair<Term,Substitution>(fixedRuleTerm, sub);
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

	/*public void resolveClauses(Map<List<ElemType>,ClauseDef> parseMap) {
	for (int i = 0; i < premises.size(); ++i) {
	    Clause c = premises.get(i);
	}
    }*/
	
	@Override
	public NonTerminal getAssumes() {
	  return getJudgment().getAssume();
	}
	
	public Judgment getJudgment() {
	  if (judgment == null) throw new InternalError("judgment not yet set!");
	  return judgment;
	}
	
	private Judgment judgment;
}

