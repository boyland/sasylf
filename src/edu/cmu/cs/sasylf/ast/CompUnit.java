package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Util.debug2;
import static edu.cmu.cs.sasylf.util.Util.debug_parse;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.ast.grammar.GrmRule;
import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.ParseUtil;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.Util;


public class CompUnit extends Node {
	public CompUnit(PackageDeclaration pack, Location loc, String n, Set<String> terms, List<Syntax> s, List<Judgment> j, List<Theorem> t) {
		super(loc);
		packageDecl=pack; 
		moduleName = n; 
		declaredTerminals = terms; 
		syntax=s; judgments=j; 
		theorems = t; 
	}
	public List<Syntax> getSyntax() { return syntax; }
	public List<Judgment> getJudgments() { return judgments; }
	public List<Theorem> getTheorems() { return theorems; }
	public PackageDeclaration getPackage() { return packageDecl; }
	public Set<String> getDeclaredTerminals() { return declaredTerminals; }

	private PackageDeclaration packageDecl;
	private String moduleName;
	private List<Syntax> syntax;
	private List<Judgment> judgments;
	private List<Theorem> theorems;
	private Set<String> declaredTerminals;

	@Override
	public void prettyPrint(PrintWriter out) {
		packageDecl.prettyPrint(out);

		if (moduleName != null) {
			out.println("module " + moduleName);
		}

		out.print("terminals ");
		for (Terminal t : getTerminals()) {
			if (Character.isJavaIdentifierStart(t.getSymbol().charAt(0))) {
				out.print(t.getGrmSymbol()); //t.prettyPrint(out);
				out.print(' ');
			}
		}

		out.println("\n\nsyntax\n");
		for (Syntax s: syntax) {
			s.prettyPrint(out);
		}

		for (Judgment j: judgments) {
			j.prettyPrint(out);
		}

		for (Theorem t: theorems) {
			t.prettyPrint(out);
		}

		out.flush();
	}

	public Set<Terminal> getTerminals() {
		Set<Terminal> s = new HashSet<Terminal>();
		for (Syntax syn: syntax) {
			s.addAll(syn.getTerminals());
		}

		for (Judgment j: judgments) {
			s.addAll(j.getTerminals());
		}
		return s;
	}

	/** typechecks this compilation unit, returning true if the check was successful,
	 * false if there were one or more errors.
	 */
	public boolean typecheck(ModuleFinder mf, ModuleId id) {
		ErrorHandler.recordLastSpan(this);
		int oldCount = ErrorHandler.getErrorCount();
		Context ctx = new Context(mf,this);
		try {
			typecheck(ctx,id);
		} catch (SASyLFError e) {
			// ignore the error; it has already been reported
			//e.printStackTrace();
		}
		return ErrorHandler.getErrorCount() == oldCount;
	}

	public boolean typecheck() {
		return typecheck(new NullModuleFinder(),(ModuleId)null);  
	}

	private void checkFilename(ModuleId id) {
		packageDecl.typecheck(id.packageName);

		if (moduleName != null) {
			if (!ParseUtil.isLegalIdentifier(id.moduleName)) {
				ErrorHandler.report(Errors.BAD_FILE_NAME,this);
			}
			if (!moduleName.equals(id.moduleName)) {
				ErrorHandler.warning(Errors.WRONG_MODULE_NAME, this, moduleName+"\n"+id.moduleName);
			}
		}
	}

	// TODO: ensures variable names do not include num or prime
	// error if NonTerminal does not match a Syntax or Variable (likely should have been a Terminal)
	public void typecheck(Context ctx, ModuleId id) {
		if (id != null) checkFilename(id);
		
		// Since syntax can be mutually recursive, we have multiple passes through the syntax
		
		// We set up variables and nonterminal maps.
		for (Syntax syn: syntax) {
			syn.updateSyntaxMap(ctx.varMap, ctx.synMap);
		}
		
		// Do some checks before type checking;
		for (Syntax syn : syntax) {
			syn.precheck(ctx);
		}

		// Finally, we're ready to check syntax
		for (Syntax syn: syntax) {
			syn.typecheck(ctx);
		}

		// checks after syntax all defined
		for (Syntax syn : syntax) {
			syn.postcheck(ctx);
		}


		computeSubordinationSyntax(ctx);

		for (Judgment j: judgments) {
			j.defineConstructor(ctx);
		}

		debug_parse("Parse Table\n---------------------------");
		for (Map.Entry<List<ElemType>,ClauseDef> ent : ctx.parseMap.entrySet()) {
			debug2(ent.toString());
		}
		for (GrmRule r : ctx.ruleSet) {
			debug_parse(r);
		}

		for (Judgment j: judgments) {
			try {
				j.typecheck(ctx);
			} catch (SASyLFError e) {
				// already reported.
			}
		}

		computeSubordinationJudgment(judgments);

		for (Theorem t: theorems) {
			try {
				t.typecheck(ctx);
			} catch (SASyLFError e) {
				// already reported, swallow the exception
			}
		}
	}

	private void computeSubordinationSyntax(Context ctx) {
		for (Syntax syntax : ctx.synMap.values()) {
			Term synType = syntax.typeTerm();
			for (Clause clause : syntax.getClauses()) {
				if (clause.isVarOnlyClause()) {
					FreeVar.setAppearsIn(synType,synType);
				}
				if (clause instanceof ClauseDef) {
					ClauseDef clauseDef = (ClauseDef) clause;
					Constant constant = (Constant)clauseDef.asTerm();
					Term typeTerm = constant.getType();
					while (typeTerm instanceof Abstraction) {
						Abstraction abs = (Abstraction)typeTerm;
						Util.debug(abs.varType.baseTypeFamily(), " < ", synType);
						FreeVar.setAppearsIn(abs.varType.baseTypeFamily(), synType);
						for (Term t = abs.varType; t instanceof Abstraction; t = ((Abstraction)t).getBody()){
							Util.debug(((Abstraction)t).varType.baseTypeFamily(), " < ", t.baseTypeFamily());
							FreeVar.setAppearsIn(((Abstraction)t).varType.baseTypeFamily(), t.baseTypeFamily());
						}
						typeTerm = abs.getBody();
					}
				}
			}
		}
	}

	private void computeSubordinationJudgment(List<Judgment> js) {
		for (Judgment j : js) {
			Term jType = j.typeTerm();
			for (Element e : j.getForm().getElements()) {
				if (e instanceof NonTerminal) {
					Term nType = ((NonTerminal)e).getTypeTerm();
					Util.debug("subordination: ", nType, " < ", jType);
					FreeVar.setAppearsIn(nType, jType);
				}
			}
			for (Rule r : j.getRules()) {
				if (r.isAssumption()) {
					Util.debug("subordination: ", jType, " < ", jType, " forced");
					FreeVar.setAppearsIn(jType,jType);
					Term cType = r.getAssumes().getTypeTerm();
					Util.debug("subordination: ", jType, " < ", cType, " forced.");
					FreeVar.setAppearsIn(jType,cType);
				}
				for (Clause cl : r.getPremises()) {
					if (!(cl instanceof ClauseUse)) continue; // avoid recovered error -> internal error
					Term pType = ((ClauseUse)cl).getTypeTerm();
					Util.debug("subordination: ", pType, " < ", jType);
					FreeVar.setAppearsIn(pType, jType);
				}
			}
		}
	}
}
