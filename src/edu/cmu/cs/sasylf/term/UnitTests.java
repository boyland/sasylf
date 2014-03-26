package edu.cmu.cs.sasylf.term;

import java.util.ArrayList;
import java.util.List;
import static edu.cmu.cs.sasylf.term.Facade.Abs;
import static edu.cmu.cs.sasylf.term.Facade.App;


import edu.cmu.cs.sasylf.util.SimpleTestSuite;
import edu.cmu.cs.sasylf.util.Util;

public class UnitTests extends SimpleTestSuite {

  public UnitTests() { }
  
  static <A,B> Pair<A,B> p(A a, B b) {
    return new Pair<A,B>(a,b);
  }
  
  static FreeVar v(String n, Term t) {
    return new FreeVar(n,t);
  }

  static BoundVar b(int n) {
    return new BoundVar(n);
  }
  
  Constant a = new Constant("a", Constant.TYPE);
  Constant a1 = new Constant("a1", a);
  Constant a2 = new Constant("a2", Facade.Abs(a, a));
  
  Constant b = new Constant("b", Facade.Abs(a, Constant.TYPE));
  Constant b1 = new Constant("b1", Facade.App(b, a1));
  Constant b2 = new Constant("b2", Facade.Abs(Facade.App(b, new FreeVar("A",a)), 
                                              Facade.App(b, Facade.App(a2, new FreeVar("A",a)))));
  
  Constant t = new Constant("t", Constant.TYPE);
  Constant top = new Constant("Top", t);
  Constant arrow = new Constant("->", Abs(t,Abs(t,t)));
  
  Constant subt = new Constant("subt", Abs(t,Abs(t,Constant.TYPE)));
  // SASyLF's term system doesn't check the application of RuleLike terms
  // because the way the handle assumed contexts.
  Constant subtTransFamily = new Constant("SA-TransTYPE", Constant.TYPE);
  Constant subtTransRule = 
       new Constant("SA-TransTERM", Abs(App(subt,v("T1",t),v("T2",t)),
                                    Abs(App(subt,v("T2",t),v("T3",t)),
                                    Abs(App(subt,v("T1",t),v("T3",t)),
                                        subtTransFamily))));
  
  static Substitution subst(Pair<String,? extends Term>... pairs) {
    List<FreeVar> vars = new ArrayList<FreeVar>();
    List<Term> terms = new ArrayList<Term>();
    List<Pair<String,Term>> varBindings = new ArrayList<Pair<String,Term>>();
    for (Pair<String,? extends Term> p : pairs) {
      vars.add(new FreeVar(p.first,p.second.getType(varBindings)));
      terms.add(p.second);
    }
    return new Substitution(terms,vars);
  }

  protected void testUnification(String description, Substitution expected, Term t1, Term t2) {
    try {
      Substitution actual = t1.unify(t2);
      if (expected == null) {
        assertTrue(description + " didn't fail as expected", false);
      } else {
        assertTrue(description + " result was " + actual + ", but expected " + expected,
            actual.containsAll(expected));
      }
    } catch (UnificationFailed ex) {
      assertTrue(description + " failed unexpectedly: " +ex.getMessage(), expected == null);
      if (expected != null) {
        Term t12 = t1.substitute(expected);
        Term t21 = t2.substitute(expected);
        if (t12.equals(t21)) {
          System.out.println("BTW: expected substitution does work.");
        } else {
          System.err.println("Test case is suspect: expected substitution didn't work.");
          System.err.println(t12+" != " + t21);
        }
      }
    }
  }
  
  public void runTests() {
    testType();
    testTypeFamily();
    testUnify();
  }

  private void testType() {
    List<Pair<String,Term>> typeList = new ArrayList<Pair<String,Term>>();
    
    assertEqual("a1.type",a,a1.getType(typeList));
    typeList.clear();
    
    assertEqual("a2.type",Facade.Abs(a,a),a2.getType(typeList));
    typeList.clear();
    
    Term a2etaLong = Facade.Abs(a, Facade.App(a2, new BoundVar(1)));
    assertEqual("a2.etaLong.type",Facade.Abs(a,a),a2etaLong.getType(typeList));
  }
  
  private void testTypeFamily() {
    assertEqual("a1.typeFamily",a,a1.getTypeFamily());
    assertEqual("a2.typeFamily",a,a2.getTypeFamily());
    assertEqual("b1.typeFamily",b,b1.getTypeFamily());
    assertEqual("b2.typeFamily",b,b2.getTypeFamily());
    Term a2etaLong = Facade.Abs(a, Facade.App(a2, new BoundVar(1)));
    assertEqual("a2.etaLong.typeFamily",a,a2etaLong.getTypeFamily());
  }

  @SuppressWarnings("unchecked")
  private void testUnify() {
    testUnification("var to constant", subst(p("A",a1)), v("A",a), a1);
    Application t2 = App(a2,a1);
    testUnification("var to structure", subst(p("A",t2)), v("A",a), t2);
    testUnification("match structure", subst(p("A",a1)), App(a2,v("A",a)), t2);
    
    // Not implemented: not in the pattern set.
    Substitution sub = subst(p("A",Abs(a,App(a2,b(1))))); 
    Application t1 = App(v("A",Abs(a,a)),a1);
    // testUnification("match function", sub, t1, t2);
    testUnification("match function", null, t1, t2);
    assertEqual("hand-unification",t1.substitute(sub),t2.substitute(sub));

    t1 = 
        App(subtTransRule,
            Abs(t,Abs(App(subt,b(1),top),
                      Abs(t,Abs(App(subt,b(1),App(arrow,b(3),b(3))),
                                App(subt,b(2),App(arrow,b(4),b(4))))))),
            Abs(t,Abs(App(subt,b(1),top),
                      Abs(t,Abs(App(subt,b(1),App(arrow,b(3),b(3))),
                                App(subt,App(arrow,b(4),b(4)),
                                         App(arrow,b(4),top)))))),
            Abs(t,Abs(App(subt,b(1),top),
                      Abs(t,Abs(App(subt,b(1),App(arrow,b(3),b(3))),
                                App(subt,b(2),App(arrow,b(4),top)))))));
    
    FreeVar v434 = v("T434",Abs(t,Abs(t,t)));
    FreeVar v435 = v("T435",Abs(t,Abs(t,t)));
    FreeVar v436 = v("T436",Abs(t,Abs(t,t)));
    
    t2 = 
        App(subtTransRule,
            Abs(t,Abs(App(subt,b(1),top),
                      Abs(t,Abs(App(subt,b(1),App(arrow,b(3),b(3))),
                                App(subt,App(v434,b(4),b(2)),
                                         App(v435,b(4),b(2))))))),
            Abs(t,Abs(App(subt,b(1),top),
                      Abs(t,Abs(App(subt,b(1),App(arrow,b(3),b(3))),
                                App(subt,App(v435,b(4),b(2)),
                                         App(v436,b(4),b(2))))))),
            Abs(t,Abs(App(subt,b(1),top),
                      Abs(t,Abs(App(subt,b(1),App(arrow,b(3),b(3))),
                                App(subt,App(v434,b(4),b(2)),
                                         App(v436,b(4),b(2))))))));
    
    sub = subst(p("T434",Abs(t,Abs(t,b(1)))),
                p("T435",Abs(t,Abs(t,App(arrow,b(2),b(2))))),
                p("T436",Abs(t,Abs(t,App(arrow,b(2),top)))));

    Util.DEBUG=true;
    testUnification("BIG",sub,t1,t2);

  }
  
  public static void main(String[] args) {
    new UnitTests().run();
  }
}
