package edu.cmu.cs.sasylf.ast;

import java.util.Set;

import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Pair;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;

// marker interface for Rule and Clause
public interface CanBeCase {

	String getErrorDescription(Term t, Context ctx);

}
