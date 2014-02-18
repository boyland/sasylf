package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;

import edu.cmu.cs.sasylf.util.ErrorHandler;

/**
 * A syntax assumption with a context, and maybe bindings
 * e.g.  t[x] assumes Gamma, x:T
 * or    t[x]     (as part of a syntax case pattern such as "fn x => t[x]")
 */
public class BindingAssumption extends SyntaxAssumption {

  public BindingAssumption(Binding b, Clause assumes) {
    super(b.getNonTerminal(), assumes);
    binding = b;
  }
  
  public BindingAssumption(Binding b) {
    this(b,null);
  }

  @Override
  public int hashCode() {
    return binding.hashCode();
  }
  
  @Override
  public boolean equals(Object other) {
    if (other instanceof BindingAssumption) {
      BindingAssumption ba = (BindingAssumption)other;
      Clause c = getContext();
      Clause co = ba.getContext();
      return ba.binding.equals(binding) && 
        (c == co || contextIsUnknown() || ba.contextIsUnknown() || (c != null && c.equals(co))); 
    }
    return false;
  }
  
  @Override
  public void prettyPrint(PrintWriter out) {
    binding.prettyPrint(out);
    if (super.getContext()!= null) {
      out.print(" assumes ");
      if (super.getContext().getElements().size() == 0) out.print("?");
      else super.getContext().prettyPrint(out);
    }
  }

  @Override
  public Element getElementBase() {
    return binding;
  }
  
  @Override
  public void typecheck(Context ctx) {
    binding.typecheck(ctx);
    if (ctx == null) { // XXX: Not sure if this should always happen.  Originally only if being added to map.
      for (Element e : binding.getElements()) {
        if (!(e instanceof Variable)) {
          if (e != null) throw new IllegalStateException("I don't know what is happening");
          ErrorHandler.report("cannot match a substituted term", this);
        }
      }
    }
    super.typecheck(ctx);
  }
  
  private Binding binding;
}
