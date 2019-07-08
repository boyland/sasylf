package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Util.debug2;
import static edu.cmu.cs.sasylf.util.Util.debug_parse;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.ast.grammar.GrmRule;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.Util;

public class Chunk {
	public List<Syntax> syntax;
	public List<Judgment> judgments;
	public List<Theorem> theorems;
	public Set<String> declaredTerminals;

	// The following are needed by the ProofOutline module.
	public List<Syntax> getSyntax() { return syntax; }
	public List<Judgment> getJudgments() { return judgments; }
	public List<Theorem> getTheorems() { return theorems; }
	
	public Chunk() {
	}

	/**
	 * @param out
	 */
	public void prettyPrint(PrintWriter out) {
		out.print("terminals ");
		for (String t : declaredTerminals) {
			out.print(t);
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
	}

	/**
	 * Type check the elements in this chunk.
	 * @param ctx context to use, must not be null
	 */
	public void typecheck(Context ctx) {
		// Since syntax can be mutually recursive, we have multiple passes through the syntax
		
		ctx.termSet.addAll(declaredTerminals);
		
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
	
		computeSubordinationJudgment();
	
		for (Theorem t: theorems) {
			try {
				t.typecheck(ctx);
			} catch (SASyLFError e) {
				// already reported, swallow the exception
			}
		}
	}

	public void computeSubordinationSyntax(Context ctx) {
		for (Syntax s : syntax) {
			s.computeSubordination();
		}
	}

	void computeSubordinationJudgment() {
		for (Judgment j : judgments) {
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
	
	/**
	 * Export all top-level declarations in this chunk
	 * @param things collection to place in, must not be null.
	 */
	public void collectTopLevel(Collection<? super Node> things) {
		for (Syntax s : syntax) {
			things.add(s);
		}
		for (Judgment j : judgments) {
			things.add(j);
		}
		for (Theorem th : theorems) {
			things.add(th);
		}
	}
	
	/**
	 * Export all rule-likes into a provided map
	 * @param map destination, must not be null.
	 */
	public void collectRuleLike(Map<String,? super RuleLike> map) {
		for (Judgment j : judgments) {
			for (Rule r : j.getRules()) {
				map.put(r.getName(),r);
			}
		}
		for (Theorem t : theorems) {
			map.put(t.getName(), t);
		}
	}
}