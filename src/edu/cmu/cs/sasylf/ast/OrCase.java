package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Util;

public class OrCase extends Case {

	public OrCase(Location l, Derivation d) {
		super(l,d.getLocation(),d.getEndLocation());
		premise = d;
	}

	@Override
	public void prettyPrint(PrintWriter out) {
		out.println("case or\n");
		premise.prettyPrint(out);
		out.println("\n\nis\n");
		super.prettyPrint(out);
	}

	private Term getOrClausePremise(Term result) {
		if (result instanceof Application) { // simple: the application of an or-rule
			return ((Application)result).getArguments().get(0);
		} else if (result instanceof Abstraction) {
			List<Abstraction> wraps = new ArrayList<Abstraction>();
			Application inside = (Application)Term.getWrappingAbstractions(result, wraps);
			Judgment pj = (Judgment)premise.getClause().getType();
			Term base = getOrClausePremise(inside); 
			if (pj.getAssume() == null) return base;
			return Term.wrapWithLambdas(wraps, base);
		} else {
			throw new RuntimeException("Cannot get or case from " + result);
		}
	}
	
	@Override
	public void typecheck(Context parent, Pair<Fact,Integer> isSubderivation) {
		Context ctx = parent.clone();
		premise.typecheck(ctx);
		premise.addToDerivationMap(ctx);
		premise.getClause().checkBindings(ctx.bindingTypes, this);

		if (!(ctx.currentCaseAnalysisElement instanceof OrClauseUse)) {
			ErrorHandler.error(Errors.OR_CASE_NOT_APPLICABLE, this);
		}

		Clause cl = premise.getClause();
		Util.verify(cl.getType() instanceof Judgment, "should have been handled already");

		Term t = cl.asTerm();
		t = ctx.toTerm(cl);

		boolean found=false;
		
		for (ClauseUse cu : ((OrClauseUse)ctx.currentCaseAnalysisElement).getClauses()) {
			if (t.equals(ctx.toTerm(ContextJudgment.unwrap(cu)))) found = true;
		}
		if (!found) {
			ErrorHandler.error(Errors.CASE_UNNECESSARY, this, "suggestion: remove it");
		}
		found= false;
		
		NonTerminal subjectRoot = ctx.currentCaseAnalysisElement.getRoot();
		NonTerminal patternRoot = cl.getRoot();
		if (subjectRoot == null) {
			if (patternRoot != null) {
				ErrorHandler.recoverableError(Errors.CASE_CONTEXT_ADDED, patternRoot.toString(), premise);
			}
		} else if (patternRoot == null) {
			if (((Judgment)cl.getType()).getAssume() != null) {
				ErrorHandler.recoverableError(Errors.CONTEXT_DISCARDED, "" + subjectRoot, premise);
			}
		} else if (!subjectRoot.equals(patternRoot)) {
			ErrorHandler.recoverableError(Errors.CASE_CONTEXT_CHANGED_EX, subjectRoot.toString(), premise);
		}

		for (Map.Entry<CanBeCase,Set<Pair<Term,Substitution>>> e : ctx.caseTermMap.entrySet()) {
			if (e.getValue().isEmpty()) continue;
			// Rule r = (Rule)e.getKey();
			// Clause p = r.getPremises().get(0);
			// System.out.println("p.getType = " + r.getJudgment().getName());
			// System.out.println("cl.getJudgment = " + cl.getType().toString());
			// if (p.getType() != cl.getType()) continue; // doesn't work with context judgments
			Pair<Term,Substitution> caseResult = e.getValue().iterator().next();
			Term pt = getOrClausePremise(caseResult.first);
			pt = ContextJudgment.invertContextJudgments(ctx, pt);
			if (pt.equals(t.substitute(caseResult.second))) {
				e.getValue().remove(caseResult);
				found = true;
				ctx.composeSub(caseResult.second);
				break;
			}
		}

		if (!found) {
			ErrorHandler.error(Errors.CASE_REDUNDANT, this, "suggestion: remove it");
		}

		super.typecheck(ctx, isSubderivation);
	}

	Derivation premise;
}
