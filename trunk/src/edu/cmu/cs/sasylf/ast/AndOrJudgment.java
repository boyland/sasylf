package edu.cmu.cs.sasylf.ast;

import java.util.List;

import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Util;

/**
 * Common superclass for and/or judgments.
 */
public abstract class AndOrJudgment extends Judgment {
  protected List<Judgment> parts;

  public AndOrJudgment(Location loc, String n, List<Rule> l, Clause c,
      NonTerminal a) {
    super(loc, n, l, c, a);
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
    for (Judgment j : getJudgments()) {
      Util.debug("subordination: ", j.typeTerm(), " < ", typeTerm());
      FreeVar.setAppearsIn(j.typeTerm(), typeTerm());
    }
  }

  public List<Judgment> getJudgments() { return parts; }

}