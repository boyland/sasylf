package edu.cmu.cs.sasylf.term;

import java.util.*;
import java.io.*;

import static edu.cmu.cs.sasylf.term.Facade.*;
import static edu.cmu.cs.sasylf.util.Util.*;

public class Abstraction extends Term {
	public Term varType;
	public String varName;
	private Term body;
	
	public static Term make(String var, Term type, Term body) {
		if (body instanceof Application && !((Application)body).isFullyAppliedFreeVar()) {
			Application bodyApp = (Application) body;
			List<? extends Term> args = bodyApp.getArguments();
			Term lastArg = args.get(args.size()-1); 
			if (bodyApp.getFunction() instanceof FreeVar && lastArg instanceof BoundVar && ((BoundVar) lastArg).getIndex() == 1) {
				Term result;
				// eta reduce
				if (args.size() == 1)
					result = bodyApp.getFunction();
				else {
					List<Term> newArgs = new ArrayList<Term>(args);
					newArgs.remove(newArgs.size()-1);
					result = new Application(bodyApp.getFunction(), newArgs);
				}
				if (!result.hasBoundVar(1))	// last check to ensure eta reduction is legal
					return result.incrFreeDeBruijn(0, -1);
			}
		}
		return new Abstraction(var, type, body);
	}

	private Abstraction(String name, Term type, Term b) {
		varName = name; varType = type; body =b;
		// verify that no eta-reduction is possible
		if (b instanceof Application && !((Application)b).isFullyAppliedFreeVar()) {
			Application bodyApp = (Application) body;
			List<? extends Term> args = bodyApp.getArguments();
			Term lastArg = args.get(args.size()-1); 
			if (bodyApp.getFunction() instanceof FreeVar && lastArg instanceof BoundVar && ((BoundVar) lastArg).getIndex() == 1) {
				boolean hasBoundVar = bodyApp.getFunction().hasBoundVar(1);
				for (int i = 0; i < args.size()-1; ++i) {
					hasBoundVar = hasBoundVar || args.get(i).hasBoundVar(1);
				}
				verify(hasBoundVar, "non-eta-normal term");
			}
			//verify(!(lastArg instanceof BoundVar && ((BoundVar) lastArg).getIndex() == 1 && !b.hasBoundVar(1)), "non-eta-normal term");
		}
	}

	Term substitute(Substitution s, int varIncrAmount) {
		Term newBody = body.substitute(s, varIncrAmount+1);
		Term newType = varType.substitute(s, varIncrAmount);
		if (newBody == body && newType == varType)
			return this;
		else
			return make(varName, newType, newBody);
	}

	void getFreeVariables(Set<FreeVar> s) {
		body.getFreeVariables(s);
		varType.getFreeVariables(s);
	}

	/** performs a unification, or fails throwing exception, then calls instanceHelper
	 * to continue.  The current substitution is applied lazily.
	 */
	void unifyCase(Term other, Substitution current, Queue<Pair<Term,Term>> worklist) {
		if (other instanceof Abstraction) {
			// see if one is eta-equivalent to a free variable
			Term myVar = getEtaEquivFreeVar();
			Term otherVar = other.getEtaEquivFreeVar();
			
			if (myVar != null || otherVar != null) {
				worklist.add(makePair(myVar == null ? this : myVar, otherVar == null ? other : otherVar));
			} else {
				worklist.add(makePair(body, ((Abstraction)other).body));
				worklist.add(makePair(varType, ((Abstraction)other).varType));
			}
			unifyHelper(current, worklist);
		} else
			// TODO: eta-normalize on the fly where necessary
			throw new UnificationFailed(other.toString() + " is not an instance of " + this + " (may need to implement eta-normalization)", this, other);
	}

	void unifyFlexApp(FreeVar function, List<? extends Term> arguments, Substitution current, Queue<Pair<Term,Term>> worklist) {
		List<Term> newArgs = new ArrayList<Term>();
		for (Term t : arguments) {
			newArgs.add(t.incrFreeDeBruijn(0, 1));
		}
		newArgs.add(new BoundVar(1));
		Application newApp = new Application(function, newArgs);
		worklist.add(makePair(newApp, body));
		unifyHelper(current, worklist);
	}

	public Term apply(List<? extends Term> arguments, int whichApplied) {
		whichApplied++;
		List<Term> newArgs = new ArrayList<Term>();
		for (Term t : arguments) {
			newArgs.add(t.incrFreeDeBruijn(1));
		}
		
		Term newBody = body.apply(newArgs, whichApplied);
		if (whichApplied <= arguments.size()) {
			// if we just applied an argument, return the body, with free vars decremented
			return newBody.incrFreeDeBruijn(-1);
		} else {
			// we're just substituting
			if (newBody == body)
				// no change
				return this;
			else
				return make(varName, varType, newBody);
		}
	}

