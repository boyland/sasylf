package edu.cmu.cs.sasylf.term;

import static edu.cmu.cs.sasylf.term.Facade.Abs;
import static edu.cmu.cs.sasylf.term.Facade.App;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.sasylf.util.SimpleTestSuite;

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
  
  Constant e = new Constant("e", Constant.TYPE);
  Constant app = new Constant("app", Abs(e,Abs(e,e)));
  
  Constant t = new Constant("t", Constant.TYPE);
  Constant top = new Constant("Top", t);
  Constant arrow = new Constant("->", Abs(t,Abs(t,t)));
  
  Constant subt = new Constant("subt", Abs(t,Abs(t,Constant.TYPE)));
  Constant subtTransFamily = new Constant("SA-TransTYPE", Constant.TYPE);
  Constant subtTransRule = 
       new Constant("SA-TransTERM", Abs(App(subt,v("T1",t),v("T2",t)),
                                    Abs(App(subt,v("T2",t),v("T3",t)),
                                    Abs(App(subt,v("T1",t),v("T3",t)),
                                        subtTransFamily))));
  
  Constant hast = new Constant("has-type", Abs(e, Abs(t, Constant.TYPE)));
  Constant hastAppFamily = new Constant("T-App", Constant.TYPE);
  Constant hastAppRule =
      new Constant("T-AppTERM", Abs(App(hast, v("E1",e), App(arrow, v("T",t), v("T'",t))),
                                Abs(App(hast, v("E2",e), v("T",t)),
                                Abs(App(hast, App(app, v("E1",e), v("E2",e)), v("T'",t)),
                                    hastAppFamily))));
  
  Constant tvarFamily = new Constant("T-Var", Constant.TYPE);
  Constant tvarRule =
      new Constant("T-VarTERM", Abs(Abs(e, Abs(App(hast, b(1), v("T",t)), App(hast, b(2), v("T",t)))),
                                    tvarFamily));
  
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
      assertEqual("unified",t1.substitute(actual),t2.substitute(actual));
      if (expected == null) {
        assertTrue(description + " didn't fail as expected", false);
      } else {
        assertTrue(description + " result was " + actual + ", but expected " + expected,
            actual.containsAll(expected));
      }
    } catch (UnificationIncomplete ex) {
      System.out.println(ex.getMessage());
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
    // new tests must be after testUnify because of fragile assignment of fresh variables
    testStripLambdas();
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

  private void testStripLambdas() {
    assertEqual("not abstraction",a,a.stripUnusedLambdas());
    assertEqual("used abstraction",Abs(a,b(1)),Abs(a,b(1)).stripUnusedLambdas());
    assertEqual("didn't used",a1,Abs(a,a1).stripUnusedLambdas());
    assertEqual("used first",Abs(a,Abs(a,b(2))), Abs(a,Abs(a,b(2))).stripUnusedLambdas());
    assertEqual("used second",Abs(a,b(1)), Abs(a,Abs(a,b(1))).stripUnusedLambdas());
    assertEqual("unused except in unused",a1, Abs(a,Abs(App(b,b(1)),a1)).stripUnusedLambdas());
    assertEqual("unused except in unused",Abs(a,App(a2,b(1))), Abs(a,Abs(App(b,b(1)),Abs(a,App(a2,b(1))))).stripUnusedLambdas());
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
    testUnification("match function", sub, t1, t2);

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

    // Util.DEBUG=true;
    testUnification("BIG",sub,t1,t2);
    
    t1 = App(hastAppRule,
             Abs(e, Abs(App(hast,b(1),v("tau",t)),
                        App(hast, App(v("e18", Abs(e,e)), b(2)),
                                  App(arrow, v("tau17",t), v("tau16",t))))),
             Abs(e, Abs(App(hast,b(1),v("tau",t)),
                        App(hast, App(v("e19", Abs(e,e)), b(2)),
                                  v("tau17",t)))),
             Abs(e, Abs(App(hast,b(1),v("tau",t)),
                        App(hast, App(app, App(v("e18", Abs(e,e)), b(2)),
                                           App(v("e19", Abs(e,e)), b(2))),
                                  v("tau16",t)))));
    
    /*
    t2 = App(hastAppRule, 
             v("has-type-20", Abs(e, Abs(App(hast,b(1),v("tau",t)),
                                         App(hast, v("E1",e), App(arrow, v("T",t), v("T'",t)))))),
             v("has-type-21", Abs(e, Abs(App(hast,b(1),v("tau",t)),
                                         App(hast, v("E2",e), v("T",t))))),
             Abs(e, Abs(App(hast,b(1),v("tau",t)), 
                 App(hast,v("E",e),v("tau'",t)))));
    */
    
    t2 = App(hastAppRule, 
        v("has-type-20", Constant.UNKNOWN_TYPE),
        v("has-type-21", Constant.UNKNOWN_TYPE),
        Abs(e, Abs(App(hast,b(1),v("tau",t)), 
                   App(hast,v("E",e),v("tau'",t)))));
    
    // Simulate the variables that Unification makes
    // If other tests come before this one, we need to adjust the
    // numbers here, or more powerfully, change testUnification to allow
    // a renaming of stamped free variables.
    FreeVar e1 = new FreeVar("e18",e,6);
    FreeVar e2 = new FreeVar("e19",e,5);
    
    sub = subst(p("e18",Abs(e,e1)),
                p("e19",Abs(e,e2)),
                p("E", App(app, e1, e2)),
                p("tau'", v("tau16", t)),
                p("has-type-20", Abs(e, Abs(App(hast,b(1),v("tau",t)), 
                                            App(hast, e1, App(arrow, v("tau17",t), v("tau16",t)))))),
                p("has-type-21", Abs(e, Abs(App(hast,b(1),v("tau",t)), 
                                            App(hast, e2, v("tau17",t))))));
    
    testUnification("good16", sub, t1, t2); 
    
    t1 = App(tvarRule, Abs(e, Abs(App(hast, b(1), v("tau5",t)), App(hast, b(2),     v("tau5",t)))));
    t2 = App(tvarRule, Abs(e, Abs(App(hast, b(1), v("tau1",t)), App(hast, v("E",e), v("tau2",t)))));

    // Util.DEBUG = true;
    testUnification("bad", null, t1, t2);
    
    t1 = App(v("F", Abs(a,a)), v("X",a));
    t2 = App(a2,a1);
    sub = subst(p("F",a2),p("X",a1));
    testUnification("non-pattern", sub, t1, t2);
    
    // make sure not eagerly binding things:
    Constant ax = new Constant("ax",Abs(a,Abs(Abs(a,a),Abs(Abs(a,a),Abs(a,Constant.TYPE)))));
    t1 = App(ax, App(v("F",Abs(a,a)),v("X",a)), Abs(a,App(v("F",Abs(a,a)),b(1))), Abs(a,a1), App(v("F",Abs(a,a)),v("X",a)));
    t2 = App(ax, App(v("G",Abs(a,a)),v("X",a)), Abs(a,b(1)), Abs(a,App(v("G",Abs(a,a)),b(1))), App(v("G",Abs(a,a)),v("X",a)));
    sub = subst(p("F",Abs(a,b(1))), p("G",Abs(a,a1)), p("X",a1));
    // Util.DEBUG=true;
    testUnification("eventual pattern", sub, t1, t2);
  }
  
  public static void main(String[] args) {
    new UnitTests().run();
  }
}
