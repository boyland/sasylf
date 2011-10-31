package edu.cmu.cs.sasylf.ast;

import java.util.*;
import java.io.*;

import edu.cmu.cs.sasylf.ast.grammar.GrmRule;
import edu.cmu.cs.sasylf.ast.grammar.GrmUtil;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.util.ErrorHandler;


public class Judgment extends Node implements ClauseType {
	public Judgment(Location loc, String n, List<Rule> l, Clause c, NonTerminal a) { super(loc); name=n; rules=l; form=c; assume = a; }
	public List<Rule> getRules() { return rules; }
	public Clause getForm() { return form; }
	public String getName() { return name; }
	public NonTerminal getAssume() { return assume; }

	private List<Rule> rules;
	private Clause form;
	private String name;
	private NonTerminal assume;

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
		ClauseDef cd = new ClauseDef(form, this, name);
		cd.checkVarUse(false);
		form = cd;
		ctx.parseMap.put(cd.getElemTypes(), cd);

		GrmRule r = new GrmRule(GrmUtil.getStartSymbol(), cd.getSymbols(), cd);
		ctx.ruleSet.add(r);

	}

	public void typecheck(Context ctx) {
		//form.typecheck(synMap, varMap);

		boolean foundAssumeRule = false;
		for (Rule r : getRules()) {
			r.typecheck(ctx, this);
			foundAssumeRule = foundAssumeRule || r.isAssumption();
		}
		
		// TODO: check that a clause exists showing how to use the assumption
		// below is right idea, but look if something we depend on shows how to use the assumption
		//if ((getAssume() != null) && !foundAssumeRule)
		//	ErrorHandler.warning(Errors.CANNOT_USE_ASSUMPTION, this);
		// CUT the above because it was too confusing right now
		if ((getAssume() == null) && foundAssumeRule)
			ErrorHandler.warning(Errors.MISSING_ASSUMES, this);
	}
	
	public Constant typeTerm() {
		if (term == null)
			term =new Constant(name, Constant.TYPE); 
		return term;
	}

	private Constant term = null;
}

