package edu.cmu.cs.sasylf.term;

import java.util.Arrays;
import java.util.List;

public class Facade {
	public static Substitution Sub(Term t, FreeVar v) {
		return new Substitution(t, v);
	}
	public static Constant Const(String s, Term type) {
		return new Constant(s, type);
	}
	/*public static Application Const(String s, Term t1) {
		return App(new Constant(s), t1);
	}
	public static Application Const(String s, Term t1, Term t2) {
		return App(new Constant(s), t1, t2);
	}
	public static Application Const(String s, Term t1, Term t2, Term t3) {
		return App(new Constant(s), new Term[] { t1, t2, t3 });
	}*/
	public static Term Abs(Term type, Term body) {
		return Abs("noName", type, body);
	}
	public static Term Abs(String var, Term type, Term body) {
		return Abstraction.make(var, type, body);
	}
	public static Application App(Atom f, Term a) {
		return new Application(f, a);
	}
	public static Application App(Atom f, Term a1, Term a2) {
		return App(f, new Term[] { a1, a2 });
	}
	public static Application App(Atom f, Term a1, Term a2, Term a3) {
		return App(f, new Term[] { a1, a2, a3 });
	}
	public static Application App(Atom f, Term [] args) {
		return new Application(f, Arrays.asList(args));
	}
	public static Application App(Atom f, List<Term> args) {
		return new Application(f, args);
	}
	public static BoundVar BVar(int i) {
		return new BoundVar(i);
	}
	public static FreeVar FVar(String txt, Term type) {
		return new FreeVar(txt, type);
	}
	public static FreeVar FreshVar(String txt, Term type) {
		return FreeVar.fresh(txt, type);
	}
	public static <X, Y> Pair<X,Y> pair(X x, Y y) {
		return new Pair<X,Y>(x, y);
	}
}
