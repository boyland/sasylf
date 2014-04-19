package edu.cmu.cs.sasylf.term;

import static edu.cmu.cs.sasylf.util.Util.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import edu.cmu.cs.sasylf.util.Util;

public class Application extends Term {
	public Application(Atom f, List<? extends Term> a) {
		function = f;
		arguments = a;
		// check and convert to eta long
		boolean convertFlag = false;
		for (Term arg : arguments)
			if (arg instanceof FreeVar && ((FreeVar)arg).getType().countLambdas() > 0)
				convertFlag = true;
		if (convertFlag) {
			List<Term> newA = new ArrayList<Term>();
			for (Term arg : a)
				newA.add(arg.toEtaLong());
			arguments = newA;
		}
		getType(new ArrayList<Pair<String, Term>>()); // make sure the types are OK
		/*if (f instanceof FreeVar && ((FreeVar)f).getType().countLambdas() > 0) {
			verify(((FreeVar)f).getType().countLambdas() == a.size(), "applied freevar " + f + " with wrong number of arguments");
		}*/ // TODO: may want to add this check eventually for consistent eta-long forms
		if (a.size() == 0)
			throw new RuntimeException("empty application args");
	}

	public Application(Atom f, Term a) {
		this(f, Arrays.asList(new Term[] { a }));
	}

	private Atom function;
	private List<? extends Term> arguments;

	public Atom getFunction() { return function; }
	public List<? extends Term> getArguments() { return arguments; }

	Term substitute(Substitution s, int varIncrAmount) {
		Term newF = function.substitute(s, varIncrAmount);
		List<Term> newArgs = new ArrayList<Term>();
		boolean isNew = false;
		for (Term a : arguments) {
			Term t = a.substitute(s, varIncrAmount);
			newArgs.add(t);
			if (t != a)
				isNew = true;
		}

		if (isNew || function != newF) {
			return newF.apply(newArgs, 0);
		} else {
			return this;
		}
	}

	public Term apply(List<? extends Term> otherArgs, int whichApplied) {
		Atom newFunction = function;
		List<Term> newArgs = new ArrayList<Term>(arguments);
		if (whichApplied > 0) {
			for (int i = 0; i < newArgs.size(); ++i) {
				newArgs.set(i, newArgs.get(i).apply(otherArgs.subList(0, Math.min(otherArgs.size(), whichApplied)), whichApplied));
			}
			newFunction = (Atom) newFunction.apply(otherArgs.subList(0, Math.min(otherArgs.size(), whichApplied)), whichApplied);
		}

		newArgs.addAll(otherArgs.subList(Math.min(otherArgs.size(), whichApplied), otherArgs.size()));
		return new Application(newFunction, newArgs);
	}

	Term incrFreeDeBruijn(int nested, int amount) {
		// note: functions are constants or FreeVars and so can't be affected

		List<Term> newArgs = new ArrayList<Term>();
		boolean isNew = false;
		for (Term a : arguments) {
			Term t = a.incrFreeDeBruijn(nested, amount);
			newArgs.add(t);
			if (t != a)
				isNew = true;
		}

		if (isNew) {
			return new Application(function, newArgs);
		} else {
			return this;
		}
	}

	@Override
	public boolean hasBoundVar(int i) {
		boolean result = function.hasBoundVar(i);
		for (Term a : arguments) {
			result = result || a.hasBoundVar(i);
		}
		return result;
	}

	@Override
	public boolean hasBoundVarAbove(int i) {
		boolean result = function.hasBoundVarAbove(i);
		for (Term a : arguments) {
			result = result || a.hasBoundVarAbove(i);
		}
		return result;
	}

	void getFreeVariables(Set<FreeVar> s) {
		function.getFreeVariables(s);
		for (Term a : arguments) {
			a.getFreeVariables(s);
		}
	}

	int getOrder() { return 1 + function.getOrder()/2; }

