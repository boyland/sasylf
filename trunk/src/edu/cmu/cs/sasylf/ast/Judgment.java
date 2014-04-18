package edu.cmu.cs.sasylf.ast;

import java.util.*;
import java.io.*;

import edu.cmu.cs.sasylf.ast.grammar.GrmRule;
import edu.cmu.cs.sasylf.ast.grammar.GrmUtil;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.SASyLFError;


public class Judgment extends Node implements ClauseType {
	public Judgment(Location loc, String n, List<Rule> l, Clause c, NonTerminal a) { 
	  super(loc); 
	  name=n; 
	  rules=l; 
	  form=c; 
	  assume = a; 
	  setEndLocation();
	}
	public Judgment(Location loc, String n, Clause s, NonTerminal a) {
	  super(loc);
	  name = n;
	  rules = Collections.emptyList();
	  form = s;
	  assume = a;
	  isAbstract = true;
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
	public String getName() { return name; }
	public NonTerminal getAssume() { return assume; }
  public boolean isAbstract() { return isAbstract; }
  
	private List<Rule> rules;
	private Clause form;
	private String name;
	private NonTerminal assume;
	private boolean isAbstract;

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
		ctx.prodMap.put(name,cd);
		ctx.parseMap.put(cd.getElemTypes(), cd);

		GrmRule r = new GrmRule(GrmUtil.getStartSymbol(), cd.getSymbols(), cd);
		ctx.ruleSet.add(r);

		if (ctx.judgMap.get(name) != null) {
		  if (ctx.judgMap.get(name) != this) { // idempotency
		    ErrorHandler.recoverableError(Errors.DUPLICATE_JUDGMENT, this);
		  }
		}
		ctx.judgMap.put(name, this);
	}
	
	protected void setForm(Clause f) {
	  form = f;
	}

	public void typecheck(Context ctx) {
		//form.typecheck(synMap, varMap);

		Syntax contextSyntax = null;
		
		for (Element f : form.getElements()) {
		  if (f instanceof NonTerminal) {
		    Syntax s = ((NonTerminal)f).getType();
		    if (s.isInContextForm()) contextSyntax = s;
		  }
		}
		
		if ((getAssume() == null) && contextSyntax != null)
			ErrorHandler.recoverableError(Errors.MISSING_ASSUMES, ". Try adding \"assumes " + contextSyntax + "\"", this, "assumes " + contextSyntax);
		else if ((getAssume() != null) && getAssume().getType() == null)
		  ErrorHandler.report(Errors.ILLEGAL_ASSUMES, ": " + getAssume(), this, "assumes " + getAssume() + "\n" +
		      (contextSyntax == null ? "" : "assumes " + contextSyntax));
		else if ((getAssume() != null) && !getAssume().getType().equals(contextSyntax))
		  ErrorHandler.recoverableError(Errors.EXTRANEOUS_ASSUMES, ": " + getAssume(), this, "assume " + getAssume());

		for (Rule r : getRules()) {
		  try {
		    r.typecheck(ctx, this);
		  } catch (SASyLFError e) {
		    // go on to next error
		  }
		}
	}
	
	public Constant typeTerm() {
		if (term == null)
			term =new Constant(name, Constant.TYPE); 
		return term;
	}

	private Constant term = null;
}

