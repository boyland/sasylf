package edu.cmu.cs.sasylf.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.ast.grammar.GrmRule;
import edu.cmu.cs.sasylf.ast.grammar.GrmUtil;
import edu.cmu.cs.sasylf.grammar.Grammar;
import edu.cmu.cs.sasylf.module.Module;
import edu.cmu.cs.sasylf.module.ModuleFinder;
import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Atom;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Util;


/** Represents a typing context */
public class Context implements Cloneable {
	// TODO: Create instances of this class rather than mutating it.

	/// The following fields represent global information

	public final ModuleFinder moduleFinder;
	public final CompUnit compUnit;
	public Set<String> termSet = new HashSet<String>();
	private Map<String,SyntaxDeclaration> synMap = new HashMap<String,SyntaxDeclaration>();
	private Map<String,SyntaxDeclaration> synTypeMap = new HashMap<String,SyntaxDeclaration>();
	private Map<String,Judgment> judgMap = new HashMap<String,Judgment>();
	private Map<String,Judgment> judgLFMap = new HashMap<String,Judgment>();
	private Map<String,ClauseDef> prodMap = new HashMap<String,ClauseDef>();
	public Map<String,Variable> varMap = new HashMap<String, Variable>();
	public Map<String,RuleLike> ruleMap = new HashMap<String, RuleLike>();
	public Map<String,Module> modMap = new HashMap<String, Module>();
	public Map<List<ElemType>,ClauseDef> parseMap = new HashMap<List<ElemType>,ClauseDef>();
	public List<GrmRule> ruleSet = new ArrayList<GrmRule>();
	private static int version; // incremented to indicate that caches should be abandoned.
	
	/// The remainder fields represent contextual (local) information

	public Map<String, Theorem> recursiveTheorems; // only changes with theorems

	public Map<String,Fact> derivationMap = new HashMap<String, Fact>();
	public Map<String, List<ElemType>> bindingTypes;
	public Substitution currentSub = new Substitution();
	public Theorem currentTheorem;
	public Term currentCaseAnalysis;
	public Term currentGoal;
	public Clause currentGoalClause;
	public NonTerminal assumedContext;
	public Set<NonTerminal> knownContexts;
	public Element currentCaseAnalysisElement;
	public Set<FreeVar> inputVars;
	public Set<FreeVar> outputVars;
	public Map<Fact,Pair<Fact,Integer>> subderivations = new HashMap<Fact,Pair<Fact,Integer>>();
	public Map<CanBeCase, Set<Pair<Term, Substitution>>> caseTermMap; // entries mutable
	public Map<String,Map<CanBeCase, Set<Pair<Term,Substitution>>>> savedCaseMap; // entries immutable
	HashMap<String,NonTerminal> varFreeNTmap= new HashMap<String,NonTerminal>(); 
	HashMap<NonTerminal,Relaxation> relaxationMap;
	public Set<FreeVar> relaxationVars;

	public Context(ModuleFinder mf, CompUnit cu) {
		moduleFinder = mf;
		compUnit = cu;
	}
	
	/**
	 * Gives the current version of this context. (NEW)
	 * @return the current version
	 */
	public int version() {
		return version;
	}
 	
	/**
	 * Updates if a change has happened, thus incrementing
	 * the version.
	 */
	public static void updateVersion() {
		++version;
	}

