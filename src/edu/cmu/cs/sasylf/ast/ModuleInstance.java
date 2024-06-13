package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.sasylf.module.Module;

public class ModuleInstance implements Module {

  private List<Part> parts = new ArrayList<Part>();

  @Override 
  public String getName() {
    // TODO Auto-generated method stub
    return "ModuleInstance";
  }

  @Override
  public boolean isAbstract() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void prettyPrint(PrintWriter out) {
    // TODO Auto-generated method stub
    out.println("ModuleInstance");
    for (Part part : parts) {
      part.prettyPrint(out);
    }
  }

  @Override
  public boolean typecheck() {
    // TODO Auto-generated method stub
    return true;
  }

  @Override
  public void collectTopLevel(Collection<? super Node> things) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'collectTopLevel'");
  }

  @Override
  public void collectRuleLike(Map<String, ? super RuleLike> map) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'collectRuleLike'");
  }

  @Override
  public Object getDeclaration(Context ctx, String name) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getDeclaration'");
  }
  
}
