package edu.cmu.cs.sasylf.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.Util;

public class PrintContext {
	public PrintContext(Term t, Set<FreeVar> varsInScope, NonTerminal contextVar) {
		Util.verify(t != null, "term is null");
		term = t;
		this.varsInScope = varsInScope;
		this.contextVarName = contextVar == null ? "" : contextVar.toString();
		varMap = new HashMap<String,String>();
		boundVars = new ArrayList<String>();
		boundVarCount = 0;
	}
	
	public PrintContext(Term t, PrintContext ctx) {
		Util.verify(t != null, "term is null");
		term = t;
		varMap = ctx.varMap;
		boundVars = ctx.boundVars;
		varsInScope = ctx.varsInScope;
		contextVarName = ctx.contextVarName;
		boundVarCount = ctx.boundVarCount;
	}
	
	public Term term;
	public List<String> boundVars;
	private Map<String,String> varMap;
	private Set<FreeVar> varsInScope;
	public final String contextVarName;
	public int boundVarCount;
	
	public String getStringFor(FreeVar term2, String nameRoot) {
		//System.err.println("getStringFor " + term2 + " and " + nameRoot + " with " + varMap);
		
		if (varsInScope.contains(term2))
			return term2.getName();
		String s = varMap.get(term2.toString());
		if (s == null) {
			int appendNum = 0;
			while (true) {
				s = nameRoot + appendNum;
				if (noConflict(s))
					break;
				appendNum++;
			}
			varMap.put(term2.toString(), s);
		}
		return s;
	}

	private boolean noConflict(String s) {
		if (varMap.values().contains(s))
			return false;
		for (FreeVar v : varsInScope) {
			if (v.toString().equals(s))
				return false;
		}
		return true;
	}
}
