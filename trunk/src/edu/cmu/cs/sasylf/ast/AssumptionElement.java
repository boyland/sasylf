/**
 * 
 */
package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Util.debug;

import java.io.PrintWriter;
import java.util.List;

import edu.cmu.cs.sasylf.grammar.Symbol;
import edu.cmu.cs.sasylf.term.Pair;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Util;

/**
 * A syntax element (binding, variable or nonterminal) that is bound in a context
 */
public class AssumptionElement extends Element {

  public AssumptionElement(Location l, Element e, Element assumes) {
    super(l);
    while (e instanceof Clause && ((Clause)e).getElements().size() == 1) {
      Clause cl = (Clause)e;
      e = cl.getElements().get(0);
    }
    base = e;
    context = assumes;
  }

  @Override
  public ElementType getType() {
    return base.getType();
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
    context = context.typecheck(ctx);
    if (context instanceof Clause) {
      context = ((Clause)context).computeClause(ctx, false);
    }
    base = base.typecheck(ctx);
    if (base instanceof Clause) {
      base = ((Clause)base).computeClause(ctx,false);
    }
    return this;
  }

  @Override
  public void prettyPrint(PrintWriter out, PrintContext ctx) {
    base.prettyPrint(out,ctx);
    out.print(" assumes ");
    context.prettyPrint(out,ctx);
  }

  public NonTerminal getRoot() {
    if (context == null) return null;
    if (context instanceof NonTerminal) return ((NonTerminal)context);
    if (context instanceof ClauseUse) return ((ClauseUse)context).getRoot();
    throw new RuntimeException("no root for assumption element: " + this);
  }
  
  @Override
  public Fact asFact(Context ctx, Element assumes) {
    Fact f = base.asFact(ctx, null);
    if (context == null) return f;
    if (f instanceof SyntaxAssumption) {
      SyntaxAssumption sa = (SyntaxAssumption)f;
      sa.setContext(context);
      return sa;
    } else {
      ErrorHandler.report("'assumes' can only be used with syntax", this);
    }
    return null;
  }

  @Override
  protected Term computeTerm(List<Pair<String, Term>> varBindings) {
    debug("Compute: " + this);
    int initialBindingSize = varBindings.size();
    if (context instanceof ClauseUse)
      ((ClauseUse)context).readAssumptions(varBindings, base.getType() instanceof Judgment);
    Term t = base.computeTerm(varBindings);
    t = ClauseUse.newWrap(t, varBindings, initialBindingSize);
    while (varBindings.size() > initialBindingSize) {
      varBindings.remove(varBindings.size()-1);
    }
    Util.debug("  result = " + t);
    return t;
  }
  
  
  @Override
  public Term adaptTermTo(Term term, Term matchTerm, Substitution sub) {
    return super.adaptTermTo(term, matchTerm, sub);
  }

  public Element getAssumes() {
    return context;
  }
  public Element getBase() {
    return base;
  }

  private Element context;
  private Element base;
}
