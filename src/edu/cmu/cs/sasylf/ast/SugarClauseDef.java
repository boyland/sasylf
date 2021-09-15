package edu.cmu.cs.sasylf.ast;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.BoundVar;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.Pair;

public class SugarClauseDef extends ClauseDef {

	private final Element definition;
	
	/**
	 * Create a sugar clause definition.
	 * @param copy the sugar syntax being defined
	 * @param type its syntactic type (Syntax or Judgment)
	 * @param defn the replacement clause
	 */
	public SugarClauseDef(Clause sugar, ClauseType type, Element defn) {
		super(sugar, type);
		definition = defn;
	}

	@Override
	public ClauseDef getBaseClauseDef() {
		return ((ClauseUse)definition).getConstructor().getBaseClauseDef();
	}
	
	@Override
	public void checkVarUse(boolean isContext) {
		super.checkVarUse(isContext);
		// for side-effect
		asTerm();
	}

	@Override
	public Term computeTerm(List<Pair<String, Term>> varBindings) {
		Term body = definition.computeTerm(varBindings);
		Constant cnst = (Constant)super.computeTerm(varBindings);
		Term ctype = cnst.getType();
		List<Abstraction> wrappers = new ArrayList<Abstraction>();
		Term.getWrappingAbstractions(ctype, wrappers);
		Substitution sub = new Substitution();
		int n = wrappers.size();
		for (int i=0; i < n; ++i) {
			Abstraction a = wrappers.get(i);
			sub.add(new FreeVar(a.varName,a.varType), new BoundVar(n-i));
		}
		Term result = Term.wrapWithLambdas(wrappers, body.substitute(sub));
		// System.out.println("term for sugar " + definition + " is " + result);
		/* Already handled in Sugar
		Set<FreeVar> unbound = result.getFreeVariables();
		if (!unbound.isEmpty()) {
			ErrorHandler.recoverableError("Not sure what to use for these variables " + unbound, this);
		}
		*/
		return result;
	}

	
}
