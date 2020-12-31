package edu.cmu.cs.sasylf.ast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.BoundVar;
import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.Util;


public class DerivationBySubstitution extends DerivationWithArgs {
	public DerivationBySubstitution(String n, Location l, Clause c) {
		super(n,l,c);
	}

	@Override
	public String prettyPrintByClause() {
		return " by substitution";
	}

	@Override
	public void typecheck(Context ctx) {
		super.typecheck(ctx);

		if (this.getArgs().size() != 2) {
			ErrorHandler.error(Errors.WRONG_SUBSTITUTION_ARGUMENTS, this);
		}

		// get terms for arguments
		Element arg0 = this.getArgs().get(0).getElement();
		Element arg1 = this.getArgs().get(1).getElement();

		if (!(arg0.getType() instanceof Judgment)) {
			ErrorHandler.error(Errors.SUBSTITUTION_ARGUMENT, this.getArgStrings().get(0));
		}
		if (!(arg1.getType() instanceof Judgment)) {
			ErrorHandler.error(Errors.SUBSTITUTION_ARGUMENT, this.getArgStrings().get(1));
		}

		checkRootMatch("substitution", (ClauseUse)arg0, (ClauseUse)getClause(), this);
		checkRootMatch("substitution", (ClauseUse)arg1, (ClauseUse)getClause(), this);

		Term subContext = ctx.toTerm(arg0);
		Term source = ctx.toTerm(arg1);

		Util.debug(this.getLocation());
		Util.debug("subContext = ", subContext);
		Util.debug("source = ", source);

		Term result = doSubst(ctx, subContext, source, new ArrayDeque<Term>());

		Util.debug("result = ", result);

		// verify result is the second substituted for (and eliminating) the assumption of the first
		Term claimedResult = ctx.toTerm(getClause());
		Util.debug("claimed = ", claimedResult);

		if (!result.equals(claimedResult)) {
			TermPrinter tp = new TermPrinter(ctx, arg0.getRoot(), getLocation(),false);
			ErrorHandler.error(Errors.SUBSTITUTION_OTHER,tp.toString(result,true),this);
		} 

		// Permit induction on this term if source was a subderivation
		if (ctx.subderivations.containsKey(getArgs().get(0))) {
			// Must require that substitution does not include course type!
			// System.out.println("Type of base is " + subContext.getTypeFamily());
			// System.out.println("Type of actual is " + source.getTypeFamily());
			if (!FreeVar.canAppearIn(subContext.getTypeFamily(),source.getTypeFamily())) {
				ctx.subderivations.put(this,ctx.subderivations.get(getArgs().get(0)));
			}
		}

	}

	/**
	 * @param func An abstraction we try to call with the arg
	 * @param arg A (possibly dependent) term to use as argument
	 * @param varTypes types of bound variables in scope, pushed on. 
	 * @return result term
	 * @throws SASyLFError if an error is found
	 */
	private Term doSubst(Context ctx, Term func, Term arg, Deque<Term> varTypes) {
		if (func instanceof Abstraction) {
			Abstraction f = (Abstraction)func;
			if (arg instanceof Abstraction) {
				Abstraction a = (Abstraction)arg;
				if (!f.varType.equals(a.varType)) {
					ErrorHandler.error(Errors.SUBSTITUTION_NO_PREFIX, this);
				}
				varTypes.push(f.varType);
				Term result = doSubst(ctx, f.getBody(),a.getBody(),varTypes);
				varTypes.pop();
				return Abstraction.make(f.varName, f.varType, result);
			} else if (f.varType.equals(arg)){
				ErrorHandler.warning(Errors.INTERNAL_ERROR, "Internal warning: Using a new feature?", this,null);
				if (f.getBody().hasBoundVar(1)) {
					ErrorHandler.error(Errors.INTERNAL_ERROR, "Internal error: uses context derivation?", this);
				}
				return f.getBody().incrFreeDeBruijn(-1);
			} else if (f.getBody() instanceof Abstraction) {
				Term tyV = f.varType;
				for (Term ty : varTypes) {
					tyV = Facade.Abs(ty, tyV);
				}
				FreeVar v = FreeVar.fresh("subst", tyV);
				int n = varTypes.size();
				Term app = v;
				if (n > 0) {
					List<Term> va = new ArrayList<Term>();
					for (int i=0; i < n; ++i) {
						va.add(new BoundVar(n-i));
					}
					app = Facade.App(v, va);
				} 
				Term fv = f.apply(Collections.singletonList(app), 0);
				Abstraction f2 = (Abstraction)fv;
				Util.debug("About to unify ", f2.varType, " and ", arg);
				Substitution sub;
				try {
					sub = f2.varType.unify(arg);
				} catch (UnificationFailed e) {
					ErrorHandler.error(Errors.SUBSTITUTION_FAILED, this, "  (" + f2.varType + " != " + arg + ")");
					return null; //NOTREACHED
				}
				Set<FreeVar> unavoided = sub.selectUnavoidable(ctx.inputVars);
				Util.debug(" sub = ",sub);
				if (!unavoided.isEmpty()) {
					ErrorHandler.error(Errors.SUBSTITUTION_CONSTRAIN, unavoided.toString(), this);
				}
				Term result = f2.getBody();
				if (result.hasBoundVar(1)) {
					ErrorHandler.error(Errors.INTERNAL_ERROR,"Internal error: uses context derivation?", this);
				} 
				return result.incrFreeDeBruijn(-1).substitute(sub);
			} else {
				ErrorHandler.error(Errors.SUBSTITUTION_FAILED,  this);
			}
		}
		ErrorHandler.error(Errors.SUBSTITUTION_NO_HOLE, this);
		return null;
	}

}
