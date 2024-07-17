package edu.cmu.cs.sasylf;

import java.util.IdentityHashMap;
import java.util.Map;
import edu.cmu.cs.sasylf.ast.Judgment;
import edu.cmu.cs.sasylf.ast.NonTerminal;
import edu.cmu.cs.sasylf.ast.Syntax;
import edu.cmu.cs.sasylf.ast.SyntaxDeclaration;
import edu.cmu.cs.sasylf.ast.Theorem;

/**
 * SubstitutionData holds information about a substitution that is being performed on a CompUnit.
 */
public class SubstitutionData {

  /**
   * IdentitySet is a set that uses object identity to determine equality.
   */
  static class IdentitySet<T> {
    private final Map<T, Boolean> set;

    public IdentitySet() {
      set = new IdentityHashMap<>();
    }

    public boolean contains (T o) {
      return set.containsKey(o);
    }

    public void add (T o) {
      set.put(o, true);
    }

  }

  private final IdentitySet<Object> set; // the set of objects that have been substituted for
  public final String from; // the name of the object being substituted for
  public final String to; // the name of the object being substituted with
  private final SyntaxDeclaration newSyntax; // the syntax that is being substituted with
  private final Judgment newJudgment; // the judgment that is being substituted with
  private final Theorem newTheorem; // the theorem that is being substituted with
  public final SyntaxDeclaration oldSyntax; // the syntax that is being substituted for
  public final Judgment oldJudgment; // the judgment that is being substituted for
  public final SubstitutionType substitutionType; // the type of substitution being performed

  /*
   * Invariants:
   * 
   * Exactly one of newSyntax, newJudgment, and newTheorem is non-null.
   * 
   * oldSyntax and oldJudgment are non-null if and only if newSyntax and newJudgment are non-null, respectively.
   * 
   * substitutionType is SYNTAX if and only if newSyntax is non-null.
   * substitutionType is JUDGMENT if and only if newJudgment is non-null.
   * substitutionType is THEOREM if and only if newTheorem is non-null.
   */

  public static enum SubstitutionType {
    SYNTAX, JUDGMENT, THEOREM
  }

  public SubstitutionData(String from, String to, SyntaxDeclaration newSyntax, SyntaxDeclaration oldSyntax) {
    set = new IdentitySet<Object>();
    this.from = from;
    this.to = to;
    this.newSyntax = newSyntax;
    this.newJudgment = null;
    this.newTheorem = null;
    this.oldSyntax = oldSyntax;
    this.oldJudgment = null;
    substitutionType = SubstitutionType.SYNTAX;
  }

  public SubstitutionData(String from, String to, Judgment newJudgment, Judgment oldJudgment) {
    set = new IdentitySet<Object>();
    this.from = from;
    this.to = to;
    this.newSyntax = null;
    this.newJudgment = newJudgment;
    this.newTheorem = null;
    this.oldSyntax = null;
    this.oldJudgment = oldJudgment;
    substitutionType = SubstitutionType.JUDGMENT;
  }

  public SubstitutionData(String from, String to, Theorem newTheorem) {
    set = new IdentitySet<Object>();
    this.from = from;
    this.to = to;
    this.newSyntax = null;
    this.newJudgment = null;
    this.newTheorem = newTheorem;
    this.oldSyntax = null;
    this.oldJudgment = null;
    substitutionType = SubstitutionType.THEOREM;
  }

  /**
   * Returns whether this SubstitutionData is substituting for the given string.
   * @param s the string to check
   * @return whether this SubstitutionData is substituting for the given string
   */
  public boolean substitutingFor(String s) {
    return sameName(this.from, s);
  }

  /**
   * Returns whether this SubstitutionData is substituting for the given NonTerminal string.
   * @param from The string of the nonterminal to check
   * @return whether this SubstitutionData is substituting for the given NonTerminal string
   */
  public boolean containsSyntaxReplacementForByString(String from) {
    return sameName(this.from, from) && newSyntax != null;
  }

  /**
   * Returns whether this SubstitutionData is substituting for the given NonTerminal.
   * @param nt The NonTerminal to check
   * @return whether this SubstitutionData is substituting for the given NonTerminal
   */
  public boolean containsSyntaxReplacementFor(NonTerminal nt) {

    // check that they have the "same name" that nt's type is abstract

    //return sameName(this.from, nt.getSymbol()) && nt.getType().isAbstract();
  
    return containsSyntaxReplacementForByString(nt.getSymbol());
  }

  /**
   * Returns whether this SubstitutionData is substituting for the given Judgment string.
   * @param from The Judgment to check
   * @return whether this SubstitutionData is substituting for the given Judgment
   */
  public boolean containsJudgmentReplacementFor(String from) {
    boolean result = sameName(this.from, from) && newJudgment != null;
    return result;
  }

  /**
   * Returns whether this SubstitutionData is substituting for the given Theorem string
   * @param from The Theorem to check
   * @return whether this SubstitutionData is substituting for the given Theorem
   */
  public boolean containsTheoremReplacementFor(String from) {
    return sameName(this.from, from) && newTheorem != null;
  }

  /**
   * Returns the syntax replacement for the substitution.
   * 
   * @throws IllegalArgumentException if there is no syntax replacement
   * @return
   */
  public Syntax getSyntaxReplacement() {
    if (newSyntax == null) {
      throw new IllegalArgumentException("No syntax replacement for " + from);
    }
    return newSyntax;
  }

  /**
   * Returns the nonterminal of the syntax replacement for the substitution.
   * 
   * @throws IllegalArgumentException if there is no syntax replacement
   * @return
   */
  public NonTerminal getSyntaxReplacementNonTerminal() {
    Syntax s = getSyntaxReplacement();

    SyntaxDeclaration sd = s.getOriginalDeclaration();
    
    return sd.getNonTerminal();
  }

  /**
   * Returns the judgment replacement for the substitution.
   * 
   * @throws IllegalArgumentException if there is no judgment replacement
   * @return
   */
  public Judgment getJudgmentReplacement() {
    if (newJudgment == null) {
      throw new IllegalArgumentException("No judgment replacement for " + from);
    }
    return newJudgment;
  }

  /**
   * Returns the theorem replacement for the substitution.
   * 
   * @throws IllegalArgumentException if there is no theorem replacement
   * @return
   */
  public Theorem getTheoremReplacement() {
    if (newTheorem == null) {
      throw new IllegalArgumentException("No theorem replacement for " + from);
    }

    return newTheorem;
  }

  /**
   * Returns whether the given object has been substituted for.
   * @param o
   * @return whether the given object has been substituted for
   */
  public boolean didSubstituteFor(Object o) {
    return set.contains(o);
  }

  public void setSubstitutedFor(Object o) {
    set.add(o);
  }
  
  /**
   * Checks if name matches the name of the nonterminal defined by prefix.
   * @param prefix
   * @param name The string to check against the prefix
   * @return whether <code> name </code> is <code> prefix </code> followed by a
   * sequence of filler characters
   */
  public static boolean sameName(String prefix, String name) {
    // check that prefix is a prefix of name
    if (!name.startsWith(prefix)) {
      return false;
    }

    // check that all characters after the prefix match are filler characters

    int prefixLength = prefix.length();

    String filler = name.substring(prefixLength);

    return filler.matches("^[0-9_']*$");
  }
}
