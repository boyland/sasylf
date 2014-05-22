package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Util.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.ast.grammar.GrmRule;
import edu.cmu.cs.sasylf.ast.grammar.GrmUtil;
import edu.cmu.cs.sasylf.grammar.Grammar;
import edu.cmu.cs.sasylf.term.Atom;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Util;


/** Represents a typing context */
public class Context implements Cloneable {
  // TODO: Create instances of this class rather than mutating it.

  /// The following fields represent global information
  
  public final CompUnit compUnit;
  public Map<String,Syntax> synMap = new HashMap<String,Syntax>();
  public Map<String,Judgment> judgMap = new HashMap<String,Judgment>();
  public Map<String,ClauseDef> prodMap = new HashMap<String,ClauseDef>();
  public Map<String,Variable> varMap = new HashMap<String, Variable>();
  public Map<String,RuleLike> ruleMap = new HashMap<String, RuleLike>();
  public Map<List<ElemType>,ClauseDef> parseMap = new HashMap<List<ElemType>,ClauseDef>();
  public List<GrmRule> ruleSet = new ArrayList<GrmRule>();

  /// The remainder fields represent contextual (local) information

  public Map<String, Theorem> recursiveTheorems; // only changes with theorems
  
  public Map<String,Fact> derivationMap = new HashMap<String, Fact>();
  public Map<String, List<ElemType>> bindingTypes;
  public Substitution currentSub = new Substitution();
  public Theorem currentTheorem;
  public Term currentCaseAnalysis;
  public Term currentGoal;
  public Clause currentGoalClause;
  public NonTerminal innermostGamma;
  public Map<NonTerminal, AdaptationInfo> adaptationMap; // Gamma -> AdaptationInfo
  public NonTerminal adaptationRoot; // TODO: generalize next three into a map from adaptationRoot to the others
  public Term matchTermForAdaptation; // TODO: set these elements of the context when unwrapping Gamma in var rule
  //public int adaptationNumber = 0;
  public Element currentCaseAnalysisElement;
  public Set<FreeVar> inputVars;
  public Set<FreeVar> outputVars;
  public Map<Fact,Pair<Fact,Integer>> subderivations = new HashMap<Fact,Pair<Fact,Integer>>();
  public Map<CanBeCase, Set<Pair<Term, Substitution>>> caseTermMap; // entries mutable
  public Map<String,Map<CanBeCase, Set<Pair<Term,Substitution>>>> savedCaseMap; // entries immutable
  public Substitution adaptationSub;
  HashMap<String,NonTerminal> varFreeNTmap= new HashMap<String,NonTerminal>(); 

  public Context(CompUnit cu) {
    compUnit = cu;
  }
  
  /** Return a copy of this context
   * which new copies of everything local and mutable
   */
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
    if (adaptationMap != null) result.adaptationMap = new HashMap<NonTerminal, AdaptationInfo>(adaptationMap);
    if (inputVars != null) result.inputVars = new HashSet<FreeVar>(inputVars);
    if (outputVars != null) result.outputVars = new HashSet<FreeVar>(outputVars);
    result.subderivations = new HashMap<Fact,Pair<Fact,Integer>>(subderivations);
    if (result.caseTermMap != null) result.caseTermMap = new HashMap<CanBeCase, Set<Pair<Term, Substitution>>>(caseTermMap);
    if (result.savedCaseMap != null) result.savedCaseMap = new HashMap<String,Map<CanBeCase, Set<Pair<Term,Substitution>>>>(savedCaseMap);
    if (adaptationSub != null) result.adaptationSub = new Substitution(adaptationSub);
    result.varFreeNTmap = new HashMap<String,NonTerminal>(varFreeNTmap);

