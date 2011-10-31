package edu.cmu.cs.sasylf.ast;

import java.util.*;
import java.io.*;

import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.SASyLFError;

import static edu.cmu.cs.sasylf.util.Util.*;


public class Theorem extends RuleLike {
	public Theorem(String n, Location l) { super(n, l); }

	public List<Fact> getForalls() { return foralls; }
	public List<Element> getPremises() {
		List<Element> l = new ArrayList<Element>();
		for (Fact f : foralls) {
			l.add(f.getElement());
		}
		return l;
	}
	public Clause getConclusion() { return exists; }
	public Clause getExists() { return exists; }
	public List<Derivation> getDerivations() { return derivations; }
	public void setAnd(Theorem next) {
		debug("setting and of "+this.getName() + " to " + next.getName());
		andTheorem = next;
	}

	/** A theorem's existential variables are those that appear in its conclusion
	 * but not in its premises.
	 */
    public Set<FreeVar> getExistentialVars() {
    	Set<FreeVar> vars = exists.asTerm().getFreeVariables();
    	for (Element e : getPremises()) {
    		vars.removeAll(e.asTerm().getFreeVariables());
    	}
    	return vars;
    }

	public void prettyPrint(PrintWriter out) {
		out.println("theorem " + getName() + ":");
		for (Fact forall : getForalls()) {
			out.print("forall ");
			forall.prettyPrint(out);
			out.print(' ');
		}
		out.print("exists ");
		getExists().prettyPrint(out);
		out.println(".");
		for (Derivation d : derivations) {
			d.prettyPrint(out);
		}
		out.println("end theorem\n");
	}
	
	public void checkInterface(Context ctx) {
		if (!interfaceChecked) {
			interfaceChecked = true;
			for (Fact f : foralls) {
				f.typecheck(ctx, false);
			}
	
			exists.typecheck(ctx);
			exists = (Clause) exists.computeClause(ctx, false);
		}
	}

	public void typecheck(Context ctx) {
		int oldErrorCount = ErrorHandler.getErrorCount();
		try {
		debug("checking theorem "+this.getName());
		
		// TODO: check that if one arg of a theorem has a context Gamma, all other arguments and result have the same context,
		// or else nothing in Gamma could be free in any of their expressions
		ctx.derivationMap = new HashMap<String, Fact>();
		ctx.inputVars = new HashSet<FreeVar>();
		ctx.outputVars = new HashSet<FreeVar>();
		ctx.currentSub = new Substitution();
		ctx.currentTheorem = this;
		ctx.inductionVariable = null;
		ctx.bindingTypes = new HashMap<String, List<ElemType>>();
		ctx.adaptationMap = new HashMap<NonTerminal,AdaptationInfo>();
		ctx.innermostGamma = null;
		
		checkInterface(ctx);
		
		for (Fact f : foralls) {
			f.addToDerivationMap(ctx);
			ctx.derivationMap.put(f.getName(), f);
			ctx.inputVars.addAll(f.getElement().asTerm().getFreeVariables());
			if (f instanceof DerivationByAssumption) {
				ClauseUse cu = (ClauseUse)f.getElement();
				cu.asTerm();
				if (cu.getRoot() != null) {
					debug("set innermostGamma to " + cu.getRoot());
					if (ctx.innermostGamma == null) {
						ctx.innermostGamma = cu.getRoot();
						ctx.adaptationRoot = cu.getRoot();
					} else
						if (!ctx.innermostGamma.equals(cu.getRoot()))
							ErrorHandler.report(Errors.INCONSISTENT_CONTEXTS,"Theorem has inconsistent contexts " + ctx.innermostGamma + " and " + cu.getRoot(), this);
				}
			}
		}

		Term theoremTerm = exists.asTerm();
		ctx.currentGoal = theoremTerm;
		ctx.currentGoalClause = exists;
		ctx.outputVars.addAll(theoremTerm.getFreeVariables());
		ctx.outputVars.removeAll(ctx.inputVars);
		
		if (andTheorem != null) {
			andTheorem.addToMap(ctx);
		}

		boolean derivationErrors = false;
		for (Derivation d : derivations) {
			try {
				d.typecheck(ctx);
			} catch (SASyLFError e) {
				// already reported, swallow the exception
				derivationErrors = true;
			}
		}

		if (!derivationErrors)
			verifyLastDerivation(ctx, theoremTerm, exists, derivations, this);
		
		ctx.ruleMap.put(getName(), this);
		ctx.recursiveTheorems = new HashMap<String, Theorem>();
		} catch (SASyLFError e) {
			// ignore the error; it has already been reported
			//e.printStackTrace();
		} finally {
			int newErrorCount = ErrorHandler.getErrorCount() - oldErrorCount;
			if (edu.cmu.cs.sasylf.util.Util.VERBOSE) {
				if (newErrorCount == 0) {
					System.out.println("Theorem " + getName() + " OK");
				} else {
					System.out.println("Error(s) in theorem " + getName());					
				}
			}
		}
	}

