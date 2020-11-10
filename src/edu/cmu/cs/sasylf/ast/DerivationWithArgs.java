package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Util;

public abstract class DerivationWithArgs extends Derivation {
	// TODO: change to have Element instead, and change names
	private List<Clause> argStrings = new ArrayList<Clause>();
	private List<Fact> args = new ArrayList<Fact>();
	
	public DerivationWithArgs(String n, Location l, Clause c) {
		super(n,l,c);
	}

	/**
	 * Add an argument, and update the end location
	 * @param cl clause to add, must not be null
	 */
	public void addArgString(Clause cl) {
		argStrings.add(cl);
		Location endLocation = cl.getEndLocation();
		if (endLocation != null) {
			setEndLocation(endLocation);
		}
	}
	
	/**
	 * Add an argument which is just a string.
	 * @param s string to add, must not be null
	 */
	public void addArgString(String s) {
		final Location l = super.getEndLocation();
		Clause cl = new Clause(l);
		cl.getElements().add(new NonTerminal(s,l));
		argStrings.add(cl);
	}
	
	

	public List<Clause> getArgStrings() { return argStrings; }
	public List<Fact> getArgs() { return args; }

	/**
	 * Return the expected clause type for the specified argument.
	 * This is used to break ambiguity in parsing, and is really
	 * only useful if the return is a nonterminal.
	 * This information in only advisory; we don't promise to check it.
	 * @param i index of parameter (0 based)
	 * @return type expected for the i'th parameter or null if there is no special expectation
	 */
	protected ClauseType getClauseTypeExpected(Context ctx, int i) {
		return null;
	}
	
	protected abstract String prettyPrintByClause();

	@Override
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

	@Override
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
					if (cl.getElements().size() == 1 && !(cl.getElements().get(0) instanceof Terminal)) {
						e = cl.getElements().get(0);
						Util.verify(!(e instanceof Clause), "clause should have been removed before");
					} else {
						ClauseType expected = getClauseTypeExpected(ctx,i);
						if (expected instanceof SyntaxDeclaration) {
							e = cl.computeClause(ctx, ((SyntaxDeclaration)expected).getNonTerminal());
						} else {
							e = cl.computeClause(ctx, false);
						}
					}
				}
				if (e instanceof Variable) {
					ErrorHandler.report(Errors.UNBOUND_VAR_USE, "Variable found outside of a binding context.", c);
				}
				if (!(e.getType() instanceof Syntax)) {
					ErrorHandler.report(Errors.SYNTAX_EXPECTED, c);
				}
				f = e.asFact(ctx, ctx.assumedContext);
			}
			if (!ctx.isKnownContext(f.getElement().getRoot())) {
				ErrorHandler.report(Errors.UNKNOWN_CONTEXT,f.getElement().getRoot().toString(),this);
			}
			c.checkBindings(ctx.bindingTypes, c);
			args.add(f);
		}
	}

	/** Gets the ith argument as a term and adapts it to the current context
	 */
	protected Term getAdaptedArg(Context ctx, int i) {
		Element element = getArgs().get(i).getElement();
		Term argTerm = ctx.toTerm(element);
		return argTerm;
	}
}
