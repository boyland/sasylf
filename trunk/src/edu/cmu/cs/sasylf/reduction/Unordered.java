package edu.cmu.cs.sasylf.reduction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import edu.cmu.cs.sasylf.ast.Context;
import edu.cmu.cs.sasylf.ast.Fact;
import edu.cmu.cs.sasylf.ast.Node;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.PermutationIterator;

/**
 * Induction on multiple items.
 * All items <em>in any order</em> must remain unchanged or else
 * reduce, and if at least one reduces, the whole reduces
 * So, if we have {A,B,C} as the schemas, then to get a reduction
 * we need a permutation of the targets such that
 * <ol>
 * <li> A must reduce (and B and C must reduce or be the same), or
 * <li> A and C must stay the same or reduce, and B must reduce, or
 * <li> A and B must stay the same or reduce and C must reduce.
 * </ol>
 * Equality happens if all stay the same. 
 * This definition is incomparable with {@link LexicographicOrder}
 * and more powerful than Twelf's Simultaneous Order
 * because we permit permutation.  In essence, we are doing induction
 * on the <em>sums</em> of the constituent reductions, except that we don't
 * need to map them into integers first.
 */
public class Unordered extends InductionSchema {

  private Unordered(List<InductionSchema> ss) {
    schemas = ss;
  }
  
  /**
   * Return a new unordered induction of the parts.
   * If there is only part, it is returned rather than create an unordered list of one.
   * @param parts induction schemas to be used together.
   * @return induction schema that uses them together.
   */
  public static InductionSchema create(InductionSchema... parts) {
    if (parts.length == 1) return parts[0];
    List<InductionSchema> schemas = new ArrayList<InductionSchema>();
    for (InductionSchema is : parts) {
      if (is instanceof Unordered) {
        for (InductionSchema is2 : ((Unordered)is).schemas) {
          schemas.add(is2);
        }
      } else {
        schemas.add(is);
      }
    }
    if (schemas.size() == 1) return schemas.get(0);
    return new Unordered(schemas);
  }

  @Override
  public boolean matches(InductionSchema s, Node errorPoint, boolean equality) {
    if (!(s instanceof Unordered)) {
      if (errorPoint != null) {
        ErrorHandler.recoverableError("Expected a single induction: " + s, errorPoint);
      }
      return false;
    }
    
    Unordered model = (Unordered)s;
    if (model.size() != size()) {
      if (errorPoint != null) {
        ErrorHandler.recoverableError("Expected " + model.size() + " inductions", errorPoint);
      }
      return false;
    }
    
    for (int i=0; i < size(); ++i) {
      if (!schemas.get(i).matches(model.get(i), errorPoint, false)) return false;
    }
    
    return true;
  }

  @Override
  public Reduction reduces(Context ctx, InductionSchema s, List<Fact> args, Node errorPoint) {
    Unordered other = (Unordered)s;
    int n = schemas.size();
    // special case: 0
    if (n == 0) return Reduction.EQUAL;
    
    Reduction result = Reduction.NONE;
    
    Iterator<List<InductionSchema>> it = new PermutationIterator<InductionSchema>(schemas);
    tryPermutation: while (it.hasNext()) {
      List<InductionSchema> permuted = it.next();
      boolean reduces = false;
      for (int i=0; i < n; ++i) {
        switch (permuted.get(i).reduces(ctx, other.get(i), args, null)) {
        case NONE: continue tryPermutation;
        case LESS:
          reduces = true;
        case EQUAL:
          default:
        }
      }
      if (reduces) return Reduction.LESS;
      result = Reduction.EQUAL;
    }
    if (result == Reduction.NONE && errorPoint != null) {
      ErrorHandler.recoverableError("Could find no permutation that reduced", errorPoint);
    }
    return result;
  }

  @Override
  public String describe() {
    StringBuilder sb = null;
    if (schemas.size() == 0) return "[none]";
    for (InductionSchema is : schemas) {
      if (sb == null) sb = new StringBuilder();
      else sb.append(", ");
      sb.append(is.describe());
    }
    return sb.toString();
  }

  private int cachedHash = -1;
  
  @Override
  public int hashCode() {
    if (cachedHash != -1) {
      cachedHash = new HashSet<InductionSchema>(schemas).hashCode();
    }
    return cachedHash;
  }
  
  public int size() { return schemas.size(); }
  
  public InductionSchema get(int i) {
    return schemas.get(i);
  }
  
  private List<InductionSchema> schemas;
}
