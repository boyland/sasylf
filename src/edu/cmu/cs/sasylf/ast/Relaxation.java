package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Util.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Util;

/**
 * A class describing how one context can be relaxed into another.
 * Relaxation is form of weakening in which a member of the LF
 * context which has been realized is pushed back into the background.
 * <p>
 * For example, after determining that the LF context has the assumption
 * <tt>x:T</tt> where <tt>x</tt> is what the variable <tt>t</tt> is,
 * then we can relax the context <tt>Gamma', x:T</tt> to <tt>Gamma</tt>,
 * and the term <tt>Gamma', x:T |- x + 3 : Nat</tt> to <tt>Gamma |- t + 3 : Nat</tt>.
 */
public class Relaxation {

	private final List<Term>     types;
	private final List<FreeVar>  values;
	private final NonTerminal    result;

	public Relaxation(List<Abstraction> abs, List<FreeVar> ts, NonTerminal r) {
		types = new ArrayList<Term>();
		for (Abstraction a : abs) {
			types.add(a.getArgType());
		}
		values = new ArrayList<FreeVar>(ts);
		while (values.size() < types.size()) {
			values.add(null);
		}
		result = r;
		Util.verify(values.size() == types.size(), "inconsistent size of relaxation");
	}

	private Relaxation(List<Term> ts, List<FreeVar> vals, NonTerminal r, boolean ignored) {
		Util.verify(vals.size() == ts.size(), "inconsistent size of relaxation");
		types = ts;
		values = vals;
		result = r;
	}

	public Relaxation(List<Pair<String,Term>> ps, FreeVar val, NonTerminal r) {
		types = new ArrayList<Term>(ps.size());
		values = new ArrayList<FreeVar>(ps.size());
		result = r;
		for (Pair<String,Term> p : ps) {
			types.add(p.second);
			if (values.isEmpty()) values.add(val);
			else values.add(null);
		}
	}

	public NonTerminal getResult() {
		return result;
	}
	
	/**
	 * Relax a term using this relaxation, 
	 * or return null if relaxation doesn't apply.
	 * @param t  term to relax
	 * @param res new context to relax to
	 * @return relaxed term, or null
	 */
	public Term relax(Term t) {
		int n = types.size();
		List<Term> tempTypes = new ArrayList<Term>(types);
		for (int i=0; i < n; ++i) {
			Term ty = tempTypes.get(i);
			if (!(t instanceof Abstraction)) {
				Util.debug("cannot relax because after ",i," terms, term to relax is ",t);
				return null;
			}
			Abstraction a = (Abstraction)t;
			if (!(ty.equals(a.getArgType()))) {
				Util.debug("cannot relax because type ", a.getArgType(), " != ", ty);
				return null;
			}
			Term val = values.get(i);
			if (val == null) {
				t = a.getBody();
				if (t.hasBoundVar(1)) {
					Util.debug("cannot relax because no replacement for ",a.varName," is available");
				}
				t = t.incrFreeDeBruijn(-1);
			} else {
				t = Facade.App(a,val);
				List<Term> tempList = new ArrayList<Term>();
				tempList.add(val);
				for (int j=i+1; j < n; ++j) {
					Term oldType = tempTypes.get(j);
					Term newType = oldType.apply(tempList, j-i).incrFreeDeBruijn(-1);
					Util.debug("relax type ",oldType," replaced with ",newType);
					tempTypes.set(j, newType);
				}
			}
		}
		return t;
	}

	/**
	 * Wrap a term in binders to reflect the relaxation.
	 * This is the opposite of the {@link #relax(Term)} method.
	 * This method is only reasonable (we are *undoing* relaxation)
	 * if the term is the bare relation, otherwise, some bindings may be lost.
	 * @param t term to wrap and replace variables
	 * @return abstraction representing term adapted to the relaxation context.
	 */
	public Term adapt(Term t) {
		int n = types.size();
		List<Term> tempTypes = new ArrayList<Term>(types);
		List<Abstraction> abs = new ArrayList<>();
		Term.getWrappingAbstractions(t, abs);
		if (!abs.isEmpty()) {
			Substitution adaptSub = new Substitution();
			for (Abstraction a : abs) {
				a.getArgType().bindInFreeVars(tempTypes, adaptSub);
			}
			t = t.substitute(adaptSub);
			Util.debug("adapting wrappers in relaxation ", t);
		}
		for (int i=n-1; i >= 0; --i) {
			Term ty = tempTypes.get(i);
			FreeVar val = values.get(i);
			if (val == null) {
				t = Facade.Abs(ty, t);
			} else {
				t = Facade.Abs(val, ty, t); 
			}
		}
		return t;
	}

