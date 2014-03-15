package edu.cmu.cs.sasylf.reduction;

import java.util.List;

import edu.cmu.cs.sasylf.ast.AssumptionElement;
import edu.cmu.cs.sasylf.ast.Binding;
import edu.cmu.cs.sasylf.ast.Clause;
import edu.cmu.cs.sasylf.ast.Context;
import edu.cmu.cs.sasylf.ast.Element;
import edu.cmu.cs.sasylf.ast.Fact;
import edu.cmu.cs.sasylf.ast.Node;
import edu.cmu.cs.sasylf.ast.NonTerminal;
import edu.cmu.cs.sasylf.ast.Theorem;
import edu.cmu.cs.sasylf.term.Pair;
import edu.cmu.cs.sasylf.util.ErrorHandler;

/**
 * A reduction schema; how to permit "recursion" in theorems.
 */
public abstract class InductionSchema {
  /**
   * Does this schema match the argument?
   * All mutually inductive theorems must use matching schemas.
   * If a problem is found alert the error handler unless
   * the error point is null.
   * @param s reduction schema to match, must not be null
   * @param errorPoint location to complain about a non-match.  Null if no
   * errors should be recorded.
   * @param equality require the two schemas to be equal
   * @return whether this schema matches the argument.
   */
  public abstract boolean matches(InductionSchema s, Node errorPoint, boolean equality);
  
  /**
   * Does this instance of mutual induction following the schema?
   * We return EQUAL if the induction information used is unchanged;
   * we return LESS if the induction information is definitely smaller.
   * We return NONE if neither situation obtains.
   * @param ctx global information, must not be null
   * @param s schema of the callee, must match this
   * @param args actual parameters of the induction, must not be null
   * @param errorPoint location to complain about a problem, Null if no
   * errors should be recorded.
   * @return whether the induction is possible, null is not permitted
   */
  public abstract Reduction reduces(Context ctx, InductionSchema s, List<Fact> args, Node errorPoint);
  
  /**
   * Return a human-readable short description of this induction schema.
   * @return string (never null) describing this induction schema.
   */
  public abstract String describe();
  
  @Override
  public String toString() {
    return describe();
  }
  
  @Override
  public final boolean equals(Object x) {
    if (!(x instanceof InductionSchema)) return false;
    return matches((InductionSchema)x,null,true);
  }
  
  @Override
  public abstract int hashCode();
  
  public static final InductionSchema nullInduction = LexicographicOrder.create();

  /**
   * Create an induction schema signified by the given list.
   * Return null if there is an error.  Error will be reported if
   * errorPoint is non-null.
   * @param thm context information, must not be null
   * @param args series of induction specifications, must not be null
   * @param errorPoint point to report errors for, if not null
   * @return induction schema, or null
   */
  public static InductionSchema create(Theorem thm, List<Clause> args, Node errorPoint) {
    InductionSchema result = nullInduction;
    for (Clause cl : args) {
      InductionSchema is = create(thm,cl,errorPoint);
      if (is == null) return null;
      result = LexicographicOrder.create(result,is);
    }
    return result;
  }

  public static InductionSchema create(Theorem thm, Element arg, Node errorPoint) {
    if (arg instanceof NonTerminal) {
      return StructuralInduction.create(thm, ((NonTerminal)arg).getSymbol(), errorPoint);
    } else if (arg instanceof AssumptionElement) {
      return create(thm, ((AssumptionElement)arg).getBase(), errorPoint);
    } else if (arg instanceof Binding) {
      return StructuralInduction.create(thm, ((Binding)arg).getNonTerminal().getSymbol(), errorPoint);
    } else if (arg instanceof Clause) {
      Clause cl = (Clause)arg;
      Pair<InductionSchema,Integer> p = parse(thm,cl.getElements(), 0, errorPoint);
      if (p.first == null) return null;
      if (p.second != cl.getElements().size()) {
        ErrorHandler.recoverableError("Cannot parse induction schema starting at " + cl.getElements().get(p.second),
                                      errorPoint);
        return null;
      }
      return p.first; 
    }
    if (errorPoint != null) {
      ErrorHandler.recoverableError("Cannot parse induction schema: " + arg, errorPoint);
    }
    return null;    
  }
  
  private static Pair<InductionSchema,Integer> parse(Theorem thm, List<Element> parts, int i, Node errorPoint) {
    if (i == parts.size()) {
      ErrorHandler.recoverableError("induction description too short", errorPoint);
      return new Pair<InductionSchema,Integer>(null,i);
    }
    Element e = parts.get(i);
    InductionSchema result;
    if (e.toString().equals("\"{\"")) {
      result = Unordered.create();
      do {
        Pair<InductionSchema,Integer> p = parse(thm, parts,i+1,errorPoint);
        if (p.first == null) return p;
        i = p.second;
        result = Unordered.create(result,p.first);
      } while (parts.get(i).toString().equals("\",\""));
      if (!parts.get(i).toString().equals("\"}\"")) {
        ErrorHandler.recoverableError("induction set missing '}'", errorPoint);
        return new Pair<InductionSchema,Integer>(null,i);
      }
      ++i;
    } else {
      result = create(thm,e,errorPoint);
      ++i;
    }
    
    if (i < parts.size() && parts.get(i).toString().equals(">")) {
      Pair<InductionSchema,Integer> p = parse(thm,parts,i+1,errorPoint);
      if (p.first == null) return null;
      result = LexicographicOrder.create(result,p.first);
      return new Pair<InductionSchema,Integer>(result,p.second);
    }
    return new Pair<InductionSchema,Integer>(result,i);
  }
}
