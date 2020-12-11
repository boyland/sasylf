package edu.cmu.cs.sasylf.reduction;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.sasylf.ast.Context;
import edu.cmu.cs.sasylf.ast.Fact;
import edu.cmu.cs.sasylf.ast.Node;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;

/**
 * Induction on multiple items.
 * Reduction can be unchanged up to one which must reduce.
 * So, if we have A > B > C as the schemas, then either 
 * <ol>
 * <li> A must reduce (and B and C can be arbitrary), or
 * <li> A must stay the same, and B must reduce (and C can change arbitrarily), or
 * <li> A and B must stay the same the C must reduce.
 * </ol>
 * Equality happens if all stay the same. 
 * This definition is the same as that in Twelf.
 */
public class LexicographicOrder extends InductionSchema {

	private LexicographicOrder(List<InductionSchema> ss) {
		schemas = ss;
	}

	/**
	 * Return a new lexicographic order of the parts.
	 * If there is only part, it is returned rather than create an order of one.
	 * @param parts induction schemas to be ordered
	 * @return induction schema that orders them lexicographically
	 */
	public static InductionSchema create(InductionSchema... parts) {
		if (parts.length == 1) return parts[0];
		List<InductionSchema> schemas = new ArrayList<InductionSchema>();
		for (InductionSchema is : parts) {
			if (is instanceof LexicographicOrder) {
				for (InductionSchema is2 : ((LexicographicOrder)is).schemas) {
					schemas.add(is2);
				}
			} else {
				schemas.add(is);
			}
		}
		if (schemas.size() == 1) return schemas.get(0);
		return new LexicographicOrder(schemas);
	}

	@Override
	public boolean matches(InductionSchema s, Node errorPoint, boolean equality) {
		if (!(s instanceof LexicographicOrder)) {
			if (errorPoint != null) {
				ErrorHandler.recoverableError(Errors.INDUCTION_MISMATCH, ": " + s, errorPoint);
			}
			return false;
		}

		LexicographicOrder model = (LexicographicOrder)s;
		if (model.size() != size()) {
			if (errorPoint != null) {
				ErrorHandler.recoverableError(Errors.INDUCTION_MISMATCH, ": " + model, errorPoint);
			}
			return false;
		}

		for (int i=0; i < size(); ++i) {
			if (!schemas.get(i).matches(model.get(i), errorPoint, equality)) return false;
		}

		return true;
	}

	@Override
	public Reduction reduces(Context ctx, InductionSchema s, List<Fact> args, Node errorPoint) {
		LexicographicOrder other = (LexicographicOrder)s;
		for (int i=0; i < schemas.size(); ++i) {
			Reduction step = schemas.get(i).reduces(ctx, other.get(i), args, errorPoint);
			if (step != Reduction.EQUAL) return step; 
		}
		return Reduction.EQUAL;
	}

	@Override
	public String describe() {
		StringBuilder sb = null;
		if (schemas.size() == 0) return "(none)";
		for (InductionSchema is : schemas) {
			if (sb == null) sb = new StringBuilder("(");
			else sb.append(", ");
			sb.append(is.describe());
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return schemas.hashCode();
	}

	public int size() { return schemas.size(); }

	public InductionSchema get(int i) {
		return schemas.get(i);
	}

	private List<InductionSchema> schemas;
}
