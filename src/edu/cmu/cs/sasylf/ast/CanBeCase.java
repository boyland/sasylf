package edu.cmu.cs.sasylf.ast;

import java.util.Set;

import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.Pair;

// interface for Rule and Clause
public interface CanBeCase {
	/**
	 * A user-informative string for this case
	 * @return an identifying string
	 */
	String getName();
	
	String getErrorDescription(Term t, Context ctx);

	/**
	 * Returns a fresh term for this possibility and a substitution that matches the term.
	 * The result will be empty if no case analysis is possible.
	 * The result can return a set of more than one possibility if variables are involved.
	 * The resulting set is mutable in that it can have elements removed from it,
	 * but clients should not attempt to add elements.
	 * @param ctx context, must not be null
	 * @param term target term being analyzed
	 * @param clause source-level of term
	 * @param source location to indicate errors and debugging messages
	 * @return possibilities that this rule could match the target
	 */
	Set<Pair<Term,Substitution>> caseAnalyze(Context ctx, Term term, Element target, Node source);
}
