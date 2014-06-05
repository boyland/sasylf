package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.util.ErrorHandler;



public class DerivationByCaseAnalysis extends DerivationByAnalysis {
    public DerivationByCaseAnalysis(String n, Location l, Clause c, String derivName) {
    	super(n,l,c, derivName);
    }
    public DerivationByCaseAnalysis(String n, Location l, Clause c, Clause subject) {
      super(n,l,c,subject);
    }

    public String byPhrase() { return "case analysis"; }

    @Override
    public void typecheck(Context ctx) {
      super.typecheck(ctx);
      if (getArgStrings().size() != 1) {
        ErrorHandler.report("case analysis can take only one argument", this);
      }
    }
    
    
}
