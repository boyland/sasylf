package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.SingletonSet;
import edu.cmu.cs.sasylf.util.Util;

public class ClauseDef extends Clause {
	public ClauseDef(Clause copy, ClauseType type) {
		this(copy, type, null);
	}
	public ClauseDef(Clause copy, ClauseType type, String cName) {
		super(copy.getLocation());
		setEndLocation(copy.getEndLocation());
		getElements().addAll(copy.getElements());
		this.type = type;
		if (cName != null) {
			consName = cName;
		} else {
			consName = "C";
			for (Element e : getElements()) {
				if (e instanceof Terminal) {
					Terminal t = (Terminal) e;
					//if (Character.isLetter(t.getSymbol().charAt(0))) {
					consName += '_' + t.getSymbol();
					//}
				} 
			}
			consName = uniqueify(consName);
		}
		for (Element e : getElements()) {
			if (e instanceof Clause) {
				ErrorHandler.error(Errors.CLAUSE_DEF_PAREN,copy);
			}
		}
	}

	public String getConstructorName() { return consName; }
	@Override
	public ClauseType getType() { return type; }
	
	/**
	 * Return the clause def that this clause def builds terms for.
	 * Normally, the same as this, but for sugar, the base is the "real"
	 * clause def.
	 * @return clause def that this def builds ters for
	 */
	public ClauseDef getBaseClauseDef() { return this; }
	
	/**
	 * Return the index of a context NT in this form, or -1 if there is
	 * no use of the context nonterminal (or no context nonterminal).
	 * Normally there is only one, but if (for example for and/or judgments)
	 * there are multiple indices, use {@link ClauseDef#getAssumeIndices()}.
	 * @return index of the context NT in this form.
	 */
	public int getAssumeIndex() {
		if (cachedAssumeIndex > -2) return cachedAssumeIndex;
		List<Integer> assumeIndices = getAssumeIndices();
		if (assumeIndices.isEmpty()) return cachedAssumeIndex=-1;
		return cachedAssumeIndex=assumeIndices.get(0);
	}
	
	private List<Integer> allAssumeIndices = null;
	/**
	 * Return a list of all indices where the context NT (if any) appears in the form.
	 * @return (unmodifiable) list of indices
	 */
	public List<Integer> getAssumeIndices() {
		if (allAssumeIndices != null) return allAssumeIndices;
		NonTerminal assumeNT = null;
		if (type instanceof Judgment) {
			assumeNT = ((Judgment)type).getAssume();
		} else if (type instanceof SyntaxDeclaration) {
			SyntaxDeclaration s = (SyntaxDeclaration)type;
			if (s.isInContextForm()) {
				assumeNT = s.getNonTerminal();
			}
		}
		if (assumeNT == null) return allAssumeIndices = Collections.emptyList();
		List<Integer> result = new ArrayList<>();
		List<Element> elements = getElements();
		for (int i=0; i < elements.size(); ++i) {
			if (assumeNT.equals(elements.get(i))) result.add(i);
		}
		if (result.isEmpty()) return allAssumeIndices = Collections.emptyList();
		return allAssumeIndices = Collections.unmodifiableList(result);
	}

	@Override
	public Term getTypeTerm() { return asTerm(); }

	private String consName;
	private ClauseType type;
	public Rule assumptionRule;	
	private int cachedAssumeIndex = -2;

	static private int uniqueint = 0;
	static private Set<String> strings = new HashSet<String>();
	static private String uniqueify(String s) {
		String result = s;
		if (strings.contains(s)) {
			result = s + uniqueint++;
		}
		strings.add(result);
		return result;
	}

	@Override
	public Term computeTerm(List<Pair<String, Term>> varBindings) {
		Term typeTerm = type.typeTerm();
		int assumeIndex = getAssumeIndex();
		List<Term> argTypes = new ArrayList<Term>();
		List<String> argNames = new ArrayList<String>();

		for (int i = 0; i < getElements().size(); ++i) {
			Element e = getElements().get(i);
			if (! (e instanceof Terminal) && i != assumeIndex 
					&& !(e instanceof Variable)) {
				Term argType = null;
				String argName = "x";
				if (e instanceof Binding) {
					Binding defB = (Binding) e;
					argType = defB.getNonTerminal().getType().typeTerm();
					argName = defB.getNonTerminal().getSymbol();

					List<Term> varTypes = new ArrayList<Term>();
					for (Element boundVarElem : defB.getElements()) {
						int varIndex = getIndexOf((Variable)boundVarElem);
						if (varIndex == -1)
							ErrorHandler.error(Errors.VAR_UNBOUND_USED,boundVarElem.toString(), this);
						Variable localVar = (Variable) getElements().get(varIndex);
						varTypes.add(localVar.getType().typeTerm());
					}
					argType = Term.wrapWithLambdas(argType, varTypes);
				} else if (e instanceof NonTerminal){
					// JTB: The following check is needed for AndClauses which can have multiple
					// contexts.
					if (((NonTerminal)e).getType().isInContextForm()) continue;
					argType = ((NonTerminal)e).getType().typeTerm();
					argName = ((NonTerminal)e).getSymbol();
				} else if (e instanceof Clause) {
					argType = ((ClauseUse)e).getConstructor().asTerm();
					argName = ((ClauseUse)e).getElemType().toString();
				} else {
					throw new RuntimeException("should be impossible case");
				}
				argTypes.add(argType);
				argNames.add(argName);
			}
		}

		typeTerm = Term.wrapWithLambdas(typeTerm, argTypes, argNames);

		return new Constant(consName, typeTerm);
	}

