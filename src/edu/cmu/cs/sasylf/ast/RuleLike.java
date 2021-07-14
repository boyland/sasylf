package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.term.Facade.App;
import static edu.cmu.cs.sasylf.term.Facade.Const;
import static edu.cmu.cs.sasylf.util.Util.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Util;


/** Common interface for Rules and Theorems */
public abstract class RuleLike extends Node implements Named {
	private String name;

	public RuleLike(String n, Location l) { super(l); name = n; }
	public RuleLike(String n) { name = n; }

	public abstract List<? extends Element> getPremises();
	public abstract Set<FreeVar> getExistentialVars();
	public abstract Clause getConclusion();

	@Override
	public String getName() { return name; }
	public abstract String getKind();

	public Constant getRuleAppConstant() {
		if (ruleAppConstant == null) {
			createRuleAppConstant();
		}
		return ruleAppConstant;
	}

	/**
	 * Return the type that this rule-like is creating.
	 * (This method should only be called once).
	 * @return the constant type that this rule-like creates terms for.
	 */
	protected Constant getRuleAppBase() {
		return Const(getName() + "BASE", Constant.TYPE);
	}
	
	/**
	 * Create the typed constant for this rule.
	 */
	protected void createRuleAppConstant() {
		Term typeTerm = getRuleAppBase();

		if (isInterfaceOK()) {
			List<Term> argTypes = new ArrayList<Term>();
			
			for (int i = 0; i < getPremises().size(); ++i) {
				argTypes.add(getPremises().get(i).getTypeTerm());
			}
			argTypes.add(((ClauseUse)getConclusion()).getConstructor().asTerm());

			typeTerm = Term.wrapWithLambdas(typeTerm, argTypes);
		} else {
			typeTerm = Constant.UNKNOWN_TYPE;
		}
		
		ruleAppConstant = Const(name + "TERM", typeTerm);
		Util.debug(ruleAppConstant,": ",typeTerm);
	}

	/** Computes a mutable list of free variables that are suitable as premises for this rule 
	 * @deprecated
	 */
	@Deprecated
	public List<Term> getFreeVarArgs(Term instanceTerm) {
		// compute adaptation amount
		//Term concTerm = getConclusion().asTerm();
		// TODO: not sure if I need adaptation here, disabling it until I'm sure I do need it
		int adaptation = 0;//((ClauseUse)getConclusion()).getAdaptationNumber(concTerm, instanceTerm);
		debug("adaptation: ", adaptation);

		List<Term> termArgs = new ArrayList<Term>();
		for (int i = 0; i < this.getPremises().size(); ++i) {
			ClauseUse clauseUse = (ClauseUse) this.getPremises().get(i);
			ClauseDef clauseDef = clauseUse.getConstructor(); 
			Term type = Constant.UNKNOWN_TYPE; //clauseDef.getTypeTerm();
			String name = clauseDef.getConstructorName();
			Term argTerm = Facade.FreshVar(name, type);
			debug("before: ", argTerm);
			argTerm = clauseUse.wrapWithOuterLambdas(argTerm, instanceTerm, adaptation);
			debug("after: ", argTerm);
			termArgs.add(argTerm);
		}
		return termArgs;
		/*termArgs.add(concTerm);
		Term appliedTerm = App(this.getRuleAppConstant(), termArgs);
		return appliedTerm;*/
	}

