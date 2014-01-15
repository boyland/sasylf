package edu.cmu.cs.sasylf.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.ast.AndJudgment.AndTerminal;
import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Atom;
import edu.cmu.cs.sasylf.term.BoundVar;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Term;

/**
 * A class to convert terms back into elements
 * @author boyland
 */
public class TermPrinter {

  private final Context ctx;
  private final Element context;
  private final Location location;
  private final Map<FreeVar,NonTerminal> varMap = new HashMap<FreeVar,NonTerminal>();
  private final Set<FreeVar> used = new HashSet<FreeVar>();
  
  public TermPrinter(Context ctx, Element gamma, Location loc) {
    this.ctx = ctx;
    context = gamma;
    location = loc;
  }

  public Element asElement(Term x) {
    return asElement(x, new ArrayList<Variable>());
  }
  
  public Element asElement(Term x, List<Variable> vars) {
    if (x instanceof FreeVar) {
      if (varMap.containsKey(x)) return varMap.get(x);
      if (ctx.inputVars.contains(x)) return new NonTerminal(x.toString(),location);
      Term ty = ((FreeVar) x).getBaseType();
      if (!(ty instanceof Constant)) {
        throw new RuntimeException("base type of " + x + " = " + ty);
      }
      String base = ty.toString();
      for (int i=0; true; ++i) {
        FreeVar v = new FreeVar(base + i, ((FreeVar) x).getType());
        if (ctx.inputVars.contains(v)) continue;
        if (ctx.outputVars.contains(v)) continue;
        if (used.contains(v)) continue;
        used.add(v);
        NonTerminal result = new NonTerminal(v.toString(),location);
        varMap.put((FreeVar)x, result);
        return result;
      }
    } else if (x instanceof BoundVar) {
      BoundVar v = (BoundVar)x;
      return vars.get(vars.size()-v.getIndex()); // index is "one" based.
    } else if (x instanceof Constant) {
      return appAsClause((Constant)x, Collections.<Element>emptyList());
    } else if (x instanceof Application) {
      Application app = (Application)x;
      List<Element> args = new LinkedList<Element>();
      for (Term arg : app.getArguments()) {
        args.add(asElement(arg,vars));
      }
      Atom func = app.getFunction();
      if (func instanceof Constant) {
        return appAsClause((Constant)func,args);
      } else {
        return new Binding(location,(NonTerminal)asElement(func),args);
      }
    } else if (x instanceof Abstraction) {
      Abstraction abs = (Abstraction)x;
      Term ty = abs.varType;
      Syntax syn = ctx.synMap.get(ty.toString());
      Variable v = new Variable(syn.getVariable().toString()+(vars.size()/2),location);
      v.setType(syn);
      vars.add(v);
      Term body1 = abs.getBody();
      if (body1 instanceof Abstraction) {
        Abstraction abs2 = (Abstraction)body1;
        ClauseUse bindingClause = assumeTypeAsElement(abs2.varType, vars);
        vars.add(new Variable("<internal>",location));
        Element bodyElem = asElement(abs2.getBody(),vars);
        if (bodyElem instanceof ClauseUse) {
          ClauseUse cu = (ClauseUse)bodyElem;
          int ai = cu.getConstructor().getAssumeIndex();
          cu.getElements().set(ai, replaceAssume(bindingClause,cu.getElements().get(ai)));
        } else if (bodyElem instanceof AssumptionElement) {
          AssumptionElement ae = (AssumptionElement)bodyElem;
          return new AssumptionElement(location,ae.getBase(),replaceAssume(bindingClause,ae.getAssumes()));
        } else {
          return new AssumptionElement(location,bodyElem,bindingClause);
        }
        return bodyElem;
      } else {
        throw new RuntimeException("abstraction with only one arg?: " + x);
      }
    } else {
      throw new RuntimeException("unknown element: " + x);
    }
  }

  /**
   * Convert a term used as the type of the hypothetical
   * assumed along with the variable.  Convert it to an element.
   * @param assumeType the type of the hypothetical assumption
   * @param vars bindings for variables in scope.
   * @return element that gives the context as an environment.
   */
  ClauseUse assumeTypeAsElement(Term assumeType, List<Variable> vars) {
    ClauseUse bindingClause = (ClauseUse)asElement(assumeType,vars);
    if (assumeType instanceof Application && 
        ctx.judgMap.containsKey(((Application)assumeType).getFunction().toString())) {
      Application vtApp = (Application)assumeType;
      Map<String,Element> vtMap = new HashMap<String,Element>();
      Judgment vtj = ctx.judgMap.get(vtApp.getFunction().toString());
      for (Rule r : vtj.getRules()) {
        if (r.isAssumption()) {
          ClauseUse vtcu = (ClauseUse)r.getConclusion();
          int ai = vtcu.getConstructor().getAssumeIndex();
          List<Element> vtes = vtcu.getElements();
          int n = vtes.size();
          Iterator<? extends Term> vtai = vtApp.getArguments().iterator();
          for (int i=0; i < n; ++i) {
            if (i == ai) continue;
            Element elem = vtes.get(i);
            if (elem instanceof NonTerminal || elem instanceof Variable) {
              if (!vtai.hasNext()) {
                throw new RuntimeException("cannot find non-terminal value for " + vtes.get(i));
              } else {
                vtMap.put(elem.toString(), asElement(vtai.next(),vars));
              }
            }
          }
          // System.out.println("map is " + vtMap);
          ClauseUse bu = (ClauseUse)vtes.get(ai);
          // System.out.println("  hand-substitution of " + bu + " using " + vtMap + ", ai = " + bu.getConstructor().getAssumeIndex());
          List<Element> bes = new ArrayList<Element>(bu.getElements());
          n = bes.size();
          for (int i=0; i < n; ++i) {
            Element elem = bes.get(i);
            // this next thing doesn't work: no assume index (I think) for var rules.
            // if (i == bu.getConstructor().getAssumeIndex()) bes.set(i,context); else 
            if (elem instanceof NonTerminal || elem instanceof Variable) {
              Element element = vtMap.get(elem.toString());
              if (element != null)
                bes.set(i,element); 
              else bes.set(i,context);
            }
          }
          bindingClause = new ClauseUse(location,bes,bu.getConstructor());
          // System.out.println("binding clause is " + bindingClause);
        }
      }
      //if (!(vtCD.get))
    } else {
      throw new RuntimeException("Can't figure out binding clause for " + assumeType);
    }
    return bindingClause;
  }

