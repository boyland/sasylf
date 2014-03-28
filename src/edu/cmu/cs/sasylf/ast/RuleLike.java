package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.term.Facade.App;
import static edu.cmu.cs.sasylf.term.Facade.Const;
import static edu.cmu.cs.sasylf.util.Util.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.Util;


/** Common interface for Rules and Theorems */
public abstract class RuleLike extends Node {
	private String name;
	
	public RuleLike(String n, Location l) { super(l); name = n; }
	public RuleLike(String n) { name = n; }

	public abstract List<? extends Element> getPremises();
    public abstract Set<FreeVar> getExistentialVars();
    public abstract Clause getConclusion();

    public String getName() { return name; }
    public abstract String getKind();

	public Constant getRuleAppConstant() {
		if (ruleAppConstant == null) {
			Term typeTerm = Const(getName() + "BASE", Constant.TYPE);
			List<Term> argTypes = new ArrayList<Term>();

			for (int i = 0; i < getPremises().size(); ++i) {
				argTypes.add(getPremises().get(i).getTypeTerm());
			}

			argTypes.add(((ClauseUse)getConclusion()).getConstructor().asTerm());
			
			typeTerm = Term.wrapWithLambdas(typeTerm, argTypes);
			ruleAppConstant = Const(name + "TERM", typeTerm);
		}
		return ruleAppConstant;
	}
	
	/** Computes a mutable list of free variables that are suitable as premises for this rule */
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
		
	// XXX: Currently the TERM is different from LF/Twelf in two ways:
	// XXX: (1) We represent the conclusion as a separate parameter to the application, and
	// XXX: (2) if the whole thing is in a non-empty variable context, that context
	// XXX: is repeated for each argument to the application, so that the TERM
	// XXX: application doesn't even type check afterwards.  This is a bad idea.
	// XXX: (1) is a way to convert a rule into a theorem.  Just fine.
	// XXX: (2) causes us to leave types off of things.
	
	/** Computes a term for this rule, adapting it to the variables in scope in instanceTerm (which should be related to the conclusion).
	 * Also freshens the variables in this term.
	 * @param wrappingSub <i>output</i>
	 */
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
	// removed because it now depends on instanceTerm
	/*public Term getRuleAppTerm(Term instanceTerm) {
		if (ruleAppTerm == null)
			ruleAppTerm = computeRuleAppTerm(instanceTerm);
		return ruleAppTerm;
	}
	private Term ruleAppTerm;*/
	private Constant ruleAppConstant;
}