	/** returns 1 if this is a non-pattern free variable application, otherwise 0 */
	boolean isNonPatFreeVarApp() {
		if (function instanceof FreeVar) {
		    for (Term t : arguments)
				if (!(t instanceof BoundVar))
					return true;
		}
		return false;
	}
	/** returns 1 if this is a non-pattern free variable application, otherwise 0 */
	boolean isNonPatFreeVarApp(Term other) {
		if (function instanceof FreeVar) {
		    for (Term t : arguments)
				if (!(t instanceof BoundVar))
					return true;//other.isNonPatFreeVarApp() || other instanceof FreeVar;
		}
		return false;
	}
	
	static boolean isPattern(Term f, List<? extends Term> args) {
		if (f instanceof FreeVar) {
			for (Term t : args)
				if (!(t instanceof BoundVar))
					return false;
			if (args.size() < 2) return true;
			Set<Integer> s = new HashSet<Integer>();
			for (Term t : args) {
			  if (!s.add(((BoundVar)t).getIndex())) return false;
			}
			return true;
		} else
			return false;
	}
	boolean isPattern() {
		return isPattern(function, arguments);
	}

	/** performs a unification, or fails throwing exception, then calls instanceHelper
	 * to continue.  The current substitution is applied lazily.
	 */
	void unifyCase(Term other, Substitution current, Queue<Pair<Term,Term>> worklist) {
		// invariant: never called with other a FreeVar (comes from ordering of Pairs)
		if (function instanceof Constant) {
			if (!(other instanceof Application
					&& ((Application)other).arguments.size() == arguments.size()))
				throw new UnificationFailed(this.toString() + " and " + other, this, other);

			Application otherApp = (Application) other;
			worklist.add(makePair(function, otherApp.function));
			for (int i = 0; i < arguments.size(); ++i)
				worklist.add(makePair(arguments.get(i), otherApp.arguments.get(i)));
			unifyHelper(current, worklist);
		} else {
			// apply current substitution to function
			FreeVar functionVar = (FreeVar) function;
			Term t = current.getMap().get(functionVar);
			if (t != null) {
				worklist.add(makePair(t.apply(arguments, 0), other));
				unifyHelper(current, worklist);
			} else {
				// TODO: apply pattern unification here first, if possible!				
				other.unifyFlexApp((FreeVar)function, arguments, current, worklist);
			}
		}
	}

