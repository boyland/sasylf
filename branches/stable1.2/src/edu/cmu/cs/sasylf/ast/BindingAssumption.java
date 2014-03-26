package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.util.ErrorHandler;

/**
 * A syntax assumption with a context, and maybe bindings
 * e.g.  t[x] assumes Gamma, x:T
 * or    t[x]     (as part of a syntax case pattern such as "fn x => t[x]")
 */
public class BindingAssumption extends NonTerminalAssumption {

  public BindingAssumption(Binding b, Element assumes) {
    super(b.getNonTerminal(), assumes);
    binding = b;
  }
  
  public BindingAssumption(Binding b) {
    this(b,null);
  }

  @Override
  public Element getElementBase() {
    return binding;
  }
  
  @Override
  public void typecheck(Context ctx) {
    super.typecheck(ctx);
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