	/** Computes a term for this rule, adapting it to the variables in scope in instanceTerm (which should be related to the conclusion).
	 * Also freshens the variables in this term.
	 * @param wrappingSub <i>output</i>
	 * @deprecated call getFreshAdaptedRule instead
	 */
	@Deprecated
	public Term getFreshRuleAppTerm(Term instanceTerm, Substitution wrappingSub, List<Term> termArgsIfKnown /* may be null */) {
		debug("getting conclusion term for rule ", getName());
		Term concTerm = getConclusion().asTerm();
		Substitution ruleSub = new Substitution();			// substitutes fresh vars in rule
		ruleSub = concTerm.freshSubstitution(ruleSub);
		concTerm = concTerm.substitute(ruleSub);
		int adaptation = ((ClauseUse)getConclusion()).getAdaptationNumber(concTerm, instanceTerm, false);

		// TODO: major hack here.  Must rationalize gamma checking.
		List<Term> args = new ArrayList<Term>();
		for (int i = 0; i < getPremises().size(); ++i) {
			Element elem = getPremises().get(i);
			Util.debug("arg = ", elem);
			Term argTerm = elem.asTerm();
			ruleSub = argTerm.freshSubstitution(ruleSub);
			argTerm = argTerm.substitute(ruleSub);
			if (elem instanceof ClauseUse) {
				ClauseUse clause = (ClauseUse) elem;
				// only adapt if elem has a Gamma variable at its root
				debug("\tgenerated argterm before adaptation: ", argTerm);
				if (clause.isRootedInVar()) {
					int localAdaptation = adaptation;
					Term localInstanceTerm = instanceTerm;
					if (termArgsIfKnown != null && !((ClauseUse)getConclusion()).isRootedInVar()) {
						localInstanceTerm = termArgsIfKnown.get(i);
						localAdaptation = clause.getAdaptationNumber(argTerm, localInstanceTerm, false);
					}
					debug("adaptation of ", argTerm, " to ", localInstanceTerm, " is ", localAdaptation);
					argTerm = clause.wrapWithOuterLambdas(argTerm, localInstanceTerm, localAdaptation, wrappingSub, false);
					// System.out.println("    wrapping sub = " + wrappingSub);
					debug("\tresult is ", argTerm);
				}
			} else if (elem instanceof AssumptionElement) {
				// TODO: merge with previous branch to avoid duplicate code
				//Clause clause = ((AssumptionElement)elem).getAssumes();
				debug("\tgenerated argterm before adaptation: ", argTerm);
				{
					int localAdaptation = adaptation;
					Term localInstanceTerm = instanceTerm;
					if (termArgsIfKnown != null && !((ClauseUse)getConclusion()).isRootedInVar()) {
						localInstanceTerm = termArgsIfKnown.get(i);
						localAdaptation = localInstanceTerm.countLambdas() - argTerm.countLambdas();
						Util.debug("  localAdaptation = ", localAdaptation);
					}
					Util.debug("adaptation of ", argTerm, " to ", localInstanceTerm, " is ", localAdaptation);
					argTerm = ClauseUse.wrapWithOuterLambdas(argTerm, localInstanceTerm, localAdaptation, wrappingSub);
					// System.out.println("    wrapping sub = " + wrappingSub);
					Util.debug("\tresult argTerm is ", argTerm);
				}
			}
			args.add(argTerm);
		}
		Util.debug("generated args = ",args);
		Util.debug("\tgenerated concterm before adaptation: ", concTerm);
		Util.debug("adaptation of ", concTerm, " to ", instanceTerm, " is ", adaptation);
		Util.debug("\twrappingSub = ", wrappingSub);
		concTerm = ((ClauseUse)getConclusion()).wrapWithOuterLambdas(concTerm, instanceTerm, adaptation, wrappingSub, false); 
		Util.debug("\tresult concTerm is ", concTerm);
		args.add(concTerm);
		Term ruleTerm = App(getRuleAppConstant(), args);

		// somehow the wrapping sub is not fully substituted. 
		Util.debug("\twrappingSub = ", wrappingSub);

		// go back and substitute the wrapping sub in previous places
		// XXX: This shouldn't be necessary, or maybe it even is a mistake
		// if the substituted thing shouldn't have been substituted anyway.
		// The fact we need to do this indicates that the local adaptation 
		// wasn't being done right.
		ruleTerm = ruleTerm.substitute(wrappingSub);

		return ruleTerm;
	}

