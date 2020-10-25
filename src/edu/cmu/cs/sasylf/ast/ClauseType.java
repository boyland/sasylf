package edu.cmu.cs.sasylf.ast;

import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.Pair;

/** Marker type for all types to which a Clause could refer.
 * Only Syntax and Judgment implement this interface.
 */
public interface ClauseType extends ElementType {
	@Override
	public Constant typeTerm();

	/**
	 * Return whether this type has its inhabitants defined here.
	 * Case analysis is not permitted on things of abstract 
	 * @return whether this judgment or syntax is abstract.
	 */
	public boolean isAbstract();
	
	/**
	 * Perform a analysis of a term in preparation for a case analysis (or inversion)
	 * @param ctx Current context (must not be null)
	 * @param target element being analyzed -- must be previously type checked
	 * @param source location of analysis needs, used for error messages and debugging
	 * @param map updated with the case analysis
	 */
	public void analyze(Context ctx, Element target, Node source, Map<CanBeCase,Set<Pair<Term,Substitution>>> map);
}
