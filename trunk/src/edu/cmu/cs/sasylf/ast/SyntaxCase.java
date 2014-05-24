package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Errors.EXTRA_CASE;
import static edu.cmu.cs.sasylf.util.Errors.INVALID_CASE;
import static edu.cmu.cs.sasylf.util.Errors.NONTERMINAL_CASE;
import static edu.cmu.cs.sasylf.util.Errors.REUSED_CONTEXT;
import static edu.cmu.cs.sasylf.util.Errors.SYNTAX_CASE_FOR_DERIVATION;
import static edu.cmu.cs.sasylf.util.Util.debug;
import static edu.cmu.cs.sasylf.util.Util.verify;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Util;


public class SyntaxCase extends Case {
	public SyntaxCase(Location l, Clause c) { super(l); conclusion = c; }
	public SyntaxCase(Location l, Clause c, Clause ca) { this(l,c); assumes = ca; }
	
	public Clause getConclusion() { return conclusion; }

	public void prettyPrint(PrintWriter out) {
		out.print("case ");
		conclusion.prettyPrint(out);
		out.println(" is ");

		super.prettyPrint(out);
	}

	public void typecheck(Context parent, Pair<Fact,Integer> isSubderivation) {
	  Context ctx = parent.clone();
		debug("    ******* case line ", getLocation().getLine());
    conclusion.typecheck(ctx);

    // make sure we were case-analyzing a nonterminal
    if (!(ctx.currentCaseAnalysisElement instanceof NonTerminal) &&
        !(ctx.currentCaseAnalysisElement instanceof AssumptionElement))
      ErrorHandler.report(SYNTAX_CASE_FOR_DERIVATION, this);
    
    NonTerminal caseNT;
    Element caseAssumptions = null; // These may be unnecessary
    if (ctx.currentCaseAnalysisElement instanceof AssumptionElement) {
      AssumptionElement ae = (AssumptionElement)ctx.currentCaseAnalysisElement;
      Element base = ae.getBase();
      if (base instanceof Binding) caseNT = ((Binding)base).getNonTerminal();
      else caseNT = (NonTerminal)base;
      caseAssumptions = ae.getAssumes();
    } else caseNT = (NonTerminal)ctx.currentCaseAnalysisElement;

    edu.cmu.cs.sasylf.grammar.NonTerminal nt = caseNT.getType().getSymbol();
    edu.cmu.cs.sasylf.grammar.Grammar g = new edu.cmu.cs.sasylf.grammar.Grammar(nt, ctx.getGrammar().getRules());
    Element concElem = conclusion.computeClause(ctx, false, g);
    Clause concDef = null;
    
    if (assumes != null) {
      assumes = assumes.typecheck(ctx);
      if (assumes instanceof Clause) {
        assumes = ((Clause)assumes).computeClause(ctx,false);
      }
    }
    
    if (concElem instanceof Variable) {
		  for (Clause c :  caseNT.getType().getClauses()) {
		    if (c.isVarOnlyClause()) {
		      concDef = c;
		    }
		  }
		  if (concDef == null) {
		      throw new InternalError("no variable clause for nonterminal: " + 
		          ctx.currentCaseAnalysisElement);
		  }
    } else if (concElem instanceof Clause) {
      ClauseUse concUse = (ClauseUse) concElem;
      concUse.checkBindings(ctx.bindingTypes, this);
      concDef = concUse.getConstructor();
      if (caseAssumptions == null) {
        //XXX: can't we add an assumption for a variable?
        if (assumes != null) ErrorHandler.report(Errors.INVALID_CASE, "Cannot add assumptions in case", this);
      } else if (assumes == null) {
        boolean lostAssumptions = false;
        if (caseAssumptions instanceof Clause) {
          for (Element ea: ((Clause)caseAssumptions).getElements()) {
            if (ea instanceof Variable) lostAssumptions = true;
          }
        }
        if (lostAssumptions) {
          ErrorHandler.report(Errors.INVALID_CASE, "Cannot change assumptions in case", this);
        }
      } else {
        if (!caseAssumptions.equals(assumes)) {
          ErrorHandler.report(Errors.INVALID_CASE, "Must keep assumptions identical in case", this);
        }
      }
      if (concUse.getConstructor().getType() != caseNT.getType()) {
        ErrorHandler.report(INVALID_CASE, "case given is not actually a case of " + caseNT, this);
      }
    } else if (concElem instanceof NonTerminal) {
      // must have been analyzing a nonterminal n and case analyzed with a case of n'
      ErrorHandler.report(NONTERMINAL_CASE, "Case " + conclusion + " is a nonterminal; it must be a decomposition of " + ctx.currentCaseAnalysisElement, this);     
    } else {
      // not clear when this error happens
      ErrorHandler.report("Case analysis of syntax may only be a syntax clause, not a variable, nonterminal, or binding", this);
    }		  
	
    // reuse code:
    if (assumes != null) concElem = new AssumptionElement(getLocation(),concElem,assumes);
    Term concTerm = concElem.asTerm();
    Util.debug("concTerm = ", concTerm);
    
    for (FreeVar fv : concTerm.getFreeVariables()) {
      if (ctx.inputVars.contains(fv)) {
        ErrorHandler.report(Errors.INVALID_CASE, "A case must use new variables, cannot reuse " + fv, this);
      }
    }
    
    // look up case analysis for this rule
    Set<Pair<Term,Substitution>> caseResult = ctx.caseTermMap.get(concDef);

    if (concElem instanceof ClauseUse) {
      verify(caseResult.size() <= 1, "internal invariant violated");
    }
    
    Term computedCaseTerm = null;
    
    for (Pair<Term, Substitution> pair : caseResult) {
      try {
        debug("case unify ", concTerm, " and ", pair.first);
        Substitution computedSub = pair.first.instanceOf(concTerm);
        debug("result is ", computedSub);
      } catch (UnificationFailed uf) {
        System.out.println("Case " + this + " does not apply to " + ctx.currentCaseAnalysisElement);
        continue;
      }
      computedCaseTerm = pair.first;
      verify(pair.second.getMap().size() == 0, "syntax case substitution should be empty, not " + pair.second);
      caseResult.remove(pair);
      break;
    }
    
    if (computedCaseTerm == null) {
      ErrorHandler.report(EXTRA_CASE, this);
    }

    Term adaptedCaseAnalysis = ctx.currentCaseAnalysis;

    int lambdaDifference =  computedCaseTerm.countLambdas() - adaptedCaseAnalysis.countLambdas();
    // TODO: SHould check matchTermForAdaptation (RuleCase code doesn't look right to me)
    if (lambdaDifference > 0) {
      if (lambdaDifference != 1) {
        ErrorHandler.report("New assumption can only add one variable",this);
      }
      // JTB: The code here is tricky because syntax adds just one variable,
      // but adaptation info must use both variables.
      verify(concElem instanceof AssumptionElement,"not an assumption element? " + concElem);
      AssumptionElement ae = (AssumptionElement)concElem;
      if (!(ae.getBase() instanceof Variable)) {
        ErrorHandler.report("new assumption can only be used with variable", this);
      }
      verify(ae.getAssumes() instanceof ClauseUse, "not a clause use? " + assumes);
      ClauseUse assumesClause = (ClauseUse)ae.getAssumes();
      NonTerminal newRoot = assumesClause.getRoot();
      if (ctx.isLocallyKnown(newRoot.getSymbol())) {
        ErrorHandler.report(REUSED_CONTEXT,"May not re-use context name " +newRoot, this);
      }

      // Substitution adaptationSub = new Substitution();
      List<Pair<String,Term>> varBindings = new ArrayList<Pair<String,Term>>();
      assumesClause.readAssumptions(varBindings, true);
      Relaxation relax = new Relaxation(varBindings,(FreeVar)ctx.currentCaseAnalysis,ctx.currentCaseAnalysisElement.getRoot());
      ctx.addRelaxation(newRoot, relax);
      
      Pair<String,Term> varBind = varBindings.get(0);
      Substitution adaptSub = new Substitution();
      adaptedCaseAnalysis.bindInFreeVars(varBind.second, adaptSub);
      adaptedCaseAnalysis = adaptedCaseAnalysis.substitute(adaptSub);
      adaptedCaseAnalysis = Facade.Abs(varBind.first,varBind.second, adaptedCaseAnalysis);
    }

		// make sure rule's conclusion unifies with the thing we're doing case analysis on
    // NB: need to handle the lambdaDifference here.
		concTerm = concTerm.substitute(ctx.currentSub);
		Substitution unifyingSub = null;
		try {
			unifyingSub = concTerm.unify(adaptedCaseAnalysis);
		} catch (UnificationFailed uf) {
		  throw new InternalError("Should not have unification error here!\n concTerm = " + concTerm + ", case analysis = "+ adaptedCaseAnalysis);
		}
		
		Util.debug("  unifyingSub = ",unifyingSub);
		
		// update the current substitution
		ctx.composeSub(unifyingSub); // modifies in place		
		
		super.typecheck(ctx, isSubderivation);

	}

	private Clause conclusion;
	private Element assumes;
}

