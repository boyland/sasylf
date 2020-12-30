package edu.cmu.cs.sasylf.reduction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import edu.cmu.cs.sasylf.ast.Context;
import edu.cmu.cs.sasylf.ast.Fact;
import edu.cmu.cs.sasylf.ast.Node;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
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
	 * All parts must match each other.
	 * @param errorPoint place to report non-self match
	 * @param parts induction schemas to be used together.
	 * @return induction schema that uses them together.
	 */
	public static InductionSchema create(Node errorPoint, InductionSchema... parts) {
		if (parts.length == 1) return parts[0];
		List<InductionSchema> schemas = new ArrayList<InductionSchema>();
		for (InductionSchema is : parts) {
			if (is == null) continue;
			if (is instanceof Unordered) {
				for (InductionSchema is2 : ((Unordered)is).schemas) {
					for (InductionSchema s : schemas) {
						if (!is2.matches(s, errorPoint, false)) return null;
					}
					schemas.add(is2);
				}
			} else {
				for (InductionSchema s : schemas) {
					if (!is.matches(s, errorPoint, false)) return null;
				}
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
				ErrorHandler.recoverableError(Errors.INDUCTION_MISMATCH, ": " + s, errorPoint);
			}
			return false;
		}

		Unordered model = (Unordered)s;
		if (model.size() != size()) {
			if (errorPoint != null) {
				ErrorHandler.recoverableError(Errors.INDUCTION_MISMATCH, ": " + model, errorPoint);
			}
			return false;
		}

		List<InductionSchema> others = new ArrayList<InductionSchema>(model.schemas);
		for (int i=0; i < size(); ++i) {
			boolean found = false;
			for (int j=0; j < others.size(); ++j) {
				if (schemas.get(i).matches(others.get(j), null, equality)) {
					found = true;
					others.remove(j);
					break;
				}
			}
			if (!found) {
				if (errorPoint != null) {
					// for side-effect
					schemas.get(i).matches(others.get(0), errorPoint, equality);
				}
				return false;
			}
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
			// TODO: find out which elements don't match anything
			ErrorHandler.recoverableError(Errors.INDUCTION_PERMUTATION, errorPoint);
		}
		return result;
	}

	@Override
	public String describe() {
		StringBuilder sb = null;
		if (schemas.size() == 0) return "{}";
		for (InductionSchema is : schemas) {
			if (sb == null) sb = new StringBuilder("{");
			else sb.append(", ");
			sb.append(is.describe());
		}
		sb.append("}");
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
