package edu.cmu.cs.sasylf.ast;

import java.util.LinkedList;
import java.util.List;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.BoundVar;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;

public class DerivationByExchange extends DerivationWithArgs {
	public DerivationByExchange(String n, Location l, Clause c) {
		super(n,l,c);
	}

	@Override
	public String prettyPrintByClause() {
		return " by exchange";
	}

	@Override
	public void typecheck(Context ctx) {
		super.typecheck(ctx);

		if (this.getArgs().size() != 1) {
			ErrorHandler.error(Errors.WRONG_EXCHANGE_ARGUMENTS, this);
			return;
		}

		Fact arg = getArgs().get(0);
		Element e = arg.getElement();
		if (!(e.getType() instanceof Judgment)) {
			ErrorHandler.error(Errors.EXCHANGE_SYNTAX, this);
		}
		checkRootMatch("exchange", (ClauseUse)e, (ClauseUse)getClause(), this);

		Term adapted = ctx.toTerm(e);
		//System.out.println("Exchange arg, adapted = " + adapted);
		Term result = ctx.toTerm(getClause());
		// System.out.println("***** " + getLocation().getLine() + ": Exchange result: " + result + " on line " + getLocation().getLine());

		if (!checkExchange(result,adapted)) {
			ErrorHandler.error(Errors.BAD_EXCHANGE, this);
		}

		if (ctx.subderivations.containsKey(arg))
			ctx.subderivations.put(this,ctx.subderivations.get(arg));
	}

	private static boolean checkExchange(Term t1, Term t2) {
		// NB: If the following line is uncommented,
		// exchange will be (safely) allowed in more situations
		// but the cases where it works will be harder to explain.
		// if (t1.equals(t2)) return true;
		if (t1 instanceof Abstraction) {
			Abstraction ab1 = (Abstraction)t1;
			List<Term> remaining = new LinkedList<Term>();
			//System.out.println("Checking " + t2);
			//System.out.println(" against " + ab1);
			Term body2 = removeMatching(t2,remaining,ab1.varName,ab1.varType);
			if (body2 == null) return false;
			//System.out.println("Proceeding with exchanged body: " + body2);
			return checkExchange(ab1.getBody(),body2);
		} else return t1.equals(t2);
	}

	/**
	 * Remove a matching abstraction from a function type.
	 * All uses of the bound variable should be directed to a bound var
	 * |rem| before this.
	 * Return the new abstraction.
	 * @param t term to look for abstraction
	 * @param rem terms between the new location of the bound variable and here
	 * @param name name of the formal to remove
	 * @param type type of the formal to remove
	 * @return null if formal not found, or if its type was wrong
	 * Otherwise a term in which an abstraction for this formal is removed
	 * and all references to the variable are redirected to a |rem| outer formal.
	 * NB: Any bindings beyond |rem| need to be bumped to give space for the new variable.
	 */
	private static Term removeMatching(Term t, List<Term> rem, String name, Term type) {
		// System.out.println("removeMatching(" + t + "," + rem + "," + name + "," + type + ")");
		if (t instanceof Abstraction) {
			Abstraction ab = (Abstraction)t;
			if (ab.varName.equals(name)) {
				if (ab.varType.equals(type)) {
					rem.add(new BoundVar(rem.size()+1));
					Term result = ab.getBody().apply(rem, rem.size());
					// System.out.println("  => " + result);
					return result;
				}
				// System.out.println("  => null // wrong type");
				return null;
			} else {
				rem.add(0, new BoundVar(rem.size()+1));
				Term result = removeMatching(ab.getBody(),rem,name,type.incrFreeDeBruijn(1));
				if (result == null) return null;
				result = Abstraction.make(ab.varName,ab.varType.incrFreeDeBruijn(rem.size()-1, 1),result);
				// System.out.println("  => " + result);
				return result;
			}
		}
		// System.out.println("  => null // hit end");
		return null;
	}
}
