package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;

import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;

public abstract class SyntaxAssumption extends Fact {

	public SyntaxAssumption(String n, Location l, Element assumes) {
		super(n, l);
		context = assumes;
		if (contextIsUnknown()) {
			new Throwable("for trace").printStackTrace();
		}
		if (assumes != null) {
			setEndLocation(assumes.getEndLocation());
		}
	}

	@Override
	public void prettyPrint(PrintWriter out) {
		getElementBase().prettyPrint(out);
		if (context != null) {
			out.print(" assumes ");
			if (contextIsUnknown()) out.print("?");
			else context.prettyPrint(out);
		}
	}

	@Override
	public void printReference(PrintWriter out) {
		out.print('(');
		prettyPrint(out);
		out.print(')');
	}

	@Override 
	public int hashCode() {
		int h = getElementBase().hashCode();
		if (context == null) return h;
		return (context.hashCode() << 3) ^ h;
	}

	@Override
	public boolean equals(Object x) {
		if (x instanceof SyntaxAssumption) {
			SyntaxAssumption sa = (SyntaxAssumption)x;
			return getElementBase().equals(sa.getElementBase()) &&
					(context == sa.getContext() || context != null && context.equals(sa.getContext()));
		}
		return false;
	}

	@Override
	public void typecheck(Context ctx) {
		if (context != null) {
			context = context.typecheck(ctx);
			if (context instanceof Clause) {
				context = ((Clause)context).computeClause(ctx,false);
			}
			ElementType type = context.getType();
			if (!(type instanceof SyntaxDeclaration) || !((SyntaxDeclaration)type).isInContextForm()) {
				ErrorHandler.error(Errors.ILLEGAL_ASSUMES_CLAUSE,": " + type, this);
			}
		}
	}

	@Override
	public final Element getElement() {
		Element base = getElementBase();
		if (context == null) return base;
		return new AssumptionElement(getLocation(),base,context);
	}

	/**
	 * Return the element for this fact ignoring the context.
	 * @return element base for this fact.
	 */
	public abstract Element getElementBase();

	protected boolean contextIsUnknown() {
		return context != null && context instanceof Clause && ((Clause)context).getElements().size() == 0;
	}

	public void setContext(Element c) { context = c; }
	public Element getContext() { return context; }

	public NonTerminal getRoot() {
		if (context == null) return null;
		if (context instanceof NonTerminal) return (NonTerminal) context;
		if (context instanceof ClauseUse) return ((ClauseUse)context).getRoot();
		throw new RuntimeException("no root for SyntaxAssumption: " + this);
	}

	private Element context;

}

