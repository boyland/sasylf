package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import edu.cmu.cs.sasylf.CopyData;
import edu.cmu.cs.sasylf.ModuleArgument;
import edu.cmu.cs.sasylf.SubstitutionData;
import edu.cmu.cs.sasylf.ast.grammar.GrmRule;
import edu.cmu.cs.sasylf.ast.grammar.GrmUtil;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.ErrorReport;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.Util;


public class Judgment extends Node implements ClauseType, Named, ModuleArgument {
	private List<Rule> rules;
	private Clause form;
	private String name;
	public NonTerminal assume;
	private boolean isAbstract;

	public Judgment(Location loc, String n, List<Rule> l, Clause c, NonTerminal a) { 
		super(loc); 
		name=n; 
		if (l == null) {
			isAbstract = true;
			rules = Collections.emptyList();
		} else {
			rules=l;
		}
		form=c; 
		assume = a; 
		setEndLocation();
	}

	protected void setEndLocation() {
		if (!rules.isEmpty()) {
			super.setEndLocation(rules.get(rules.size()-1).getEndLocation());
		} else if (assume != null) {
			super.setEndLocation(assume.getEndLocation());
		} else {
			super.setEndLocation(form.getEndLocation());
		}
	}

	public List<Rule> getRules() { return rules; }
	public Clause getForm() { return form; }
	@Override
	public String getName() { return name; }
	public NonTerminal getAssume() { return assume; }
	@Override
	public boolean isAbstract() { return isAbstract; }


	@Override
	public void prettyPrint(PrintWriter out) {
		out.print("judgment ");
		out.print(name);
		out.print(": ");
		form.prettyPrint(out);
		out.println();

		if (assume != null) {
			out.print("assumes ");
			assume.prettyPrint(out);
			out.println();
		}
		out.println();

		for (Rule r : getRules()) {
			r.prettyPrint(out);
		}
		out.println("\n");
	}

	public Set<Terminal> getTerminals() {
		return form.getTerminals();
	}

	public void defineConstructor(Context ctx) {
		form.typecheck(ctx);
		ClauseDef cd;
		if (form instanceof ClauseDef) cd = (ClauseDef)form;
		else cd = new ClauseDef(form, this, name);
		cd.checkVarUse(false);
		form = cd;
		ctx.setProduction(typeTerm().getName(),cd);
		ctx.parseMap.put(cd.getElemTypes(), cd);

		GrmRule r = new GrmRule(GrmUtil.getStartSymbol(), cd.getSymbols(), cd);
		ctx.ruleSet.add(r);

		ctx.setJudgment(name, this);
	}

	protected void setForm(ClauseDef f) {
		form = f;
	}

	public void typecheck(Context ctx) {

		NonTerminal contextNT = null;

		if (assume != null) {
			assume.typecheck(ctx);
		}

		if (isAbstract) {
			Util.verify(rules.isEmpty(), "not caught by parser?");
		}

		for (Element f : form.getElements()) {
			if (f instanceof NonTerminal) {
				SyntaxDeclaration s = ((NonTerminal)f).getType();
				if (s.isInContextForm()) {
					// we permit and/or clauses to have multiple contexts,
					// but the system ensures they are always the same
					if (!(this instanceof AndOrJudgment) && contextNT != null) {
						ErrorHandler.recoverableError(Errors.DUPLICATE_ASSUMES, contextNT.toString(), f);
					}
					contextNT = (NonTerminal)f;
				}
			}
		}

		if (assume != null) {
			SyntaxDeclaration assumeSyntax = assume.getType();
			String fixInfo = "assumes " + assume + "\n" +
					(contextNT == null ? "" : "assumes " + contextNT.toString());
			if (assumeSyntax == null) {
				// no error needed (already errored)
			} else if (contextNT == null || !assumeSyntax.equals(contextNT.getType())) {
				Errors error = assumeSyntax.isInContextForm() ? Errors.EXTRANEOUS_ASSUMES : Errors.ILLEGAL_ASSUMES;
				ErrorHandler.recoverableError(error, assume, fixInfo);
			} else if (!contextNT.equals(assume)) {
				ErrorHandler.recoverableError(Errors.WRONG_ASSUMES, assume, fixInfo);
			}
			if (contextNT == null) assume = null;
			else assume = contextNT; // fix for now
		} else if (contextNT != null && !Util.X_CONTEXT_IS_SYNTAX) {
			ErrorHandler.recoverableError(Errors.MISSING_ASSUMES, "'assumes " + contextNT + "'", this, "assumes " + contextNT);
		}
		
		for (Rule r : getRules()) {
			try {
				r.typecheck(ctx, this);
			} catch (SASyLFError e) {
				// go on to next error
			}
		}
	}

