package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Util.debug;
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
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Pair;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;


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
  public Map<String,Fact> derivationMap = new HashMap<String, Fact>();
  public Map<List<ElemType>,ClauseDef> parseMap = new HashMap<List<ElemType>,ClauseDef>();
  public Map<String, Theorem> recursiveTheorems = new HashMap<String, Theorem>();
  public List<GrmRule> ruleSet = new ArrayList<GrmRule>();

  /// The remainder fields represent contextual (local) information

  public Map<String, List<ElemType>> bindingTypes;
  public Substitution currentSub = new Substitution();
  public Fact inductionVariable;
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
  public List<Fact> subderivations = new ArrayList<Fact>();
  public int inductionPosition;
  public Map<CanBeCase, Set<Pair<Term, Substitution>>> caseTermMap;
  public Substitution adaptationSub;
  public Set<NonTerminal> varfreeNTs = new HashSet<NonTerminal>();

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
    result.bindingTypes = new HashMap<String, List<ElemType>>(bindingTypes);
    result.currentSub = new Substitution(currentSub);
    result.adaptationMap = new HashMap<NonTerminal, AdaptationInfo>(adaptationMap);
    result.inputVars = new HashSet<FreeVar>(inputVars);
    result.outputVars = new HashSet<FreeVar>(outputVars);
    result.subderivations = new ArrayList<Fact>(subderivations);
    result.caseTermMap = new HashMap<CanBeCase, Set<Pair<Term, Substitution>>>(caseTermMap);
    result.adaptationSub = new Substitution(adaptationSub);
    result.varfreeNTs = new HashSet<NonTerminal>(varfreeNTs);
    
    return result;
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
    for (NonTerminal vfnt : varfreeNTs) {
      Term vft = vfnt.asTerm().substitute(currentSub);
      debug(vft + " contains " + t + " ?");
      if (vft.contains(t)) {
        return true;
      }
    }
    return false;
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
