/**
 * 
 */
package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Util.*;

import java.io.PrintWriter;
import java.util.List;

import edu.cmu.cs.sasylf.grammar.Symbol;
import edu.cmu.cs.sasylf.term.Pair;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;

/**
 * A syntax element (binding, variable or nonterminal) that is bound in a context
 */
public class AssumptionElement extends Element {

  public AssumptionElement(Location l, Element e, Clause assumes) {
    super(l);
    while (e instanceof Clause && ((Clause)e).getElements().size() == 1) {
      Clause cl = (Clause)e;
      e = cl.getElements().get(0);
    }
    base = e;
    context = assumes;
  }

  @Override 
  public Term getTypeTerm() {
    return base.getTypeTerm();
  }
  
  @Override
  public ElemType getElemType() {
    return base.getElemType();
  }

  @Override
  public Symbol getGrmSymbol() {
    return base.getGrmSymbol();
  }

  @Override
  protected String getTerminalSymbolString() {
    return base.getTerminalSymbolString();
  }

  @Override
  public Element typecheck(Context ctx) {
    context = (Clause)context.typecheck(ctx);
    Element e = context.computeClause(ctx, false);
    if (e instanceof Clause) context = (Clause)e;
    base = base.typecheck(ctx);
    return this;
  }

  @Override
  public void prettyPrint(PrintWriter out, PrintContext ctx) {
    base.prettyPrint(out,ctx);
    out.print(" assumes ");
    context.prettyPrint(out,ctx);
  }

  @Override
  protected Term computeTerm(List<Pair<String, Term>> varBindings) {
    debug("Compute: " + this);
    int initialBindingSize = varBindings.size();
    if (context instanceof ClauseUse)
      ((ClauseUse)context).readAssumptions(varBindings, true);
    Term t = base.computeTerm(varBindings);
    t = ClauseUse.newWrap(t, varBindings, initialBindingSize);
    while (varBindings.size() > initialBindingSize) {
      varBindings.remove(varBindings.size()-1);
    }
    debug("  result = " + t);
    return t;
  }
  
  
  @Override
  public Term adaptTermTo(Term term, Term matchTerm, Substitution sub) {
    System.out.println("AssumptionElement.adaptTermTo ?? ");
    return super.adaptTermTo(term, matchTerm, sub);
  }

  public Clause getAssumes() {
    return context;
  }
  public Element getBase() {
    return base;
  }

  private Clause context;
  private Element base;
}
