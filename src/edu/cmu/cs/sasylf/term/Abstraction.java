package edu.cmu.cs.sasylf.term;

import static edu.cmu.cs.sasylf.term.Facade.Abs;
import static edu.cmu.cs.sasylf.term.Facade.pair;
import static edu.cmu.cs.sasylf.util.Util.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Util;

public class Abstraction extends Term {
	public Term varType;
	public String varName;
	private Term body;

	public Term getArgType() {
		return varType;
	}

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
	
	/**
	 * Create an abstraction while substituting the formal variable with
	 * free variables in the body.
	 * @param v variable used in the body (perhaps)
	 * @param type type
	 * @param body body of abstraction
	 * @return new abstraction
	 */
	public static Term make(FreeVar v, Term type, Term body) {
		Substitution sub = new Substitution();
		sub.add(v, new BoundVar(1));
		return make(v.getName(), type, body.substitute(sub));
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

	@Override
	Term substitute(Substitution s, int varIncrAmount) {
		Term newBody = body.substitute(s, varIncrAmount+1);
		Term newType = varType.substitute(s, varIncrAmount);
		if (newBody == body && newType == varType)
			return this;
		else
			return make(varName, newType, newBody);
	}

	@Override
	void getFreeVariables(Set<FreeVar> s) {
		body.getFreeVariables(s);
		varType.getFreeVariables(s);
	}
	
	@Override
	protected void getBoundVariables(List<Pair<String, Term>> s) {
		s.add(new Pair<String, Term>(varName, varType));
		body.getBoundVariables(s);
		varType.getBoundVariables(s);
	}

	@Override
	protected Term remakeHelper(List<Pair<String, Term>> varBindings) {
		if (varBindings.isEmpty()) return this;
		Pair<String, Term> nextNew = varBindings.remove(0);
		if (!nextNew.second.equals(varType)) return this;
		return make(nextNew.first, nextNew.second, 
			body.remakeHelper(varBindings));
	}

	/** performs a unification, or fails throwing exception, then calls instanceHelper
	 * to continue.  The current substitution is applied lazily.
	 */
	@Override
	void unifyCase(Term other, Substitution current, Queue<Pair<Term,Term>> worklist) {
		if (other instanceof Abstraction) {
			// Earlier, we would test to see if one was eta-equivalent to a free var
			// but that causes problems if both mentioned the same variable.
			worklist.add(makePair(body, ((Abstraction)other).body));
			worklist.add(makePair(varType, ((Abstraction)other).varType));
			unifyHelper(current, worklist);
		} else
			throw new UnificationFailed(other.toString() + " is not an instance of " + this + " (may need to implement eta-normalization)", this, other);
	}

	@Override
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

	@Override
	protected boolean selectUnusablePositions(int bound,
			Set<Pair<FreeVar, Integer>> unusable) {
		return varType.selectUnusablePositions(bound, unusable) &&
				body.selectUnusablePositions(bound+1, unusable);
	}

	@Override
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
			Term newType = varType.apply(arguments, whichApplied-1);
			if (newBody == body && newType == varType)
				// no change
				return this;
			else
				return make(varName, newType, newBody);
		}
	}

	@Override
	public Term incrFreeDeBruijn(int nested, int amount) {
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

	@Override
	public String toString() {
		return "fn " + varName + ":" + varType + " => " + body;
	}

	@Override
	public int countLambdas() {
		return body.countLambdas()+1;
	}

	@Override
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
	public void bindInFreeVars(List<Term> typeTerms, Substitution sub) {
		body.bindInFreeVars(typeTerms, sub);
		varType.bindInFreeVars(typeTerms, sub);
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

	@Override
	public FreeVar getEtaPermutedEquivFreeVar(FreeVar src, Substitution revSub) {
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
			// doesn't take into account arguments being eta-long
			// but then again neither does getEtaEquivFreeVar
			if (!(a.getFunction() instanceof FreeVar)) {
				Util.debug("  Not a free var: ", a.getFunction());
				return null;
			}
			int[] indices = new int[argCount];
			int[] reverse = new int[argCount];
			for (int i = 0; i < argCount; ++i) {
				Term arg = a.getArguments().get(i);
				if (!(arg instanceof BoundVar)) {
					Util.debug("  Arg #", i, " is not a bound var: ", arg);
					return null;
				}
				int index = ((BoundVar)arg).getIndex();
				if (index > argCount) return null;
				if (reverse[argCount - index] != 0) return null; // not a permutation
				indices[i] = argCount - index;
				reverse[argCount - index] = argCount - i;
			}
			List<Term> revArgs = new ArrayList<Term>(argCount);
			for (int i=0; i < argCount; ++i) {
				revArgs.add(new BoundVar(reverse[i]));
			}
			Abstraction w = this;
			Abstraction[] wrappers = new Abstraction[argCount];
			wrappers[0] = w;
			for (int i=1; i < argCount; ++i) {
				w = (Abstraction)w.body;
				wrappers[i] = w;
			}
			FreeVar fv = (FreeVar)a.getFunction();
			if (src != null) {
				t = new Application(src,revArgs);
				for (int i=argCount-1; i >= 0; --i) {
					String name = wrappers[indices[i]].varName;
					Term type = wrappers[indices[i]].varType;
					t = new Abstraction(name,type,t);
				}
				revSub.add(fv,t);
			}
			return fv;
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

	@Override
	public Term stripUnusedLambdas() {
		Term newBody = body.stripUnusedLambdas();
		if (newBody.hasBoundVar(1)) {
			if (body == newBody) return this;
			return make(varName,varType,newBody); 
		} else {
			return newBody.incrFreeDeBruijn(-1);
		}
	}

	@Override
	public boolean contains(Term other) {
		boolean first = super.contains(other);
		if (first) return true;
		// special case for induction:
		// \x.a[x] >= a[i] if result cannot appear in i
		if (other instanceof Application) {
			Application app = (Application)other;
			if (app.getFunction().equals(this.getEtaEquivFreeVar())) {
				Util.debug("found eta-equiv function application");
				if (!FreeVar.canAppearIn(app.getFunction().getTypeFamily(), varType.baseTypeFamily())) {
					Util.debug("Yes! ", app.getFunction().getTypeFamily(), " /< ", varType.baseTypeFamily());
					return true;
				}
				Util.debug("Nope ", app.getFunction().getTypeFamily(), " < ", varType.baseTypeFamily());
			}
		}
		return false;
	}

	@Override
	public boolean containsProper(Term other) {
		// another special case
		// \x.a[x] >= \x.b[x] if a[x] >= b[x]
		if (other instanceof Abstraction && varType.equals(((Abstraction)other).varType)) {
			if (body.containsProper(((Abstraction)other).getBody())) {
				return true;
			}
		}
		return body.contains(other);
	}
}