	/**
	 * Find a rule for this judgment using the given constant.
	 * @param c constant to check
	 * @return rule using this constant, or null if none found
	 */
	public CanBeCase findRule(Constant c) {
		for (Rule r : rules) {
			if (r.getRuleAppConstant() == c) return r;
		}
		return null;
	}
	
	@Override
	public final Constant typeTerm() {
		if (term == null)
			term = computeTypeTerm(); 
		return term;
	}

	/**
	 * Compute the LF base type for elements of this type.
	 * In practice, this constructs a unique {@link Constant} instance.
	 * @return LF base type family for this judgment.
	 */
	protected Constant computeTypeTerm() {
		return new Constant(name, Constant.TYPE);
	}

	private Constant term = null;

	@Override
	public void analyze(Context ctx, Element target, Node source, 
			Map<CanBeCase, Set<Pair<Term, Substitution>>>  result) {
		if (isAbstract()) {
			ErrorHandler.error(Errors.CASE_SUBJECT_ABSTRACT, getName(), source);
		}
		Util.verify(target instanceof ClauseUse, "Judgment#analyze called with bad element: " + target);
		ClauseUse cl = (ClauseUse)target;
		Term t = ctx.toTerm(target);
		for (Rule rule : getRules()) {
			Set<Pair<Term,Substitution>> caseResult = null;
			if (rule.isInterfaceOK()) {
				caseResult = rule.caseAnalyze(ctx, t, cl, source);
			}
			if (caseResult == null || caseResult.isEmpty()) continue; // caseResult = Collections.emptySet(); 
			result.put(rule, caseResult);
		}
	}

	/**
	 * Get the original declaration of this judgment. If this judgment is a renaming,
	 * then this method will return the original judgment that was renamed.
	 * If this judgment is not a renaming, then this method will return this judgment.
	 * @return the original declaration of this judgment
	 */
	public Judgment getOriginalDeclaration() {
		// This judgment is its own original declaration
		return this;
	}

	@Override
	public void substitute(SubstitutionData sd) {
		if (sd.didSubstituteFor(this)) return;
		sd.setSubstitutedFor(this);
		
		for (Rule r : rules) {
			r.substitute(sd);
		}

		/*
		 * The name of the judgment is not substituted because we want the name to stay the
		 * same within the module.
		 */

		form.substitute(sd);

		/*
		 * Since we are substituting in this Judgment, its term is no longer the same.
		 * So, set it to null, and the next time typeTerm() is called, it will be recomputed
		 * and cached in term.
		 */

		term = null;
			
		if (assume != null) {
			assume.substitute(sd);
		}

	}

	@Override
	public Judgment copy(CopyData cd) {
		if (cd.containsCopyFor(this)) return (Judgment) cd.getCopyFor(this);
		Judgment clone = (Judgment) super.clone();
		
		cd.addCopyFor(this, clone);
	

		List<Rule> newRules = new ArrayList<>();
		for (Rule r : rules) {
			newRules.add(r.copy(cd));
		}
		clone.rules = newRules;
		
		clone.form = form.copy(cd);

		clone.term = null;

		if (clone.assume != null) {
			clone.assume = (NonTerminal) assume.copy(cd);
		}
		
		return clone;
		
	}

