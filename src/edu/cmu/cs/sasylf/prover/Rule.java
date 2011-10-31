package edu.cmu.cs.sasylf.prover;

import java.util.List;

import edu.cmu.cs.sasylf.term.Substitution;

public interface Rule {
	public String toString();
	public int hashCode();
	public boolean equals(Object other);

	Substitution getSubstitution();
	List<Judgment> getPreconditions();
	boolean hasPreconditions();
	Judgment getResult();
	
	String prettyPrint();
}