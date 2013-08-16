package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;

import edu.cmu.cs.sasylf.util.ErrorHandler;


public class SyntaxAssumption extends Fact {
	public SyntaxAssumption(String n, Location l, boolean isTheoremArg, Clause assumes) {
		this(new NonTerminal(n, l));
		this.isTheoremArg = isTheoremArg;
		this.context = assumes;
		/*super(n, l);
		nonTerminal = new NonTerminal(n,l);*/
	}
	public SyntaxAssumption(String n, Location l) {
		this(new NonTerminal(n, l));
	}
  public SyntaxAssumption(String n, Location l, Clause assumes) {
    this(new NonTerminal(n, l),assumes);
  }
  public SyntaxAssumption(NonTerminal nt, Clause assumes) {
    super(nt.getSymbol(), nt.getLocation());
    nonTerminal = nt;
    context = assumes;
  }
	public SyntaxAssumption(NonTerminal nt) {
		this(nt,null);
	}
	
	public Syntax getSyntax() { return nonTerminal.getType(); }
	public final Element getElement() {
	  Element base = getElementBase();
	  if (context == null) return base;
	  /*if (context.getElements().size() == 1 && 
	      context.getElements().get(0) instanceof NonTerminal)
	    return base;
	  else*/ return new AssumptionElement(getLocation(),base,context);
	}
	public Element getElementBase() { return nonTerminal; }
	public boolean isTheoremArg() { return isTheoremArg; }

	@Override
	public int hashCode() { return nonTerminal.hashCode(); }

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (obj.getClass() != SyntaxAssumption.class) return false;
    SyntaxAssumption sa = (SyntaxAssumption) obj;
    Clause c = getContext();
    Clause co = sa.getContext();
		return nonTerminal.equals(sa.nonTerminal) &&
		  (c == co || contextIsUnknown() || sa.contextIsUnknown() || (c != null && c.equals(co))); 
	}

	protected boolean contextIsUnknown() {
	  return context != null && context.getElements().size() == 0;
	}
	
	public void prettyPrint(PrintWriter out) {
		out.print(getName());
		if (context!= null) {
		  out.print(" assumes ");
		  if (contextIsUnknown()) out.print("?");
		  else context.prettyPrint(out);
		}
	}

	@Override
	public void typecheck(Context ctx) {
    if (context != null) {
      context = (Clause)context.typecheck(ctx);
      Element computed = context.computeClause(ctx,false);
      if (!(computed instanceof NonTerminal)) {
        if (computed instanceof Clause) context = (Clause)computed;
        else ErrorHandler.report("assumes must be a nonterminal or clause", this);
      }
    }
		Element e = nonTerminal.typecheck(ctx);
		if (e != nonTerminal)
			ErrorHandler.report("No syntax match for " + getName(), this);
		/*String strippedName = Util.stripId(getName());
		syntax = ctx.synMap.get(strippedName);
		if (syntax == null)*/
	}

	public void setContext(Clause c) { context = c; }
	public Clause getContext() { return context; }
	
	private NonTerminal nonTerminal;
	private boolean isTheoremArg = false;
  private Clause context;

}
