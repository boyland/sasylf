package edu.cmu.cs.sasylf.ast;

import java.util.ArrayList;
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
	protected boolean acceptPlaceholder() {
		return true;
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
			TermPrinter tp = new TermPrinter(ctx,getElement().getRoot(),this.getLocation(),false);
			String extraInfo = tp.toString(e.term1,false) + " =?= " + tp.toString(e.term2,false);
			ErrorHandler.error(Errors.RULE_APP_UNIFICATION_INCOMPLETE, extraInfo, this,
					"(was checking " + subject + " instance of " + pattern + ",\n got exception " + e);      
			return; // tell Java we're gone.
		} catch (UnificationFailed e1) {
			TermPrinter tp = new TermPrinter(ctx,getElement().getRoot(),this.getLocation(),false);
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
			Errors errorType = Errors.RULE_APP_UNIFICATION_FAILED;
			String explanationString = null;
			String infoString = "SASyLF was unifying " + subject + " and " + pattern;
			try {
				Util.debug(newSubject,".unify(",pattern,")");
				Substitution learnAboutErrors = newSubject.unify(pattern);
				learnAboutErrors.avoid(ctx.inputVars);
				Term explanationTerm = learnAboutErrors.getSubstituted(concVar);
				explanationString = tp.toString(tp.asClause(explanationTerm));
				errorType = Errors.RULE_APP_CONCLUSION_OTHER;
			} catch (UnificationFailed e2) {
				if (e2.term1 != null && e2.term2 != null) {
					infoString += ", but failed because " + tp.toString(e2.term1,false) + " =?= " + tp.toString(e2.term2,false); 
				}
			}
			ErrorHandler.error(errorType, explanationString, this, infoString);
			return; // for Java
		}
		// System.out.println("subject = " + subject + ", pattern = " + pattern + ", callSub = " + callSub + ", concFreeVars = " + conclusionFreeVars);

		// We have taken care of most context discarding issues, but
		// we still need to worry about an implicit syntactic parameter to a rule conclusion
		boolean contextCheckNeeded = 
				ruleLike instanceof Rule &&   // only rules have implicit parameters only in conclusion
				ctx.assumedContext != null && // only if we have a context currently, and
				(ruleLike.getAssumes() == null ||       // either the rule has no context, or
				ctx.assumedContext.getType() != 
				ruleLike.getAssumes().getType() ||      // or the type differs
				getClause().getRoot() == null); // or the result has an empty context

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
			Set<FreeVar> unavoidedOutput = new HashSet<>(unavoided);
			unavoidedOutput.retainAll(ctx.outputVars);
			final String extraInfo = "\t(could not remove variables "+unavoided+ " from sub " + callSub + ")";
			if (!unavoidedOutput.isEmpty()) {
				FreeVar out = unavoidedOutput.iterator().next();
				ErrorHandler.error(Errors.RULE_APP_PREMATURE_OUTPUT, " " + out, this, extraInfo);
			}
			ErrorHandler.error(Errors.RULE_APP_RESTRICT, unavoided.toString(), this, extraInfo);
		}  

		if (contextCheckNeeded) {
			for (FreeVar v : conclusionFreeVars) {
				if (!ctx.assumedContext.getType().canAppearIn(v.getType())) continue;
				Term actual = v.substitute(callSub);
				if (ctx.isVarFree(actual)) continue;
				// System.out.println(actual + "(was " + v + ") is not free: " + ctx.varFreeNTmap.keySet());
				ErrorHandler.recoverableError(Errors.CONTEXT_DISCARDED_APPL, v.getName() + " assumes " + ctx.assumedContext, this, "\t(variable bound to " + actual + ")");
			}
		}
		
		// See good37.slf
		Set<FreeVar> poorVars = callSub.selectUnavoidable(subject.getFreeVariables());
		poorVars.removeAll(ctx.outputVars); // output variables are often not-free
		if (!poorVars.isEmpty()) {
			for (Fact f : this.getArgs()) {
				if (f instanceof DerivationPlaceholder) {
					DerivationPlaceholder ph = (DerivationPlaceholder)f;
					if (poorVars.remove(ph.getTerm())) {
						ph.setPlaceholder(ctx, callSub, this);
					}
				}
			}
			if (!poorVars.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				for (FreeVar v : poorVars) {
					sb.append(v + "->" + callSub.getMap().get(v));
				}
				ErrorHandler.warning(Errors.WHERE_MISSING_EXT, "" + poorVars, this, sb.toString());
			}
		}
		
		ctx.composeSub(callSub);
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
			if (this instanceof DerivationByInductionHypothesis || self.getForalls().isEmpty()) 
				ErrorHandler.error(Errors.INDUCTION_MISSING, this);
			ErrorHandler.warning(Errors.INDUCTION_IMPLICIT, this);
			mySchema = yourSchema = new StructuralInduction(0);
		}
		// we need to check matching, even though this is already done for
		// the mutual inductive theorems when they were declared,
		// because otherwise "reduces" can crash
		if (mySchema.matches(yourSchema, this, false)) { // "false" means don't print error
			Reduction r = mySchema.reduces(ctx, yourSchema, getArgs(), this);
			switch (r) {
			case NONE: break; // error already printed
			case LESS: break; // no problem
			case EQUAL: // maybe problem
				if (self.getGroupIndex() <= other.getGroupIndex()) {
					if (self != other) {
						ErrorHandler.error(Errors.INDUCTION_NOT_EARLIER, this);
					} else {
						ErrorHandler.error(Errors.NOT_SUBDERIVATION_EQ, this);
					}
				}        
			}
		}
	}
}
