package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.term.Facade.Abs;
import static edu.cmu.cs.sasylf.util.Util.*;
import static edu.cmu.cs.sasylf.ast.Errors.*;

import java.util.*;
import java.io.*;

import edu.cmu.cs.sasylf.ast.NonTerminal;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Pair;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.util.ErrorHandler;


public class SyntaxCase extends Case {
	public SyntaxCase(Location l, Clause c) { super(l); conclusion = c; }

	public Clause getConclusion() { return conclusion; }

	public void prettyPrint(PrintWriter out) {
		out.print("case ");
		conclusion.prettyPrint(out);
		out.println(" is ");

		super.prettyPrint(out);
	}

	public void typecheck(Context ctx, boolean isSubderivation) {
		debug("    ******* case line " + getLocation().getLine());
		Map<String, List<ElemType>> oldBindingTypes = ctx.bindingTypes;
		conclusion.typecheck(ctx);

		// make sure we were case-analyzing a nonterminal
		if (!(ctx.currentCaseAnalysisElement instanceof NonTerminal))
			ErrorHandler.report(SYNTAX_CASE_FOR_DERIVATION, this);
		
		//if (ctx.currentCaseAnalysisElement instanceof NonTerminal) {
			edu.cmu.cs.sasylf.grammar.NonTerminal nt = ((NonTerminal)ctx.currentCaseAnalysisElement).getType().getSymbol();
			edu.cmu.cs.sasylf.grammar.Grammar g = new edu.cmu.cs.sasylf.grammar.Grammar(nt, ctx.getGrammar().getRules());
			Element concElem = conclusion.computeClause(ctx, false, g);
			if (concElem instanceof Clause) {
				conclusion = (Clause) concElem;
			} else {
				if (concElem instanceof NonTerminal)
					// must have been analyzing a nonterminal n and case analyzed with a case of n'
					ErrorHandler.report(NONTERMINAL_CASE, "Case " + conclusion + " is a nonterminal; it must be a decomposition of " + ctx.currentCaseAnalysisElement, this);			
				else if (concElem instanceof Variable)
					// must have been analyzing a nonterminal t and case analyzed with a case of a variable x
					ErrorHandler.report(UNBOUND_VAR_CASE, "Case " + conclusion + " is an unbound variable and so cannot stand on its own.\n\tTry case-analyzing on a judgment instead; in the variable rule case, the variable will be bound in the context.", this);
				else
					ErrorHandler.report("Case analysis of syntax may only be a syntax clause, not a variable, nonterminal, or binding", this);
			}
		/*} else {
			conclusion = (Clause) conclusion.computeClause(ctx.getGrammar());
		}*/
		//conclusion = new ClauseUse(conclusion, ctx.parseMap);
		conclusion.checkBindings(ctx.bindingTypes, this);

		// look up case analysis for this rule
		Set<Pair<Term,Substitution>> caseResult = ctx.caseTermMap.get(((ClauseUse)conclusion).getConstructor());
		verify(caseResult != null && caseResult.size() <= 1, "internal invariant violated");
		if (caseResult.size() == 0)
			ErrorHandler.report(DUPLICATE_CASE, this);
			
		Pair<Term, Substitution> pair = caseResult.iterator().next();
		Term computedCaseTerm = pair.first;
		verify(caseResult.remove(pair), "internal invariant broken");

		
		// check that we match the computed case analysis term
		Term concTerm = conclusion.asTerm();
		try {
			debug("case unify " + concTerm + " and " + computedCaseTerm);
			Substitution computedSub = computedCaseTerm.instanceOf(concTerm);
		} catch (UnificationFailed uf) {
			ErrorHandler.report(INVALID_CASE, "Case does not apply to " + ctx.currentCaseAnalysisElement, this);
		}
		
		// make sure rule's conclusion unifies with the thing we're doing case analysis on
		concTerm = concTerm.substitute(ctx.currentSub);
		Substitution unifyingSub = null;
		try {
			unifyingSub = concTerm.unify(ctx.currentCaseAnalysis);
		} catch (UnificationFailed uf) {
			// shouldn't really get this far -- if we're going to fail, it would have been above
			ErrorHandler.report(INVALID_CASE, "Case " + conclusion + " is not actually a case of " + ctx.currentCaseAnalysisElement, this);			
		}
		
		// update the current substitution
		Substitution oldSub = new Substitution(ctx.currentSub);
		ctx.currentSub.compose(unifyingSub); // modifies in place
		
		// update the set of free variables
		Set<FreeVar> oldInputVars = ctx.inputVars;
		ctx.inputVars = new HashSet<FreeVar>(oldInputVars);
		ctx.inputVars.addAll(concTerm.getFreeVariables());
		debug("current case analysis: " + ctx.currentCaseAnalysis);
		if (ctx.currentCaseAnalysis instanceof FreeVar) // might be an expression, after substitution
			ctx.inputVars.remove((FreeVar)ctx.currentCaseAnalysis);

		// update the set of subderivations
		List<Fact> oldSubderivations = new ArrayList<Fact>(ctx.subderivations);
		if (isSubderivation) {
			// add each part of the clause to the list of subderivations
			ctx.subderivations.addAll(((ClauseUse)conclusion).getNonTerminals());
		}
		
		super.typecheck(ctx, isSubderivation);

		// restore the current substitution and input vars
		ctx.currentSub = oldSub;
		ctx.inputVars = oldInputVars;
		ctx.subderivations = oldSubderivations;
		ctx.bindingTypes = oldBindingTypes;
	}

	private Clause conclusion;
}

