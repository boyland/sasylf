package edu.cmu.cs.sasylf.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.reduction.InductionSchema;
import edu.cmu.cs.sasylf.reduction.Reduction;
import edu.cmu.cs.sasylf.reduction.StructuralInduction;
import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.BoundVar;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.term.UnificationIncomplete;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Util;

public abstract class DerivationByIHRule extends DerivationWithArgs {
	public DerivationByIHRule(String n, Location l, Clause c) {
		super(n,l,c);
	}

	abstract public RuleLike getRule(Context ctx);
	abstract public String getRuleName();


	@Override
	protected ClauseType getClauseTypeExpected(Context ctx, int i) {
		RuleLike rule = getRule(ctx);
		if (rule != null && rule.getPremises().size() > i) {
			final ElementType type = rule.getPremises().get(i).getType();
			if (type instanceof ClauseType) return (ClauseType)type;
		}
		return super.getClauseTypeExpected(ctx,i);
	}

	@Override
	public void typecheck(Context ctx) {
		super.typecheck(ctx);
		Util.debug("line: ", this.getLocation().getLine());

		RuleLike ruleLike = getRule(ctx);
		int n = getArgs().size();

		List<Abstraction> addedContext = new ArrayList<Abstraction>();
		Term subject = ruleLike.checkApplication(ctx, getArgs(), this, addedContext, this, false);
		// form the rule term
		Set<FreeVar> conclusionFreeVars = new HashSet<FreeVar>();
		Term pattern = ruleLike.getFreshAdaptedRuleTerm(addedContext, conclusionFreeVars); 
		// Term newSubject = Facade.App(ruleLike.getRuleAppConstant(), allArgs);

		Substitution callSub;    
		// Now we get the substitution.
		// Then there are complex catch clauses for failed unification.
		// The complication only concerns what error message to generate.
		try {
			callSub = subject.unify(pattern);
		} catch (UnificationIncomplete e) {
			String extraInfo = "\n\tcouldn't unify " + e.term1 + " and " + e.term2;
			ErrorHandler.report(Errors.BAD_RULE_APPLICATION, "SASyLF ran into incompleteness of unification while checking this rule" + extraInfo, this,
					"(was checking " + subject + " instance of " + pattern + ",\n got exception " + e);      
			return; // tell Java we're gone.
		} catch (UnificationFailed e1) {
			Util.debug("failure checking ",subject," instanceof ",pattern,": ",e1);
			// try to be more helpful
			FreeVar concVar = FreeVar.fresh("conclusion", Term.wrapWithLambdas(addedContext,Constant.UNKNOWN_TYPE));
			Term applied = concVar;
			if (!addedContext.isEmpty()) {
				List<Term> args = new ArrayList<Term>();
				int m = addedContext.size();
				for (int j=0; j < m; ++j) {
					args.add(new BoundVar(m-j));
				}
				Util.debug("Facade.App(",concVar,",",args,")");
				applied = Facade.App(concVar,args);
			}
			List<Term> newArgs = new ArrayList<Term>(((Application)subject).getArguments());
			newArgs.set(n, applied);
			Term newSubject = Facade.App(((Application)subject).getFunction(),newArgs);
			Term explanationTerm = null;
			String explanationString = null;
			try {
				Util.debug(newSubject,".unify(",pattern,")");
				Substitution learnAboutErrors = newSubject.unify(pattern);
				learnAboutErrors.avoid(ctx.inputVars);
				explanationTerm = learnAboutErrors.getSubstituted(concVar);
				TermPrinter tp = new TermPrinter(ctx,getElement().getRoot(),this.getLocation());
				explanationString = tp.toString(tp.asClause(explanationTerm));
			} catch (UnificationFailed e2) {
				// do nothing; can't give good error message
			} catch (RuntimeException ex) {
				System.err.println("Couldn't print term: " + explanationTerm);
				ex.printStackTrace();
			}
			if (explanationTerm == null)
				ErrorHandler.report(Errors.BAD_RULE_APPLICATION, getRuleName() + " cannot legally be applied to the arguments", this,
						"(was checking " + subject + " instance of " + pattern + ",\n got exception " + e1);
			else if (explanationString == null) 
				ErrorHandler.report(Errors.BAD_RULE_APPLICATION, "Claimed fact " + getElement() + " is not a consequence of applying " + getRuleName() + " to the arguments", this,
						"SASyLF computed that the result LF term should be " + explanationTerm);
			else if (suspectOutputVarError == null)
				ErrorHandler.report(Errors.BAD_RULE_APPLICATION, "Claimed fact " + getElement() + " is not a consequence of applying " + getRuleName() + " to the arguments" +
						"\nSASyLF computed that the result should be " + explanationString, this);
			else 
				ErrorHandler.report(Errors.BAD_RULE_APPLICATION, "Claimed fact " + getElement() + " is not a consequence of applying " + getRuleName() + " to the arguments" +
						"\nSASyLF computed that the result should be " + explanationString + 
						"\nPerhaps these output variables were set prematurely: " + suspectOutputVarError, this);
			return;
		}
		// System.out.println("subject = " + subject + ", pattern = " + pattern + ", callSub = " + callSub + ", concFreeVars = " + conclusionFreeVars);

		// We have taken care of most context discarding issues, but
		// we still need to worry about an implicit syntactic parameter to a rule conclusion
		boolean contextCheckNeeded = 
				ruleLike instanceof Rule &&   // only rules have implicit parameters only in conclusion
				ctx.assumedContext != null && // only if we have a context currently, and
				(ruleLike.getAssumes() == null ||       // either the rule has no context, or
				ctx.assumedContext.getType() != 
				ruleLike.getAssumes().getType());      // or the type differs

		Set<FreeVar> mustAvoid = new HashSet<FreeVar>(ctx.inputVars);
		if (ruleLike instanceof Theorem) {        
			mustAvoid.addAll(conclusionFreeVars);
		}

		if (!callSub.avoid(mustAvoid)) {
			subject.unify(pattern);
			Util.debug("while unifying ", subject, " with ", pattern, " and sub ", callSub, "\n\ttrying to avoid ", mustAvoid);
			Util.debug("\tctx.inputVars = ", ctx.inputVars);
			Util.debug("\tctx.currentSub = ", ctx.currentSub);
			Set<FreeVar> unavoided = callSub.selectUnavoidable(mustAvoid);
			ErrorHandler.report(Errors.BAD_RULE_APPLICATION, "The claimed fact is not justified by applying rule " + getRuleName() + " to the argument (the rule restricts " + unavoided + ")", 
					this, "\t(could not remove variables "+unavoided+ " from sub " + callSub + ")");
		}  

		if (contextCheckNeeded) {
			for (FreeVar v : conclusionFreeVars) {
				if (!ctx.assumedContext.getType().canAppearIn(v.getType())) continue;
				Term actual = v.substitute(callSub);
				if (ctx.isVarFree(actual)) continue;
				// System.out.println(actual + "(was " + v + ") is not free: " + ctx.varFreeNTmap.keySet());
				ErrorHandler.recoverableError("passing " + v.getName() + " implicitly to " + ruleLike.getName() +
						" discards its context " + ctx.assumedContext, this, "\t(variable bound to " + actual + ")");
			}
		}
		
		// See good37.slf
		Set<FreeVar> poorVars = callSub.selectUnavoidable(subject.getFreeVariables());
		poorVars.removeAll(ctx.outputVars); // output variables are often not-free
		if (!poorVars.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (FreeVar v : poorVars) {
				sb.append(v + "->" + callSub.getMap().get(v));
			}
			ErrorHandler.warning("The result used variables that are not free: " + poorVars, this, sb.toString());
		}
		
		ctx.composeSub(callSub);

		/*
		// build ruleTerm: the rule claimed by the user
		// and adapted to the variables in scope at this point (i.e. Gamma unrolled as necessary)
		Element elem = getElement();
		Term derivTerm = DerivationByAnalysis.adapt(elem.asTerm(), elem, ctx, false);

		// build appliedTerm: the element claimed by the user for the conclusion and the
		// premises claimed by the user as the arguments
		List<Term> termArgs = new ArrayList<Term>();
		List<Term> termArgsWithVar = new ArrayList<Term>();
		Set<FreeVar> argFreeVars = new HashSet<FreeVar>();

		 *
		 * We just fixed a minor problem that masked a major problem.
		 * Minor problem:
		 * We didn't check about losing context for an explicit syntax parameter
		 * and we didn't correctly avoid complaining about losing context for an implicit
		 * parameter of the conclusion.
		 * 
		 * Major problem:
		 * if the actual parameter is syntax that could depend on a variable
		 * in the target context, but is NOT in the target context, then 
		 * getFreshRuleAppTerm will (correctly) generate a function for it,
		 * but the callee doesn't make a function unless there is an assumes
		 * in the actual parameter. We need to regularize what is happening.
		 * 
		 * Maybe we will need to fix the way rule applications are generated to
		 * out the context on the outside, in one place, where it belongs.
		 * 
		 * And all this is so that adaptation can be understood/cleaned up so
		 * we can start the new adaptation technique that goes the other way,
		 * and adapts Gamma' to Gamma and never changes innermostGamma.
		 * I put in some print statements to print out what this would be and
		 * found some surprising adaptations: 
		 * If Gamma' is found when the subject context is Gamma, x:T
		 * then Gamma', x':T, x:T is the pattern context (so far so good)
		 * but the adaptation from Gamma', x':T to Gamma is [t[x],_]
		 * where "x" is out of scope.  Hmm.  Better would be to assign t[x] to a new
		 * free variable t_24, and then bind x to t_24.  This means that RuleCase
		 * will need to figure out carefully how to adapt the subject to get
		 * a function with the same number of lambdas.
		 *



		for (int i = 0; i < getArgs().size(); ++i) {
			Term argTerm = getAdaptedArg(ctx,i);
			termArgs.add(argTerm);
			termArgsWithVar.add(argTerm);
			argFreeVars.addAll(argTerm.getFreeVariables());
		}
    termArgs.add(derivTerm);
    Term appliedTerm = App(ruleLike.getRuleAppConstant(), termArgs);

    // add a FreeVar for the conclusion; we are trying to discover what this should be
		FreeVar concTermVar = FreeVar.fresh("conclusion", Constant.UNKNOWN_TYPE); 
		termArgsWithVar.add(concTermVar);
		Term appliedTermWithVar = App(ruleLike.getRuleAppConstant(), termArgsWithVar);

    Substitution wrappingSub = new Substitution();	
    Util.debug("termArgs = ",termArgs);
		Term ruleTerm = ruleLike.getFreshRuleAppTerm(derivTerm, wrappingSub, termArgs);
		Util.debug("wrappingSub for rule application is ", wrappingSub);

		// unify the two terms, and check that the appropriate instance relationships hold
		Substitution sub;
		try {
		  Util.debug("appliedTerm = "+ appliedTerm, ", ruleTerm = ", ruleTerm);
			sub = appliedTerm.unify(ruleTerm);
			Util.debug("  gets sub = ", sub);

			if (foundError) {
			  System.out.println("SASyLF was able to unify " + appliedTerm + " with " + ruleTerm);
			}
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

			if (ruleLike instanceof Theorem) {				
				mustAvoid.addAll(ruleConcVarSet);
			} else if (contextCheckNeeded) {
			  for (FreeVar v : ruleConcVarSet) {
			    if (!ctx.innermostGamma.getType().canAppearIn(v.getType())) continue;
			    Term actual = v.substitute(sub);
			    if (ctx.isVarFree(actual)) continue;
			    ErrorHandler.recoverableError("passing " + v.getName() + " implicitly to " + ruleLike.getName() +
			        " discards its context " + ctx.innermostGamma, this, "\t(variable bound to " + actual + ")");
			  }
			}

			if (!sub.avoid(mustAvoid)) {
				debug("while unifying ", appliedTerm, " with ", ruleTerm, " and sub ", sub, "\n\ttrying to avoid ", mustAvoid);
				debug("\tctx.inputVars = ", ctx.inputVars);
				debug("\tctx.currentSub = ", ctx.currentSub);
				Set<FreeVar> unavoided = sub.selectUnavoidable(mustAvoid);
        ErrorHandler.report(Errors.BAD_RULE_APPLICATION, "The claimed fact is not justified by applying rule " + getRuleName() + " to the argument (the rule restricts " + unavoided + ")", this, "\t(could not remove variables "+unavoided+ " from sub " + sub + ")");
			}			

			ctx.composeSub(sub);
		} catch (UnificationIncomplete e) {
      ErrorHandler.report(Errors.BAD_RULE_APPLICATION, "SASyLF ran into incompleteness of unification while checking this rule", this,
          "(was checking " + appliedTerm + " instance of " + ruleTerm + ",\n got exception " + e);		  
		} catch (UnificationFailed e) {
			Term explanationTerm = null;
			try {
				//System.out.println("trying to explain with " + appliedTermWithVar + " and " + ruleTerm);
				Substitution learnAboutErrors = appliedTermWithVar.unify(ruleTerm);
				explanationTerm = learnAboutErrors.getSubstituted(concTermVar);
			} catch (UnificationFailed e2) {
				// do nothing; can't give good error message
			}
			debug("\tctx.currentSub = ", ctx.currentSub);
			if (explanationTerm == null)
				ErrorHandler.report(Errors.BAD_RULE_APPLICATION, "The rule cannot legally be applied to the arguments", this,
						"(was checking " + appliedTerm + " instance of " + ruleTerm + ",\n got exception " + e);
			else {
				debug("(was checking ", appliedTerm, " instance of ", ruleTerm, ",\n got exception ", e);
				ErrorHandler.report(Errors.BAD_RULE_APPLICATION, "Claimed fact " + getElement() + " is not a consequence of applying rule " + getRuleName() + " to the arguments", this,
						"SASyLF computed that result LF term should be " + explanationTerm);
			}
		}

		// Now we must check that all relations that assume a context don't lose that context
		boolean anyContextsIn = false;
    for (int i=0; i < n; ++i) {
		  Element formal = ruleLike.getPremises().get(i);
		  Element actual = getArgs().get(i).getElement();
		  if (actual instanceof ClauseUse && ((ClauseUse)actual).isRootedInVar()) {
		    anyContextsIn = true;
		    checkRootMatch(ctx,actual,formal,this);
		  }
		}
    // if we never passed any contexts into the rule/theorem, we expect none out,
    // and so can avoid this check.
    if (anyContextsIn) {
      checkRootMatch(ctx,ruleLike.getConclusion(),this.getElement(),this);
    }*/
	}


