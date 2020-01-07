package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.util.Location;

/**
 * A case analysis that need not be complete.
 * The remaining cases are saved so that later case analyses don't need to
 * handle them.
 * <p>
 * The implementation is handled by an <tt>instanceof</tt> check in
 * {@link DerivationByAnalysis}.
 */
public class PartialCaseAnalysis extends DerivationByCaseAnalysis {

	public PartialCaseAnalysis(Location l, String derivName) {
		super("_", l, null, derivName);
	}
	public PartialCaseAnalysis(Location l, Clause subject) {
		super("_", l, null, subject);
	}

}
