package edu.cmu.cs.sasylf.ast;

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
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.term.UnificationIncomplete;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Pair;
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
	private int isAssumpt = 0; // 0 = not an assumption, > 0 number of abstractions represented
  private int clauseVarIndex = -1; // where is variable in the judgment clause
  private int appVarIndex = -1; // where is variable in the judgment term
	
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
		
    if (judge.getAssume() != null && !isAssumption()) { // bad15
      NonTerminal nt = myConc.getRoot();
      if (nt == null) {
        ErrorHandler.report(Errors.EMPTY_CONCLUSION_CONTEXT, conclusion);
      } else if (myConc.hasVariables()) {
        ErrorHandler.report(Errors.VAR_CONCLUSION_CONTEXT, conclusion);
      }
    }
    
    Set<NonTerminal> neverRigid = getNeverRigid();
    if (!neverRigid.isEmpty()) {
      ErrorHandler.warning("The following meta-variables never occur outside of a binding: " + neverRigid + 
          "\nThis is likely to lead to incomplete unification later on.", this);
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
		
		int countVariables = 0;
		int countNTs = 0;
		int concSize = conclusion.getElements().size();
		for (int i=0; i < concSize; ++i) {
		  if (i == assumeIndex) continue;
		  Element e = conclusion.getElements().get(i);
		  if (e instanceof Variable) {
		    Util.verify(countVariables==0, "can't handle more than one variable binding at a time");
		    clauseVarIndex = i;
		    appVarIndex = countNTs;
        ++countVariables;
		    ++countNTs;
		  } else if (e instanceof NonTerminal) {
		    ++countNTs;
		  }
		}
		
		isAssumpt = 1 + countVariables;
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
		  if (e instanceof NonTerminal || 
		      e instanceof Variable) used.add(e);
		  if (e instanceof Variable) {
		    if (conclusion.getElements().indexOf(e) < 0) {
		      ErrorHandler.report("Variable in assumption rule must be at top-level.",this);
		    }
		  }
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
		return isAssumpt>0 ;
	}
	public int isAssumptionSize() {
	  return isAssumpt;
	}
	
	/**
	 * Return the index of the variable in the clause if this is an assumption rule
	 * with a variable.  Otherwise return -1
	 * @return index of variable in the clause
	 */
	public int getClauseVarIndex() {
	  return clauseVarIndex;
	}
	
	/**
	 * Return the index of the variable in the application arguments of the judgment
	 * term constant if this is an assumption rule with a variable.
	 * Otherwise return -1.
	 * @return index of the variable in the judgment term application arguments
	 */
	public int getAppVarIndex() {
	  return appVarIndex;
	}
	
	/**
	 * Return true if the rule has some free variables that never occur in rigid positions.
	 */
	public Set<NonTerminal> getNeverRigid() {
	  Set<NonTerminal> nts = new HashSet<NonTerminal>();
	  for (Clause p : premises) {
	    p.getFree(nts, true);
	  }
	  conclusion.getFree(nts, true);
	  Set<NonTerminal> rigid = new HashSet<NonTerminal>(nts);
    for (Clause p : premises) {
      p.getFree(nts, false);
    }
    conclusion.getFree(nts, false);
    nts.removeAll(rigid);
	  return nts;
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
		        clause.getElements().get(i) instanceof NonTerminal &&
		        ctx.isVarFree((NonTerminal)clause.getElements().get(i))) {
		      Util.debug("no vars in ", clause);
		      return result;
		    }
		  }
		}
		
    Set<Pair<Term,Substitution>> pairs = new HashSet<Pair<Term,Substitution>>();
		
		List<Abstraction> abs = new ArrayList<Abstraction>();
		Term bare = Term.getWrappingAbstractions(term, abs);
    Application appTerm = this.getFreshAdaptedRuleTerm(abs, null);
    Term goalTerm = appTerm.getArguments().get(premises.size());
    
		if (isAssumption()) {
      Util.debug("** On line (for assumption) ", source.getLocation().getLine());
      // assumption rules, unlike all other rules,
      // have abstractions in the goal:
      List<Abstraction> newAbs = new ArrayList<Abstraction>();
      Term bareGoal = Term.getWrappingAbstractions(goalTerm, newAbs);
      Term subject = Term.wrapWithLambdas(abs, Facade.App(getRuleAppConstant(), bare));
		  if (clause.getRoot() != null) {
        List<Term> newTypes = new ArrayList<Term>();
        for (Abstraction a : newAbs) {
          newTypes.add(a.getArgType());
        }
        List<Term> oldTypes = newTypes;
		    if (appVarIndex >= 0) { // if no variables (error, or extension, don't try
		      Util.debug("** bare = ", bare, ", appVarIndex = ", appVarIndex);
		      Term wouldBeVar = ((Application)bare).getArguments().get(appVarIndex);
		      if (ctx.relaxationVars != null && ctx.relaxationVars.contains(wouldBeVar)) {
		        Util.debug("\t !! would be var: ",wouldBeVar," is already identified as a relax var.");
		        if (ctx.isRelaxationInScope(clause.getRoot(), (FreeVar)wouldBeVar)) {
		          Util.debug("cannot match newly because variable is already matched: ",wouldBeVar);
		          oldTypes = null; // i.e. not possible, skip this possibility
		        } else { 
		          oldTypes = ctx.getRelaxationTypes((FreeVar)wouldBeVar);
		          Util.debug("newTypes = ",newTypes);
		          Util.debug("oldTypes = ",oldTypes);
		        }
		      }
		    }
	      if (oldTypes != null && oldTypes.get(0).equals(newTypes.get(0))) {
	        Substitution adaptSub = new Substitution();
	        term.bindInFreeVars(newTypes, adaptSub);
	        Term adaptedSubject = Term.wrapWithLambdas(newAbs, subject.substitute(adaptSub));
	        // pattern = \assumpt . \context . goal(^size(context))
	        Term newGoal = Term.wrapWithLambdas(abs, Facade.App(getRuleAppConstant(), bareGoal.incrFreeDeBruijn(abs.size())));
	        Term pattern = Term.wrapWithLambdas(newGoal.substitute(adaptSub),oldTypes);
	        Util.debug("adaptSub = ", adaptSub);
	        checkCaseApplication(ctx,pairs, adaptedSubject,pattern, adaptedSubject, adaptSub, source);
	      }
	   } else {
		    Util.debug("no root, so no special assumption rule");
		  }
      /* now we try to find the assumption goals inside the existing context */
      tryInsert: for (int i = abs.size() - newAbs.size(); i >=0; --i) {
        // make sure types match:
        for (int k=0; k < newAbs.size(); ++k) {
          Constant oldFam = abs.get(k+i).getArgType().baseTypeFamily();
          Constant newFam = newAbs.get(k).getArgType().baseTypeFamily();
          if (!newFam.equals(oldFam)) {
            // incompatible: don't even try (can get type error during unification)
            continue tryInsert;
          }
        }
        int j = i + newAbs.size();
        Term shiftedGoal = Facade.App(getRuleAppConstant(),bareGoal.incrFreeDeBruijn(abs.size()-j));
        Term pattern = Term.wrapWithLambdas(abs,Term.wrapWithLambdas(newAbs, Term.wrapWithLambdas(abs, shiftedGoal,j,abs.size())),0,i);
        checkCaseApplication(ctx,pairs, subject,pattern, subject, null, source);
      }		  
		} else { // not assumption
      Util.debug("** On line (non assumption) ",source.getLocation().getLine());
      checkCaseApplication(ctx, pairs, Term.wrapWithLambdas(abs, appTerm), goalTerm, bare, null, source);
		}
		return pairs;
	}
	
  /** Checks if this rule applies to term, assuming ruleTerm is the term for the rule
   * and appliedTerm is the rule term built up from term.  
   * @param ctx global context: must not be null
   * @param result place to put any resulting pair
   * @param term full pattern to use (after substituting with unifier)
   * @param pattern kernel of pattern
   * @param subject kernel of subject
   * @param adaptSub adaptation substitution (may be null if no adaptation)
   * @param source location to drop errors
   */
  private void checkCaseApplication(Context ctx, Set<Pair<Term,Substitution>> result,
      Term term, Term pattern, Term subject, Substitution adaptSub, Node source) {
    Util.debug("pattern ", pattern);
    Util.debug("subject ",subject);
    Substitution sub = null;
    try {
      sub = pattern.unify(subject);
      Util.debug("found sub ", sub, " for case analyzing ", term, " with rule ", getName());
    } catch (UnificationIncomplete e) {
      Util.debug("unification incomplete on ", pattern, " and ", subject);
      ErrorHandler.recoverableError("Unification incomplete for case " + getName(), source, "SASyLF tried to unify " + e.term1 + " and " + e.term2);
    } catch (UnificationFailed e) {
      Util.debug("failure: " + e.getMessage());
      Util.debug("unification failed on ", pattern, " and ", subject);
      sub = null;
    }
    if (sub != null) {
      // if (adaptSub != null) sub.compose(adaptSub);
      Util.debug("at check, adaptSub = ",adaptSub);
      if (!ctx.canCompose(sub)) return;
      Util.debug("\t added result: ", term, sub);
      result.add(new Pair<Term,Substitution>(term.substitute(sub),sub));
    }
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

