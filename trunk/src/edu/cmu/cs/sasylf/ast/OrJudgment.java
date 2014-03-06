package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.sasylf.util.ErrorHandler;

public class OrJudgment extends Judgment {
  // TODO: COmplete implementation of OR.
  public static class OrTerminal extends Terminal {
    public OrTerminal(Location loc) {
      super("'or'",loc);
    }

    @Override
    public void prettyPrint(PrintWriter out, PrintContext ctx) {
      out.print("or");
    }
    
  }
  
  private static Terminal makeOrTerminal(Location loc) {
    return new OrTerminal(loc);
  }
  
  public static void addOr(Clause cl, Location or, Clause more) {
    cl.getElements().add(new OrTerminal(or));
    cl.getElements().addAll(more.getElements());
  }
  
  private OrJudgment(Location l, List<Judgment> parts) {
    super(null,makeName(parts),new ArrayList<Rule>(),makeForm(l,parts),findAssume(l,parts));
    String name = super.getName();
    this.parts = parts;
    int u = 0;
    List<Clause> premises = new ArrayList<Clause>();
    List<Element> concElems = new ArrayList<Element>();
    for (Judgment j : parts) {
      if (!premises.isEmpty()) {
        concElems.add(makeOrTerminal(l));
      }
      List<Element> es = new ArrayList<Element>();
      NonTerminal root = j.getAssume();
      for (Element e : j.getForm().getElements()) {
        if (e instanceof NonTerminal && !e.equals(root)) {
          Syntax s = ((NonTerminal)e).getType();
          NonTerminal gen = new NonTerminal(s.toString()+ ++u,l);
          gen.setType(s);
          es.add(gen);
          concElems.add(gen);
        } else {
          es.add(e);
          concElems.add(e);
        }
      }
      premises.add(new ClauseUse(l,es,(ClauseDef)j.getForm()));
    }
    ClauseDef cd = new ClauseDef(super.getForm(), this, super.getName());
    super.setForm(cd);
    Clause result = new ClauseUse(l,concElems,cd);
    int i=1;
    for (Clause premise : premises) {
      ArrayList<Clause> premiseList = new ArrayList<Clause>(1);
      premiseList.add(premise);
      Rule rule = new Rule(l,name+"#"+i,premiseList,result);
      ++i;
      super.getRules().add(rule);
    }
    this.prettyPrint(new PrintWriter(System.out));
  }

  /**
   * Create the name for an "or" judgment for 
   * @param parts
   * @return
   */
  private static String makeName(List<Judgment> parts) {
    StringBuilder sb = new StringBuilder();
    sb.append("or");
    for (Judgment j : parts) {
      sb.append("-");
      sb.append(j.getName());
    }
    return sb.toString();
  }
  
  private static Clause makeForm(Location l, List<Judgment> parts) {
    Clause result = new Clause(l);
    boolean started = false;
    NonTerminal context = null;
    for (Judgment j : parts) {
      if (started) result.getElements().add(makeOrTerminal(l));
      else started = true;
      for (Element e : j.getForm().getElements()) {
        if (e instanceof NonTerminal && ((NonTerminal)e).getType().isInContextForm()) {
          if (context == null) {
            context = (NonTerminal)e;
          } else {
            if (!context.equals(e)) {
              ErrorHandler.report("All contexts in an 'or' judgment must be the same", l);
            }
          }
        }
        result.getElements().add(e);
      }
    }
    return result;
  }
  
  private static NonTerminal findAssume(Location loc, List<Judgment> parts) {
    NonTerminal result = null;
    for (Judgment j : parts) {
      NonTerminal a = j.getAssume();
      if (a != null) {
        if (result == null) result = a;
        else if (!result.equals(a)) {
          ErrorHandler.report("cannot conjoin judgments with different assumptions", loc);
        }
      }
    }
    return result;
  }
  
  private static Map<List<Judgment>,OrJudgment> cache = new HashMap<List<Judgment>,OrJudgment>();
  
  /**
   * Generate an "or" judgment for 'or'ing a series of judgments together.
   * @param loc location where generation was (first) required
   * @param parts list of judgments 
   * @return judgment that is the disjunction of the parts
   */
  public static OrJudgment makeOrJudgment(Location loc, Context ctx, List <Judgment> parts) {
    OrJudgment result = cache.get(parts);
    if (result == null) {
      parts = new ArrayList<Judgment>(parts); // defensive programming
      result = new OrJudgment(loc,parts);
      result.defineConstructor(ctx);
      result.typecheck(ctx);
      cache.put(parts,result);
    }
    return result;
  }
  
  public static OrJudgment makeEmptyOrJudgment(Location loc) {
    List<Judgment> empty = Collections.<Judgment>emptyList();
    OrJudgment result = cache.get(empty);
    if (result == null) {
      result = new OrJudgment(loc,empty);
      cache.put(empty, result);
    }
    return result;
  }

  private List<Judgment> parts;
  
  @Override
  public void defineConstructor(Context ctx) {
    super.getForm().typecheck(ctx);
  }
  
  
  @Override
  public void typecheck(Context ctx) {
    super.typecheck(ctx);
    for (Rule r : super.getRules()) {
      r.typecheck(ctx, this);
    }
  }

  public List<Judgment> getJudgments() { return parts; }
}