    return result;
  }
  
  public boolean isVarFree(NonTerminal nt) {
    return varFreeNTmap.get(nt.getSymbol()) != null;
  }
  
  public boolean isVarFree(Element e) {
    if (e instanceof Terminal) return true;
    if (e instanceof NonTerminal) return varFreeNTmap.get(e.toString()) != null;
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
    if (innermostGamma == null) {
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
      bindingTypes.containsKey(s) ||
      inputVars.contains(fake) ||
      outputVars.contains(fake) ||
      currentSub.getMap().containsKey(fake) ||
      adaptationMap.containsKey(new NonTerminal(s,null)) ||
      isTerminalString(s); // terminals are pervavise
  }
  
  public boolean isTerminalString(String s) {
    return compUnit.getDeclaredTerminals().contains(s);
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
  
  public void checkConsistent(Node here) {
    boolean problem = false;
    for (FreeVar fv : inputVars) {
      if (currentSub.getSubstituted(fv) != null) {
        System.out.println("Internal error: input var " + fv + " has binding to " + currentSub.getSubstituted(fv));
        problem = true;
      }
    }
    for (Map.Entry<Atom,Term> e : currentSub.getMap().entrySet()) {
      if (e.getValue().substitute(currentSub) != e.getValue()) {
        System.out.println("Internal error: currentSub is not idempotent for " + e.getValue());
        System.out.println("  " + e.getValue().substitute(currentSub));
        problem = true;
      }
    }
    
    for (Map.Entry<Atom,Term> e : currentSub.getMap().entrySet()) {
      Atom key = e.getKey();
      if (!(key instanceof FreeVar)) {
        System.out.println("Internal error: binding a constant " + key + " to " + e.getValue());
        problem = true;
      }
    }
    
    for (Map.Entry<Atom,Term> e : currentSub.getMap().entrySet()) {
      Set<FreeVar> free = e.getValue().getFreeVariables();
      if (adaptationSub != null) free.removeAll(adaptationSub.getMap().keySet());
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
      if (adaptationSub != null) free.removeAll(adaptationSub.getMap().keySet());
      if (!free.isEmpty()) {
        System.out.println("Internal error: missing input vars in " + d.getName() + ": " + free);
        problem = true;
      }
    }

    if (problem) {
      ErrorHandler.report("Internal error: inconsistent context: ",here,currentSub.toString());
    } 
    removeUnreachableVariables();
  }
  
  public void composeSub(Substitution sub) {
    // System.out.println("ctx(" + currentSub + ").composeSub(" + sub + ")");
    Set<FreeVar> unavoidableInputVars = sub.selectUnavoidable(inputVars);
    // System.out.println("unavoidable = " + unavoidableInputVars);
    inputVars.removeAll(unavoidableInputVars);
    currentSub.compose(sub);  // modifies in place
    Set<FreeVar> newVars = new HashSet<FreeVar>();
    for (Map.Entry<Atom,Term> e : sub.getMap().entrySet()) {
      Set<FreeVar> freeVariables = e.getValue().getFreeVariables();
      newVars.addAll(freeVariables);
      if (varFreeNTmap != null && varFreeNTmap.containsKey(e.getKey().toString())) {
        for (FreeVar fv : freeVariables) {
          NonTerminal fake = new NonTerminal(fv.toString(),null); //XXX put in information from existing
          varFreeNTmap.put(fv.toString(), fake);
        }
      }
    }
    if (adaptationSub != null) newVars.removeAll(adaptationSub.getMap().keySet());
    // System.out.println("new vars = " + newVars);
    inputVars.addAll(newVars);
  }
  
  public void removeUnreachableVariables() {
    if (adaptationSub != null) {
      boolean changed = false;
      Substitution newAdaptSub = new Substitution();
      for (Map.Entry<Atom, Term> e : adaptationSub.getMap().entrySet()) {
        Term newTerm = e.getValue().substitute(currentSub);
        newAdaptSub.add(e.getKey(),newTerm);
        if (newTerm != e.getValue()) {
          changed = true;
        }
      }
      if (changed) adaptationSub = newAdaptSub;
    }
    boolean changed = false;
    Substitution newSub = new Substitution();
    for (Map.Entry<Atom,Term> e : currentSub.getMap().entrySet()) {
      Atom key = e.getKey();
      if (key instanceof FreeVar) {
        FreeVar fv = (FreeVar)key;
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
  
  public void addVarFree(Element e) {
    if (e instanceof NonTerminal) {
      varFreeNTmap.put(e.toString(),(NonTerminal) e);
    } else if (e instanceof Clause) {
      for (Element e1: ((Clause)e).elements) {
        addVarFree(e1);
      }
    }
  }
  
  public boolean isKnownContext(NonTerminal root) {
    return root == null || root.equals(innermostGamma) || adaptationMap.containsKey(root);
  }
  
  /*public RuleNode parse(List<? extends Terminal> list) throws NotParseableException, AmbiguousSentenceException {
		if (g == null) {
			g = new edu.cmu.cs.sasylf.grammar.Grammar(GrmUtil.getStartSymbol(), ruleSet);
		}
		return g.parse(list);
	}*/

  public Grammar getGrammar() {
    if (g == null) {
      g = new edu.cmu.cs.sasylf.grammar.Grammar(GrmUtil.getStartSymbol(), ruleSet);
      ruleSize = ruleSet.size();
    } else { verify(ruleSize == ruleSet.size(), "rule set increased!"); }
    return g;
  }

  private Grammar g;
  int ruleSize = 0;
}
