package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.term.Facade.App;
import static edu.cmu.cs.sasylf.util.Util.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.util.ErrorHandler;

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
		
		boolean contextCheckNeeded = false;
		
		if (ctx.innermostGamma != null && !ctx.innermostGamma.equals(getRule(ctx).getAssumes())) {
		  NonTerminal nt = getRule(ctx).getAssumes();
		  if (nt == null) {
		    contextCheckNeeded = true;
		  } else if (ctx.innermostGamma.getType() != nt.getType()) {
		    contextCheckNeeded = true;
		  }
		  // JTB: There are two possible problems:
		  // 1. An explicit syntactic parameter to a lemma when that param could depend on our Gamma
		  // 2. An implicit syntactic parameter to a rule conclusion where again the param could depend on Gamma
		  // We don't need to worry about implicit syntactic parameters to a lemma -- 
		  // these will only be used in rule parameters that either have context, or not.
		  // If they have context, this situation never shows up.
		  // If they don't have context then their creation would detect the problem.
		  // We also don't need to worry about implicit existential syntactic parameters
		  // of a lemma because these are in the opposite direction.
		  // With regard to rule, we don't need to worry about implicit syntactic parameters
		  // used in the premises for the same reason as with rules.  But we need to
		  // worry about implicit syntactic parameters in the conclusion, because these are
		  // used to BUILD relations that might be used elsewhere (as in bad5.slf).
		  //
		  // Detecting 1 is (fairly) easy and can be done here.  Detecting 2 is harder and is done later
		  if (contextCheckNeeded) {
	      for (Element e : getRule(ctx).getPremises()) {
	        if (e instanceof NonTerminal) {
	          if (!ctx.innermostGamma.getType().canAppearIn(((NonTerminal)e).getType().typeTerm())) continue;
	          if (ctx.varfreeNTs.contains(e)) continue;
	          // Detect bad4.slf
	          ErrorHandler.recoverableError("Passing " + e + " to " + getRule(ctx).getName() + " could conceal context", this);
	        }
	      }		    
		  }
		}
		
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
			
      // compute the set of variables that appear in the rule's conclusion but not its arguments
      List<? extends Term> ruleTermArgs = ((Application)ruleTerm).getArguments();
      Term ruleConcTerm = ruleTermArgs.get(ruleTermArgs.size()-1);
      Set<FreeVar> ruleConcVarSet = ruleConcTerm.getFreeVariables();//ruleConcTerm.substitute(sub).getFreeVariables();
      for (int i=0; i<ruleTermArgs.size()-1; ++i) {
        ruleConcVarSet.removeAll(ruleTermArgs.get(i).getFreeVariables());//ruleTermArgs.get(i).substitute(sub).getFreeVariables());
      }
      
			if (getRule(ctx) instanceof Theorem) {				
				mustAvoid.addAll(ruleConcVarSet);
				
				// compute new input vars - they're what ruleConcVarSet substitutes to
				sub.selectUnavoidable(derivTerm.getFreeVariables()); // avoid conclusion free vars where possible
				Set<FreeVar> freshVarSet = new HashSet<FreeVar>();
				for(FreeVar v : ruleConcVarSet) {
					FreeVar t = v.substitute(sub).getEtaEquivFreeVar();
					// TODO: JTB: I think this error is redundant; it will be detected below if not here
					if (t == null  /*!(t instanceof FreeVar) -- without calling getEta... above*/) {
						debug("conclusion " + ruleConcTerm + " ruleConcVarSet was " + ruleConcVarSet + " problem with " + v);
						ErrorHandler.report(Errors.BAD_RULE_APPLICATION, "The claimed fact is not justified by applying theorem " + getRuleName()
								+ " to the argument", this, "incorrectly instantiated an output variable " + v + " with a term " + v.substitute(sub) + "\n\twhile unifying " + appliedTerm + " with rule term " + ruleTerm);
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
			} else if (contextCheckNeeded) {
			  for (FreeVar v : ruleConcVarSet) {
			    if (v.getType() instanceof Constant) {
            if (!ctx.innermostGamma.getType().canAppearIn(v.getType())) continue;
            NonTerminal nt = new NonTerminal(v.substitute(sub).toString(), this.getLocation());
            if (ctx.varfreeNTs.contains(nt)) continue;
			      // Detect bad5.slf
			      ErrorHandler.recoverableError("passing " + v.getName() + " implicitly to " + getRule(ctx).getName() +
			          " may conceal its context.", this, "\t(variable bound to " + nt + ")");
			    }
			  }
			}
			
			if (!sub.avoid(mustAvoid)) {
				debug("while unifying " + appliedTerm + " with " + ruleTerm + " and sub " + sub + "\n\ttrying to avoid " + mustAvoid);
				debug("\tctx.inputVars = " + ctx.inputVars);
				debug("\tctx.currentSub = " + ctx.currentSub);
				Set<FreeVar> unavoided = sub.selectUnavoidable(mustAvoid);
        ErrorHandler.report(Errors.BAD_RULE_APPLICATION, "The claimed fact is not justified by applying rule " + getRuleName() + " to the argument (the rule restricts " + unavoided.iterator().next() + ")", this, "\t(could not remove variables "+unavoided+ " from sub " + sub + ")");
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
				ErrorHandler.report(Errors.BAD_RULE_APPLICATION, "Claimed fact " + getElement() + " is not a consequence of applying rule " + getRuleName() + " to the arguments", this,
						"SASyLF computed that result LF term should be " + explanationTerm);
			}
		}
	}
}