	public int getVariableIndex() {
		int index = 0;
		int result = -1;
		for (Element e : getElements()) {
			if (e instanceof Variable) {
				if (result == -1) result = index;
				else {
					Util.verify(false, "context clause with more than one variable?");
				}
			}
			++index;
		}
		return result;
	}

	public int getIndexOf(Variable boundVar) {
		return getElements().indexOf(boundVar);
	}

	/** Computes a sample term for use in case analysis.
	 * Consists of the clause constant applied to fresh variables.
	 */
	public Term getSampleTerm() {
		Constant constant = (Constant)asTerm();
		Term typeTerm = constant.getType();
		List<Term> arguments = new ArrayList<Term>();
		while (typeTerm instanceof Abstraction) {
			Abstraction abs = (Abstraction)typeTerm;
			Term var = Facade.FreshVar(abs.varName, abs.varType);
			arguments.add(var);
			typeTerm = abs.getBody();
		}
		return constant.apply(arguments, 0);
	}

	/** All top-level vars should also be present inside bindings
	 * Only variables that appear at top level should be present inside bindings
	 * @param isContext 
	 */
	public void checkVarUse(boolean isContext) {
		Set<Variable> topVars = new HashSet<Variable>(), boundVars = new HashSet<Variable>();
		for (int i = 0; i < elements.size(); ++i) {
			Element e = elements.get(i);
			if (e instanceof Variable)
				topVars.add((Variable) e);
			if (e instanceof Binding) {
				for (Element boundE : ((Binding)e).getElements()) {
					if (boundE instanceof Variable)
						boundVars.add((Variable) boundE);
					else
						ErrorHandler.error(Errors.BAD_SYNTAX_BINDING, boundE);
				}
			}
		}
		if (!topVars.equals(boundVars)) {
			if (!topVars.containsAll(boundVars)) {
				// a variable in a binding was not bound outside
				boundVars.removeAll(topVars);
				for (Variable v : boundVars) {
					ErrorHandler.recoverableError(Errors.VAR_UNBOUND, v.toString(), v);
				}
				return;
			} else {
				// a variable declared at the top was not used in a binding
				topVars.removeAll(boundVars);
				if (isContext) {
					for (Variable v : topVars) {
						SyntaxDeclaration varType = v.getType();
						varType.setContext(this);
					}
				} else {
					ErrorHandler.error(Errors.VAR_UNBOUND_UNUSED, topVars + "", this);
				}
			}
		}
	}

	/**
	 * Set subordination to take into account the
	 * parts of this clause: every nonterminal in the clause
	 * can be part of the result.
	 * @param includeVars if true, variables are assumed also
	 * (NB: includeVars == true is needed to follow Twelf.)
	 */
	public void computeSubordination(boolean includeVars) {
		Constant c1 = type.typeTerm();
		for (Element e : elements) {
			NonTerminal nt = null;
			if (e instanceof NonTerminal) nt = (NonTerminal)e;
			else if (e instanceof Binding) nt = ((Binding)e).getNonTerminal();
			else if (includeVars && e instanceof Variable) nt = ((Variable)e).getType().getNonTerminal();
			if (nt == null) continue;
			Constant c2 = nt.getType().typeTerm();
			if (!FreeVar.canAppearIn(c2, c1)) {
				Util.debug("sub: ",c2," < ",c1);
			}
			FreeVar.setAppearsIn(c2, c1);
		}
	}
	
