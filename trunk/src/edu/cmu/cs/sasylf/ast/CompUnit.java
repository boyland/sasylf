package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Util.debug2;
import static edu.cmu.cs.sasylf.util.Util.debug_parse;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sasylf.util.ParseUtil;

import edu.cmu.cs.sasylf.ast.grammar.GrmRule;
import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.SASyLFError;


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

	public void getVariables(Context ctx) {
		for (Syntax syn: syntax) {
			syn.getVariables(ctx.varMap);
		}
	}

	/** typechecks this compilation unit, returning true if the check was successful,
	 * false if there were one or more errors.
	 */
	public boolean typecheck(String filename) {
		int oldCount = ErrorHandler.getErrorCount();
		Context ctx = new Context(this);
		try {
			getVariables(ctx);
			checkFilename(filename);
			typecheck(ctx);
		} catch (SASyLFError e) {
			// ignore the error; it has already been reported
			//e.printStackTrace();
		}
		return ErrorHandler.getErrorCount() == oldCount;
	}
	
	public boolean typecheck() {
    int oldCount = ErrorHandler.getErrorCount();
    Context ctx = new Context(this);
    try {
      getVariables(ctx);
      typecheck(ctx);
    } catch (SASyLFError e) {
      // ignore the error; it has already been reported
      //e.printStackTrace();
    }
    return ErrorHandler.getErrorCount() == oldCount;	  
	}

	private void checkFilename(String filename) {
	  File f = new File(filename);
	  String name = f.getName();
	  LinkedList<String> dirs = new LinkedList<String>();
	  for (;;) {
	    String p = f.getParent();
	    if (p == null) break;
	    f = new File(p);
	    dirs.addFirst(f.getName());
	  }
	  packageDecl.typecheck(dirs.toArray(new String[dirs.size()]));
	  
	  if (moduleName != null) {
	    if (name.endsWith(".slf")) {
	      name = name.substring(0, name.length()-4);
	    } else {
	      ErrorHandler.report(Errors.BAD_FILE_NAME_SUFFIX,this);
	    } 
	    if (!ParseUtil.isLegalIdentifier(name)) {
	      ErrorHandler.report(Errors.BAD_FILE_NAME,this);
	    }
	    if (!moduleName.equals(name)) {
	      ErrorHandler.warning(Errors.WRONG_MODULE_NAME, this, moduleName+"\n"+name);
	    }
	  }
	}
	
	// TODO: ensures variable names do not include num or prime
	// computes Syntax type for each variable
	// computes Syntax for each NonTerminal
	// converts NonTerminal into Variable where appropriate
	// error if NonTerminal does not match a Syntax or Variable (likely should have been a Terminal)
	public void typecheck(Context ctx) {
		for (Syntax syn: syntax) {
			if (declaredTerminals.contains(syn.getNonTerminal().getSymbol()))
				ErrorHandler.report("Syntax nonterminal " + syn.getNonTerminal().getSymbol() + " may not appear in the terminals list", syn);
			syn.computeVarTypes(ctx.varMap);
			ctx.synMap.put(syn.getNonTerminal().getSymbol(), syn);
		}

		for (Syntax syn: syntax) {
			syn.typecheck(ctx);
		}
    
		// check if useless
		for (Syntax syn : syntax) {
		  if (!syn.isProductive()) {
		    ErrorHandler.recoverableError("Syntax is unproductive.  You need a production that can actually generate a string.", syn);
		  }
		}
		
		// check variables are bound in exactly context (two passes)
		for (Syntax syn : syntax) {
		  syn.registerVarTypes();
		}
		for (Syntax syn : syntax) {
		  syn.checkVarTypeRegistered();
		}

		
		computeSubordination(ctx);

		for (Judgment j: judgments) {
			j.defineConstructor(ctx);
		}

		debug_parse("Parse Table\n---------------------------");
		for (Map.Entry<List<ElemType>,ClauseDef> ent : ctx.parseMap.entrySet()) {
			debug2(ent.toString());
		}
		for (GrmRule r : ctx.ruleSet) {
			debug_parse(r.toString());
		}

		for (Judgment j: judgments) {
			try {
        j.typecheck(ctx);
      } catch (SASyLFError e) {
        // already reported.
      }
		}

		for (Theorem t: theorems) {
			try {
				t.typecheck(ctx);
			} catch (SASyLFError e) {
				// already reported, swallow the exception
			}
		}
	}
	private void computeSubordination(Context ctx) {
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
						Term varType = abs.varType;
						// base of varType may appear in synType: compute base then set it up
						while (varType instanceof Abstraction)
							varType = ((Abstraction)varType).getBody();
						FreeVar.setAppearsIn(varType, synType);
						typeTerm = abs.getBody();
					}
				}
			}
		}
		
		FreeVar.computeAppearsInClosure();
	}

}
