package edu.cmu.cs.sasylf.ast;

import java.util.*;
import java.io.*;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.util.ErrorHandler;

import static edu.cmu.cs.sasylf.term.Facade.App;
import static edu.cmu.cs.sasylf.util.Util.*;

public abstract class DerivationByIHRule extends DerivationWithArgs {
	public DerivationByIHRule(String n, Location l, Clause c) {
		super(n,l,c);
	}

	abstract public RuleLike getRule(Context ctx);
	abstract public String getRuleName();


	/* TODO: would be nice to have 4 error messages for unification failed:
	 * 1) you assumed more than was justified in the conclusion
	 * 2) after applying that rule to the premises you should get a conclusion of X (if unifies with premises)
	 * 3) if you want a conclusion of X you need a premise of form Y (if unifies with conclusion)
	 * 4) worse case: you can't apply that rule to the premises (if doesn't unify with premises or conclusion)
	 */ 
	public void typecheck(Context ctx) {
		super.typecheck(ctx);

		debug("line: " + this.getLocation().getLine());

		// make sure the number of arguments is correct
		if (getArgs().size() != getRule(ctx).getPremises().size())
			ErrorHandler.report(Errors.RULE_PREMISE_NUMBER, getRuleName(), this);

		// build ruleTerm: the rule claimed by the user, with all free variables freshified,
		// and adapted to the variables in scope at this point (i.e. Gamma unrolled as necessary)
		Element elem = getElement();
		Term derivTerm = DerivationByAnalysis.adapt(elem.asTerm(), elem, ctx, false);
			//getElement().asTerm().substitute(ctx.currentSub);
		Substitution wrappingSub = new Substitution(); // TODO: use carried adaptation sub
		
		// build appliedTerm: the element claimed by the user for the conclusion and the
		// premises claimed by the user as the arguments
		List<Term> termArgs = new ArrayList<Term>();
		List<Term> termArgsWithVar = new ArrayList<Term>();
		Set<FreeVar> argFreeVars = new HashSet<FreeVar>();
		for (int i = 0; i < getArgs().size(); ++i) {
			Term argTerm = getAdaptedArg(ctx, wrappingSub, i);
			termArgs.add(argTerm);
			termArgsWithVar.add(argTerm);
			argFreeVars.addAll(argTerm.getFreeVariables());
		}
		// add a FreeVar for the conclusion; we are trying to discover what this should be
		termArgs.add(derivTerm); // tried concTermVar
		FreeVar concTermVar = FreeVar.fresh("conclusion", Constant.UNKNOWN_TYPE); 
		termArgsWithVar.add(concTermVar);
		Term appliedTerm = App(getRule(ctx).getRuleAppConstant(), termArgs);
		Term appliedTermWithVar = App(getRule(ctx).getRuleAppConstant(), termArgsWithVar);
		Term ruleTerm = getRule(ctx).getFreshRuleAppTerm(derivTerm, wrappingSub, termArgs);

		// unify the two terms, and check that the appropriate instance relationships hold
		Substitution sub;
		try {
			sub = appliedTerm.unify(ruleTerm);

			// check to make sure the user didn't assume too much in derivTerm
			// for RULES, this is already covered
				// case 1: instantiate var free in conclusion but not in premises - can do so in any way
				// case 2: instantiate var free in conclusion and also in premises - will match something in actual premises
					// after matching premises, the only free vars will be argFreeVars
					// if derivTerm assumes too much, it will be because it instantiated something in argFreeVars
			// for LEMMAS, we need to ensure that in case 1, the vars are not instantiated (we assume they are existentials)
			
			Set<FreeVar> mustAvoid = new HashSet<FreeVar>(ctx.inputVars);
			
			if (getRule(ctx) instanceof Theorem) {
				// compute the set of variables that appear in the rule's conclusion but not its arguments
				List<? extends Term> ruleTermArgs = ((Application)ruleTerm).getArguments();
				Term ruleConcTerm = ruleTermArgs.get(ruleTermArgs.size()-1);
				Set<FreeVar> ruleConcVarSet = ruleConcTerm.getFreeVariables();//ruleConcTerm.substitute(sub).getFreeVariables();
				for (int i=0; i<ruleTermArgs.size()-1; ++i) {
					ruleConcVarSet.removeAll(ruleTermArgs.get(i).getFreeVariables());//ruleTermArgs.get(i).substitute(sub).getFreeVariables());
				}
				
				mustAvoid.addAll(ruleConcVarSet);
				
				// compute new input vars - they're what ruleConcVarSet substitutes to
				sub.selectUnavoidable(derivTerm.getFreeVariables()); // avoid conclusion free vars where possible
				Set<FreeVar> freshVarSet = new HashSet<FreeVar>();
				for(FreeVar v : ruleConcVarSet) {
					FreeVar t = v.substitute(sub).getEtaEquivFreeVar();
					if (t == null  /*!(t instanceof FreeVar) -- without calling getEta... above*/) {
						debug("conclusion " + ruleConcTerm + " ruleConcVarSet was " + ruleConcVarSet + " problem with " + v);
						ErrorHandler.report(Errors.BAD_RULE_APPLICATION, "The claimed fact is not justified by applying theorem " + getRuleName()
								+ " to the argument", this, "incorrectly instantiated a free variable with a term " + t + "\n\twhile unifying " + appliedTerm + " with rule term " + ruleTerm);
					}
					freshVarSet.add(t);
				}
				
				// As far as I can tell the code below is an unnecessary restriction
				//Set<FreeVar> intersectSet = new HashSet<FreeVar>(freshVarSet);
				//intersectSet.retainAll(ctx.outputVars);
				//if (!intersectSet.isEmpty())
				//	ErrorHandler.report("cannot use variable name(s) " + intersectSet + " that is an output of the theorem", this);
				debug("line " + this.getLocation() + " adds vars " + freshVarSet);
				ctx.inputVars.addAll(freshVarSet);
			}
			
			if (!sub.avoid(mustAvoid)) {
				debug("while unifying " + appliedTerm + " with " + ruleTerm + " and sub " + sub + "\n\ttrying to avoid " + mustAvoid);
				debug("\tctx.inputVars = " + ctx.inputVars);
				debug("\tctx.currentSub = " + ctx.currentSub);
				ErrorHandler.report(Errors.BAD_RULE_APPLICATION, "The claimed fact is not justified by applying rule " + getRuleName() + " to the argument", this, "\t(could not remove variables "+ctx.inputVars+ " from sub " + sub + ")");
			}			

		} catch (UnificationFailed e) {
			Term explanationTerm = null;
			try {
				//System.out.println("trying to explain with " + appliedTermWithVar + " and " + ruleTerm);
				Substitution learnAboutErrors = appliedTermWithVar.unify(ruleTerm);
				explanationTerm = learnAboutErrors.getSubstituted(concTermVar);
			} catch (UnificationFailed e2) {
				// do nothing; can't give good error message
			}
			debug("\tctx.currentSub = " + ctx.currentSub);
			debug("\tctx.adaptationSub = " + ctx.adaptationSub);
			if (explanationTerm == null)
				ErrorHandler.report(Errors.BAD_RULE_APPLICATION, "The rule cannot legally be applied to the arguments", this,
						"(was checking " + appliedTerm + " instance of " + ruleTerm + ",\n got exception " + e);
			else {
				debug("(was checking " + appliedTerm + " instance of " + ruleTerm + ",\n got exception " + e);
				ErrorHandler.report(Errors.BAD_RULE_APPLICATION, "Claimed fact " + getElement() + "\n\tis not a consequence of applying rule " + getRuleName() + " to the arguments", this,
						"SASyLF computed that result LF term should be " + explanationTerm);
			}
		}
	}
}
