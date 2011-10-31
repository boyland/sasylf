package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Util.debug2;
import static edu.cmu.cs.sasylf.util.Util.tdebug;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;

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
		for (int i = 0; i < argStrings.size(); ++i) {
			Clause c = argStrings.get(i);
			Fact f = null;
			if (c.getElements().size() == 1 && !(c.getElements().get(0) instanceof Clause)) {
				// case for a reference to a derivation 
				String s = c.getElements().get(0).toString();
				f = ctx.derivationMap.get(s);
				if (f == null) {
					// case for a use of a one element clause
					f = new SyntaxAssumption(s, getLocation());
					f.typecheck(ctx, true);
				}
			} else {
				// case for a clause given directly
				debug2("computing fact for " + c + " in class " + this.getClass().getName());
				c = (Clause) c.typecheck(ctx);
				c = (Clause) c.computeClause(ctx, false);
				argStrings.set(i,c);
				f = new ClauseAssumption(c, getLocation());
				f.typecheck(ctx, true);
			}
			args.add(f);
		}

		super.typecheck(ctx);
	}

	/** Gets the ith argument as a term and adapts it to the current context using wrappingSub
	 */
	protected Term getAdaptedArg(Context ctx, Substitution wrappingSub, int i) {
		Element element = getArgs().get(i).getElement();
		
		Term argTerm = DerivationByAnalysis.adapt(element.asTerm(), element, ctx, false);
		//Term argTerm = element.asTerm().substitute(ctx.currentSub);
		
		/*tdebug("arg " + i);
		if (ctx.adaptationNumber > 0 && element instanceof ClauseUse) {
			ClauseUse cu = (ClauseUse) element;
			tdebug("ctx.adaptationRoot = " + ctx.adaptationRoot);
			if (cu.getRoot().equals(ctx.adaptationRoot))
				argTerm = cu.wrapWithOuterLambdas(argTerm, ctx.matchTermForAdaptation, ctx.adaptationNumber, wrappingSub, false);
		}*/
		return argTerm;
	}

	/** Gets the ith argument as a term and adapts it to the current context
	 */
	protected Term getAdaptedArg(Context ctx, int i) {
		return getAdaptedArg(ctx, new Substitution(), i);
	}

	private List<Clause> argStrings = new ArrayList<Clause>();
	private List<Fact> args = new ArrayList<Fact>();
}