	/**
	 * @param name
	 * @param formal
	 * @param actual
	 * @param allContexts
	 * @param allArgs
	 */
	protected void getArgContextAndTerm(Context ctx, String name, Element formal,
			Element actual, List<List<Abstraction>> allContexts, List<Term> allArgs) {
		Term f = formal.asTerm();
		Term a = actual.asTerm().substitute(ctx.currentSub);
		int diff = a.countLambdas() - f.countLambdas();
		if (diff < 0) {
			ErrorHandler.report(name + " to " + getRuleName() + " expects more in context than given",this,
					"SASyLF expected the " + name + " to be " +f+ " but was given " + a + " from " + actual);
		} else if (diff == 0) {
			allContexts.add(Collections.<Abstraction>emptyList());
			allArgs.add(a);
		} else {
			if (formal.getRoot() == null) {
				ErrorHandler.report(name + " to " + getRuleName() + " doesn't expect/permit extra bindings",   this,
						"SASyLF computed the " + name + " supplied as " + a); 
			}
			List<Abstraction> context = new ArrayList<Abstraction>();
			allContexts.add(context);
			allArgs.add(Term.getWrappingAbstractions(a, context, diff));
		}
	}

	protected List<Abstraction> unionContexts(List<List<Abstraction>> contexts) {
		List<Abstraction> result = Collections.<Abstraction>emptyList();
		Set<String> argNames = new HashSet<String>();
		boolean copied = false;
		for (List<Abstraction> con : contexts) {
			if (con.size() > 0) {
				if (result.size() == 0) {
					result = con;
					for (Abstraction a : result) {
						argNames.add(a.varName);
					}
				} else {
					// merge any new things from con into result
					int i=0,j=0;
					Set<String> seen = new HashSet<String>();
					while (j < con.size()) {
						Abstraction b = con.get(j);
						if (seen.contains(b.varName)) {
							ErrorHandler.report("Computed context for " + getRuleName() + " has inconsistent placement of " + b.varName, this);
						}
						if (!argNames.add(b.varName)) {
							while (!result.get(i).varName.equals(b.varName)) {
								++i;
							}
						} else {
							if (!copied) {
								result = new ArrayList<Abstraction>(result);
								copied = true;
							}
							result.add(i, b);
						}
						// invariant i and j both point ton abstraction with the name b.varName
						++i; ++j;
						seen.add(b.varName);
					}
				}
			}
		}
		if (copied) {
			System.out.println("On line " + getLocation().getLine() + " Merging ");
			for (List<Abstraction> abs : contexts) {
				System.out.println("  " + Term.wrappingAbstractionsToString(abs));
			}
			System.out.println("= " + Term.wrappingAbstractionsToString(result));
		}
		return result;
	}