	@Override
	public Optional<SubstitutionData> matchesParam(
		ModuleArgument paramModArg,
		ModulePart mp,
		Map<Syntax, Syntax> paramToArgSyntax,
		Map<Judgment, Judgment> paramToArgJudgment) {
			
			Judgment arg = getOriginalDeclaration();

			if (!(paramModArg instanceof Judgment)) {
				// the wrong type of argument has been provided

				String argKind = this.getKind();
				String paramKind = paramModArg.getKind();
				
				// throw an exception

				ErrorHandler.modArgTypeMismatch(argKind, paramKind, mp);
				return Optional.empty();
			}

			// they are of the same type, so cast the parameter to a Judgment

			Judgment param = (Judgment) paramModArg;

			// now, we need to check if the two judgments are compatible with eachother
			
			if (paramToArgJudgment.containsKey(this)) {
				// check if the parameter judgment is bound to the same argument judgment

				Judgment boundJudgment = paramToArgJudgment.get(this).getOriginalDeclaration();

				if (boundJudgment != param) {
					ErrorHandler.modArgMismatchJudgment(arg, param, boundJudgment, mp);
					return Optional.empty();
				}

			}

			// otherwise, bind the param to arg in the map

			paramToArgJudgment.put(param, arg);

			// verify that the forms of the judgments have the same structure

			Clause argForm = arg.getForm();
			Clause paramForm = param.getForm();

			Clause.checkClauseSameStructure(
				paramForm,
				argForm,
				paramToArgSyntax,
				paramToArgJudgment,
				new HashMap<String, String>(),
				mp
			);

			if (!param.isAbstract()) {
				// This is a concrete judgment declaration, so there are rules to check
				// check that param and arg have the same number of rules

				List<Rule> paramRules = param.getRules();
				List<Rule> argRules = arg.getRules();

				if (paramRules.size() != argRules.size()) {
					ErrorHandler.modArgumentJudgmentWrongNumRules(arg, param, mp);
					return Optional.empty();
				}

				// check that each pair of rules has the same structure

				for (int j = 0; j < paramRules.size(); j++) {
					Rule paramRule = paramRules.get(j);
					Rule argRule = argRules.get(j);

					// check the premises of the rules
					List<Clause> paramPremises = paramRule.getPremises();
					List<Clause> argPremises = argRule.getPremises();
					if (paramPremises.size() != argPremises.size()) {
						ErrorHandler.modArgRuleWrongNumPremises(argRule, paramRule, mp);
					}

					// check that each pair of premises has the same structure

					Map<String, String> nonTerminalMapping = new HashMap<String, String>();

					for (int k = 0; k < paramPremises.size(); k++) {
						Clause paramPremise = paramPremises.get(k);
						Clause argPremise = argPremises.get(k);
						Clause.checkClauseSameStructure(
							paramPremise,
							argPremise,
							paramToArgSyntax,
							paramToArgJudgment,
							nonTerminalMapping,
							mp
						);
					}

					// check the conclusions

					Clause paramConclusion = paramRule.getConclusion();
					Clause argConclusion = argRule.getConclusion();

					Clause.checkClauseSameStructure(
						paramConclusion,
						argConclusion,
						paramToArgSyntax,
						paramToArgJudgment,
						nonTerminalMapping,
						mp
					);
				}

			}

			// they match
			
			SubstitutionData sd = new SubstitutionData(param.getName(), arg.getName(), arg);
			return Optional.of(sd);

	}

	@Override
	public boolean provideTo(CompUnit cu, ModulePart mp, Map<Syntax, Syntax> paramToArgSyntax, Map<Judgment, Judgment> paramToArgJudgment) {

		// get the next parameter of cu
		Optional<ModuleArgument> paramOpt = cu.getNextParam();
		if (paramOpt.isEmpty()) return false;
		ModuleArgument param = paramOpt.get();

		Optional<SubstitutionData> sdOpt = matchesParam(param, mp, paramToArgSyntax, paramToArgJudgment);

		if (sdOpt.isPresent()) {
			// the parameter matches the judgment and we can substitute

			SubstitutionData sd = sdOpt.get();

			// substitute the judgment
			
			cu.substitute(sd);

			return true;
		}
		else {
			return false;
		}

	}

	@Override
	public String getKind() {
		return "judgment";
	}

}

