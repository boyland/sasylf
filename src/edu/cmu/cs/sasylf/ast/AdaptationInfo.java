package edu.cmu.cs.sasylf.ast;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.sasylf.term.Term;

public class AdaptationInfo {
	public AdaptationInfo(NonTerminal nc) {
		nextContext = nc;
	}
	
    public final NonTerminal nextContext; // the context one adaptation inside the original one
    public final List<String> varNames = new ArrayList<String>();
    public final List<Term> varTypes = new ArrayList<Term>();
}
