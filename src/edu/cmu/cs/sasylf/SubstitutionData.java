package edu.cmu.cs.sasylf;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.ast.Judgment;
import edu.cmu.cs.sasylf.ast.NonTerminal;
import edu.cmu.cs.sasylf.ast.Syntax;
import edu.cmu.cs.sasylf.ast.SyntaxDeclaration;
import edu.cmu.cs.sasylf.ast.Theorem;

public class SubstitutionData {
  private final Map<Object, Boolean> set;
  public final String from;
  public final String to;
  private final Syntax newSyntax;
  private final Judgment newJudgment;
  private final Theorem newTheorem;
  public final SubstitutionType substitutionType;

  public static enum SubstitutionType {
    SYNTAX, JUDGMENT, THEOREM
  }

  public SubstitutionData(String from, String to, Syntax newSyntax) {
    set = new IdentityHashMap<>();
    this.from = from;
    this.to = to;
    this.newSyntax = newSyntax;
    this.newJudgment = null;
    this.newTheorem = null;
    substitutionType = SubstitutionType.SYNTAX;
  }

  public SubstitutionData(String from, String to, Judgment newJudgment) {
    set = new IdentityHashMap<>();
    this.from = from;
    this.to = to;
    this.newSyntax = null;
    this.newJudgment = newJudgment;
    this.newTheorem = null;
    substitutionType = SubstitutionType.JUDGMENT;
  }

  public SubstitutionData(String from, String to, Theorem newTheorem) {
    set = new IdentityHashMap<>();
    this.from = from;
    this.to = to;
    this.newSyntax = null;
    this.newJudgment = null;
    this.newTheorem = newTheorem;
    substitutionType = SubstitutionType.THEOREM;
  }

  public boolean substitutingFor(String s) {
    return sameName(this.from, s);
  }

  public boolean containsSyntaxReplacementFor(String from) {
    return sameName(this.from, from) && newSyntax != null;
  }

  public boolean containsJudgmentReplacementFor(String from) {
    return sameName(this.from, from) && newJudgment != null;
  }

  public boolean containsTheoremReplacementFor(String from) {
    return sameName(this.from, from) && newTheorem != null;
  }

  public Syntax getSyntaxReplacement() {
    if (newSyntax == null) {
      throw new IllegalArgumentException("No syntax replacement for " + from);
    }
    return newSyntax;
  }

  public NonTerminal getSyntaxReplacementNonTerminal() {
    Syntax s = getSyntaxReplacement();
    // s should be an instance of SyntaxDeclaration

    SyntaxDeclaration sd = (SyntaxDeclaration) s;
    
    // return the nonterminal of the syntax declaration
    
    return sd.getNonTerminal();
  }

  public Judgment getJudgmentReplacement() {
    if (newJudgment == null) {
      throw new IllegalArgumentException("No judgment replacement for " + from);
    }
    return newJudgment;
  }

  public Theorem getTheoremReplacement() {
    if (newTheorem == null) {
      throw new IllegalArgumentException("No theorem replacement for " + from);
    }
    return newTheorem;
  }

  public boolean didSubstituteFor(Object o) {
    return set.containsKey(o);
  }

  public void setSubstitutedFor(Object o) {
    set.put(o, true);
  }
  
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
