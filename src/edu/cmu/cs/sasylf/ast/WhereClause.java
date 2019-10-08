package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Util.COMP_WHERE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.util.DefaultSpan;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.Span;

/** 
 * Utility class for where clause operations 
 * @author Michael Ariotti
 */
public class WhereClause {
	
	/**
	 * Verifies the user-written where clauses for this RuleCase.<br>
	 * Checks for the following errors:
	 * <ul>
	 * <li>unneeded LHS</li>
	 * <li>duplicate LHS</li>
	 * <li>LHS argument errors</li>
	 * <li>incorrect RHS (though alpha-equivalence is allowed)</li>
	 * <li>missing where clauses (ignored entirely if "compwhere" is not enabled)</li>
	 * </ul>
	 * If a second-order RHS is incorrect, suggests correct RHS based on the user's
	 * bound variables.<br>
	 * Missing clauses are listed by LHS; second-order missing vars show arguments.
	 * @param userWhereClauses clauses to check, as parsed from the source
	 * @param ctx global and local context, used for parsing
	 * @param cas case analysis subject term
	 * @param rcc rule case conclusion term; this is null for inversions
	 * @param su substitution representing restrictions imposed on free variables; when
	 *   these are variables in the CAS, they need where clauses; this is null for rule cases
	 *   (computed here)
	 * @param errorSpan a span representing the full block of code where the clauses apply;
	 *   marked when where clauses are missing
	 */
	public static void checkWhereClauses(
			List<Pair<Element, Clause>> userWhereClauses,
			Context ctx, Term cas, Term rcc, Substitution su,
			Span errorSpan) {
		
		/*
		 * Unification fails here if the context changes from CAS to RCC,
		 * because the CAS doesn't include adaptation.
		 * For now, skip the whole case, because other where clauses will fail
		 * to verify correctly also.
		 * For inversions, su comes in precomputed.
		 * Only give this error if the user has written where clauses.
		 */
		if (su == null) {
			try {
				su = cas.unify(rcc);
			}
			catch (UnificationFailed ex) {
				if (!userWhereClauses.isEmpty() || COMP_WHERE) {
					ErrorHandler.warning(
						"SASyLF cannot (yet) produce or verify where clauses for this case",
						errorSpan);
				}
				return;
			}
		}
		
		// assure all internal RHS's are in terms of user-created variables
		// for inversions (which don't have an RCC), this step is already done
		if (rcc != null) {
			su.selectUnavoidable(rcc.getFreeVariables());
		}
		
		// create map of vars to check clauses for, from s_u
		// used to check for missing clauses
		Map<FreeVar, Boolean> checked = new HashMap<FreeVar, Boolean>();
		Set<FreeVar> fvCAS = cas.getFreeVariables();
		int clausesNeeded = 0;
		for (FreeVar v : su.getMap().keySet())
			// TODO not sure how generated variables can get into the CAS...
			if (fvCAS.contains(v) && !v.isGenerated()) { // this variable should have a where clause
				checked.put(v, false);
				clausesNeeded++;
			}
		
		// check if no clauses are needed, but the user wrote some
		if (clausesNeeded == 0 && !userWhereClauses.isEmpty()) {
			ErrorHandler.recoverableError(
				"No where clauses are needed here.", errorSpan
			);
			return;
		}
		
		// check the user clauses
		nextUserClause:
		for (Pair<Element, Clause> userWC : userWhereClauses) {
			
			// sanity check: pattern match the LHS
			// Clause.toString() removes meaningless whitespace within and around LHS
			String originalLHS = userWC.first.toString();
			String lhsPattern = 
				"[(](\\p{L})+(\\d|')*(\\[[(](\\p{L})+(\\d|')*[)]\\]|\\[(\\p{L})+(\\d|')*\\])*[)]"+
				  "|(\\p{L})+(\\d|')*(\\[[(](\\p{L})+(\\d|')*[)]\\]|\\[(\\p{L})+(\\d|')*\\])*";
			if (!originalLHS.matches(lhsPattern)) {
				ErrorHandler.recoverableError(
					"The left-hand side of this where clause is not the correct form: " +
						originalLHS, userWC.first);
				continue nextUserClause;
			}
			
			// extract the variable name from the LHS string
			String lhsVarName = originalLHS;
			if (lhsVarName.startsWith("(")) // no parens
				lhsVarName = lhsVarName.substring(1, lhsVarName.length() - 1);
			int i = lhsVarName.indexOf('[');
			if (i != -1) // just name, no args
				lhsVarName = lhsVarName.substring(0, i);

			// check if the LHS variable name is known in the (local) context
			if (!ctx.isLocallyKnown(lhsVarName)) {
				ErrorHandler.recoverableError(
					"The variable name is unrecognized in the local context: " +
						lhsVarName, userWC.first
				);
				continue nextUserClause;
			}
		
			// find the matching variable in the CAS
			FreeVar matchingVar = null;
			for (FreeVar fv : fvCAS)
				if (fv.getName().equals(lhsVarName))
					matchingVar = fv;
			if (matchingVar == null) {
				ErrorHandler.recoverableError(
					"Where clause not needed for (not a member of the case analysis subject): " +
						lhsVarName, userWC.first
				);
				continue nextUserClause;
			}
			
			// make sure this LHS is mapped by s_u
			Term rhsCorrect = su.getSubstituted(matchingVar);
			if (rhsCorrect == null) {
				ErrorHandler.recoverableError(
					"Where clause not needed for: " + matchingVar, userWC.first
				);
				continue nextUserClause;
			}

			// make sure this isn't a duplicate clause (will check the RHS anyway, tentatively)
			if (checked.get(matchingVar)) {
				ErrorHandler.recoverableError(
					"Where clause for " + originalLHS + " already written.", userWC.first
				);
			}
			checked.put(matchingVar, true);
			
			// parse the LHS; should either be a NonTerminal or a Binding
			Element lhsElement = userWC.first.typecheck(ctx);
			
			// make sure the LHS isn't a judgment (probably would have failed the pattern match already)
			if (!(lhsElement instanceof NonTerminal) && !(lhsElement instanceof Binding)) {
				ErrorHandler.recoverableError(
					"The left-hand side of this where clause is not the correct form: " +
						lhsElement, userWC.first);
				continue nextUserClause;
			}

			// parse the LHS's bindings, if present
			List<Pair<String, Term>> lhsBindings = new ArrayList<Pair<String, Term>>();
			List<Term> lhsArgTypes = new ArrayList<Term>();
			List<String> lhsArgNames = new ArrayList<String>();
			if (lhsElement instanceof Binding) { // the LHS has arguments listed
				for (Element e : ((Binding)lhsElement).getElements()) {
					if (e instanceof Variable) { // argument is a Variable => can use on the RHS
						Variable v = (Variable)e;
						// save for later, for parsing RHS
						Pair<String, Term> pair = 
							new Pair<String, Term>(v.getSymbol(), v.getType().typeTerm());
					    lhsBindings.add(pair);
					    lhsArgTypes.add(pair.second);
					    lhsArgNames.add(pair.first);
					}
					else {
						// non-variable bindings on the LHS are not allowed
						ErrorHandler.recoverableError(
							"Non-variable bound on the left-hand side of the clause: " + e,
							userWC.first
						);
						continue nextUserClause;
					}
				}
			}
			
			// check number of LHS arguments; if wrong, don't check the RHS
			int numArgs = lhsBindings.size();
			if (rhsCorrect.getType() instanceof Abstraction) {
				// if the clause is second-order, check the number of arguments
				int lambdas = rhsCorrect.countLambdas();
				String error = 
					" arguments bound on the left-hand side of the clause. " +
					"Should have " + lambdas + " binding" + (lambdas > 1 ? "s" : "") +
					", not " + numArgs;
				if (lambdas < numArgs) {
					ErrorHandler.recoverableError(
						"Too many" + error, userWC.first
					);
					continue nextUserClause;
				}
				else if (lambdas > numArgs) {
					ErrorHandler.recoverableError(
						"Not enough" + error, userWC.first
					);
					continue nextUserClause;
				}
			}
			// make sure this first-order clause doesn't have arguments
			else if (numArgs > 0) {
				ErrorHandler.recoverableError(
					"Arguments not needed for first-order where clause. " +
					"Left-hand side should be: " + matchingVar, userWC.first
				);
				continue nextUserClause;
			}
			
			// infer the grammar rule used to parse the LHS from its
			//   appearance in the CAS;
			// create dummy syntax rule from matching var's type
			Term varType = matchingVar.getType();
			while (varType instanceof Abstraction)
				varType = ((Abstraction)varType).getBody();
			edu.cmu.cs.sasylf.grammar.Rule matchRule = 
				new edu.cmu.cs.sasylf.ast.grammar.GrmRule(
					new edu.cmu.cs.sasylf.ast.grammar.GrmNonTerminal(
						varType.toString()), null);
			
			// pass the (topmost) grammar rule used to parse the LHS to help
			// disambiguate parses of the RHS
			Element rhsElement = ((Clause)userWC.second.typecheck(ctx)).computeClause(ctx, false, matchRule);
			
			// compute the term of the RHS with the LHS's additional bindings
			// wrap those extra bindings around the computed term
			Term rhsUser = Term.wrapWithLambdas(rhsElement.asTerm(lhsBindings), lhsArgTypes, lhsArgNames);
			
			// check for LF equality; allows alpha-equivalence
			if (!rhsUser.equals(rhsCorrect)) {
				
				// collect bound variables from wrapped version of user RHS
				List<Pair<String, Term>> userBindings = rhsUser.getBoundVariables();
				
				// check for reused bound variable name among user bindings
				Map<String, Boolean> seenVars = new HashMap<String, Boolean>();
				for (Pair<String, Term> pair : userBindings) {
					String varName = pair.first;
					if (seenVars.get(varName) != null) {
						ErrorHandler.recoverableError(
							"Variable bound more than once in where clause: " + varName,
							new DefaultSpan(userWC.first.getLocation(), 
								userWC.second.getEndLocation())
						);
						continue nextUserClause;
					}
					seenVars.put(varName, true);
				}
				
				// remake correct RHS in terms of user's bindings (for better suggestion)
				rhsCorrect = rhsCorrect.remakeWithBoundVars(userBindings);
				
				// "unparse" suggestion
				// false here maintains new binding names in suggestion, and free variable names
				TermPrinter termPrinter = new TermPrinter(ctx, null, errorSpan.getLocation(), false);
				String suggestion = termPrinter.asElement(rhsCorrect).toString();
				i = suggestion.indexOf("assumes"); // don't print "assumes ..."
				if (i != -1)
					suggestion = suggestion.substring(0, i - 1);
				
				ErrorHandler.recoverableError(
					"Right-hand side of where clause for " + matchingVar + " is incorrect. " +
					"Should be: " + suggestion, userWC.second
				);
			}
		}
		
		// check for missing where clauses (if they are compulsory)
		if (COMP_WHERE) {
			Set<String> missing = new HashSet<String>();
			for (FreeVar lhs : checked.keySet()) {
				if (!checked.get(lhs)) {
					StringBuilder sb = new StringBuilder(lhs.toString());
					Term rhs = su.getSubstituted(lhs);
					
					// if (correct) RHS is an abstraction, add bindings to
					// missing LHS name for each outer lambda
					if (rhs instanceof Abstraction) {
						// create a new valid name for each binding
						Map<String, Boolean> seenVars = new HashMap<String, Boolean>();
						while (rhs instanceof Abstraction) {
							Abstraction abs = (Abstraction)rhs;
							String nextName = abs.varName;
							while (seenVars.get(nextName) != null)
								nextName += "'";
							seenVars.put(nextName, true);
							sb.append("[" + nextName + "]");
							rhs = abs.getBody();
						}
					}
					missing.add(sb.toString());
				}
			}
			if (!missing.isEmpty()) {
				String list = missing.toString();
				ErrorHandler.warning( // don't print brackets on list
					"Missing where clause" +
					(missing.size() > 1 ? "s" : "") +
					" for: " + list.substring(1, list.length() - 1), errorSpan
				);
			}
		}
	}
	
	private WhereClause() {}
}