	/** Return a copy of this context
	 * which new copies of everything local and mutable
	 */
	@Override
	public Context clone() {
		Context result;
		try {
			result = (Context)super.clone();
		} catch (CloneNotSupportedException ex) {
			return null;
		}
		if (derivationMap != null) result.derivationMap = new HashMap<String,Fact>(derivationMap);
		if (bindingTypes != null) result.bindingTypes = new HashMap<String, List<ElemType>>(bindingTypes);
		result.currentSub = new Substitution(currentSub);
		if (inputVars != null) result.inputVars = new HashSet<FreeVar>(inputVars);
		if (outputVars != null) result.outputVars = new HashSet<FreeVar>(outputVars);
		result.subderivations = new HashMap<Fact,Pair<Fact,Integer>>(subderivations);
		if (result.caseTermMap != null) result.caseTermMap = new HashMap<CanBeCase, Set<Pair<Term, Substitution>>>(caseTermMap);
		if (result.savedCaseMap != null) result.savedCaseMap = new HashMap<String,Map<CanBeCase, Set<Pair<Term,Substitution>>>>(savedCaseMap);
		result.varFreeNTmap = new HashMap<String,NonTerminal>(varFreeNTmap);
		if (knownContexts != null) result.knownContexts = new HashSet<NonTerminal>(knownContexts);
		if (relaxationMap != null) result.relaxationMap = new HashMap<NonTerminal,Relaxation>(relaxationMap);
		if (relaxationVars != null) result.relaxationVars = new HashSet<FreeVar>(relaxationVars);
		return result;
	}

	/**
	 * get the representation of this element as an LF term,
	 * substituting with the current substitution.
	 * @param e element to convert.  Must have been type-checked already
	 * @return LF term for this element
	 */
	public Term toTerm(Element e) {
		return e.asTerm().substitute(currentSub);
	}

	/**
	 * Set the syntax declaration associated with nonterminals using this name.
	 * @param name name to bind, should not be null
	 * @param sd syntax declaration, must not be null
	 */
	public void setSyntax(String name, SyntaxDeclaration sd) {
		if (sd == null) throw new NullPointerException("Need a valid syntax for " + name);
		synMap.put(name, sd);
		synTypeMap.put(sd.typeTerm().getName(), sd);
	}
	
	/**
	 * Get the syntax declaration associated with a nonterminal with this name.
	 * @param name name to check
	 * @return syntax declaration, or null if none known for this name
	 */
	public SyntaxDeclaration getSyntax(String name) {
		return synMap.get(name);
	}
	
	/**
	 * Get the user-visible syntax declaration associated with an LF type
	 * @param ty LF type, must not be null
	 * @return syntax declaration, or null is this LF type is not known in this syntax
	 */
	public SyntaxDeclaration getSyntax(Term ty) {
		return synTypeMap.get(ty.baseTypeFamily().getName()); 
	}
	
	/**
	 * Set the judgment for this name.
	 */
	public void setJudgment(String name, Judgment j) {
		if (j == null) throw new NullPointerException("Need a valid judgment for " + name);
		Judgment jp = judgMap.put(name, j);
		if (jp != null && jp != j) {
			ErrorHandler.recoverableError(Errors.DUPLICATE_JUDGMENT, j);
		}
		judgLFMap.put(j.typeTerm().getName(), j);
	}
	
	/**
	 * Get the judgment associated with this name in the context.
	 * @param name name to look up
	 * @return judgment mapped.
	 */
	public Judgment getJudgment(String name) {
		return judgMap.get(name);
	}
	
	/**
	 * Get the judgment associated with this LF constant.
	 * @param con constant to look up
	 * @return judgment in the context with this LF name
	 */
	public Judgment getJudgment(Atom con) {
		return judgLFMap.get(con.getName());
	}
	
	/**
	 * Register a production for an LF constant
	 * @param name name of LF constant
	 * @param prod defining clause for this production
	 */
	public void setProduction(String name, ClauseDef cd) {
		prodMap.put(name, cd);
	}
	
	/**
	 * Get the production associated with this constant
	 * @param con constant to look up
	 */
	public ClauseDef getProduction(Constant con) {
		return prodMap.get(con.getName());
	}
	
