package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.CloneData;
import edu.cmu.cs.sasylf.SubstitutionData;
import edu.cmu.cs.sasylf.util.Location;


public class ClauseAssumption extends SyntaxAssumption {

	public ClauseAssumption(Clause c, Location location) {
		this(c,location,null);
	}

	public ClauseAssumption(Clause c, Location location, Element a) {
		super(null,location,a);
		clause = c;
	}


	@Override
	public Element getElementBase() {
		return clause;
	}

	private Clause clause;

	@Override
	public void typecheck(Context ctx) {
		super.typecheck(ctx);
		clause = clause.typecheck(ctx);
	}

	// ClauseAssumptions don't get added to the derivation map
	@Override
	public void addToDerivationMap(Context ctx) {
		throw new UnsupportedOperationException();
	}

	public void substitute(String from, String to, SubstitutionData sd) {
		if (sd.didSubstituteFor(this)) return;
		sd.setSubstitutedFor(this);
		clause.substitute(from, to, sd);
	}

	public ClauseAssumption copy(CloneData cd) {
		if (cd.containsCloneFor(this)) return (ClauseAssumption) cd.getCloneFor(this);

		ClauseAssumption clone;

		try {
			clone = (ClauseAssumption) this.clone();
		}
		catch (CloneNotSupportedException e) {
			System.out.println("Clone not supperted in " + this.getClass());
			System.exit(1);
			return null;
		}

		cd.addCloneFor(this, clone);
		
		clone.clause = clause.copy(cd);
	
		

		return clone;

	}
}