	void unifyFlexApp(FreeVar otherVar, List<? extends Term> otherArgs, Substitution current, Queue<Pair<Term,Term>> worklist) {
    Application otherApp =  new Application(otherVar, otherArgs);
		if (function instanceof Constant) {
			// avoid infinite loop
			if (this.getFreeVariables().contains(otherVar)) {
				throw new UnificationFailed("recursion detected", otherApp, this);
			}

			/* case: C e1...en = E e1'...em'
			 * 
			 * let H1..Hn = fresh, takes same arguments as E but result type is ith argument of C
			 * 
			 * worklist += (e1 ~= (H1 e1'...em')) ...
			 * 
			 * set E = \x1...\xm . C (H1 e1''...em'') ... (Hn...)
			 * 
			 * in above, E is otherVar, e1'...em' is otherArgs, C is function, e1..en is arguments
			 * e1''...em'' is xm...x1.
			 */
			
			if (!otherApp.isPattern()) {
			  throw new UnificationIncomplete("Not in pattern subset: " + this + " ?=? " + otherApp,this,otherApp);
			}

			List<Term> helperArgs = new ArrayList<Term>();
			int m = otherArgs.size();
			for (int i=0; i < m; ++i) {
			  helperArgs.add(new BoundVar(m-i));
			}
			
			Constant constant = (Constant) function;
			List<Term> newArgs = new ArrayList<Term>();
			Term partialFunctionType = constant.getType();
			List<Term> otherVarArgTypes = getArgTypes(otherVar.getType(), otherArgs.size());
			for (int i = 0; i < arguments.size(); ++i) {
				// takes same arguments as otherVar but result type is ith argument of function
				Term newVarType = wrapWithLambdas(((Abstraction)partialFunctionType).varType, otherVarArgTypes);
				partialFunctionType = ((Abstraction)partialFunctionType).getBody();
				
				FreeVar newVar = FreeVar.fresh("none", newVarType);
        newArgs.add(new Application(newVar, helperArgs));
				Application argApp = new Application(newVar, otherArgs);
				worklist.add(makePair(argApp, arguments.get(i))); 
			}

			Term varMatch = new Application(constant, newArgs);
			varMatch = wrapWithLambdas(varMatch, otherVarArgTypes);

			current.add(otherVar, varMatch);

			unifyHelper(current, worklist);
		} else {
			// apply current substitution to function
			FreeVar functionVar = (FreeVar) function;
			Term t = current.getMap().get(functionVar);
			if (t != null) {
				worklist.add(makePair(t.apply(arguments, 0), otherVar.apply(otherArgs, 0)));
				unifyHelper(current, worklist);
				return;
			}

			if (otherVar.equals(function.substitute(current))) {
				verify(arguments.size() == otherArgs.size(), "internal invariant: args to var must be of equal length");

				// assume all args are used
				// TODO: is this an assumption we really want? 
				// Safe if existential variable is universally quantified, otherwise not!
				// certainly OK for vars - in that case we are in Nipkow flexflex1
				// unify all args
				for (int i = 0; i < arguments.size(); ++i)
					worklist.add(makePair(otherArgs.get(i), arguments.get(i)));

				unifyHelper(current, worklist);
			} else {
				/* case: F x1...xn = G y1...ym
				 * 
				 * let H = fresh, takes arguments common to x1...xn and y1..ym
				 *                then result type is same as F after x1...xn applied
				 * 
				 * set E = \x1...\xm . C (H1 e1'...em') ... (Hn...)
				 * 
				 * in above, E is otherVar, e1'...em' is otherArgs, C is function, e1..en is arguments
				 */ 

				// quick fix for supporting eta-long forms better
				// TODO: is this general enough?
				

				// verify that this is a pattern
				if (!otherApp.isPattern()) {
				  Util.debug("not pattern: ", otherApp);
					otherApp.tryEtaLongCase(this, current, worklist);
					return;
				}

				if (!isPattern()) {
          Util.debug("not pattern: ", this);
					tryEtaLongCase(otherApp, current, worklist);
					return;
				}


				// invariant (of Queue): if we get here, all other things are flex-flex patterns (Nipkow flexflex2)



				// create a new free variable
				// result type is same as function after arguments applied (or G after y1...ym applied)
				// but takes arguments from commonArgs 

				List<BoundVar> commonArgs = new ArrayList<BoundVar>(arguments.size());
				for (Term arg : arguments) {
				  commonArgs.add((BoundVar)arg);
				}
				//commonArgs.retainAll((List) otherArgs);
				List<Term> functionArgTypes = getArgTypes(function.getType(), arguments.size());
				Term residualType = function.getType();
				// compute commonArgs.retainAll((List) otherArgs) but cut down functionArgTypes as well
				for (int i = 0; i < commonArgs.size();) {
					Term arg = commonArgs.get(i);
					if (otherArgs.contains(arg)) {
						++i;
					} else {
						commonArgs.remove(i);
						functionArgTypes.remove(i);
					}
					residualType = ((Abstraction)residualType).getBody();
				}

				Term HType = wrapWithLambdas(residualType, functionArgTypes);
				FreeVar H = FreeVar.fresh("H", HType);

				Term varMatch = computeVarMatch(H, commonArgs, arguments, getArgTypes(function.getType(), arguments.size()), current, otherApp);
				Term otherVarMatch = computeVarMatch(H, commonArgs, otherArgs, getArgTypes(otherVar.getType(), otherArgs.size()), current, otherApp);

				if (Util.DEBUG && varMatch.equals(otherVarMatch)) {       
				  Util.debug("pointless substitution for ",function," and ",otherVar);
				}
				current.add(function, varMatch);
				current.add(otherVar, otherVarMatch);

				unifyHelper(current, worklist);

				/*
			// OLD: we will allow non-patterns, but require the following:
			// form: F(e1...en) instanceof G(e1'...em')
			// thus e1...en must be a (possibly reordered) subset of e1'...em'
			// G = lam y1...ym F(...) - where ... is y's chosen to match e1...en

			// example: F(1, 3, 2) instanceof G(4, 2, 3, 1)
			// G = lam x4 lam x2 lam x3 lam x1 F (x1 x3 x2)  ==  F(1 2 3)
			// develop map: 4 => 4, 2 => 3, 3 => 2, 1 => 1
			// apply map to (1 3 2) yields (1 2 3)
			List<Term> substitutedOtherArgs = new ArrayList<Term>();
			for (Term t : otherArgs)
				substitutedOtherArgs.add(t.substitute(current));

			List<Term> fArgs = new ArrayList<Term>();
			for (Term t : arguments) {
				t = t.substitute(current);

				// find position of t in otherArgs
				int foundIndex = substitutedOtherArgs.indexOf(t);

				// if not found, unification fails
				if (foundIndex == -1)
					throw new UnificationFailed(this.toString() + " is not an instance of " + errorApp + ": could not find argument " + t, errorApp, this);

				// if found, newIndex = otherArgs.size() - foundIndex
				int newIndex = otherArgs.size() - foundIndex;
				fArgs.add(new BoundVar(newIndex));
			}
			Term varMatch = new Application(function, fArgs);

			// wrap varMatch in an application for each argument in otherArgs
			for (int i = 0; i < otherArgs.size(); ++i) {
				varMatch = new Abstraction("x", "X", varMatch);
			}

			current.add(otherVar, varMatch);

			unifyHelper(current, worklist);
				 */
			}
		}
	}