	/**
	 * Adapt the given term to 
	 * @param t
	 * @param abs
	 * @param doWrap
	 * @return
	 */
	public Term adapt(Term t, List<Abstraction> abs, boolean doWrap) {
		List<Term> types = new ArrayList<Term>();
		for (Abstraction a : abs) {
			types.add(a.varType);
		}
		Substitution adaptSub = new Substitution();
		t.bindInFreeVars(types, adaptSub);
		for (NonTerminal nt : varFreeNTmap.values()) {
			adaptSub.remove((FreeVar)nt.asTerm());
		}
		t = t.substitute(adaptSub);
		if (doWrap) t = Term.wrapWithLambdas(abs, t);
		return t;
	}

	public boolean isVarFree(NonTerminal nt) {
		return varFreeNTmap.get(nt.getSymbol()) != null;
	}

	/**
	 * Return true if the element could never include <i>implicit</i> references to
	 * variables in a named context.
	 * @param e element to examine
	 * @return true if the element could never depend on a variable
	 * hidden inside a named context.
	 */
	public boolean isVarFree(Element e) {
		if (e instanceof Terminal) return true;
		if (e instanceof NonTerminal) return varFreeNTmap.get(e.toString()) != null;
		else if (e instanceof Variable) return true; // method is asking about variable from context nonterminal
		else if (e instanceof Binding) {
			Binding b = (Binding)e;
			if (!isVarFree(b.getNonTerminal())) return false;
			for (Element a : b.getElements()) {
				if (!isVarFree(a)) return false;
			}
			return true;
		} else if (e instanceof Clause) {
			for (Element a : ((Clause)e).getElements()) {
				if (!isVarFree(a)) return false;
			}
			return true;
		} else if (e instanceof AssumptionElement) {
			AssumptionElement ae = (AssumptionElement)e;
			return ae.getRoot() == null;
		} else {
			throw new RuntimeException("Internal error: what sort of elemnt is this ? " + e);
		}
	}

	/**
	 * Return true if the given term can be reached as a subterm of one
	 * of the "var free NTs" in this context.
	 * @param t term to check
	 * @return whether it is a (possibly improper) subterm of a previously
	 * recognized "var free NT".
	 */
	public boolean isVarFree(Term t) {
		if (assumedContext == null) {
			throw new RuntimeException("Internal error: isVarFree doesn't make sense with no context");
		}
		if (t instanceof FreeVar) {
			return varFreeNTmap.get(t.toString()) != null;
		} else {
			for (FreeVar fv : t.getFreeVariables()) {
				if (varFreeNTmap.get(fv.toString()) == null) return false; 
			}
			return true;
		}
	}

	/**
	 * Return true if the given string has some sort of mapping out there.
	 * Such a name is not suitable for a fresh variable.
	 * @param s string to look up, must not be null
	 * @return whether the context is aware of anything with this name
	 */
	public boolean isKnown(String s) {
		return synMap.containsKey(s) ||
				judgMap.containsKey(s) ||
				prodMap.containsKey(s) ||
				varMap.containsKey(s) ||
				ruleMap.containsKey(s) ||
				recursiveTheorems.containsKey(s) ||
				isLocallyKnown(s);
	}

	/**
	 * Is this identifier known locally as a nonterminal or derivation?
	 * Similar to {@link #isKnown(String)} but doesn't include global tables.
	 * @param s string to look up
	 * @return boolean if something is around using this name already.
	 */
	public boolean isLocallyKnown(String s) {
		FreeVar fake = new FreeVar(s,null);
		return derivationMap.containsKey(s) ||
				// bindingTypes.containsKey(s) ||
				inputVars.contains(fake) ||
				// outputVars.contains(fake) ||
				currentSub.getMap().containsKey(fake) ||
				relaxationMap != null && relaxationMap.containsKey(new NonTerminal(s,null)) ||
				isTerminalString(s); // terminals are pervasive
	}
	
	public boolean isKnownVar(FreeVar fv) {
		return inputVars.contains(fv) || 
				outputVars.contains(fv) ||
				currentSub.getMap().containsKey(fv);
	}