	/**
	 * Ensure that this clause doesn't include bindings
	 * that are infeasible.  For example if we have t[X]
	 * as a binding, then X must be a variable that can occur in
	 * terms of type t.  Errors are generated but not thrown.
	 * We return after the first error is found, if one is found.
	 * @return true if no errors found
	 * (it is not expected that clients will use the return value).
	 */
	public boolean checkSubordination() {
		for (Element e : elements) {
			if (!(e instanceof Binding)) continue;
			Binding b = (Binding)e;
			SyntaxDeclaration sd = b.getNonTerminal().getType();
			Constant c1 = sd.typeTerm();
			for (Element sub : b.getElements()) {
				ElementType et = sub.getType();
				if (!(et instanceof SyntaxDeclaration)) continue;
				Constant c2 = (Constant) et.typeTerm();
				if (FreeVar.canAppearIn(c2, c1)) continue;
				ErrorHandler.recoverableError(Errors.SYNTAX_SUBORDINATION_ERROR, b.getNonTerminal().toString(), sub);
				return false;
			}
		}
		return true;
	}

	@Override
	public void prettyPrint(PrintWriter out, PrintContext ctx) {
		if (ctx == null) {
			super.prettyPrint(out, null);
			return;
		}
		//System.err.println("clausedef.prettyPrint for " + (ctx == null ? "" : ctx.term));
		Term origT = ctx.term;
		Term t = origT;
		List<String> origBoundVars = ctx.boundVars;
		int origBoundVarCount = ctx.boundVarCount;
		Set<String> ctxVars = null;
		if (getAssumeIndex() != -1 && t instanceof Abstraction) {
			ctxVars = new HashSet<String>();
			ctx.boundVars = new ArrayList<String>(ctx.boundVars);
			while (t instanceof Abstraction) {
				String varName = "<aVar" + ctx.boundVarCount + ">";
				ctx.boundVars.add(varName);
				ctxVars.add(varName);
				t = ((Abstraction)t).getBody();
				if (t instanceof Abstraction) {
					t = ((Abstraction)t).getBody();					
					ctx.boundVars.add("<aVar" + ctx.boundVarCount + "assumption>");
				}
				ctx.boundVarCount++;
			}
		}
		boolean prev = false;
		Iterator<? extends Term> termIter = null;
		if (t instanceof Application) {
			List<? extends Term> elemTerms = ((Application)t).getArguments();
			termIter = elemTerms.iterator();
		}
		for (int i = 0; i < elements.size(); ++i) {
			Element e = elements.get(i);
			if (prev)
				out.print(' ');
			if (e instanceof Clause)
				out.print('(');
			t = null;
			if (!(e instanceof Terminal) && !(e instanceof Variable) && termIter != null && termIter.hasNext() && i != getAssumeIndex())
				t = termIter.next();
			//System.err.println("type " + e.getClass().getName() + " for " + e + " with term " + t);
			if (i == getAssumeIndex()) {
				out.print(ctx.contextVarName);
				if (origT instanceof Abstraction)
					out.print("<expanded with vars:"+ ctxVars +">");
			}
			else
				e.prettyPrint(out, t == null? null : new PrintContext(t, ctx));
			if (e instanceof Clause)
				out.print(')');
			prev = true;
		}

		if (origBoundVars != null) {
			ctx.boundVars = origBoundVars;
			ctx.boundVarCount = origBoundVarCount;
		}
	}
	@Override
	public Set<Pair<Term, Substitution>> caseAnalyze(Context ctx, Term targetTerm,
			Element target, Node source) {
		Util.verify(getType() instanceof SyntaxDeclaration, "case analyze should be called on syntax clauses, not " + this);		
		
		List<Abstraction> context = new ArrayList<Abstraction>();
		Term.getWrappingAbstractions(targetTerm, context);

		Term term = getSampleTerm();
		Substitution freshSub = term.freshSubstitution(new Substitution());
		term = term.substitute(freshSub);
		// adaptation!
		term = ctx.adapt(term, context, true);

		Util.debug("------Unify?");
		Util.debug("term = ",term);
		Util.debug("subj = ",targetTerm);
		Substitution checkSub;
		try {
			checkSub = term.unify(targetTerm);
		} catch (UnificationFailed ex) {
			Util.debug("error = ",ex.getMessage());
			Util.debug("term = ",term,": ",term.getType());
			Util.debug("subj = ",targetTerm,": ",targetTerm.getType());
			ErrorHandler.error(Errors.INTERNAL_ERROR, ": Unification should not fail: " + term + " ? " + targetTerm,this);
			return null; // NOTREACHED
		}
		Util.debug("checking checkSub = ",checkSub);
		if (!ctx.canCompose(checkSub)) {
			Util.debug("can't compose.");
			return Collections.emptySet();
		}
		
		return SingletonSet.create(new Pair<Term,Substitution>(term, checkSub));
	}
	
	
}
