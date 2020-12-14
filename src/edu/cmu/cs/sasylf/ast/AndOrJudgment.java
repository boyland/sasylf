package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.IdentityArrayMap;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Span;
import edu.cmu.cs.sasylf.util.Util;

/**
 * Common superclass for and/or judgments.
 */
public abstract class AndOrJudgment extends Judgment {
	public static class OpTerminal extends Terminal {
		public OpTerminal(String op, Span loc) {
			super("'"+op+"'",loc);
		}
	
		@Override
		public void prettyPrint(PrintWriter out, PrintContext ctx) {
			out.print(getOpName());
		}

		/**
		 * Return the operator name for this terminal
		 * @return string of the operation
		 */
		public String getOpName() {
			final String qopq = super.getName();
			return qopq.substring(1,qopq.length()-1);
		}
	
	}

	/**
	 * Create the name for an and/or judgment. 
	 * @param op operation (either "and" or "or")
	 * @param parts judgments being joined.
	 * @return name formed from operator and names of judgments.
	 */
	protected static String makeName(String op, List<Judgment> parts) {
		StringBuilder sb = new StringBuilder(op);
		sb.append("[");
		boolean first = true;
		for (Judgment j : parts) {
			if (first) first = false; else sb.append(',');
			sb.append(j.getName());
		}
		sb.append(']');
		return sb.toString();
	}

	protected static Clause makeForm(String op, Location l, List<Judgment> parts) {
		Clause result = new Clause(l);
		boolean started = false;
		NonTerminal context = null;
		for (Judgment j : parts) {
			if (started) result.getElements().add(new OpTerminal(op, l));
			else started = true;
			for (Element e : j.getForm().getElements()) {
				if (e instanceof NonTerminal && ((NonTerminal)e).getType().isInContextForm()) {
					if (context == null) {
						context = (NonTerminal)e;
					} else {
						if (!context.equals(e)) {
							ErrorHandler.error(Errors.ANDOR_CONTEXT, context + " != " + e, l);
						}
					}
				}
				result.getElements().add(e);
			}
		}
		return result;
	}

	protected static NonTerminal findAssume(Location loc, List<Judgment> parts) {
		NonTerminal result = null;
		for (Judgment j : parts) {
			NonTerminal a = j.getAssume();
			if (a != null) {
				if (result == null) result = a;
				else if (!result.equals(a)) {
					ErrorHandler.error(Errors.ANDOR_CONTEXT,result + " != " + a, loc);
				}
			}
		}
		return result;
	}

	protected List<Judgment> parts;

	protected AndOrJudgment(String op, Location l, List<Judgment> parts, List<ClauseUse> uses) {
		super(null,makeName(op,parts),new ArrayList<Rule>(),makeForm(op,l,parts),findAssume(l,parts));
		this.parts = parts;
		complete(l, parts, uses);
	}
	
	public AndOrJudgment(Location loc, String n, List<Rule> l, Clause c,
			NonTerminal a) {
		super(loc, n, l, c, a);
	}

	public abstract Terminal makeSeparator(Span l);

	/**
	 * Create a clause use (used in rule creation) for this judgment.
	 * @param loc location to use
	 * @param elems individual elements (including separators)
	 * @param cd the clause def to use
	 * @param clauses list of clauses being joined
	 * @return clause use of the correct type.
	 */
	protected abstract AndOrClauseUse makeClauseUse(Location loc, List<Element> elems, ClauseDef cd, List<ClauseUse> clauses);
	
	/**
	 * Set the rules for this And/Or Judgment
	 * @param l location to use for the rules
	 * @param name name of the judgment
	 * @param premises premises for the rule(s)
	 * @param result conclusion of the rule(s)
	 */
	protected abstract void setRules(Location l, String name, List<ClauseUse> premises,
			Clause result);

	/**
	 * Define the form and rule for this judgment.
	 * @param l
	 * @param parts
	 * @param uses
	 */
	protected void complete(Location l, List<Judgment> parts, List<ClauseUse> uses) {
		String name = super.getName();
		this.parts = parts;
		List<ClauseUse> premises = new ArrayList<>();
		List<Element> concElems = new ArrayList<Element>();
		ElementGenerator gen = new ElementGenerator();
		Iterator<ClauseUse> usesIt = uses.iterator();
		for (Judgment j : parts) {
			ClauseUse use = usesIt.next();
			if (!premises.isEmpty()) {
				concElems.add(makeSeparator(l));
			}
			List<Element> es = new ArrayList<Element>();
			NonTerminal root = j.getAssume();
			Iterator<Element> useIt = use.getElements().iterator();
			gen.forgetVariables();
			for (Element e : j.getForm().getElements()) {
				Element u = useIt.next();
				Element newE = gen.makeCopy(e, u, !e.equals(root));
				es.add(newE);
				concElems.add(newE);
			}
			premises.add(new ClauseUse(l,es,(ClauseDef)j.getForm()));
		}
		ClauseDef cd = new ClauseDef(super.getForm(), this, typeTerm().getName());
		super.setForm(cd);
		Clause result = makeClauseUse(l,concElems,cd,premises);
		setRules(l, name, premises, result);
	}

	@Override
	public void defineConstructor(Context ctx) {
		this.getForm().typecheck(ctx);
		ctx.setProduction(typeTerm().getName(), (ClauseDef)getForm());
		ctx.setJudgment(getName(), this);
	}

	@Override
	public void typecheck(Context ctx) {
		super.typecheck(ctx);
		for (Judgment j : getJudgments()) {
			Util.debug("subordination: ", j.typeTerm(), " < ", typeTerm());
			FreeVar.setAppearsIn(j.typeTerm(), typeTerm());
		}
	}

	public List<Judgment> getJudgments() { return parts; }

	private static IdentityArrayMap<Constant> typeTerms = new IdentityArrayMap<Constant>();

	@Override
	protected Constant computeTypeTerm() {
		int n = parts.size();
		Object[] key = new Object[n+1];
		for (int i=0; i < n; ++i) {
			key[i] = parts.get(i).typeTerm();
		}
		String name = super.getName();
		name = name.substring(0,name.indexOf('['));
		key[n] = name.intern();
		Constant result = typeTerms.get(key);
		if (result == null) {
			result = super.computeTypeTerm();
			typeTerms.put(key, result);
		}
		// System.out.println("Computed typeTerm for " + getName() + " to be " + result);
		return result;
	}

	
}