package edu.cmu.cs.sasylf.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Util;

/**
 * A class describing how one context can be relaxed into another.
 * Relaxation is form of weakening in which a member of the LF
 * context which has been realized is pushed back into the background.
 * <p>
 * For example, after determining that the LF context has the assumption
 * <tt>x:T</tt> where <tt>x</tt> is what the variable <tt>t</tt> is,
 * then we can relax the context <tt>Gamma', x:T</tt> to <tt>Gamma</tt>,
 * and the term <tt>Gamma', x:T |- x + 3 : Nat</tt> to <tt>Gamma |- t + 3 : Nat</tt>.
 */
public class Relaxation {

  private final List<Term>     types;
  private final List<FreeVar>  values;
  private final NonTerminal    result;
  
  public Relaxation(List<Abstraction> abs, List<FreeVar> ts, NonTerminal r) {
    types = new ArrayList<Term>();
    for (Abstraction a : abs) {
      types.add(a.getArgType());
    }
    values = new ArrayList<FreeVar>(ts);
    result = r;
  }

  private Relaxation(List<Term> ts, List<FreeVar> vals, NonTerminal r, boolean ignored) {
    types = ts;
    values = vals;
    result = r;
  }
  
  public Relaxation(List<Pair<String,Term>> ps, FreeVar val, NonTerminal r) {
    types = new ArrayList<Term>(ps.size());
    values = new ArrayList<FreeVar>(ps.size());
    result = r;
    for (Pair<String,Term> p : ps) {
      types.add(p.second);
      if (values.isEmpty()) values.add(val);
      else values.add(null);
    }
  }
  
  /**
   * Relax a term using this relaxation, 
   * or return null if relaxation doesn't apply.
   * @param t  term to relax
   * @param res new context to relax to
   * @return relaxed term, or null
   */
  public Term relax(Term t, NonTerminal res) {
    if (res == null) {
      Util.tdebug("cannot relax because result needs to be null");
      return null;
    }
    if (!res.equals(result)) {
      Util.tdebug("cannot relax because ",res," != ",result);
      return null;
    }
    int n = types.size();
    List<Term> tempTypes = new ArrayList<Term>(types);
    for (int i=0; i < n; ++i) {
      Term ty = tempTypes.get(i);
      if (!(t instanceof Abstraction)) {
        Util.tdebug("cannot relax because after ",i," terms, term to relax is ",t);
        return null;
      }
      Abstraction a = (Abstraction)t;
      if (!(ty.equals(a.getArgType()))) {
        Util.tdebug("cannot relax because type ", a.getArgType(), " != ", ty);
        return null;
      }
      Term val = values.get(i);
      if (val == null) {
        t = a.getBody();
        if (t.hasBoundVar(1)) {
          Util.tdebug("cannot relax because no replacement for ",a.varName," is available");
        }
        t = t.incrFreeDeBruijn(-1);
      } else {
        t = Facade.App(a,val);
        List<Term> tempList = new ArrayList<Term>();
        tempList.add(val);
        for (int j=i+1; j < n; ++j) {
          Term oldType = tempTypes.get(j);
          tempList.add(0,null);
          Term newType = oldType.apply(tempList, j-i);
          Util.tdebug("relax type ",oldType," replaced with ",newType);
          tempTypes.set(j, newType);
        }
      }
    }
    return t;
  }
  
  public Relaxation substitute(Substitution sub) {
    boolean changed = false;
    List<Term> newTypes = null;
    List<FreeVar> newValues = null;
    int n = types.size();
    for (int i=0; i < n; ++i) {
      Term oldType = types.get(i);
      FreeVar oldValue = values.get(i);
      Term newType = oldType.substitute(sub);
      FreeVar newValue = oldValue == null ? null : (FreeVar)oldValue.substitute(sub);
      boolean needChange = oldType != newType || oldValue != newValue;
      if (needChange && !changed) {
        newTypes = new ArrayList<Term>(types.subList(0, i));
        newValues = new ArrayList<FreeVar>(values.subList(0, i));
        changed = true;
      }
      if (changed) {
        newTypes.add(newType);
        newValues.add(newValue);
      }
    }
    if (changed) {
      return new Relaxation(newTypes,newValues,result,false);
    }
    return this;
  }
  
  public void getFreeVars(Set<FreeVar> container) {
    for (FreeVar val : values) {
      if (val != null) container.add(val);
    }
    for (Term ty : types) {
      container.addAll(ty.getFreeVariables());
    }
  }
  
  public String checkConsistent(Context ctx) {
    for (FreeVar val : values) {
      if (val == null) continue;
      Util.debug("ctx.inputVars = ",ctx.inputVars);
      Util.debug("ctx.currentSub = ", ctx.currentSub);
      if (!ctx.inputVars.contains(val)) return "value not free: " + val;
    }
    for (Term ty : types) {
      if (!ctx.inputVars.containsAll(ty.getFreeVariables())) {
        return "non-input free vars in " + ty;
      }
    }
    return null;
  }
  
  @Override
  public String toString() {
    return "Relaxation(" + types + "," + values + "," + result + ")";
  }
}