	Term incrFreeDeBruijn(int nested, int amount) {
		Term newBody = body.incrFreeDeBruijn(nested+1, amount);
		Term newType = varType.incrFreeDeBruijn(nested, amount);
		if (newBody == body && newType == varType)
			return this;
		else
			return make(varName, newType, newBody);
	}

	@Override
	public boolean hasBoundVar(int i) {
		return body.hasBoundVar(i+1) || varType.hasBoundVar(i);
	}

	@Override
	public boolean hasBoundVarAbove(int i) {
		return body.hasBoundVarAbove(i+1) || varType.hasBoundVarAbove(i);
	}

	@Override
	public int hashCode() { return body.hashCode(); }

	@Override
	public boolean typeEquals(Term otherType) {
		if (super.typeEquals(otherType))
			return true;
		if (!(otherType instanceof Abstraction))
			return false;
		Abstraction a = (Abstraction) otherType;
		return body.typeEquals(a.body) && varType.typeEquals(a.varType);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Abstraction)) return false;
		Abstraction a = (Abstraction) obj;
		return body.equals(a.body) && varType.typeEquals(a.varType);
	}

	public String toString() {
		return "fn " + varName + ":" + varType + " => " + body;
	}
	
	public int countLambdas() {
		return body.countLambdas()+1;
	}
	
	public Term getType(List<Pair<String, Term>> varBindings) {
		List<Pair<String, Term>> newBindings = new ArrayList<Pair<String, Term>>(varBindings);
		newBindings.add(pair(varName, varType));
		Term bodyType = body.getType(newBindings);
		return Abs(varName, varType, bodyType);
	}

	public void setBody(Term body) {
		this.body = body;
	}

	public Term getBody() {
		return body;
	}

	/** Produces a substitution that will bind an outer bound variable in all free variables.
	 * Here we just recurse to the argument.
	 */
	@Override
	public void bindInFreeVars(Term typeTerm, Substitution sub) {
		body.bindInFreeVars(typeTerm, sub);
		varType.bindInFreeVars(typeTerm, sub);
	}

	@Override
	public void bindInFreeVars(Term typeTerm, Substitution sub, int i) {
		body.bindInFreeVars(typeTerm, sub, i);
		varType.bindInFreeVars(typeTerm, sub, i);
	}

	@Override
	public void bindInFreeVars(List<Term> typeTerms, Substitution sub, int idx) {
		body.bindInFreeVars(typeTerms, sub, idx);
		varType.bindInFreeVars(typeTerms, sub, idx);
	}

	/** Binds the ith bound variable in all free variables.
	 * Modifies the substitution to reflect changes.
	 * We just recurse down into the body, with an incremented i
	 */
	@Override
	@Deprecated
	public Term oldBindInFreeVars(int i, Term typeTerm, Substitution sub) {
		Term newBody = body.oldBindInFreeVars(i+1, typeTerm, sub);
		if (newBody == body)
			return this;
		else
			return make(varName, varType, newBody);
	}

	/** Attempts to remove all bound variables above index i and above from the expression.
	 * If this is impossible a UnificationFailedException is thrown.
	 * We just recurse down into the body, with an incremented i
	 */
	@Override
	public Term removeBoundVarsAbove(int i) {
		Term newBody = body.removeBoundVarsAbove(i+1);
		if (newBody == body)
			return this;
		else
			return make(varName, varType, newBody);
	}

	/** Attempts to remove all bound variables above index i and above from the expression.
	 * If this is impossible a UnificationFailedException is thrown.
	 * We just recurse down into the body, with an incremented i
	 */
	@Override
	public void removeBoundVarsAbove(int i, Substitution sub) {
		body.removeBoundVarsAbove(i+1, sub);
		varType.removeBoundVarsAbove(i, sub);
	}
	
	@Override
	public FreeVar getEtaEquivFreeVar() {
		Term t = this;
		int argCount = 0;
		while (t instanceof Abstraction) {
			t = ((Abstraction)t).body;
			argCount++;
		}
		if (t instanceof Application) {
			Application a = (Application) t;
			if (a.getArguments().size() != argCount)
				return null;
			if (!(a.getFunction() instanceof FreeVar))
				return null;
			else {
				for (int i = 0; i < argCount; ++i) {
					Term arg = a.getArguments().get(i);
					if (!(arg instanceof BoundVar))
						return null;
					if (((BoundVar)arg).getIndex() != argCount - i)
						return null;
				}
				return (FreeVar)a.getFunction();
			}
		} else {
			return null;
		}
	}

	public Term subInside(Substitution sub, int size) {
		Term newBody = (size > 1) ? ((Abstraction)body).subInside(sub, size-1) : body.substitute(sub);
		if (newBody == body)
			return this;
		else
			return make(varName, varType, newBody);			
	}
}
