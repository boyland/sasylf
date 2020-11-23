package edu.cmu.cs.sasylf.term;

import static edu.cmu.cs.sasylf.term.Facade.Abs;
import static edu.cmu.cs.sasylf.term.Facade.App;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.SimpleTestSuite;

public class UnitTests extends SimpleTestSuite {

	public UnitTests() { }

	static <A,B> Pair<A,B> p(A a, B b) {
		return new Pair<A,B>(a,b);
	}

	static FreeVar v(String n, Term t) {
		return new FreeVar(n,t);
	}

	static Pair<String,Term> pv(FreeVar v, Term t) {
		return p(v.getName(), t);
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

	@SafeVarargs
	static Substitution subst(Pair<String,? extends Term>... pairs) {
		List<FreeVar> vars = new ArrayList<FreeVar>();
		List<Term> terms = new ArrayList<Term>();
		List<Pair<String,Term>> varBindings = new ArrayList<Pair<String,Term>>();
		for (Pair<String,? extends Term> p : pairs) {
			vars.add(new FreeVar(p.first,p.second.getType(varBindings)));
			terms.add(p.second);
		}
		return new Substitution(vars,terms);
	}

	static final Substitution NO_MGU = new Substitution();

	protected void testUnification(String description, Substitution expected, Term t1, Term t2) {
		try {
			Substitution actual = t1.unify(t2);
			assertEqual("unified",t1.substitute(actual),t2.substitute(actual));
			if (expected == null) {
				assertTrue(description + " didn't fail as expected", false);
			} else if (expected == NO_MGU) {
				assertTrue(description + " should have failed due to incompleteness", false);
			} else {
				assertTrue(description + " result was " + actual + ", but expected " + expected,
						actual.containsAll(expected));
			}
		} catch (UnificationIncomplete ex) {
			if (expected == null) {
				assertTrue(description + " should have fully failed, not " + ex.getMessage(), false);
			} else if (expected == NO_MGU) {
				assertTrue("correct",true);
			} else {
				Term t12 = t1.substitute(expected);
				Term t21 = t2.substitute(expected);
				if (t12.equals(t21)) {
					assertTrue(description + " should have succeeded, not " + ex.getMessage(), false);
					System.out.println("BTW: expected substitution does work.");
				} else {
					System.err.println("Test case is suspect: expected substitution didn't work.");
					System.err.println(t12+" != " + t21);
				}    
			}
		} catch (UnificationFailed ex) {
			assertTrue(description + " failed unexpectedly: " +ex.getMessage(), expected == null);
			if (expected != null && expected != NO_MGU) {
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

	@Override
	public void runTests() {
		testType();
		testTypeFamily();
		testUnify();
		// new tests must be after testUnify because of fragile assignment of fresh variables
		testStripLambdas();
		testBindVars();
		testAvoidHO();
		testCompose();
		testHigherOrder();
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
		assertEqual("used first",Abs(a,b(1)), Abs(a,Abs(a,b(2))).stripUnusedLambdas());
		assertEqual("used second",Abs(a,b(1)), Abs(a,Abs(a,b(1))).stripUnusedLambdas());
		assertEqual("unused except in unused",a1, Abs(a,Abs(App(b,b(1)),a1)).stripUnusedLambdas());
		assertEqual("unused except in unused",Abs(a,App(a2,b(1))), Abs(a,Abs(App(b,b(1)),Abs(a,App(a2,b(1))))).stripUnusedLambdas());
	}
	
	private void testBindVars() {
		FreeVar av = v("a",a);
		Term t = Abs(av,a,av);
		assertEqual("bound var correctly",t,Abs(a,b(1)));
	}
	
	private void testAvoidHO() {
		FreeVar v1 = v("F",Abs(a,a));
		Term v1etalong = Abs(a, App(v1, new BoundVar(1)));
		FreeVar v2 = v("G",Abs(a,a));
		Term v2etalong = Abs(a, App(v2, new BoundVar(1)));
		Substitution sub1 = subst(p(v1.getName(),v2etalong));
		assertTrue("can avoid F", sub1.avoid(Collections.singleton(v1)));
		assertEqual("should eta expand F",v1etalong,sub1.getSubstituted(v2));
	}
	
	private void testHigherOrder() {
		FreeVar v1 = v("A",a);
		Term b = App(new BoundVar(1), v1);
		Term f = Abs(Abs(a,a),b);
		FreeVar v2 = v("F",Abs(a,a));
		Term v2etalong = Abs(a, App(v2, new BoundVar(1)));
		Term r = App(v2,v1);
		assertEqual("should substitute properly",r,App(f,v2etalong));
	}
	
	private void testCompose() {
		FreeVar v1 = v("F",Abs(a,a));
		Term v1etalong = Abs(a, App(v1, new BoundVar(1)));
		FreeVar v2 = v("G",Abs(a,a));
		Term v2etalong = Abs(a, App(v2, new BoundVar(1)));
		FreeVar v3 = v("H",Abs(a,a));
		Term v3etalong = Abs(a, App(v3, new BoundVar(1)));
		Term Ka1 = Abs(a, a1);
		Term a2etalong = Abs(a, App(a2, new BoundVar(1)));
		Substitution  empty = subst(pv(v1,v1etalong));
		assertTrue("NOP subst should be empty", empty.isEmpty());
		Substitution sFG = subst(pv(v1,v2etalong));
		assertTrue("FG subst should have one element", sFG.getMap().size() == 1);
		Substitution sFH_GH = subst(pv(v1,v3etalong),pv(v2,v3etalong));
		assertTrue("FH+GH subst should have two elements", sFH_GH.getMap().size() == 2);
		Substitution sFG_GH = subst(pv(v1,v2etalong),pv(v2,v3etalong));
		assertTrue("FG+GH should = FH+GH: " + sFG_GH + " != " + sFH_GH, sFG_GH.equals(sFH_GH));
		Substitution sGH = subst(pv(v2,v3etalong));
		Substitution sGF = subst(pv(v2,v1etalong));
		Substitution sHG = subst(pv(v3,v2etalong));
		Substitution sFG_HG = subst(pv(v1,v2etalong),pv(v3,v2etalong));
		Substitution sGa1 = subst(pv(v2,Ka1));
		Substitution sHa1 = subst(pv(v3,Ka1));
		Substitution sHa2 = subst(pv(v3,a2etalong));
		assertCompose("left identity",empty,sFG,sFG);
		assertCompose("right identity",sGH,empty,sGH);
		assertCompose("transitive",sFG,sGH,sFG_GH);
		assertCompose("transitive",sGH,sHa2,subst(pv(v2,a2etalong),pv(v3,a2etalong)));
		assertCompose("parallel",sFG,sHG,sFG_HG);
		assertCompose("'rotate' F->G back to G->F",sFG,sGF,sGF);
		assertCompose("'rotate' G->H back to H->G",sFH_GH,sHG,sFG_HG);
		assertCompose("idempotent",sFG,sFG,sFG);
		assertCompose("idempotent",sGa1,sGa1,sGa1);
		assertCompose("inconsistent",sGF,sGa1,null);
		assertCompose("occurs check",sGa1,sFG,null);
		
		// merge tests
		assertMerge("left identity",empty,sFG,sFG);
		assertMerge("right identity",sHa1,empty,sHa1);
		assertMerge("transitive",sFG,sGH,sFG_GH);
		assertMerge("transitive",sGH,sHa2,subst(pv(v2,a2etalong),pv(v3,a2etalong)));
		assertMerge("parallel",sFG,sHG,sFG_HG);
		assertMerge("attempted 'rotate' F->G back to G->F",sFG,sGF,sFG);
		assertMerge("attempted 'rotate' G->H back to H->G",sFH_GH,sHG,sFH_GH);
		assertMerge("idempotent",sFG,sFG,sFG);
		assertMerge("idempotent",sGa1,sGa1,sGa1);
		assertMerge("mergable",sGF,sGa1,subst(pv(v1,Ka1),pv(v2,Ka1)));
		assertMerge("reverse comp",sGa1,sFG,subst(pv(v1,Ka1),pv(v2,Ka1)));
		assertMerge("unmergable",sHa1,sHa2,null);
	}
	
	private void assertCompose(String name, Substitution s1, Substitution s2, Substitution s12) {
		final String description = name + ": " + s1 + ".compose(" + s2 + ")";
		Substitution mut1 = new Substitution(s1);
		Substitution mut2 = new Substitution(s2);
		try {
			mut1.compose(mut2);
			if (s12 == null) {
				assertTrue(description + " should have failed, but produced " + mut1,false);
			} else {
				assertEqual(description, s12, mut1);
			}
		} catch (RuntimeException ex) {
			if (s12 == null) {
				assertTrue(description + "correctly failed", true);			
			} else {
				ex.printStackTrace();
				assertTrue(description + " failed with exception: " + ex, false);
			}
		}
		assertEqual("Composition argument afterwards",s2,mut2);
	}

	private void assertMerge(String name, Substitution s1, Substitution s2, Substitution s12) {
		final String description = name + ": " + s1 + ".merge(" + s2 + ")";
		Substitution mut1 = new Substitution(s1);
		Substitution mut2 = new Substitution(s2);
		try {
			mut1.merge(mut2);
			if (s12 == null) {
				assertTrue(description + " should have failed, but produced " + mut1,false);
			} else {
				assertEqual(description, s12, mut1);
			}
		} catch (RuntimeException ex) {
			if (s12 == null) {
				assertTrue(description + "correctly failed", true);			
			} else {
				ex.printStackTrace();
				assertTrue(description + " failed with exception: " + ex, false);
			}
		}
		assertEqual("Merge argument afterwards",s2,mut2);
	}

	private void testUnify() {
		testUnification("var to constant", subst(p("A",a1)), v("A",a), a1);
		Term t2 = App(a2,a1);
		testUnification("var to structure", subst(p("A",t2)), v("A",a), t2);
		testUnification("match structure", subst(p("A",a1)), App(a2,v("A",a)), t2);

		// Not implemented: not in the pattern set.
		Substitution sub = subst(p("A",Abs(a,App(a2,b(1))))); 
		Term t1 = App(v("A",Abs(a,a)),a1);
		testUnification("match function", NO_MGU, t1, t2);

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
		testUnification("non-pattern", NO_MGU, t1, t2);

		// make sure not eagerly binding things:
		Constant ax = new Constant("ax",Abs(a,Abs(Abs(a,a),Abs(Abs(a,a),Abs(a,Constant.TYPE)))));
		t1 = App(ax, App(v("F",Abs(a,a)),v("X",a)), Abs(a,App(v("F",Abs(a,a)),b(1))), Abs(a,a1), App(v("F",Abs(a,a)),v("X",a)));
		t2 = App(ax, App(v("G",Abs(a,a)),v("X",a)), Abs(a,b(1)), Abs(a,App(v("G",Abs(a,a)),b(1))), App(v("G",Abs(a,a)),v("X",a)));
		sub = subst(p("F",Abs(a,b(1))), p("G",Abs(a,a1)), p("X",a1));
		// Util.DEBUG=true;
		testUnification("eventual pattern", sub, t1, t2);

		t1 = App(v("F", Abs(a,a)), v("X1",a));
		t2 = App(v("F", Abs(a,a)), v("X1",a));
		testUnification("identical", subst(), t1, t2);

		t1 = App(v("F", Abs(a,a)), v("X1",a));
		t2 = App(v("F", Abs(a,a)), v("X2",a));
		testUnification("not rigid", NO_MGU, t1, t2);

		t1 = App(v("F", Abs(a,a)), a1);
		t2 = App(v("F", Abs(a,a)), App(a2,a1));
		FreeVar g7 = new FreeVar("G",a,7);
		testUnification("not pattern", subst(p("F",Abs(a,g7))), t1, t2);

		Constant ay = new Constant("ay", Abs(a,Abs(a,Abs(a,Constant.TYPE))));
		Term t11 = App(v("F", Abs(a,a)), App(v("G", Abs(a,a)), v("X1",a)));
		t1 = App(ay, t11, App(v("G",Abs(a,a)),v("X1",a)), t11);
		Term t22 = App(v("F", Abs(a,a)), v("X2",a));
		t2 = App(ay, t22, v("X2",a), t22);
		testUnification("eventually identical", subst(p("X2",App(v("G",Abs(a,a)),v("X1",a)))), t1, t2);

		t1 = Abs(a,Abs(a,Abs(a,App(v("F",Abs(a,Abs(a,Abs(a,a)))),b(1),b(2),b(3)))));
		t2 = Abs(a,Abs(a,Abs(a,App(v("F",Abs(a,Abs(a,Abs(a,a)))),b(3),b(2),b(1)))));
		FreeVar g8 = new FreeVar("G",Abs(a,a),8);
		testUnification("switched", subst(p("F",Abs(a,Abs(a,Abs(a,App(g8,b(2))))))),t1,t2);

		t1 = Abs(a,Abs(a,App(v("F",Abs(a,Abs(a,a))),b(1),b(2))));
		t2 = Abs(a,Abs(a,App(v("F",Abs(a,Abs(a,a))),b(2),b(1))));
		FreeVar g9 = new FreeVar("G",a,9);
		testUnification("all-never-eq", subst(p("F",Abs(a,Abs(a,g9)))),t1,t2);
	}

	public static void main(String[] args) {
		new UnitTests().run();
	}
}
