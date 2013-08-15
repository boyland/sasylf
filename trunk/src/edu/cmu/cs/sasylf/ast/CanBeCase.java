package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.term.Term;

// marker interface for Rule and Clause
public interface CanBeCase {

	String getErrorDescription(Term t, Context ctx);

}
