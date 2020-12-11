package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Span;

public class NotJudgment extends Judgment {
	// TODO: Complete implementation of NOT.
	public static class NotTerminal extends Terminal {
		public NotTerminal(Span loc) {
			super("'not'",loc);
		}

		@Override
		public void prettyPrint(PrintWriter out, PrintContext ctx) {
			out.print("not");
		}

	}

	private static Terminal makeOrTerminal(Span loc) {
		return new NotTerminal(loc);
	}

	private NotJudgment(Location l, Judgment part) {
		// currently we make 'not' abstract, so people can't case analysis on it.
		super(null,makeName(part),null,makeForm(l,part),findAssume(l,part));
		String name = super.getName();
		this.part = part;
		int u = 0;
		List<Clause> premises = new ArrayList<Clause>();
		List<Element> concElems = new ArrayList<Element>();
		Judgment j = part;
		if (!premises.isEmpty()) {
			concElems.add(makeOrTerminal(l));
		}
		List<Element> es = new ArrayList<Element>();
		NonTerminal root = j.getAssume();
		for (Element e : j.getForm().getElements()) {
			if (e instanceof NonTerminal && !e.equals(root)) {
				SyntaxDeclaration s = ((NonTerminal)e).getType();
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

		ClauseDef cd = new ClauseDef(super.getForm(), this, super.getName());
		super.setForm(cd);
		Clause result = new ClauseUse(l,concElems,cd);
		int i=1;
		for (Clause premise : premises) {
			Rule rule = new Rule(l,name+"#"+i,Collections.singletonList(premise),result);
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
	private static String makeName(Judgment part) {
		StringBuilder sb = new StringBuilder();
		sb.append("not-"+part.getName());
		return sb.toString();
	}

	private static Clause makeForm(Location l, Judgment j) {
		Clause result = new Clause(l);
		result.getElements().add(makeOrTerminal(l));
		for (Element e : j.getForm().getElements()) {
			result.getElements().add(e);
		}
		return result;
	}

	private static NonTerminal findAssume(Location loc, Judgment j) {
		if (j.getAssume() != null) {
			ErrorHandler.error(Errors.NOT_ASSUMPTION, loc);
		}
		return null;
	}

	private static Map<Judgment,NotJudgment> cache = new HashMap<Judgment,NotJudgment>();

	/**
	 * Generate an "not" judgment for a base judgment.
	 * @param loc location where generation was (first) required
	 * @param ctx context to check this judgment in
	 * @param j judgment to negate: must not have an "assume"d context
	 * @return judgment that is the negation of the parameter judgment
	 */
	public static NotJudgment makeOrJudgment(Location loc, Context ctx, Judgment j) {
		NotJudgment result = cache.get(j);
		if (result == null) {
			result = new NotJudgment(loc,j);
			result.defineConstructor(ctx);
			result.typecheck(ctx);
			cache.put(j,result);
		}
		return result;
	}

	private Judgment part;

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

	public Judgment getJudgment() { return part; }
}
