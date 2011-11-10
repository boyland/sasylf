package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.util.ErrorHandler;

/**
 * A syntax assumption with a context, and maybe bindings
 * e.g.  t[x] assumes Gamma, x:T
 * or    t[x]     (as part of a syntax case pattern such as "fn x => t[x]")
 */
public class BindingAssumption extends SyntaxAssumption {
  
  public BindingAssumption(Binding b) {
    super(b.getNonTerminal());
    binding = b;
  }

  @Override
  public int hashCode() {
    return binding.hashCode();
  }
  
  @Override
  public boolean equals(Object other) {
    if (other instanceof BindingAssumption) {
      BindingAssumption ba = (BindingAssumption)other;
      return ba.binding == binding; 
    }
    return false;
  }
  
  @Override
  public Element getElement() {
    return binding;
  }
  
  @Override
  public void typecheck(Context ctx, boolean addToMap) {
    binding.typecheck(ctx);
    if (addToMap) {
      for (Element e : binding.getElements()) {
        if (!(e instanceof Variable)) {
          ErrorHandler.report("cannot match a substituted term", this);
        }
      }
    }
    super.typecheck(ctx, addToMap);
  }
  
  private Binding binding;
}
