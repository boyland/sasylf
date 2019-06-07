package edu.cmu.cs.sasylf.reduction;

import java.util.List;

import edu.cmu.cs.sasylf.ast.AssumptionElement;
import edu.cmu.cs.sasylf.ast.Binding;
import edu.cmu.cs.sasylf.ast.Clause;
import edu.cmu.cs.sasylf.ast.Context;
import edu.cmu.cs.sasylf.ast.Element;
import edu.cmu.cs.sasylf.ast.Fact;
import edu.cmu.cs.sasylf.ast.Node;
import edu.cmu.cs.sasylf.ast.NonTerminal;
import edu.cmu.cs.sasylf.ast.Terminal;
import edu.cmu.cs.sasylf.ast.Theorem;
import edu.cmu.cs.sasylf.util.ErrorHandler;

/**
 * A reduction schema; how to permit "recursion" in theorems.
 */
public abstract class InductionSchema {
	/**
	 * Does this schema match the argument?
	 * All mutually inductive theorems must use matching schemas.
	 * If a problem is found alert the error handler unless
	 * the error point is null.
	 * @param s reduction schema to match, must not be null
	 * @param errorPoint location to complain about a non-match.  Null if no
	 * errors should be recorded.
	 * @param equality require the two schemas to be equal
	 * @return whether this schema matches the argument.
	 */
	public abstract boolean matches(InductionSchema s, Node errorPoint, boolean equality);

	/**
	 * Does this instance of mutual induction following the schema?
	 * We return EQUAL if the induction information used is unchanged;
	 * we return LESS if the induction information is definitely smaller.
	 * We return NONE if neither situation obtains.
	 * @param ctx global information, must not be null
	 * @param s schema of the callee, must match this
	 * @param args actual parameters of the induction, must not be null
	 * @param errorPoint location to complain about a problem, Null if no
	 * errors should be recorded.
	 * @return whether the induction is possible, null is not permitted
	 */
	public abstract Reduction reduces(Context ctx, InductionSchema s, List<Fact> args, Node errorPoint);

	/**
	 * Return a human-readable short description of this induction schema.
	 * @return string (never null) describing this induction schema.
	 */
	public abstract String describe();

	@Override
	public String toString() {
		return describe();
	}

	@Override
	public final boolean equals(Object x) {
		if (!(x instanceof InductionSchema)) return false;
		return matches((InductionSchema)x,null,true);
	}

	@Override
	public abstract int hashCode();

	public static final InductionSchema nullInduction = LexicographicOrder.create();

	/**
	 * Create an induction schema signified by the given list.
	 * If there are multiple elements, it will create a lexicographical order.
	 * Return null if there is an error.  Error will be reported if
	 * errorPoint is non-null.
	 * @param thm context information, must not be null
	 * @param args series of induction specifications, must not be null
	 * @param errorPoint point to report errors for, if not null
	 * @return induction schema, or null
	 */
	public static InductionSchema create(Theorem thm, List<Clause> args, Node errorPoint) {
		InductionSchema result = nullInduction;
		for (Clause cl : args) {
			InductionSchema is = create(thm,cl,errorPoint);
			if (is == null) return null;
			result = LexicographicOrder.create(result,is);
		}
		return result;
	}

	public static InductionSchema create(Theorem thm, Element arg, Node errorPoint) {
		if (arg instanceof NonTerminal) {
			return StructuralInduction.create(thm, ((NonTerminal)arg).getSymbol(), errorPoint);
		} else if (arg instanceof AssumptionElement) {
			return create(thm, ((AssumptionElement)arg).getBase(), errorPoint);
		} else if (arg instanceof Binding) {
			return StructuralInduction.create(thm, ((Binding)arg).getNonTerminal().getSymbol(), errorPoint);
		} else if (arg instanceof Clause) {
			Clause cl = (Clause)arg;
			return parse(thm,cl.getElements(), errorPoint);
		}
		if (errorPoint != null) {
			ErrorHandler.recoverableError("Cannot parse induction schema: " + arg, errorPoint);
		}
		return null;    
	}

	/**
	 * Parse a clause that is supposed to refer to an induction schema.
	 * We use the following grammar:
	 * <pre>
	 * l ::= t l | t      // Unordered
	 * t ::= f SEP t | f  // Lexicographic
	 * SEP ::= "," | ">"
	 * </pre> 
	 * @param thm context information, must not be null
	 * @param parts elements of the clause
	 * @param errorPoint place to note errors
	 * @return induction schema or null (if an error was reported)
	 */
	private static InductionSchema parse(Theorem thm, List<Element> parts, Node errorPoint) {
		if (parts.isEmpty()) {
			if (errorPoint != null) {
				ErrorHandler.recoverableError("empty induction description",errorPoint);
			}
			return null;
		}
		InductionSchema result = null;
		InductionSchema factor = create(thm,parts.get(0),errorPoint);
		int i = 1;
		while (i < parts.size()) {
			Element e = parts.get(i);
			if (Terminal.matches(e, ",") || Terminal.matches(e, ">")) {
				if (++i >= parts.size()) {
					if (errorPoint != null) {
						ErrorHandler.recoverableError("induction description too short", errorPoint);
					}
					return null;
				}
				factor = LexicographicOrder.create(factor,create(thm,parts.get(i),errorPoint));
			} else {
				if (result == null) result = factor;
				else result = Unordered.create(result,factor);
				factor = create(thm,e,errorPoint);
			}
			++i;
		}
		if (result == null) result = factor;
		else result = Unordered.create(result,factor);
		return result;
	}
}