	private void tryEtaLongCase(Application application, Substitution current, Queue<Pair<Term, Term>> worklist) {
	  // This code tries to find a sound way to handle an otherwise illegal flexflex case.
	  // In the past this code was hardly ever used and harbored major errors.
	  Util.verify(function instanceof FreeVar, "tryEtaLongCase requires flexflex, function is " + function);
	  Util.verify(application.getFunction() instanceof FreeVar, "tryEtaLongCase requires flexflex, application is "+application);
	  Util.verify(!isPattern(), "tryEtaLongCase should only be called if this is not a pattern: " + this);

	  // We have to be very careful in our heuristics.  SASyLF relies on unification.
	  // If we report an ordinary unification failure for a case, this means the
	  // case can be omitted.
	  
	  // Case:
	  //    F x1 ... xn = G y1 ... ym
	  // where y1 ... ym are distinct bound variables (G y1 ... ym is a pattern)
	  // and x1 ... xn use only bound variables from y1 ... ym
	  // We bind G to \m...\1 . F x'1 ... x'n
	  // where in x'i, we replace every yj with j.  This is accomplished by applying
	  //     (\N...\1 . F x1 ... xn) y-1_N .. y-1_1
	  // where N = max yj and 
	  //       y-1_i 
	  //
	  // If F took other bound variables, then G would be bound to something with variables
	  // which will cause problems eventually.
	  case1: if (application.isPattern()) {
	    // Compute N and the y's
	    int m = application.arguments.size();
	    int N = 0;
	    for (Term t : application.arguments) {
	      int i = ((BoundVar)t).getIndex();
	      if (i > N) N = i;
	    }
	    int[] inverse = new int[N];
	    for (int j=0; j < m; ++j) {
	      BoundVar bv = (BoundVar)application.arguments.get(j);
	      int i = bv.getIndex();
	      Util.verify(inverse[i-1] == 0, "not really a pattern");
	      inverse[i-1] = m-j;
	    }
      Util.debug("Potential eta-long-case 1: ",this," = ",application,", with " + Arrays.toString(inverse));
	    
	    // Check condition
	    if (this.hasBoundVarAbove(N)) {
	      Util.debug("  has BV over " + N);
	      break case1;
	    }
	    for (int i=0; i < N; ++i) {
	      if (inverse[i] == 0 && this.hasBoundVar(i+1)) {
	        Util.debug("  ",this,".hasBoundVar(" + (i+1) + ")");
	        break case1;
	      }
	    }
	    
	    Util.debug("Found eta-long-case 1: ",this," = ",application,", with " + Arrays.toString(inverse));
	    // Construct the binding
	    List<Abstraction> abs = new ArrayList<Abstraction>();
	    getWrappingAbstractions(application.function.getType(),abs);
	    List<Term> tempTypes = new ArrayList<Term>();
	    for (int i=0; i < N; ++i) {
	      int j = inverse[i]-1;
	      if (j < 0) tempTypes.add(Constant.UNKNOWN_TYPE);
	      else tempTypes.add(abs.get(j).varType);
	    }
	    Collections.reverse(tempTypes);
	    Term converted = wrapWithLambdas(this,tempTypes);
	    Util.debug("  converted = ",converted);
	    List<Term> tempArgs = new ArrayList<Term>();
	    for (int i=0; i < N; ++i) {
	      int j = inverse[i]-1;
	      if (j < 0) tempArgs.add(this);
	      else tempArgs.add(new BoundVar(j+1));
	    }
	    Collections.reverse(tempArgs);
	    Util.debug("  tempArgs = " + tempArgs);
	    converted = converted.apply(tempArgs, 0);
	    Util.debug("  body = " + converted);
	    converted = Term.wrapWithLambdas(abs, converted);
	    Util.debug("  result = " + converted);
      Util.debug("fixing up eta long case in pattern unification: ", application.function, " ==> ", converted);
      current.add((FreeVar)application.getFunction(), converted);
      unifyHelper(current, worklist);
      return;
	  }

	  // Some previous unsound heuristics:
	  // 
	  // @ If the applications were the same size, equate everything:
	  //    (F X) = (G X) ==> F = G
	  //   Not sound: this doesn't include the solution
	  //     F = \x.x,  G = \x.a,  X = a
	  //
	  // @ If one application has all the arguments of the other, then bind the shorter
	  //   variable to the longer one.  This doesn't work for the same reason as above.
	  
		throw new UnificationIncomplete("not implemented: non-pattern unification case after delay: " + application + " and " + this, application, this);
		
	}

