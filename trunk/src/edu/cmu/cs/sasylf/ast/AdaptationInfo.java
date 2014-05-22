package edu.cmu.cs.sasylf.ast;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.Pair;

public class AdaptationInfo {
	public AdaptationInfo(NonTerminal nc) {
		nextContext = nc;
	}
	
	public AdaptationInfo(NonTerminal nc, List<Pair<String,Term>> varBindings) {
	  nextContext = nc;
	  for (Pair<String,Term> p : varBindings) {
	    varNames.add(p.first);
	    varTypes.add(p.second);
	  }
	}
	
	public AdaptationInfo(List<Abstraction> wraps, NonTerminal nc) {
	  nextContext = nc;
	  for (Abstraction a : wraps) {
	    varNames.add(a.varName);
	    varTypes.add(a.varType);
	  }
	}
    public final NonTerminal nextContext; // the context one adaptation inside the original one
    public final List<String> varNames = new ArrayList<String>();
    public final List<Term> varTypes = new ArrayList<Term>();
}
