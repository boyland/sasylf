package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.term.Facade.Abs;
import static edu.cmu.cs.sasylf.util.Util.debug;
import static edu.cmu.cs.sasylf.util.Util.tdebug;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Pair;
import edu.cmu.cs.sasylf.term.Substitution;
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
		for (int i = 0; i < argStrings.size(); ++i) {
			Clause c = argStrings.get(i);
			// remove all (c) parens:
			while (c.getElements().size() == 1 && c.getElements().get(0) instanceof Clause) {
			  argStrings.set(i,c = (Clause)c.getElements().get(0));
			}
			Fact f = null;
			if (c.getElements().size() == 1) {
			  Element e = c.getElements().get(0);
			  Clause assumes = null;
			  if (e instanceof AssumptionElement) {
			    assumes = ((AssumptionElement)e).getAssumes();
			    e = ((AssumptionElement)e).getBase();
			  }
			  if (e instanceof Binding) {
			    Binding b = (Binding)e;
			    f = new BindingAssumption(b,assumes);
			    f.typecheck(ctx, false);		    
			  } else if (e instanceof NonTerminal) {
			    // case for a reference to a derivation 
			    String s = e.toString();
			    f = ctx.derivationMap.get(s);
			    if (f == null) {
			      FreeVar fake = new FreeVar(s,null);
			      if (ctx.varMap.containsKey(s) || ctx.synMap.containsKey(s) || ctx.inputVars.contains(fake)) {
			        // case for a use of a one element clause
			        f = new SyntaxAssumption(s, getLocation(),assumes);
			        f.typecheck(ctx, false);
			      } else {
			        ErrorHandler.report(Errors.DERIVATION_NOT_FOUND, "No derivation found for " + s, this);
			      }
			    } 
			  } else {
          throw new InternalError("What sort of arg is this ? " + e);
        } 
			} else {
				// case for a clause given directly
			  c = (Clause)c.typecheck(ctx);
				debug("computing fact for " + c + " of class " + c.getClass().getName());
				c = (Clause) c.computeClause(ctx, false);
				if (!(((ClauseUse)c).getConstructor().getType() instanceof Syntax)) {
				  ErrorHandler.report(Errors.SYNTAX_EXPECTED, c);
				}
				argStrings.set(i,c);
				f = new ClauseAssumption(c, getLocation());
				f.typecheck(ctx, false);
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