	private  Term computeVarMatch(FreeVar H, List<BoundVar> commonArgs, List<? extends Term> arguments2, List<Term> arguments2Types, Substitution current, Application errorApp) {
		// otherArgs => arguments2?
		// arguments => commonArgs?
		
		// build up a map
		List<Term> substitutedOtherArgs = new ArrayList<Term>();
		for (Term t : arguments2)
			substitutedOtherArgs.add(t.substitute(current));
		
		// apply the map to construct new arguments
		List<Term> fArgs = new ArrayList<Term>();
		for (Term t : commonArgs) {
			t = t.substitute(current);

			// find position of t in otherArgs
			int foundIndex = substitutedOtherArgs.indexOf(t);

			// if not found, unification fails
			if (foundIndex == -1)
				throw new UnificationFailed(this.toString() + " is not an instance of " + errorApp + ": could not find argument " + t, errorApp, this);

			// if found, newIndex = otherArgs.size() - foundIndex
			int newIndex = arguments2.size() - foundIndex;
			fArgs.add(new BoundVar(newIndex));
		}
		Term varMatch = fArgs.isEmpty()? H : new Application(H, fArgs);

		varMatch = wrapWithLambdas(varMatch, arguments2Types);
		
		// wrap varMatch in an application for each argument in otherArgs
		/*for (int i = 0; i < arguments2.size(); ++i) {
			varMatch = Abstraction.make("x", "X", varMatch);
		}*/

		return varMatch;
	}

	@Override
  protected boolean selectUnusablePositions(int bound,
      Set<Pair<FreeVar, Integer>> unusable) {
	  boolean result = true;
	  if (function instanceof FreeVar) {
	    for (int i=0; i < arguments.size(); ++i) {
	      Term t = arguments.get(i);
	      if (t instanceof BoundVar) {
	        BoundVar b = (BoundVar)t;
	        if (b.getIndex() > bound) {
	          unusable.add(new Pair<FreeVar, Integer>((FreeVar)function,i));
	        }
	      } else {
	        result &= t.selectUnusablePositions(bound, unusable);
	      }
	    }
	  } else {
	    for (Term t : arguments) {
	      result &= t.selectUnusablePositions(bound, unusable);
	    }
	  }
	  return result;
  }