	public Relaxation substitute(Substitution sub) {
		boolean changed = false;
		List<Term> newTypes = null;
		List<FreeVar> newValues = null;
		int n = types.size();
		for (int i=0; i < n; ++i) {
			Term oldType = types.get(i);
			FreeVar oldValue = values.get(i);
			Term newType = oldType.substitute(sub);
			FreeVar newValue = oldValue == null ? null : (FreeVar)oldValue.substitute(sub);
			boolean needChange = oldType != newType || oldValue != newValue;
			if (needChange && !changed) {
				newTypes = new ArrayList<Term>(types.subList(0, i));
				newValues = new ArrayList<FreeVar>(values.subList(0, i));
				changed = true;
			}
			if (changed) {
				newTypes.add(newType);
				newValues.add(newValue);
			}
		}
		if (changed) {
			return new Relaxation(newTypes,newValues,result,false);
		}
		return this;
	}

	/**
	 * Get the values for this relaxation.
	 * @return
	 */
	public List<FreeVar> getValues() {
		return Collections.unmodifiableList(values);
	}
	
	public Set<FreeVar> getRelaxationVars() {
		Set<FreeVar> result = new HashSet<FreeVar>(values);
		result.remove(null);
		return result;
	}

	public List<Term> getTypes() {
		return types;
	}

	public void getFreeVars(Set<FreeVar> container) {
		for (FreeVar val : values) {
			if (val != null) container.add(val);
		}
		for (Term ty : types) {
			container.addAll(ty.getFreeVariables());
		}
	}

