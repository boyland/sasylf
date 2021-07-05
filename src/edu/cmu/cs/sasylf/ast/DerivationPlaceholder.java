package edu.cmu.cs.sasylf.ast;

import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.UpdatableErrorReport;

/**
 * A placeholder for a derivation whose contents isn't known.
 * This is used to try to give better error messages for
 * undeclared derivations.
 */
public class DerivationPlaceholder extends Derivation {
	private final FreeVar variable;
	private final UpdatableErrorReport report;
	private Term asTerm;
	
	/**
	 * Create a placeholder derivation with the given name and
	 * report where we can place quick fix information
	 * @param n name of derivation, must not be null
	 * @param rep error report, must not be null
	 */
	public DerivationPlaceholder(String n, UpdatableErrorReport rep) {
		super(n, rep.getSpan().getLocation(), AndClauseUse.makeEmptyAndClause(rep.getSpan().getLocation()));
		variable = FreeVar.fresh(n, Constant.UNKNOWN_TYPE);
		report = rep;
	}

	public UpdatableErrorReport getReport() {
		return report;
	}
	
	/**
	 * Return the term associated with this derivation
	 * (bypassing the element).  
	 * @return term for this derivation, possibly a free variable with 
	 * unknown type.
	 */
	public Term getTerm() {
		if (asTerm != null) return asTerm;
		return variable;
	}
	
	/**
	 * If the substitution contains a binding for the placeholder's variable
	 * then use that information to fill in the placeholder.
	 * @param ctx context
	 * @param sub substitution, must not be null
	 */
	public void setPlaceholder(Context ctx, Substitution sub, Derivation foundIn) {
		if (!sub.getDomain().contains(variable)) return;
		TermPrinter tp = setTerm(ctx, sub.getSubstituted(variable), foundIn);
		for (Map.Entry<FreeVar, NonTerminal> e : tp.getVarMap().entrySet()) {
			FreeVar fv = e.getKey();
			NonTerminal nt = e.getValue();
			sub.add(fv, nt.asTerm());
		}
	}
	
	/**
	 * Set the term that is associated with this placeholder.
	 * This also sets the term and the quick fix information
	 * in the report.  There is some awkwardness if the term associated
	 * with this placeholder ends up being syntax (not a derivation after all).
	 * @param ctx context to use, must not be null
	 * @param t term this variable should be bound to.
	 */
	public TermPrinter setTerm(Context ctx, Term t, Derivation foundIn) {
		if (asTerm != null && !asTerm.equals(t)) {
			throw new IllegalStateException("term already set!");
		}
		TermPrinter tp = new TermPrinter(ctx, ctx.assumedContext, report.getSpan().getEndLocation());
		Constant type = t.getTypeFamily();
		boolean isSyntax = false;
		if (ctx.getSyntax(type) != null) {
			isSyntax = true;
			clause = new Clause(this.getLocation());
			clause.add(tp.asElement(t));
			asTerm = t;
		} else {
			clause = tp.asClause(t);
		}
		clause = clause.typecheck(ctx);
		if (!isSyntax) asTerm = clause.asTerm();
		Set<FreeVar> free = asTerm.getFreeVariables();
		free.removeAll(ctx.inputVars);
		if (free.isEmpty()) {
			if (!isSyntax) {
				// look to see if we can find something that fits what we need
				for (Map.Entry<String,Fact> e : ctx.derivationMap.entrySet()) {
					Fact f = e.getValue();
					if (f != this && asTerm.equals(f.getElement().asTerm())) {
						report.setExtraInformation(f.getName());
						System.out.println("Found replacement: " + f);
						return tp;
					}
				}
			}
		} else {
			ctx.inputVars.addAll(free);
		}
		if (isSyntax) {
			ctx.derivationMap.remove(getName()); // oops, shouldn't be here.
			report.setExtraInformation(tp.toString(clause));
			return tp;
		}
		final int diff = getLocation().getLine()-foundIn.getLocation().getLine();
		report.setExtraInformation(getName() + ": " + tp.toString(clause) + "\n" + diff);
		return tp;
	}
}