	/**
	 * Return true if the given string is a declared terminal string
	 * @param s string to check
	 * @return true if the string is a declared terminal
	 */
	public boolean isTerminalString(String s) {
		return termSet.contains(s);
	}

	/**
	 * Generate an identifier with the given prefix.
	 * The result will not be known {@link #isKnown(String)}.
	 * @param prefix string to start names with, must not be null
	 * @return fresh identifier
	 */
	public String genFresh(String prefix) {
		for (int i=0; true; ++i) {
			String s = prefix + i;
			if (!isKnown(s)) return s;
		}
	}

	/**
	 * See if there is an existing relaxation and return the key if so
	 * @param r relaxation to look for
	 * @return key that is defined by this relaxation
	 */
	public NonTerminal findRelaxation(Relaxation r) {
		if (relaxationMap == null) return null;
		for (Map.Entry<NonTerminal, Relaxation> e : relaxationMap.entrySet()) {
			if (e.getValue().equals(r)) return e.getKey();
		}
		return null;
	}
	
	public void addRelaxation(NonTerminal key, Relaxation relax) {
		Util.debug("Adding relaxation: ",key,"->",relax);
		if (relaxationMap == null) {
			relaxationMap = new HashMap<NonTerminal,Relaxation>();
			relaxationVars = new HashSet<FreeVar>();
		}
		if (relaxationMap.put(key, relax) == relax) return; // NOP
		Set<FreeVar> newVars = relax.getRelaxationVars();
		relaxationVars.addAll(newVars);
		relax.getFreeVars(inputVars); //XXX: What is this doing?  It doesn't do anything?!?
		Util.debug("ctx.relaxationVars = ", relaxationVars);
	}

	public void checkConsistent(Node here) {
		boolean problem = false;
		for (FreeVar fv : inputVars) {
			if (currentSub.getSubstituted(fv) != null) {
				System.out.println("Internal error: input var " + fv + " has binding to " + currentSub.getSubstituted(fv));
				problem = true;
			}
		}
		for (Map.Entry<FreeVar,Term> e : currentSub.getMap().entrySet()) {
			if (e.getValue().substitute(currentSub) != e.getValue()) {
				System.out.println("Internal error: currentSub is not idempotent for " + e.getValue());
				System.out.println("  " + e.getValue().substitute(currentSub));
				problem = true;
			}
		}

		for (Map.Entry<FreeVar,Term> e : currentSub.getMap().entrySet()) {
			Atom key = e.getKey();
			if (!(key instanceof FreeVar)) {
				System.out.println("Internal error: binding a constant " + key + " to " + e.getValue());
				problem = true;
			}
		}

		for (Map.Entry<FreeVar,Term> e : currentSub.getMap().entrySet()) {
			Set<FreeVar> free = e.getValue().getFreeVariables();
			if (!inputVars.containsAll(free)) {
				free.removeAll(inputVars);
				System.out.println("Internal error: missing input vars: " + free);
				new Throwable("for the trace").printStackTrace();
				problem = true;
			}
		}

		for (Fact d : derivationMap.values()) {
			Set<FreeVar> free = d.getElement().asTerm().getFreeVariables();
			free.removeAll(inputVars);
			free.removeAll(outputVars);
			free.removeAll(currentSub.getMap().keySet());
			if (!free.isEmpty()) {
				System.out.println("Internal error: missing input vars in " + d.getName() + ": " + free);
				problem = true;
			}
		}

		if (relaxationMap != null) {
			if (relaxationVars == null) {
				System.out.println("Internal error: relaxtionvars is null");
				problem = true;
			} else {
				Set<FreeVar> check = new HashSet<FreeVar>();
				for (Map.Entry<NonTerminal,Relaxation> e : relaxationMap.entrySet()) {
					if (e.getKey() == null) {
						System.out.println("Internal error: null key in relaxation map");
						problem = true;
					} else {
						Relaxation r = e.getValue();
						String s = r.checkConsistent(this);
						if (s != null) {
							System.out.println("Internal error: " + s);
							problem = true;
						}
						check.addAll(r.getRelaxationVars());
					}
				}
				if (!check.equals(relaxationVars)) {
					System.out.println("relaxation vars out of date: " + relaxationVars);
					problem = true;
				}
			}
		}
		if (problem) {
			ErrorHandler.error(Errors.INTERNAL_ERROR,": inconsistent context: ",here,currentSub.toString());
		} 
		removeUnreachableVariables();
	}