	protected Term weakenArg(List<Abstraction> current, List<Abstraction> desired, Term t) {
		int i=current.size()-1, j=desired.size()-1;
		while (j >= 0) {
			Abstraction b = desired.get(j);
			if (i >= 0 && current.get(i).varName.equals(b.varName)) {
				--i; --j;
			} else {
				t = t.incrFreeDeBruijn(1);
				--j;
			}
		}
		return t;
	}

	/**
	 * Check inductive calls.
	 * @param ctx global context
	 * @param self caller theorem
	 * @param other callee theorem
	 */
	protected void checkInduction(Context ctx, Theorem self, Theorem other) {
		InductionSchema mySchema = self.getInductionSchema();
		InductionSchema yourSchema = other.getInductionSchema();
		if (self == other && mySchema == InductionSchema.nullInduction) {
			ErrorHandler.warning("Implicit induction deprecated.  Please use explicit induction", this);
			mySchema = yourSchema = StructuralInduction.create(self, self.getForalls().get(0).getName(), this);
		}
		if (mySchema.matches(yourSchema, this, false)) {
			Reduction r = mySchema.reduces(ctx, yourSchema, getArgs(), this);
			switch (r) {
			case NONE: break; // error already printed
			case LESS: break; // no problem
			case EQUAL: // maybe problem
				if (self.getGroupIndex() <= other.getGroupIndex()) {
					if (self != other) {
						ErrorHandler.report(Errors.MUTUAL_NOT_EARLIER, this);
					} else {
						ErrorHandler.report(Errors.NOT_SUBDERIVATION, " " + mySchema + " is unchanged", this);
					}
				}        
			}
		}
	}
}
