package edu.cmu.cs.sasylf.prover;

import java.util.List;

import edu.cmu.cs.sasylf.term.Substitution;

public interface Rule {
	@Override
	public String toString();
	@Override
	public int hashCode();
	@Override
	public boolean equals(Object other);

	Substitution getSubstitution();
	List<Judgment> getPreconditions();
	boolean hasPreconditions();
	Judgment getResult();

	String prettyPrint();
}