	/**
	 * Check if the substitution passed in is compatible with the 
	 * current relaxation.
	 * <p>
	 * It's not clear what to do if the argument is illegal; should
	 * we use the (new) {@link Substitution#canCompose(Substitution)} method
	 * to check for this?  If so, then an internal error may silently be ignored
	 * possibly being converted into unsoundness.
	 * For now {@link DerivationByAnalysis} does the other check as well.
	 * @param sub substitution to check, must not be null
	 * @return true composition does not interfere with relaxation variables.
	 */
	public boolean canCompose(Substitution sub) {
		//if (!currentSub.canCompose(sub)) return false;
		// We have to reject a substitution that changes a relaxation variable to anything but another free variable.
		// An earlier version of this code permitted it to be substituted with a bound variable,
		// presumably its own variable, but:
		// (1) That caused an exception because the relaxation cannot handle the substitution, and
		// (2) The only rule that can cause the relaxation variable to be replaced
		//     by a variable is precisely the one that was already done to cause the relaxation
		// This change moves the bug in good28.slf from an internal error (exception throw)
		// to an unsound rejection of the matching.  It will be fixed by changing
		// case analysis to handle the special relation as a special case.
		if (relaxationVars != null) {
			for (FreeVar relax : relaxationVars) {
				Term subbed = sub.getSubstituted(relax);
				if (subbed == null) continue;
				Util.debug(relax," -> ",subbed);
				if (subbed instanceof FreeVar) continue;
				Util.debug("non-viable substition of ", relax, " with ",subbed);
				return false;
			}
		}
		return true;
	}

	public void composeSub(Substitution sub) {
		// System.out.println("ctx(" + currentSub + ").composeSub(" + sub + ")");
		//XXX: Set<FreeVar> unavoidableInputVars = sub.selectUnavoidable(inputVars);
		// System.out.println("unavoidable = " + unavoidableInputVars);
		inputVars.removeAll(sub.getDomain()); // (unavoidableInputVars);
		currentSub.compose(sub);  // modifies in place
		Set<FreeVar> newVars = new HashSet<FreeVar>();
		for (Map.Entry<FreeVar,Term> e : sub.getMap().entrySet()) {
			Set<FreeVar> freeVariables = e.getValue().getFreeVariables();
			newVars.addAll(freeVariables);
			if (varFreeNTmap != null && varFreeNTmap.containsKey(e.getKey().toString())) {
				addVarFree(freeVariables,varFreeNTmap.get(e.getKey().toString()).getLocation());
			}
		}
		if (relaxationMap != null) {
			relaxationVars.clear();
			for (Map.Entry<NonTerminal, Relaxation> e : relaxationMap.entrySet()) {
				Relaxation r = e.getValue();
				r = r.substitute(sub);
				relaxationVars.addAll(r.getRelaxationVars());
				e.setValue(r);
			}
		}
		// System.out.println("new vars = " + newVars);
		inputVars.addAll(newVars);
	}
	