	public String checkConsistent(Context ctx) {
		for (FreeVar val : values) {
			if (val == null) continue;
			Util.debug("ctx.inputVars = ",ctx.inputVars);
			Util.debug("ctx.currentSub = ", ctx.currentSub);
			if (!ctx.inputVars.contains(val)) return "value not free: " + val;
		}
		for (Term ty : types) {
			if (!ctx.inputVars.containsAll(ty.getFreeVariables())) {
				return "non-input free vars in " + ty;
			}
		}
		return null;
	}

	
	@Override
	public int hashCode() {
		return Objects.hash(this.types,this.values,this.result);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Relaxation)) return false;
		Relaxation other = (Relaxation)obj;
		return this.types.equals(other.types) &&
				this.values.equals(other.values) &&
				this.result.equals(other.result);
	}

	@Override
	public String toString() {
		return "Relaxation(" + types + "," + values + "," + result + ")";
	}

	/**
	 * Compute a relaxation for the pattern matching of the assumption rule
	 * @param ctx context: for error messages.  The current substitution may also be updated.
	 * @param subject the subject term matching the assumption conclusion.  
	 * 	It may be null if the pattern is guaranteed to be valid.
	 * @param subjectRoot The context for the subject term
	 * @param subjectTerm The term for the subject
	 * @param patternRoot The context for the pattern.  It may be null if it is unknown, unspecified.
	 * @param patternTerm The term for the pattern
	 * @param theRule The assumption rule
	 * @param theNode The node for which to report errors
	 * @return a relaxation, never null.  It may be a reuse of an existing relaxation
	 * or may be a new relaxation, that is not installed in the map.
	 */
	public static Relaxation computeRelaxation(Context ctx,
			ClauseUse subject, 
			NonTerminal subjectRoot, Term subjectTerm, 
			NonTerminal patternRoot, Term patternTerm,
			Rule theRule, Node theNode) {
		int diff = patternTerm.countLambdas() - subjectTerm.countLambdas();
		verify(theRule.isAssumptionSize() == diff, "pattern is invalid");
		// we need to make sure the subject pattern has a simple variable where
		// we are going to have a variable because we need this for the relaxation.
		List<FreeVar> relaxVars = new ArrayList<FreeVar>();
		{
			Application bareSubject = (Application)Term.getWrappingAbstractions(subjectTerm, null);
			ClauseUse ruleConc = (ClauseUse)theRule.getConclusion();
			int j=0;
			int n = ruleConc.getElements().size();
			int ai = ((ClauseDef)theRule.getJudgment().getForm()).getAssumeIndex();
			Map<Variable,FreeVar> relaxMap = new HashMap<>();
			for (int i=0; i < n; ++i) {
				if (i == ai) continue;
				Element e = ruleConc.getElements().get(i);
				if (e instanceof Variable) {
					Term t = bareSubject.getArguments().get(j);
					FreeVar relaxVar = relaxMap.get(e);
					if (t instanceof FreeVar) {
						if (relaxVar == null) {
							relaxVars.add((FreeVar)t);
							relaxMap.put((Variable)e, (FreeVar)t);
						} else {
							Substitution canonSub = new Substitution();
							canonSub.add((FreeVar)t, relaxVar);
							Util.debug("Found canonSub = ",canonSub);
							ctx.composeSub(canonSub);
						}
					} else if (!(t instanceof Application) || !(((Application)t).getFunction() instanceof FreeVar)) {
						verify(subject != null, "didn't anticipate pattern error");
						ErrorHandler.error(Errors.CASE_ASSUMPTION_IMPOSSIBLE, subject.getElements().get(i).toString(), theNode);
					} else {
						// Why create a new variable?  We need to because the old one had parameters, the new one not.
						Application app = (Application)t;
						FreeVar funcVar = (FreeVar)app.getFunction();
						List<Abstraction> argTypes = new ArrayList<Abstraction>();
						Constant baseType = (Constant)Term.getWrappingAbstractions(funcVar.getType(), argTypes);
						if (relaxVar == null) {
							relaxVar = FreeVar.fresh(baseType.toString(),baseType);
							relaxVars.add(relaxVar);
							relaxMap.put((Variable)e, relaxVar);
						}
						Substitution canonSub = new Substitution();
						canonSub.add(funcVar, Term.wrapWithLambdas(argTypes, relaxVar));
						Util.debug("Found canonSub = ",canonSub);
						// bareSubject = (Application) bareSubject.substitute(canonSub);
						// subjectTerm = subjectTerm.substitute(canonSub);
						ctx.composeSub(canonSub);
					}
					++j;
				} else if (e instanceof NonTerminal || e instanceof Binding) {
					++j;
				}
			}
			relaxVars.add(null); // for the assumption itself
		}
		
		Relaxation relax = null;
		if (ctx.relaxationMap != null) { // try to find an existing relaxation
			for (Map.Entry<NonTerminal, Relaxation> e : ctx.relaxationMap.entrySet()) {
				Relaxation r = e.getValue();
				if (r.getResult().equals(subjectRoot) &&
						r.getValues().equals(relaxVars)) {
					if (patternRoot == null || e.getKey().equals(patternRoot)) {
						relax = r;
					} else {
						ErrorHandler.warning(Errors.CASE_CONTEXT_MAYBE_KNOWN, "" + e.getKey(), theNode);
					}
					break;
				}
			}
		}
		
		if (relax == null) {
			if (patternRoot != null && ctx.isKnownContext(patternRoot)) {
				ErrorHandler.error(Errors.REUSED_CONTEXT, patternRoot.toString(), theNode);
			}
			List<Abstraction> newWrappers = new ArrayList<Abstraction>();
			Term.getWrappingAbstractions(patternTerm, newWrappers, diff);
			// Util.debug("Introducing ",patternRoot,"+",Term.wrappingAbstractionsToString(newWrappers));
			relax = new Relaxation(newWrappers,relaxVars,subjectRoot);
			// System.out.println("Relaxation is " + relax);
		}
		
		// We don't add the relaxation to the context
		return relax;
	}
}
