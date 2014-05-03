package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.term.Facade.App;
import static edu.cmu.cs.sasylf.util.Errors.JUDGMENT_EXPECTED;
import static edu.cmu.cs.sasylf.util.Errors.WRONG_JUDGMENT;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.BoundVar;
import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Pair;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.term.UnificationIncomplete;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.Util;

public class Rule extends RuleLike implements CanBeCase {
	public Rule(Location loc, String n, List<Clause> l, Clause c) { 
	  super(n, loc); 
	  premises=l; 
	  conclusion=c; 
	  super.setEndLocation(c.getEndLocation());
	}
	
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
		  // The following exception cannot be permitted:
		  // case analysis will crash with an internal error (see later in this file)
		  // if (e instanceof Variable) continue;
		  ErrorHandler.report("assumption rule doesn't use " + e, concClauseUse);
		}
	}
	
	public boolean isAssumption() {
		return isAssumpt ;
	}
	
	/** Returns a fresh term for the rule and a substitution that matches the term.
	 * sub will be null if no case analysis is possible
	 * @param source TODO
	 * @param term2 TODO
	 * @param clauseUse TODO
	 */
	public Set<Pair<Term,Substitution>> caseAnalyze(Context ctx, Term term, ClauseUse clause, Node source) {
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
		
    // compute term to check against rule
    List<Term> termArgs = this.getFreeVarArgs(term);
    termArgs.add(term);
    Term appliedTerm = App(this.getRuleAppConstant(), termArgs);

    Pair<Term, Substitution> pair ;
    
    if (!isAssumption()) {
		  // compute term for rule
		  Term ruleTerm = this.getFreshRuleAppTerm(term, new Substitution(), null);
		  Util.debug("\tfor rule ", getName(), " computed rule term ", ruleTerm);

		  // see if the rule applies
		  pair = checkRuleApplication(term, ruleTerm, appliedTerm, source);
		  if (pair != null) {
		    Util.debug("\tadding (1) ", pair.first);
		    result.add(pair);
		  }
		}
		
		if (isAssumption()) {
      // First disassemble the rule term to get the base.
      // XXX: If we add non-variable assumptions, we need to change this
      Abstraction ruleConcTerm = (Abstraction)conclusion.asTerm();
      ruleConcTerm = (Abstraction)ruleConcTerm.substitute(ruleConcTerm.freshSubstitution(new Substitution()));
      Abstraction ruleConcTermInner = (Abstraction)ruleConcTerm.getBody();
      Term ruleConcBodyTerm = ruleConcTermInner.getBody();
      // XXX: boundVarIndex only exists if we have a variable "if (x instanceof Variable) continue;" above
      int boundVarIndex = ((Application)ruleConcBodyTerm).getArguments().indexOf(new BoundVar(2));
      Util.debug("boundVarIndex = ",boundVarIndex);
      if (boundVarIndex < 0) {
        Util.verify(ErrorHandler.getErrorCount()>0, "should have been caught already");
        return result;
      }
		  
      // Collect all variables from term 
      List<Abstraction> outer = new ArrayList<Abstraction>();
      Term base = Term.getWrappingAbstractions(term,outer);
      int abstractionDepth = outer.size();
      Term destinedVar = ((Application)base).getArguments().get(boundVarIndex);
      Util.debug("term destined to be a variable: ",destinedVar);
		  
      // First we go through all variables in the current context
      // to see if they could be the one we are matching here
      for (int i=0; i < outer.size(); ++i) {
        Abstraction a = outer.get(i);
        if (a.varType instanceof Application) {
          Util.debug("is ",a.varType, " an application of ", judgment.typeTerm());
          if (((Application)a.varType).getFunction().equals(judgment.typeTerm())) {
            Term ruleTerm2 = a.varType.incrFreeDeBruijn(outer.size()-i);
            ruleTerm2 = Term.wrapWithLambdas(outer,ruleTerm2);
            // put it in a rule
            ruleTerm2 = Facade.App(getRuleAppConstant(), ruleTerm2);
            Util.debug("Constructed ruleTerm2 = ", ruleTerm2);
            Util.debug("\tappliedTerm = ", appliedTerm);
            pair = checkRuleApplication(term, ruleTerm2, appliedTerm, source);
            if (pair != null) {
              Util.debug("\tadded result! (2a) ", pair.first,", ",pair.second);
              result.add(pair);
            }
          }
        }
      }
      
      if (!clause.isRootedInVar()) {
        Util.debug("cannot find a variable since not in variable context");
        return result;
      }
      
      // Consider the possibility that the context is binding the result
      
     
      // now reassemble the rule term after inserting all outer bindings
      Term ruleTerm2 = ruleConcBodyTerm.incrFreeDeBruijn(abstractionDepth);
      ruleTerm2 = Term.wrapWithLambdas(outer,ruleTerm2);
      // add back the rule variables
      ruleTerm2 =  Facade.Abs(ruleConcTermInner.varName, ruleConcTermInner.varType, ruleTerm2);
      ruleTerm2 =  Facade.Abs(ruleConcTerm.varName, ruleConcTerm.varType, ruleTerm2);
      // put it in a rule
      ruleTerm2 = Facade.App(getRuleAppConstant(), ruleTerm2);

      // now figure out how to change the applied term to handle variables.
      // We use the blunt instrument of bindInFreeVars
      // We could rather see if the destinedVar is a variable or an application
      // of a variable, and if so, construct a substitution, and if not, give up.
      Substitution varSubst = new Substitution();
      destinedVar.bindInFreeVars(ruleConcTerm.varType, varSubst);
      varSubst.incrFreeDeBruijn(1);
      Util.debug("varSubst = ",varSubst);

      // adapt the applied term
      Term appliedTerm2 = term.substitute(varSubst);
      appliedTerm2 =  Facade.Abs(ruleConcTermInner.varName, ruleConcTermInner.varType, appliedTerm2);
      appliedTerm2 = Facade.Abs(ruleConcTerm.varName, ruleConcTerm.varType, appliedTerm2);
      appliedTerm2 = Facade.App(((Application)appliedTerm).getFunction(), appliedTerm2);

      //now try it out
      Util.debug("found a term with assumptions!\n\truleTerm2 = ", ruleTerm2, "\n\tappliedTerm2 = ", appliedTerm2);
      Util.debug("\tappliedTerm = ", appliedTerm);
      pair = checkRuleApplication(term, ruleTerm2, appliedTerm2, source);
      if (pair != null) {
        Util.debug("\tadded result! (2b) ", pair.first,", ",pair.second);
        result.add(pair);
      }
		}
		return result;
	}
	
	/** Checks if this rule applies to term, assuming ruleTerm is the term for the rule
	 * and appliedTerm is the rule term built up from term.  
	 * @param source TODO
	 */
	private Pair<Term, Substitution> checkRuleApplication(Term term,
			Term ruleTerm, Term appliedTerm, Node source) {
		Substitution sub = null;
		try {
			sub = ruleTerm.unify(appliedTerm);
			Util.debug("found sub ", sub, " for case analyzing ", term, " with rule ", getName());
		} catch (UnificationIncomplete e) {
		  Util.debug("unification incomplete on ", ruleTerm, " and ", appliedTerm);
		  ErrorHandler.recoverableError("Unification incomplete for case " + getName(), source, "SASyLF tried to unify " + e.term1 + " and " + e.term2);
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