	/**
	 * Change the current substitution (if possible) to avoid mapping
	 * the given variables.  If the substitution is changed, the other
	 * parts of the context are updated as well.
	 * @param vars variables to avoid mapping, updated to remove variables
	 * that could be avoided.
	 * @return substitution that handles the changes that this call performs.
	 */
	public Substitution avoidIfPossible(Set<FreeVar> vars) {
		// System.out.println("avoidIfPossible(" + vars + ")");
		// System.out.println("  current = " + currentSub);
		// System.out.println("  inpuVars = " + inputVars);
		Substitution original = new Substitution(currentSub);
		Set<FreeVar> requested = new HashSet<FreeVar>(vars);
		vars.clear();
		vars.addAll(currentSub.selectUnavoidable(requested));
		requested.removeAll(vars); // now has all the ones made input vars
		Substitution result = new Substitution(currentSub);
		result.removeAll(original.getDomain());
		inputVars.removeAll(result.getDomain());
		inputVars.addAll(requested);
		if (relaxationMap != null) {
			relaxationVars.clear();
			for (Map.Entry<NonTerminal, Relaxation> e : relaxationMap.entrySet()) {
				Relaxation r = e.getValue();
				r = r.substitute(result);
				relaxationVars.addAll(r.getRelaxationVars());
				e.setValue(r);
			}
		}	
		// System.out.println("  updated = " + currentSub);
		// System.out.println("  new inputVars = " + inputVars);
		// System.out.println("  not avoided = " + vars);
		// System.out.println("  result = " + result);
		return result;
	}

	public NonTerminal getCurrentCaseAnalysisRoot() {
		Term t = this.currentCaseAnalysis;
		// System.out.println("Finding root for " + t);
		//TODO: This doesn't work if the element is Gamma |- t : T2 -- not a freevar
		if (relaxationVars != null && relaxationVars.contains(t)) {
			// System.out.println("  A relaxation variable!");
			// System.out.println("Finding root for " + t);
			for (Map.Entry<NonTerminal,Relaxation> entry : this.relaxationMap.entrySet()) {
				if (entry.getValue().getRelaxationVars().contains(t)) return entry.getKey();
			}
			throw new InternalError("can't find relaxation context");
		}
		return this.currentCaseAnalysisElement.getRoot();
	}
	
	public void removeUnreachableVariables() {
		boolean changed = false;
		Substitution newSub = new Substitution();
		for (Map.Entry<FreeVar,Term> e : currentSub.getMap().entrySet()) {
			FreeVar key = e.getKey();
			if (key instanceof FreeVar) {
				FreeVar fv = key;
				if (fv.getStamp() != 0) {
					Util.debug("removing unreachable variable binding: ", fv, " = ", e.getValue());
					changed = true;
				} else {
					newSub.add(key, e.getValue());
				}
			}
		}
		if (changed) currentSub = newSub;
	}

	public void addVarFree(Set<FreeVar> vars, Location l) {
		for (FreeVar f : vars) {
			SyntaxDeclaration syn = getSyntax(f.getType());
			if (syn == null) {
				ErrorHandler.error(Errors.INTERNAL_ERROR, "no syntactic type for "+f, l);
			}
			varFreeNTmap.put(f.toString(),new NonTerminal(f.toString(),l,syn));
		}
	}

	public void addVarFree(Element e) {
		if (e instanceof NonTerminal) {
			varFreeNTmap.put(e.toString(),(NonTerminal) e);
		} else if (e instanceof Binding) {
			NonTerminal nt = ((Binding)e).getNonTerminal();
			varFreeNTmap.put(nt.getSymbol(), nt);
		} else if (e instanceof Clause) {
			for (Element e1: ((Clause)e).elements) {
				addVarFree(e1);
			}
		}
	}

	public void addKnownContext(NonTerminal root) {
		if (!isKnownContext(root)) {
			if (knownContexts == null) knownContexts = new HashSet<NonTerminal>();
			knownContexts.add(root);
		}
	}
	
	public boolean isKnownContext(NonTerminal root) {
		return root == null || 
				root.equals(assumedContext) || 
				knownContexts != null && knownContexts.contains(root) ||
				relaxationMap != null && relaxationMap.containsKey(root);
	}

