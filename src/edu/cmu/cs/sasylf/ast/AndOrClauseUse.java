package edu.cmu.cs.sasylf.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.ast.ContextJudgment.NoCommonPrefixException;
import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Span;
import edu.cmu.cs.sasylf.util.Util;

public abstract class AndOrClauseUse extends ClauseUse {

	protected AndOrClauseUse(Location loc, List<Element> elems, ClauseDef cd, List<ClauseUse> clauses) {
		super(loc, elems, cd);
		this.clauses = clauses;
	}

	public List<ClauseUse> getClauses() { return clauses; }

	private List<ClauseUse> clauses;


	@Override
	void checkVariables(Set<String> bound, boolean defining) {
		for (ClauseUse u: clauses) {
			u.checkVariables(bound, defining);
		}
	}
	
	@Override
	public Term computeTerm(List<Pair<String, Term>> varBindings) {
		Util.verify(varBindings.size() == 0, "should only be at top level");
		// XXX: Code duplicated with ClauseUse (getBaseTerm and computeTerm) 
		int assumeIndex = getConstructor().getAssumeIndex();
		if (assumeIndex >= 0) {
			getElements().get(assumeIndex).readAssumptions(varBindings, true);
		}
		List<Term> pieces = new ArrayList<>();
		for (ClauseUse cl : clauses) {
			Term p = cl.asTerm();
			List<Abstraction> wrappers = new ArrayList<>();
			Term b = Term.getWrappingAbstractions(p, wrappers);
			if (wrappers.size() != 0) {
				Util.verify(wrappers.size() == varBindings.size(),"mismatch in context for " + this);
			}
			pieces.addAll(((Application)b).getArguments());
		}
		Term t = Facade.App(getConstructor().asTerm(), pieces);
		if (assumeIndex != -1) {
			t = newWrap(t,varBindings,0);
		}
		Util.debug("computed term for ", this, " as ", t);
		return t;
	}

	public AndOrClauseUse create(Span sp, Context ctx, List<ClauseUse> parts) {
		AndOrJudgment rcvr = (AndOrJudgment)getType();
		Element prefix = null;
		NonTerminal assumption = null;
		List<Judgment> types = new ArrayList<>();
		for (ClauseUse p : parts) {
			ClauseType type = p.getType();
			if (!(type instanceof Judgment)) {
				ErrorHandler.error(Errors.ANDOR_NOSYNTAX, ""+p, sp);
			}
			final Judgment judg = (Judgment)type;
			types.add(judg);
			final NonTerminal localAssume = judg.getAssume();
			if (localAssume == null) continue;
			if (assumption == null) assumption = localAssume;
			else if (!assumption.equals(localAssume)) {
				if (p.getRoot() == null) continue; // OK -- not involved with prefix
				ErrorHandler.error(Errors.ANDOR_CONTEXT,assumption + " != " + localAssume, sp);
			}
			Element e = p.getAssumes();
			Util.verify(e != null, "How can it be null if the judgment has an assumption?");
			if (prefix == null) prefix = e;
			else try {
				prefix = ContextJudgment.getCommonPrefix(prefix, e);
			} catch (NoCommonPrefixException e1) {
				ErrorHandler.error(Errors.ANDOR_PREFIX,p + " >/= " + prefix, sp);								
			}
		}
		List<ClauseUse> clauses = new ArrayList<>(parts); // so we can mutate
		int n = types.size();
		for (int i=0; i < n; ++i) {
			Judgment j = types.get(i);
			ClauseUse cu = clauses.get(i);
			if (j.getAssume() == null) continue;
			Element context = cu.getAssumes();
			if (context.equals(prefix)) continue;
			if (!j.getAssume().equals(assumption)) {
				Util.verify(cu.getRoot() == null, "This was checked already.");
				j = ContextJudgment.create(getLocation(), ctx, j, cu, null, context);
				cu = ContextJudgment.convert(cu, null);
			} else {
				j = ContextJudgment.create(getLocation(), ctx, j, cu, prefix, context);
				cu = ContextJudgment.convert(cu, prefix);				
			}
			Util.debug("converted: ",cu);
			types.set(i, j);
			clauses.set(i, cu);
		}
		List<Element> newElements = new ArrayList<Element>();
		for (ClauseUse cl : clauses) {
			if (!newElements.isEmpty()) {
				newElements.add(rcvr.makeSeparator(sp));
			}
			for (Element e : cl.getElements()) {
				newElements.add(e);
			}
		}
		AndOrClauseUse result;
		if (this instanceof AndClauseUse) {
			ClauseDef cd = (ClauseDef)AndJudgment.makeAndJudgment(sp.getLocation(), ctx, types, clauses).getForm();
			result = new AndClauseUse(sp.getLocation(),newElements,cd,clauses);
		} else {
			ClauseDef cd = (ClauseDef)OrJudgment.makeOrJudgment(sp.getLocation(), ctx, types, clauses).getForm();
			result = new OrClauseUse(sp.getLocation(),newElements,cd,clauses);
		}
		result.setEndLocation(sp.getEndLocation());
		return result;
	}
	
	public static AndOrClauseUse create(boolean isAnd, Location loc, Context ctx, List<ClauseUse> parts) {
		AndOrClauseUse recvr;
		if (isAnd) recvr = AndClauseUse.makeEmptyAndClause(loc);
		else recvr = OrClauseUse.makeEmptyOrClause(loc);
		return recvr.create(loc, ctx, parts);
	}
}