	/**
	 * Generate a term for the application of this rule/theorem with the given
	 * inputs and outputs.   We also return the implied binding of the assumed
	 * context as a list of wrapping abstractions.
	 * @param ctx
	 * @param inputs
	 * @param output
	 * @param addedContext Output parameter:
	 * what context do the actuals (inputs and outputs) 
	 * assume is passed implicitly.
	 * @param errorPoint
	 * @param isPattern
	 * @return
	 */
	public Application checkApplication(Context ctx, List<? extends Fact> inputs, Fact output, List<Abstraction> addedContext, Node errorPoint, boolean isPattern) {
		Util.verify(addedContext != null && addedContext.isEmpty(), "output parameter should be empty");
		int n = getPremises().size();
		if (inputs.size() != n) {
			ErrorHandler.error(Errors.RULE_PREMISE_NUMBER, getKind()+" "+getName()+", which expects "+n, errorPoint);
		}

		// For better error reporting, do a first stab at type-checking the arguments and result
		for (int i=0; i < n; ++i) {
			Element formal = getPremises().get(i);
			Fact input = inputs.get(i);
			if (input instanceof DerivationPlaceholder) continue; // don't check
			Element actual = input.getElement();
			if (formal.getType().typeTerm() != actual.getType().typeTerm()) {
				ErrorHandler.error(Errors.RULE_PREMISE_MISMATCH, (i+1) + ": " + formal.getType().getName(), isPattern ? input : errorPoint);
			}
		}
		Element concElem = output.getElement();
		if (concElem.getType().typeTerm() != getConclusion().getType().typeTerm()) {
			if (!isPattern && getConclusion().asTerm() == OrJudgment.getContradictionConstant()) {
				ErrorHandler.error(Errors.RULE_CONCLUSION_CONTRADICTION, concElem.getType().getName(), 
							errorPoint,"_: contradiction");
			} else {
				ErrorHandler.error(Errors.RULE_CONCLUSION_MISMATCH, getConclusion().getType().getName() + " != " + concElem.getType().getName(), 
						isPattern ? output : errorPoint);
			}
		}

		List<Term> allArgs = new ArrayList<Term>();

		// we now go through the arguments and match each against the corresponding formal
		// and split each into the context that needs to be split off to match the formal
		// and a term (with bindings into the context).
		List<List<Abstraction>> allContexts = new ArrayList<List<Abstraction>>();
		for (int i=0; i < n; ++i) {
			Element formal = getPremises().get(i);
			Fact input = inputs.get(i);
			if (input instanceof DerivationPlaceholder) {
				DerivationPlaceholder ph = (DerivationPlaceholder)input;
				if (ph.getTerm() instanceof FreeVar) {
					allContexts.add(Collections.emptyList());
					allArgs.add(ph.getTerm());
					continue; // don't check
				}
			}
			Element actual = input.getElement();
			String name = "argument #"+ (i+1);
			if (isPattern) { // flow complex
				if (formal.getRoot() != null && concElem.getRoot() != null) {
					if (!concElem.getRoot().equals(actual.getRoot())) {
						ErrorHandler.error(Errors.CONTEXT_DISCARDED,"" + concElem.getRoot(), input);
					}
				} else if (concElem.getRoot() == null && actual.getRoot() != null) {
					ErrorHandler.recoverableError(Errors.PREMISE_CONTEXT_MISMATCH, input);
				}
			} else if (!Derivation.checkRootMatch(ctx,actual,formal,null)) {
				Node source = errorPoint;
				if (errorPoint instanceof DerivationWithArgs) {
					source = ((DerivationWithArgs)errorPoint).getArgStrings().get(i);
				}
				ErrorHandler.error(Errors.CONTEXT_DISCARDED,"" + actual.getRoot(),source);
			}
			getArgContextAndTerm(ctx, name, formal, actual, allContexts, allArgs, isPattern ? input : errorPoint);
		}
		getArgContextAndTerm(ctx, "conclusion", getConclusion(), concElem, allContexts, allArgs, output);

		addedContext.addAll(unionContexts(ctx,allContexts, errorPoint));


		// Now weaken all premises to fit the combined context
		// Except (1) the conclusion cannot be weakened to fit because it's an output
		//        (2) In general, we want to force the user to explicitly weaken all derivations
		//            so only syntax can be explicitly weakened.
		//        (3) We don't need to weaken things that don't have a root in the rule.
		// If this is a rule pattern match, we can't allow any weakenings, but since we currently
		// only weaken SyntaxAssumptions and rules don't have these, we are fine.  
		// Then we use a different error message however.
		for (int i=0; i < n; ++i) {
			Element formal = getPremises().get(i);
			if (formal.getRoot() == null) continue;
			if (formal.getType() instanceof SyntaxDeclaration) {
				List<Abstraction> abs = allContexts.get(i);
				Term t = allArgs.get(i);
				allArgs.set(i,weakenArg(abs,addedContext,t));
			} else if (allContexts.get(i).size() != addedContext.size()) {
				if (isPattern)
					ErrorHandler.error(Errors.CASE_CONTEXT_INCONSISTENT, inputs.get(i));
				else
					ErrorHandler.error(Errors.OTHER_CONTEXT_NEEDED, Term.wrappingAbstractionsToString(addedContext), errorPoint);
			}
		}
		if (getConclusion().getRoot() != null &&
				allContexts.get(n).size() != addedContext.size()) {
			if (isPattern)
				ErrorHandler.error(Errors.CASE_CONTEXT_INCONSISTENT, output);
			else
				ErrorHandler.error(Errors.OTHER_CONTEXT_JUSTIFIED, Term.wrappingAbstractionsToString(addedContext), errorPoint);
		}


		return Facade.App(getRuleAppConstant(), allArgs);
	}

