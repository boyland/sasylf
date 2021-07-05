package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.DefaultSpan;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.Span;
import edu.cmu.cs.sasylf.util.UpdatableErrorReport;
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
	
	/**
	 * Get the span of the list of arguments
	 * @return
	 */
	public Span getArgSpan() {
		if (argStrings.isEmpty()) return this.getEndLocation();
		Location l1 = argStrings.get(0).getLocation();
		Location l2 = argStrings.get(argStrings.size()-1).getEndLocation();
		return new DefaultSpan(l1,l2);
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
		
		final NonTerminal root = getClause().getRoot();
		if (!ctx.isKnownContext(root)) {
			ErrorHandler.error(Errors.UNKNOWN_CONTEXT, root.toString(), root);
		}

		args.clear(); // needed for idempotency
		for (int i = 0; i < argStrings.size(); ++i) {
			Clause c = argStrings.get(i);
			// remove all (c) parens:
			while (c.getElements().size() == 1 && c.getElements().get(0) instanceof Clause) {
				argStrings.set(i,c = (Clause)c.getElements().get(0));
			}
			Fact f = parseAsDerivation(ctx, c); // maybe this argument refers to a derivation (or derivations)
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
					ErrorHandler.error(Errors.VAR_UNBOUND, e.toString(), e);
				}
				if (!(e.getType() instanceof Syntax)) {
					ErrorHandler.error(Errors.SYNTAX_EXPECTED, c);
				}
				f = e.asFact(ctx, ctx.assumedContext);
			}
			if (!ctx.isKnownContext(f.getElement().getRoot())) {
				ErrorHandler.error(Errors.UNKNOWN_CONTEXT,f.getElement().getRoot().toString(),c);
			}
			c.checkBindings(ctx.bindingTypes, c);
			args.add(f);
		}
	}

	/**
	 * If an argument is undeclared, can we handle a placeholder?
	 * @return
	 */
	protected boolean acceptPlaceholder() {
		return false;
	}
	
	/**
	 * Try to parse a clause as a conjunction of derivations.
	 * @param ctx
	 * @param cl clause to examine must not be null
	 * @return derivation from this clause, or null if it does something we can't see as a derivation
	 */
	protected Derivation parseAsDerivation(Context ctx, Clause cl) {
		List<Element> elements = cl.getElements();
		if (elements.size() == 0) return null; //XXX: or maybe as an empty AndClauseUse ?
		boolean needComma = false;
		List<Derivation> pieces = new ArrayList<>();
		for (Element e : elements) {
			if (needComma) {
				if (!(e instanceof Terminal)) return null;
				if (!e.toString().equals(",")) return null;
				needComma = false;
			} else {
				Fact f = null;
				if (e instanceof NonTerminal) {
					String s = e.toString();
					if (s.equals("_")) { // treat "_" specially
						do {
							FreeVar v = FreeVar.fresh("", Constant.UNKNOWN_TYPE);
							s = v.toString();
						} while (ctx.derivationMap.containsKey(s));
					}
					f = ctx.derivationMap.get(s);
					if (f == null && !ctx.isKnown(s)) {
						UpdatableErrorReport report = new UpdatableErrorReport(Errors.DERIVATION_NOT_FOUND, e.toString(), e);
						ErrorHandler.report(report);
						if (acceptPlaceholder()) {
							f = new DerivationPlaceholder(s, report);
							((Derivation)f).typecheckAndAssume(ctx);
						} else {
							throw new SASyLFError(report);
						}
					}
				} else if (e instanceof Clause) {
					f = parseAsDerivation(ctx,(Clause)e);
				}
				if (!(f instanceof Derivation)) return null;
				pieces.add((Derivation)f);
				needComma = true;
			} 
		}
		Util.verify(pieces.size() > 0, "Must have at least one piece to get here");
		if (pieces.size() == 1) return pieces.get(0);
		List<ClauseUse> clauses = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		for (Fact f : pieces) {
			if (f instanceof DerivationPlaceholder) {
				DerivationPlaceholder ph = (DerivationPlaceholder)f;
				throw new SASyLFError(ph.getReport());
			}
			if (!clauses.isEmpty()) sb.append(",");
			sb.append(f.getName());
			final Element element = f.getElement();
			if (!(element instanceof ClauseUse)) return null;
			clauses.add((ClauseUse)element);
		}
		final AndClauseUse combined = AndClauseUse.makeAndClause(cl.getLocation(), ctx, clauses);
		combined.setEndLocation(cl.getEndLocation());
		final DerivationByAssumption result = new DerivationByAssumption(sb.toString(),cl.getLocation(),combined);
		result.setEndLocation(cl.getEndLocation());
		return result;
	}
	
	/** Gets the ith argument as a term and adapts it to the current context
	 */
	protected Term getAdaptedArg(Context ctx, int i) {
		Element element = getArgs().get(i).getElement();
		Term argTerm = ctx.toTerm(element);
		return argTerm;
	}
}
