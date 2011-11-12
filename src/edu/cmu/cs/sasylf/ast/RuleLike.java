package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.term.Facade.App;
import static edu.cmu.cs.sasylf.term.Facade.Const;
import static edu.cmu.cs.sasylf.util.Util.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Atom;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;


/** Common interface for Rules and Theorems */
public abstract class RuleLike extends Node {
	private String name;
	
	public RuleLike(String n, Location l) { super(l); name = n; }
	public RuleLike(String n) { name = n; }

	public abstract List<? extends Element> getPremises();
    public abstract Set<FreeVar> getExistentialVars();
    public abstract Clause getConclusion();

    public String getName() { return name; }

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
		debug("adaptation: " + adaptation);
				
		List<Term> termArgs = new ArrayList<Term>();
		for (int i = 0; i < this.getPremises().size(); ++i) {
			ClauseUse clauseUse = (ClauseUse) this.getPremises().get(i);
			ClauseDef clauseDef = clauseUse.getConstructor(); 
			Term type = Constant.UNKNOWN_TYPE; //clauseDef.getTypeTerm();
			String name = clauseDef.getConstructorName();
			Term argTerm = Facade.FreshVar(name, type);
			debug("before: " + argTerm);
			argTerm = clauseUse.wrapWithOuterLambdas(argTerm, instanceTerm, adaptation);
			debug("after: " + argTerm);
			termArgs.add(argTerm);
		}
		return termArgs;
		/*termArgs.add(concTerm);
		Term appliedTerm = App(this.getRuleAppConstant(), termArgs);
		return appliedTerm;*/
	}
		
	/** Computes a term for this rule, adapting it to the variables in scope in instanceTerm (which should be related to the conclusion).
	 * Also freshens the variables in this term.
	 */
	public Term getFreshRuleAppTerm(Term instanceTerm, Substitution wrappingSub, List<Term> termArgsIfKnown /* may be null */) {
		debug("getting conclusion term for rule " + getName());
		Term concTerm = getConclusion().asTerm();
		Substitution ruleSub = new Substitution();			// substitutes fresh vars in rule
		ruleSub = concTerm.freshSubstitution(ruleSub);
		concTerm = concTerm.substitute(ruleSub);
		int adaptation = ((ClauseUse)getConclusion()).getAdaptationNumber(concTerm, instanceTerm, false);
		// TODO: check that premises and conclusion are rooted in the same variable!!!
		// TODO: major hack here.  Must rationalize gamma checking.
		List<Term> args = new ArrayList<Term>();
		//Substitution wrappingSub = new Substitution();
		for (int i = 0; i < getPremises().size(); ++i) {
			Element elem = getPremises().get(i);
			Term argTerm = elem.asTerm();
			ruleSub = argTerm.freshSubstitution(ruleSub);
			argTerm = argTerm.substitute(ruleSub);
			if (elem instanceof ClauseUse) {
				ClauseUse clause = (ClauseUse) elem;
				// only adapt if elem has a Gamma variable at its root
				debug("\tgenerated argterm before adaptation: " + argTerm);
				if (clause.isRootedInVar()) {
					int localAdaptation = adaptation;
					Term localInstanceTerm = instanceTerm;
					if (termArgsIfKnown != null && !((ClauseUse)getConclusion()).isRootedInVar()) {
						localInstanceTerm = termArgsIfKnown.get(i);
						localAdaptation = clause.getAdaptationNumber(argTerm, localInstanceTerm, false);
					}
					debug("adaptation of " + argTerm + " to " + localInstanceTerm + " is " + localAdaptation);
					argTerm = clause.wrapWithOuterLambdas(argTerm, localInstanceTerm, localAdaptation, wrappingSub, false);
					// System.out.println("    wrapping sub = " + wrappingSub);
					debug("\tresult is " + argTerm);
				}
			}
	    // System.out.println("  argTerm = " + argTerm);
			args.add(argTerm);
		}
		debug("\tgenerated concterm before adaptation: " + concTerm);
		debug("adaptation of " + concTerm + " to " + instanceTerm + " is " + adaptation);
		concTerm = ((ClauseUse)getConclusion()).wrapWithOuterLambdas(concTerm, instanceTerm, adaptation, wrappingSub, false); 
		debug("\tresult is " + concTerm);
		args.add(concTerm);
		Term ruleTerm = App(getRuleAppConstant(), args);
		
		// JTB: fix defect #4
		// We need to determine if any variable was substituted in a wrapping
		// sub but is still being used under its old name: we need to return it
		// to the old (unqualified) name.  We painfully substitute it back.
		Set<FreeVar> ruleFreeVars = ruleTerm.getFreeVariables();
		Substitution fixSub = null;
		for (Map.Entry<Atom,Term> e : wrappingSub.getMap().entrySet()) {
		  if (ruleFreeVars.contains(e.getKey())) {
		    // ErrorHandler.warning("case for rule was wrong, trying to fix it", this);
		    if (fixSub == null) fixSub = new Substitution();
		    // Horrid kludge: fixSub.add(e.getKey(), e.getValue());
		    Term appl = e.getValue();
		    Abstraction absMatchTerm = (Abstraction)instanceTerm;
		    List<Term> varTypes = new ArrayList<Term>();
		    List<String> varNames = new ArrayList<String>();
		    
        List<? extends Term> fixargs = ((Application)appl).getArguments();
		    ClauseUse.readNamesAndTypes(absMatchTerm, fixargs.size(), varNames, varTypes);
		    Term body = e.getKey();
		    for (Term ty : varTypes) {
		      body = Facade.Abs(ty, body);
		    }
		    Atom function = ((Application)appl).getFunction();
        // System.out.println("substituting " + function + " with " + body);
		    fixSub.add(function,body);
		  }
		}
		if (fixSub != null) {
		  ruleTerm = ruleTerm.substitute(fixSub);
		}

		/*debug("\tgenerated term before freshification: " + ruleTerm);
		
		Substitution ruleSub = new Substitution();			// substitutes fresh vars in rule
		ruleSub = ruleTerm.freshSubstitution(ruleSub);
		ruleTerm = ruleTerm.substitute(ruleSub);*/

		return ruleTerm;
	}

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