	private void addToMap(Context ctx) {
		checkInterface(ctx);
		ctx.recursiveTheorems.put(getName(), this);
		
		if (andTheorem != null) {
			andTheorem.addToMap(ctx);
		}
	}

	/**
	 * Verifies that the last derivation is what this theorem requires
	 * 
	 * @param ctx
	 * @param theoremTerm
	 */
	public static void verifyLastDerivation(Context ctx, Term theoremTerm, Element theoremElem, List<Derivation> derivs, Node errorNode) {
		// verify: that last derivation is what theorem requires
		if (derivs.size() == 0)
			ErrorHandler.report(Errors.NO_DERIVATION, errorNode);
		else {
			Derivation last = derivs.get(derivs.size()-1);
			Term derivTerm = DerivationByAnalysis.adapt(last.getElement().asTerm(), ((ClauseUse)last.getElement()).getRoot(), ctx);
			debug("orig theoremTerm: "+theoremTerm);
			debug("orig derivTerm: "+derivTerm);
			theoremTerm = DerivationByAnalysis.adapt(theoremTerm, ((ClauseUse)theoremElem).getRoot(), ctx);
			debug("adapted theoremTerm: " + theoremTerm);

			try {
				debug("end of theorem ("+last.getLocation().getLine()+"): unifying " + derivTerm + " to match " + theoremTerm);
				debug("current sub = " + ctx.currentSub);
				Substitution instanceSub = derivTerm.instanceOf(theoremTerm);
				// must not require instantiating free variables
				if (!instanceSub.avoid(ctx.inputVars)) {
					Set<FreeVar> unavoidable = instanceSub.selectUnavoidable(ctx.inputVars);
					ErrorHandler.report(Errors.WRONG_RESULT,"\n    could not avoid vars " + unavoidable, last);
				}
				// TODO: probably should add to currentSub for DerivationByAnalysis, but it's not clear this is necessary for soundness
				// important - would need to do it BEFORE analyzing the cases, then take it out of currentSub.  So it doesn't work to put it here, as below.
				/*if (!instanceSub.getMap().isEmpty() && last instanceof DerivationByAnalysis) {
					// could support this by adding to currentSub and replacing in outputVars, but for now just say it's an error
					//ctx.currentSub.compose(instanceSub);
					tdebug("instanceSub = " + instanceSub);
					ErrorHandler.report(Errors.WRONG_RESULT,"\n    SASyLF does not currently support substituting one variable with another in the expected result during case analysis", last);
				}*/
			} catch (UnificationFailed e) {
				ErrorHandler.report(Errors.WRONG_RESULT, last, "\twas checking " + derivTerm + " instance of " + theoremTerm);
			}

		}
	}


	public void setExists(Clause c) { exists = c; }

	private List<Fact> foralls = new ArrayList<Fact>();
	private Clause exists;
	private List<Derivation> derivations = new ArrayList<Derivation>();
	private Theorem andTheorem;
	private boolean interfaceChecked=false;

}

