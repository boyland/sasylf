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

  public SubstitutionData(String from, String to, Syntax newSyntax) {
    set = new IdentityHashMap<>();
    this.from = from;
    this.to = to;
    this.newSyntax = newSyntax;
    this.newJudgment = null;
    this.newTheorem = null;
  }

  public SubstitutionData(String from, String to, Judgment newJudgment) {
    set = new IdentityHashMap<>();
    this.from = from;
    this.to = to;
    this.newSyntax = null;
    this.newJudgment = newJudgment;
    this.newTheorem = null;
  }

  public SubstitutionData(String from, String to, Theorem newTheorem) {
    set = new IdentityHashMap<>();
    this.from = from;
    this.to = to;
    this.newSyntax = null;
    this.newJudgment = null;
    this.newTheorem = newTheorem;
  }

  public boolean containsSyntaxReplacementFor(String from) {
    return this.from.equals(from) && newSyntax != null;
  }

  public boolean containsJudgmentReplacementFor(String from) {
    return this.from.equals(from) && newJudgment != null;
  }

  public boolean containsTheoremReplacementFor(String from) {
    return this.from.equals(from) && newJudgment != null;
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
}
