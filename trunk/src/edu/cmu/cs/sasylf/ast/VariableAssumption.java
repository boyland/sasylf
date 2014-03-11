package edu.cmu.cs.sasylf.ast;

public class VariableAssumption extends SyntaxAssumption {

  public VariableAssumption(String n, Location l, Element assumes) {
    super(n, l, assumes);
    variable = new Variable(n,l);
  }
  
  public VariableAssumption(Variable v) {
    super(v.getSymbol(),v.getLocation(),null);
    variable = v;
  }

  @Override
  public Element getElementBase() {
    return variable;
  }

  private Variable variable;
}