  public int hashCode() { return function.hashCode() + arguments.hashCode(); }

	@Override
	public boolean typeEquals(Term otherType) {
		if (super.typeEquals(otherType))
			return true;
		if (!(otherType instanceof Application))
			return false;
		Application a = (Application) otherType;
		if (arguments.size() != a.arguments.size())
			return false;
		for (int i = 0; i < arguments.size(); ++i) {
			if (!arguments.get(i).typeEquals(a.arguments.get(i)))
				return false;
		}
		return function.typeEquals(a.function);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Application)) return false;
		if (obj.getClass() != this.getClass()) return false;
		Application a = (Application) obj;
		return function.equals(a.function) && arguments.equals(a.arguments);
	}

	public String toString() {
		return "(" + function + " " + arguments + ")";
	}
	public Term getType(List<Pair<String, Term>> varBindings) {
		Term funType = function.getType(varBindings);
		for (Term t : arguments) {
			if (!(funType instanceof Abstraction))
				verify(false, "applied " + arguments.size() + " arguments to function " + function + " of type " + function.getType(varBindings));
			Abstraction funTypeAbs = (Abstraction) funType;
			Term argType = t.getType(varBindings);
			Term absType = funTypeAbs.varType;
			verify(argType.typeEquals(absType) || function.toString().contains("TERM"), "types do not match when applying " + argType + " to arg type "+ absType + " in function " + function);
			funType = funTypeAbs.getBody();
		}
		return funType;
	}
	
	/** Produces a substitution that will bind an outer bound variable in all free variables.
	 * Here we just recurse to the function and arguments.
	 */
	@Override
	public void bindInFreeVars(Term typeTerm, Substitution sub) {
		function.bindInFreeVars(typeTerm, sub);
		for (Term a : arguments) {
			a.bindInFreeVars(typeTerm, sub);
		}
	}

	@Override
	public void bindInFreeVars(Term typeTerm, Substitution sub, int i) {
		function.bindInFreeVars(typeTerm, sub, i);
		for (Term a : arguments) {
			a.bindInFreeVars(typeTerm, sub, i);
		}
	}

	@Override
	public void bindInFreeVars(List<Term> typeTerms, Substitution sub, int idx) {
		function.bindInFreeVars(typeTerms, sub, idx);
		for (Term a : arguments) {
			a.bindInFreeVars(typeTerms, sub, idx);
		}
	}

	/** Binds the ith bound variable in all free variables.
	 * Modifies the substitution to reflect changes.
	 * We just recurse down into the function and arguments
	 */
	@Override
	@Deprecated
	public Term oldBindInFreeVars(int i, Term typeTerm, Substitution sub) {
		boolean isNew = false;
		Term newFunction = function.oldBindInFreeVars(i, typeTerm, sub);
		if (newFunction != function)
			isNew = true;

		List<Term> newArgs = new ArrayList<Term>();
		for (Term a : arguments) {
			Term t = a.oldBindInFreeVars(i, typeTerm, sub);
			newArgs.add(t);
			if (t != a)
				isNew = true;
		}

		if (isNew) {
			return newFunction.apply(newArgs, 0);
		} else {
			return this;
		}
	}

	/** Attempts to remove all bound variables above index i and above from the expression.
	 * If this is impossible a UnificationFailedException is thrown.
	 * We recurse down into the function and arguments.
	 * If the function is a free variable, we try to perform elimination as necessary.
	 */
	@Override
	@Deprecated
	public Term removeBoundVarsAbove(int i) {
		boolean isNew = false;
		Atom theFunction = function;
		List<? extends Term> theArguments = arguments;
		
		// is the function a free variable?  If so, let's try to do elimination of unwanted arguments
		if (theFunction instanceof FreeVar) {
			// get argument types for function
			List<Term> argTypes = getArgTypes(theFunction.getType());
			
			// build up a new set of arguments and argument types
			List<Term> newArgs = new ArrayList<Term>();
			List<Term> newArgTypes = new ArrayList<Term>();
			for (int j = 0; j < theArguments.size(); ++j) {
				Term a = theArguments.get(j);
				if (!(a instanceof BoundVar && ((BoundVar)a).getIndex() > i)) {
					// keep this argument
					newArgs.add(a);
					newArgTypes.add(argTypes.get(j));
				}
			}
			
			// if we replaced anything, update theFunction and theArguments
			if (newArgs.size() != theArguments.size()) {
				// TODO: replace getBaseType() with this.getType()
				Term newType = wrapWithLambdas(((FreeVar)theFunction).getBaseType(), newArgTypes);
				theFunction = Facade.FreshVar(((FreeVar)theFunction).getName(), newType);
				theArguments = newArgs;
				isNew = true;
			}
		}
		
		// now, recurse down into the (possibly new) function and arguments.
		Term newFunction = theFunction.removeBoundVarsAbove(i);
		if (newFunction != theFunction)
			isNew = true;

		List<Term> newArgs = new ArrayList<Term>();
		for (Term a : theArguments) {
			Term t = a.removeBoundVarsAbove(i);
			newArgs.add(t);
			if (t != a)
				isNew = true;
		}

		if (isNew) {
			return newFunction.apply(newArgs, 0);
		} else {
			return this;
		}
	}

	/** Attempts to remove all bound variables above index i and above from the expression.
	 * If this is impossible a UnificationFailedException is thrown.
	 * We recurse down into the function and arguments.
	 * If the function is a free variable, we try to perform elimination as necessary.
	 */
	@Override
	public void removeBoundVarsAbove(int i, Substitution sub) {
		Atom theFunction = function;
		List<? extends Term> theArguments = arguments;
		
		// is the function a free variable?  If so, let's try to do elimination of unwanted arguments
		if (theFunction instanceof FreeVar) {
			// have we already substituted?  If so, substitute and continue
			if (sub.getSubstituted(theFunction) != null) {
				sub.getSubstituted(theFunction).apply(arguments, 0).removeBoundVarsAbove(i, sub);
				return;
			}
			
			// get argument types for function
			List<Term> argTypes = getArgTypes(theFunction.getType());
			
			// build up a new set of arguments and argument types
			List<Term> newArgs = new ArrayList<Term>();
			List<Term> newArgTypes = new ArrayList<Term>();
			for (int j = 0; j < theArguments.size(); ++j) {
				Term a = theArguments.get(j);
				if (!(a instanceof BoundVar && ((BoundVar)a).getIndex() > i)) {
					// keep this argument
					newArgs.add(a);
					newArgTypes.add(argTypes.get(j));
				}
			}
			
			// if we replaced anything, update theFunction and theArguments, and create a new substitution
			if (newArgs.size() != theArguments.size()) {
				Term newType = wrapWithLambdas(((FreeVar)theFunction).getBaseType(), newArgTypes);
				theFunction = Facade.FreshVar(((FreeVar)theFunction).getName(), newType);
				theArguments = newArgs;
				
				Term newTerm = theFunction.apply(theArguments, 0);
				newTerm = Term.wrapWithLambdas(newTerm, argTypes);
				sub.add(function, newTerm);
			}
		}
		
		// now, recurse down into the (possibly new) function and arguments.
		theFunction.removeBoundVarsAbove(i, sub);

		for (Term a : theArguments) {
			a.removeBoundVarsAbove(i, sub);
		}
	}

	public boolean isFullyAppliedFreeVar() {
		if (function instanceof FreeVar) {
			int numTypeLambdas = function.getType().countLambdas();
			return arguments.size() == numTypeLambdas;
		} else {
			return false;
		}
	}

  @Override
  public boolean containsProper(Term other) {
    if (function.contains(other)) return true;
    for (Term arg : arguments) {
      if (arg.contains(other)) return true;
    }
    return false;
  }
	
	
}
