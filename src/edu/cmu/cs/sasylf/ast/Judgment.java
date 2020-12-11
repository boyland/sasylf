package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.ast.grammar.GrmRule;
import edu.cmu.cs.sasylf.ast.grammar.GrmUtil;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.Util;


public class Judgment extends Node implements ClauseType, Named {
	public Judgment(Location loc, String n, List<Rule> l, Clause c, NonTerminal a) { 
		super(loc); 
		name=n; 
		if (l == null) {
			isAbstract = true;
			rules = Collections.emptyList();
		} else {
			rules=l;
		}
		form=c; 
		assume = a; 
		setEndLocation();
	}

	protected void setEndLocation() {
		if (!rules.isEmpty()) {
			super.setEndLocation(rules.get(rules.size()-1).getEndLocation());
		} else if (assume != null) {
			super.setEndLocation(assume.getEndLocation());
		} else {
			super.setEndLocation(form.getEndLocation());
		}
	}

	public List<Rule> getRules() { return rules; }
	public Clause getForm() { return form; }
	@Override
	public String getName() { return name; }
	public NonTerminal getAssume() { return assume; }
	@Override
	public boolean isAbstract() { return isAbstract; }

	private List<Rule> rules;
	private Clause form;
	private String name;
	private NonTerminal assume;
	private boolean isAbstract;

	@Override
	public void prettyPrint(PrintWriter out) {
		out.print("judgment ");
		out.print(name);
		out.print(": ");
		form.prettyPrint(out);
		out.println();

		if (assume != null) {
			out.print("assumes ");
			assume.prettyPrint(out);
			out.println();
		}
		out.println();

		for (Rule r : getRules()) {
			r.prettyPrint(out);
		}
		out.println("\n");
	}

	public Set<Terminal> getTerminals() {
		return form.getTerminals();
	}

	public void defineConstructor(Context ctx) {
		form.typecheck(ctx);
		ClauseDef cd;
		if (form instanceof ClauseDef) cd = (ClauseDef)form;
		else cd = new ClauseDef(form, this, name);
		cd.checkVarUse(false);
		form = cd;
		ctx.setProduction(typeTerm().getName(),cd);
		ctx.parseMap.put(cd.getElemTypes(), cd);

		GrmRule r = new GrmRule(GrmUtil.getStartSymbol(), cd.getSymbols(), cd);
		ctx.ruleSet.add(r);

		ctx.setJudgment(name, this);
	}

	protected void setForm(ClauseDef f) {
		form = f;
	}

	public void typecheck(Context ctx) {

		SyntaxDeclaration contextSyntax = null;

		if (assume != null) {
			assume.typecheck(ctx);
		}

		if (isAbstract && !rules.isEmpty()) {
			ErrorHandler.recoverableError(Errors.JUDGMENT_ABSTRACT, this); // NB: parser currently handles this
		}

		for (Element f : form.getElements()) {
			if (f instanceof NonTerminal) {
				SyntaxDeclaration s = ((NonTerminal)f).getType();
				if (s.isInContextForm()) contextSyntax = s;
			}
		}

		if ((getAssume() == null) && contextSyntax != null && !Util.X_CONTEXT_IS_SYNTAX)
			ErrorHandler.recoverableError(Errors.MISSING_ASSUMES, ". Try adding \"assumes " + contextSyntax + "\"", this, "assumes " + contextSyntax);
		else if ((getAssume() != null) && getAssume().getType() == null)
			ErrorHandler.error(Errors.ILLEGAL_ASSUMES, ": " + getAssume(), getAssume(), "assumes " + getAssume() + "\n" +
					(contextSyntax == null ? "" : "assumes " + contextSyntax));
		else if ((getAssume() != null) && !getAssume().getType().equals(contextSyntax))
			ErrorHandler.recoverableError(Errors.EXTRANEOUS_ASSUMES, ": " + getAssume(), getAssume(), "assumes " + getAssume());

		for (Rule r : getRules()) {
			try {
				r.typecheck(ctx, this);
			} catch (SASyLFError e) {
				// go on to next error
			}
		}
	}

	/**
	 * Find a rule for this judgment using the given constant.
	 * @param c constant to check
	 * @return rule using this constant, or null if none found
	 */
	public CanBeCase findRule(Constant c) {
		for (Rule r : rules) {
			if (r.getRuleAppConstant() == c) return r;
		}
		return null;
	}
	
	@Override
	public final Constant typeTerm() {
		if (term == null)
			term = computeTypeTerm(); 
		return term;
	}

	/**
	 * Compute the LF base type for elements of this type.
	 * In practice, this constructs a unique {@link Constant} instance.
	 * @return LF base type family for this judgment.
	 */
	protected Constant computeTypeTerm() {
		return new Constant(name, Constant.TYPE);
	}

	private Constant term = null;

	@Override
	public void analyze(Context ctx, Element target, Node source, 
			Map<CanBeCase, Set<Pair<Term, Substitution>>>  result) {
		if (isAbstract()) {
			ErrorHandler.error(Errors.CASE_SUBJECT_ABSTRACT, ": " + getName(), source);
		}
		Util.verify(target instanceof ClauseUse, "Judgment#analyze called with bad element: " + target);
		ClauseUse cl = (ClauseUse)target;
		Term t = ctx.toTerm(target);
		for (Rule rule : getRules()) {
			Set<Pair<Term,Substitution>> caseResult = null;
			if (rule.isInterfaceOK()) {
				caseResult = rule.caseAnalyze(ctx, t, cl, source);
			}
			if (caseResult == null) caseResult = Collections.emptySet(); 
			result.put(rule, caseResult);
		}
	}
	
	
}