	/**
	 * Return true if the argument is a relaxation variable for a variable already
	 * visible inside the scope.  If so, this means this variable cannot refer to
	 * anything outside the scope, that is, inside the given nonterminal.
	 * @param root context nonterminal
	 * @param fv relaxation variable (should be a member of ctx.relaxationVars)
	 * @return if the variable is bound already
	 */
	public boolean isRelaxationInScope(NonTerminal root, FreeVar fv) {
		Relaxation r;
		if (relaxationMap == null) return false;
		while ((r = relaxationMap.get(root)) != null) {
			if (r.getRelaxationVars().contains(fv)) return true;
			root = r.getResult();
		}
		return false;
	}
	
	/**
	 * Return true if the argument is a relaxation variable 
	 * and so cannot be matched with anything other than the bound variable already in scope.
	 * @param name name of nonterminal being checked
	 * @return if the variable is bound already
	 */
	public boolean isRelaxationVar(NonTerminal nt) {
		return relaxationVars != null && relaxationVars.contains(new FreeVar(nt.toString(),null));
	}
	
	/**
	 * Return true if the argument is a relaxation variable 
	 * and so cannot be matched with anything other than the bound variable already in scope.
	 * @param fv free variable being checked
	 * @return if the variable is bound already
	 */
	public boolean isRelaxationVar(FreeVar fv) {
		return relaxationVars != null && relaxationVars.contains(fv);
	}

	public List<Term> getRelaxationTypes(FreeVar relaxVar) {
		for (Relaxation r : relaxationMap.values()) {
			if (r.getRelaxationVars().contains(relaxVar)) {
				return r.getTypes();
			}
		}
		return null;
	}

	/**
	 * Return whether the first context is part of the second context
	 * @param n1 first context, may be null
	 * @param n2 second context, may be null
	 * @return whether weakening from n1 to n2 is legal.
	 */
	public boolean canRelaxTo(NonTerminal n1, NonTerminal n2) {
		if (n1 == null) return true;
		if (n2 == null) return false;
		if (n1.equals(n2)) return true;
		if (relaxationMap == null) return false;
		Relaxation r = relaxationMap.get(n1);
		if (r == null) return false;
		return canRelaxTo(r.getResult(),n2);
	}

	/**
	 * Return the context that binds any variables inside this free variable.
	 * Null means the variable is var free 
	 * (has no variables except those already handled by the binding parameters).
	 * @param fv variable to check, must not be null 
	 * @return context for this variable, or null if defined in the empty context.
	 */
	public NonTerminal getContext(FreeVar fv) {
		if (varFreeNTmap.containsKey(fv.toString())) return null;
		if (relaxationMap != null) {
			for (Map.Entry<NonTerminal, Relaxation> p : relaxationMap.entrySet()) {
				Relaxation r = p.getValue();
				if (r.getRelaxationVars().contains(fv)) return r.getResult();
				Set<FreeVar> container = new HashSet<FreeVar>();
				r.getFreeVars(container);
				if (container.contains(fv)) return p.getKey(); // the variable was used in this context.
			}
		}
		return assumedContext;
	}

	public NonTerminal getContext(NonTerminal nt) {
		return getContext(new FreeVar(nt.toString(),null));
	}

	/*public RuleNode parse(List<? extends Terminal> list) throws NotParseableException, AmbiguousSentenceException {
		if (g == null) {
			g = new edu.cmu.cs.sasylf.grammar.Grammar(GrmUtil.getStartSymbol(), ruleSet);
		}
		return g.parse(list);
	}*/

	public Grammar getGrammar() {
		if (g != null && ruleSize == ruleSet.size()) return g;
		// sometimes we increase the grammar
		g = new edu.cmu.cs.sasylf.grammar.Grammar(GrmUtil.getStartSymbol(), ruleSet);
		ruleSize = ruleSet.size();
		return g;
	}

	private Grammar g;
	int ruleSize = 0;
}