	/**
	 * @param name
	 * @param formal
	 * @param actual
	 * @param allContexts
	 * @param allArgs
	 */
	protected void getArgContextAndTerm(Context ctx, String name, Element formal,
			Element actual, List<List<Abstraction>> allContexts, List<Term> allArgs, Node errorPoint) {
		Term f = formal.asTerm();
		Term a = actual.asTerm().substitute(ctx.currentSub);
		int diff = a.countLambdas() - f.countLambdas();
		// Leave context errors until later when we can give specific
		// error messages depending on whether this is a case or a call
		/*if (diff < 0) {
			ErrorHandler.error(name + " to " + getName() + " expects more in context than given",errorPoint,
					"SASyLF expected the " + name + " to be " +f+ " but was given " + a + " from " + actual);
		} else*/ if (diff <= 0) {
			allContexts.add(Collections.<Abstraction>emptyList());
			allArgs.add(a);
		} else {
			/*if (formal.getRoot() == null) {
				ErrorHandler.error(name + " to " + getName() + " doesn't expect/permit extra bindings", errorPoint,
						"SASyLF computed the " + name + " supplied as " + a); 
			}*/
			List<Abstraction> context = new ArrayList<Abstraction>();
			allContexts.add(context);
			allArgs.add(Term.getWrappingAbstractions(a, context, diff));
		}
	}