  private ClauseUse appAsClause(Constant c, List<Element> args) {
    String fname = c.getName();
    ClauseDef cd = ctx.prodMap.get(fname);
    if (cd != null) {
      List<Element> contents = new ArrayList<Element>(cd.getElements());
      int n = contents.size();
      int ai = cd.getAssumeIndex();
      Iterator<Element> actuals = args.iterator();
      for (int i=0; i < n; ++i) {
        Element old = contents.get(i);
        if (i == ai) {
          contents.set(i,context);
        } else if (old instanceof NonTerminal) {
          contents.set(i,actuals.next());
        }
      }
      return new ClauseUse(location,contents,cd);
    }
    throw new RuntimeException("no cd for " + fname);
  }
  
  private ClauseUse replaceAssume(ClauseUse repl, Element old) {
    if (old instanceof NonTerminal) return repl;
    if (!(old instanceof ClauseUse)) {
      throw new RuntimeException("Couldn't locate context in " + old);
    }
    ClauseUse cu = (ClauseUse)old;
    int n = cu.getElements().size();
    boolean onlyTerminals = true;
    for (int i=0; i < n; ++i) {
      Element e = cu.getElements().get(i);
      if (e.getType().equals(repl.getType())) {
        cu.getElements().set(i,replaceAssume(repl,e));
        // System.out.println("Replaced is now " + cu);
        return cu;
      }
      if (!(e instanceof Terminal)) onlyTerminals = false;
    }
    if (onlyTerminals) return repl;
    throw new RuntimeException("Couldn't locate context in clause " + old);
  }

  /**
   * Take a term produced by (missing) case analysis (either rule case or syntax case)
   * and return a string for the case.  The string will end in a newline iff
   * we have a rule case.
   * @param caseTerm term for the (missing) case
   * @return text of the case indicated.
   */
  public String caseToString(Term caseTerm) {
    StringBuilder sb = new StringBuilder();
    if (caseTerm instanceof Application) {
      Application app = (Application)caseTerm;
      if (app.getFunction() instanceof Constant) {
        String funcName = app.getFunction().getName();
        if (funcName.endsWith("TERM")) {
          String rName = funcName.substring(0, funcName.length()-4);
          if (ctx.ruleMap.containsKey(rName)) {
            int n = app.getArguments().size();
            for (int i=0; i < n; ++i) {
              if (i == n-1) {
                sb.append("--------------- ");
                sb.append(rName);
                sb.append("\n");
              }
              ClauseUse u = (ClauseUse)asElement(app.getArguments().get(i));
              prettyPrint(sb,u,false, 0);
              sb.append('\n');
            }
            return sb.toString();
          }
        }
      }
    }
    prettyPrint(sb,asElement(caseTerm),false, 0);
    return sb.toString();
  }
  
  private static int SUSPECT_INFINITE_RECURSION = 100;
  
  private void prettyPrint(StringBuilder sb, Element e, boolean parenthesize, int level) {
    if (level > SUSPECT_INFINITE_RECURSION) {
      sb.append("#");
      return;
    }
    if (e instanceof NonTerminal || e instanceof Variable) {
      sb.append(e.toString());
    } else if (e instanceof AndTerminal) {
      sb.append("and");
    } else if (e instanceof Terminal) {
      sb.append(((Terminal)e).getTerminalSymbolString());
    } else if (e instanceof Binding) {
      Binding b = (Binding)e;
      sb.append(b.getNonTerminal());
      int n = b.getElements().size();
      for (int i=0; i < n; ++i) {
        sb.append(i == 0 ? '[' : ',');
        prettyPrint(sb,b.getElements().get(i),false, level+1);
      }
      sb.append(']');
    } else if (e instanceof Clause) {
      List<Element> es = ((Clause)e).getElements();
      if (parenthesize && es.size() > 1) sb.append('(');
      int n = es.size();
      for (int i=0; i < n; ++i) {
        if (i > 0) sb.append(' ');
        prettyPrint(sb,es.get(i),true, level+1);
      }
      if (parenthesize && es.size() > 1) sb.append(')');
    } else if (e instanceof AssumptionElement) {
      AssumptionElement ae = (AssumptionElement)e;
      if (parenthesize) sb.append('(');
      prettyPrint(sb,ae.getBase(),false, level+1);
      sb.append(" assumes ");
      prettyPrint(sb,ae.getAssumes(),false, level+1);
      if (parenthesize) sb.append(')');
    } else {
      throw new RuntimeException("??" + e);
    }
  }
}
