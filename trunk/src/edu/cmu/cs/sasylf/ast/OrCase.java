package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Pair;

public class OrCase extends Case {

  public OrCase(Location l, Derivation d) {
    super(l);
    premise = d;
  }

  @Override
  public void prettyPrint(PrintWriter out) {
    out.println("case or\n");
    premise.prettyPrint(out);
    out.println("\n\nis\n");
    super.prettyPrint(out);
  }

  @Override
  public void typecheck(Context parent, Pair<Fact,Integer> isSubderivation) {
    Context ctx = parent.clone();
    premise.typecheck(ctx);
    premise.addToDerivationMap(ctx);
    premise.getClause().checkBindings(ctx.bindingTypes, this);
    
    if (!(ctx.currentCaseAnalysisElement instanceof OrClauseUse)) {
      ErrorHandler.report(Errors.OR_CASE_NOT_APPLICABLE, this);
    }
    
    Clause cl = premise.getClause();
    if (!(cl instanceof ClauseUse) || !(cl.getType() instanceof Judgment)) {
      ErrorHandler.report(Errors.OR_SYNTAX, this);
    }
    
    Term t = cl.asTerm();
    t = ctx.toTerm(cl);
    
    boolean found=false;
    
    for (Map.Entry<CanBeCase,Set<Pair<Term,Substitution>>> e : ctx.caseTermMap.entrySet()) {
      if (e.getValue().isEmpty()) continue;
      Rule r = (Rule)e.getKey();
      Clause p = r.getPremises().get(0);
      // System.out.println("p.getType = " + r.getJudgment().getName());
      // System.out.println("cl.getJudgment = " + cl.getType().toString());
      if (p.getType() != cl.getType()) continue;
      Pair<Term,Substitution> caseResult = e.getValue().iterator().next();
      Term pt = ((Application)caseResult.first).getArguments().get(0);
      if (pt.equals(t.substitute(caseResult.second))) {
        e.getValue().remove(caseResult);
        found = true;
        ctx.composeSub(caseResult.second);
        break;
      } else {
        // System.out.println(pt + " != " + t.substitute(caseResult.second));
      }
    }
    
    if (!found) {
      ErrorHandler.report(Errors.INVALID_CASE, "Found no match for derivation in disjunction", this);
    }
    
    super.typecheck(ctx, isSubderivation);
  }

  Derivation premise;
}
