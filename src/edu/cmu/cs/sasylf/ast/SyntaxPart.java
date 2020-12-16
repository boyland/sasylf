package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import edu.cmu.cs.sasylf.util.SASyLFError;

/**
 * Syntax declarations that may be mutually recursive.
 */
public class SyntaxPart implements Part {
	private List<Syntax> syntax;
	
	/**
	 * Create a syntax part from a list of syntax declarations.
	 * @param sdecls syntax declarations (must not be null)
	 */
	public SyntaxPart(List<Syntax> sdecls) {
		syntax = new ArrayList<Syntax>(sdecls);
	}
	
	/**
	 * @param out
	 */
	@Override
	public void prettyPrint(PrintWriter out) {
		out.println("\n\nsyntax\n");
		for (Syntax s: syntax) {
			s.prettyPrint(out);
		}
	}

	/**
	 * Type check the elements in this chunk.
	 * @param ctx context to use, must not be null
	 */
	@Override
	public void typecheck(Context ctx) {
		// Since syntax can be mutually recursive, we have multiple passes through the syntax
		
		// We set up variables and nonterminal maps.
		for (Syntax syn: syntax) {
			syn.updateContext(ctx);
		}
		
		// Do some checks before type checking;
		for (Syntax syn : syntax) {
			syn.precheck(ctx);
		}
	
		// Finally, we're ready to check syntax
		for (Syntax syn: syntax) {
			try {
				syn.typecheck(ctx);
			} catch (SASyLFError ex) {
				// already reported
			}
		}
	
		// checks after syntax all defined
		for (Syntax syn : syntax) {
			try {
				syn.postcheck(ctx);
			} catch (SASyLFError ex) {
				// already reported
			}
		}
	
		computeSubordination(ctx);	
	}

	protected void computeSubordination(Context ctx) {
		for (Syntax s : syntax) {
			s.computeSubordination();
		}
		for (Syntax s : syntax) {
			s.checkSubordination();
		}
	}
	
	@Override
	public void collectTopLevel(Collection<? super Node> things) {
		for (Syntax s : syntax) {
			things.add(s);
		}
	}

	@Override
	public void collectRuleLike(Map<String,? super RuleLike> map) {
		// do nothing
	}

	@Override
	public void collectQualNames(Consumer<QualName> consumer) {
		for (Syntax syn : syntax) {
			syn.collectQualNames(consumer);
		}
	}
}