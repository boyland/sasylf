package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;

abstract public class DerivationWithArgs extends Derivation {
	public DerivationWithArgs(String n, Location l, Clause c) {
		super(n,l,c);
	}

	public List<Clause> getArgStrings() { return argStrings; }
	public List<Fact> getArgs() { return args; }

	protected abstract String prettyPrintByClause();

	public void prettyPrint(PrintWriter out) {
		super.prettyPrint(out);
		out.print(prettyPrintByClause());

		boolean first = true;
		for (Fact arg : args) {
			if (first) {
				if (!(this instanceof DerivationByPrevious))
					out.print(" on ");
			} else
				out.print(", ");
			arg.printReference(out);
			//out.print(arg.getName());
			//arg.getElement().prettyPrint(out);//out.print(arg);
			first = false;
		}
		out.println();
	}

	public void typecheck(Context ctx) {
    super.typecheck(ctx);
    
    args.clear(); // needed for idempotency
    for (int i = 0; i < argStrings.size(); ++i) {
			Clause c = argStrings.get(i);
      // remove all (c) parens:
      while (c.getElements().size() == 1 && c.getElements().get(0) instanceof Clause) {
        argStrings.set(i,c = (Clause)c.getElements().get(0));
      }
      Fact f = null;
      // special case for a reference to a derivation 
      if (c.getElements().size() == 1 && c.getElements().get(0) instanceof NonTerminal) {
        String s = ((NonTerminal)c.getElements().get(0)).getSymbol();
        f = ctx.derivationMap.get(s);
        if (f == null && !ctx.isKnown(s)) {
          ErrorHandler.report(Errors.DERIVATION_NOT_FOUND, "No derivation found for " + s, this);
        }
        // fall through: handle as a nonterminal
      }
      if (f == null) {
        Element e = c.typecheck(ctx);
        if (e instanceof Clause) {
          Clause cl = (Clause)e;
          if (cl.getElements().size() == 1 && cl.getElements().get(0) instanceof NonTerminal) {
            e = cl.getElements().get(0);
          } else {
            e = cl.computeClause(ctx, false);
          }
        }
        f = e.asFact(ctx, ctx.innermostGamma);
      }
			args.add(f);
		}
	}

	/** Gets the ith argument as a term and adapts it to the current context
	 */
	protected Term getAdaptedArg(Context ctx, int i) {
		Element element = getArgs().get(i).getElement();
    Term argTerm = DerivationByAnalysis.adapt(element.asTerm(), element, ctx, false);
    return argTerm;
	}
	
	private List<Clause> argStrings = new ArrayList<Clause>();
	private List<Fact> args = new ArrayList<Fact>();
}
