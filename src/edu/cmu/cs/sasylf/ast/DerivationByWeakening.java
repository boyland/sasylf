package edu.cmu.cs.sasylf.ast;

import java.util.Objects;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;

public class DerivationByWeakening extends DerivationWithArgs {
	public DerivationByWeakening(String n, Location l, Clause c) {
		super(n,l,c);
	}

	@Override
	public String prettyPrintByClause() {
		return " by weakening";
	}

	@Override
	public void typecheck(Context ctx) {
		super.typecheck(ctx);

		if (this.getArgs().size() != 1) {
			ErrorHandler.error(Errors.WRONG_WEAKENING_ARGUMENTS, this);
			return;
		}

		Fact arg = getArgs().get(0);
		Element e = arg.getElement();

		if (!(e.getType() instanceof Judgment)) {
			ErrorHandler.error(Errors.WEAKENING_SYNTAX, this);
		}

		NonTerminal srcRoot = e.getRoot();
		NonTerminal trgRoot = getClause().getRoot();
		Term source = ctx.toTerm(e);
		Term result = ctx.toTerm(getClause());

		if (source.equals(result) && Objects.equals(srcRoot, trgRoot)) {
			ErrorHandler.error(Errors.WEAKENING_NOP, this);
		}
		
		// perform relaxation first
		while (srcRoot != null && !srcRoot.equals(trgRoot)) {
			Relaxation r;
			if (ctx.relaxationMap == null || (r = ctx.relaxationMap.get(srcRoot)) == null || !r.getResult().equals(trgRoot)) {
				ErrorHandler.error(Errors.RELAX_UNKNOWN, srcRoot + " <= " + trgRoot, this);    
				return;
			}
			Term relaxed = r.relax(source);
			if (relaxed == null) {
				ErrorHandler.error(Errors.RELAX_WRONG, this);
			}
			source = relaxed;
			srcRoot = r.getResult();
		}

		while (source instanceof Abstraction) {
			Abstraction ab1 = (Abstraction)source;
			if (!(result instanceof Abstraction)) {
				ErrorHandler.error(Errors.BAD_WEAKENING, this); // missing variable binding for ab1.varName in result
				return;
			}
			Abstraction ab2 = (Abstraction)result;
			if (ab1.varName.equals(ab2.varName)) {
				if (!ab1.varType.equals(ab2.varType)) {
					ErrorHandler.error(Errors.BAD_WEAKENING, this); // variable binding for ab1.varName different in result
					return;
				}
				source = ab1.getBody();
				result = ab2.getBody();
			} else {
				// This is presumed to be a variable that is added.
				// It should not be used (or else it's not a weakening!)
				// But because the way contexts work, a syntax variable
				// is ALWAYS used in the next variable (the assumption rule).
				// So, we can't give an error if it's used
				// but we also can't just ignore it (see bad74.slf).
				// So we replace it with a generated free variable.
				FreeVar v = FreeVar.fresh(ab2.varName, ab2.varType);
				result = Facade.App(result, v);
			}
		}
		while (result instanceof Abstraction) {
			result = ((Abstraction)result).getBody();
			// At this point, it's OK to just ignore the variable.
			// BY decrementing the deBruijn, we let legitimate variables
			// match the source, where these supposedly unused new variables go negative.
			result = result.incrFreeDeBruijn(-1); // remove variable      
		}
		if (!result.equals(source)) {
			ErrorHandler.error(Errors.BAD_WEAKENING, this, result + " != " + source); // main part of derivation is different
			return;
		}

		checkRootMatch(ctx,e,this.getElement(),this);

		// System.out.println("Weakening succeeds!");

		// Permit induction on this term if source was a subderivation
		if (ctx.subderivations.containsKey(arg))
			ctx.subderivations.put(this,ctx.subderivations.get(arg));
	}
}
