package edu.cmu.cs.sasylf.ast;

import java.util.List;

/**
 * Common superclass for and/or judgments.
 */
public abstract class AndOrJudgment extends Judgment {
  protected List<Judgment> parts;

  public AndOrJudgment(Location loc, String n, List<Rule> l, Clause c,
      NonTerminal a) {
    super(loc, n, l, c, a);
  }

  public AndOrJudgment(Location loc, String n, Clause s, NonTerminal a) {
    super(loc, n, s, a);
  }

  public abstract Terminal makeSeparator(Location l);
  
  @Override
  public void defineConstructor(Context ctx) {
    this.getForm().typecheck(ctx);
    ctx.prodMap.put(getName(), (ClauseDef)getForm());
    ctx.judgMap.put(getName(), this);
  }

  @Override
  public void typecheck(Context ctx) {
    super.typecheck(ctx);
    for (Rule r : this.getRules()) {
      r.typecheck(ctx, this);
    }
  }

  public List<Judgment> getJudgments() { return parts; }

}