	protected List<Abstraction> unionContexts(Context ctx, List<List<Abstraction>> contexts, Node errorPoint) {
		// XXX: consider requiring them all to be the same, except for syntax
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
							ErrorHandler.error(Errors.RULE_CONTEXT_INCONSISTENT,": " + b.varName, errorPoint);
						}
						if (!argNames.add(b.varName)) {
							while (!result.get(i).varName.equals(b.varName)) {
								seen.add(result.get(i).varName);
								++i;
							}
						} else {
							if (!copied) {
								result = new ArrayList<Abstraction>(result);
								copied = true;
							}
							result.add(i, b);
						}
						// invariant: i and j both point to an abstraction with the name b.varName
						if (!result.get(i).varType.equals(b.varType)) {
							// see bad50.slf
							ErrorHandler.error(Errors.RULE_CONTEXT_INCONSISTENT, " (" + assumeTypeToString(ctx,con,j) + ") != (" + assumeTypeToString(ctx,result,i) + ")", errorPoint);
						}
						++i; ++j;
						seen.add(b.varName);
					}
				}
			}
		}
		if (copied && Util.DEBUG) {
			System.out.println("On line " + errorPoint.getLocation().getLine() + " Merging ");
			for (List<Abstraction> abs : contexts) {
				System.out.println("  " + Term.wrappingAbstractionsToString(abs));
			}
			System.out.println("= " + Term.wrappingAbstractionsToString(result));
		}
		return result;
	}

	protected String assumeTypeToString(Context ctx, List<Abstraction> context, int i) {
		Term t = Term.wrapWithLambdas(context, context.get(i).varType.incrFreeDeBruijn(1), 0, i+1);
		TermPrinter tp = new TermPrinter(ctx,ctx.assumedContext,this.getLocation(),false);
		ClauseUse e = tp.asClause(t);
		int a = e.getConstructor().getAssumeIndex();
		return tp.toString(e.getElements().get(a));
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
	 * Return a fresh application of the rule or theorem in a particular given context.
	 * We copy the premises and conclusion, given them fresh variables, including
	 * possible dependencies on the given context (adaptation) and return an application
	 * of the "AppConstant" to these parts.  We do not wrap the result into the context;
	 * the caller may do so if desired.  We return (via the concFreeVars parameter) if desired
	 * the unique free variables of the conclusion.  For Rule applications this is used to
	 * find what variables we need to check for context loss for.  For Theorem application,
	 * this set includes things that should not be bound by the caller.
	 * @param context      List of binders that are in scope at this use
	 * @param concFreeVars null or empty list; if not null, then it will have all variables
	 *        that occur free in the conclusion that do not occur in the premises.
	 * @return fresh adapted rule application in the given context
	 */
	public Application getFreshAdaptedRuleTerm(List<Abstraction> context, Set<FreeVar> concFreeVars) {
		Util.verify(concFreeVars == null || concFreeVars.isEmpty(), "concFreeVars is output only");
		Util.verify(context != null, "context must be a list");

		List<Term> ruleArgs = new ArrayList<Term>();

		// the substitution to get fresh variables
		Substitution freshSub = new Substitution();

		// the substitution to handle possible dependencies on the context
		// (this cannot be used until we have removed bindings of variable free NTs)
		Substitution adaptSub = new Substitution();

		// the variable-free NTs that should not be adapted:
		Set<FreeVar> varFree = new HashSet<FreeVar>();

		// after adaptation, the free variables in the premises 
		Set<FreeVar> freeVars = new HashSet<FreeVar>();

		List<Term> addedTypes = new ArrayList<Term>();
		int n = getPremises().size();
		for (Abstraction a : context) {
			addedTypes.add(a.varType);
		}

		// freshen each premise, and start to find adaptation
		for (int i=0; i < n; ++i) {
			Element element = this.getPremises().get(i);      
			Term f = getFreshAdaptedTerm(element, addedTypes, freshSub, adaptSub, varFree);
			ruleArgs.add(f);
		}
		ruleArgs.add(getFreshAdaptedTerm(this.getConclusion(), addedTypes,freshSub, adaptSub, varFree));

		// now remove any variable that should not be adapted, and then perform adaptation
		adaptSub.removeAll(varFree);
		for (int i=0; i <= n; ++i) { // including the conclusion!
			Term adapted = ruleArgs.get(i).substitute(adaptSub);
			if (concFreeVars != null) { // caller wants these
				Set<FreeVar> fvs = adapted.getFreeVariables();
				if (i < n) {
					freeVars.addAll(fvs);
				} else {
					fvs.removeAll(freeVars);
					concFreeVars.addAll(fvs);
				}
			}
			ruleArgs.set(i, adapted);
		}

		return Facade.App(this.getRuleAppConstant(), ruleArgs);
	}

	/**
	 * @param element
	 * @param addedTypes
	 * @param freshSub
	 * @param adaptSub
	 * @param varFree
	 * @return
	 */
	protected Term getFreshAdaptedTerm(Element element, List<Term> addedTypes,
			Substitution freshSub, Substitution adaptSub, Set<FreeVar> varFree) {
		Term f = element.asTerm();
		f.freshSubstitution(freshSub);
		f = f.substitute(freshSub);
		if (element.getRoot() == null) {
			varFree.addAll(f.getFreeVariables());
		} else {
			f.bindInFreeVars(addedTypes, adaptSub);
		}
		return f;
	}

	/**
	 * Return whether this rule-like entity has a sensible interface that can be checked.
	 * If not, there's no point is checking uses.
	 * @return whether the interface for this rule-like entity is reasonable.
	 */
	public abstract boolean isInterfaceOK();

	/**
	 * Return the assumes for a judgment / outermostGamma for a theorem.
	 * @return
	 */
	public abstract NonTerminal getAssumes();

	/** Returns a term for this rule, adapting it to the variables in scope in instanceTerm (which should be related to the conclusion) */

	private Constant ruleAppConstant;
}
