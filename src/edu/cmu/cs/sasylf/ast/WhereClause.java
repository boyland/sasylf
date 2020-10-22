package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Util.COMP_WHERE;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.util.DefaultSpan;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.Span;
import edu.cmu.cs.sasylf.util.Util;

/** 
 * User-written substitutions.
 * See original paper by Ariotti and Boyland.
 */
public class WhereClause extends Node {
	
	private final List<Pair<Element,Clause>> clauses;
	private Substitution computed;
	
	public WhereClause(Location l) {
		super(l);
		clauses = new ArrayList<Pair<Element,Clause>>();
	}
	
	@Override
	public void prettyPrint(PrintWriter out) {
		out.println("where ");
		boolean first = true;
		for (Pair<Element,Clause> p : clauses) {
			if (first) first = false;
			else out.println(" and ");
			out.println(p.first + " := " + p.second);
		}
	}

	/**
	 * Add a new where clause LHS := RHS
	 * @param lhs lhs of the clause
	 * @param rhs rhs of the clause
	 */
	public void add(Element lhs, Clause rhs) {
		clauses.add(new Pair<Element,Clause>(lhs,rhs));
	}
	
	/**
	 * Convert where clauses to a substitution.
	 * It's important to call this method once before the context's substitution
	 * includes SASylF's own analysis of the case.
	 * @param ctx context to check using, must not be null
	 * @return where clauses converted to a substitution, not null
	 * @throws SASyLFError in the clauses are not wellFormed
	 */
	public Substitution typecheck(Context ctx) throws SASyLFError {
		if (computed != null) return computed;
		Substitution result = new Substitution();
		nextUserClause:
		for (Pair<Element, Clause> userWC : clauses) {

			// parse the LHS; should either be a NonTerminal or a Binding
			Element lhsElement = userWC.first.typecheck(ctx);
			
			// make sure the LHS isn't a judgment (probably would have failed the pattern match already)
			if (!(lhsElement instanceof NonTerminal) && !(lhsElement instanceof Binding)) {
				ErrorHandler.recoverableError(
					"The left-hand side of this where clause is not the correct form: " +
						lhsElement, userWC.first);
				continue nextUserClause;
			}
			NonTerminal lhsNT;
			if (lhsElement instanceof Binding) {
				lhsNT = ((Binding)lhsElement).getNonTerminal();
			} else {
				lhsNT = (NonTerminal)lhsElement; 
			}
			
			List<Pair<String, Term>> lhsBindings = new ArrayList<Pair<String, Term>>();
			List<Term> lhsArgTypes = new ArrayList<Term>();
			List<String> lhsArgNames = new ArrayList<String>();
			if (lhsElement instanceof Binding) { // the LHS has arguments listed
				for (Element e : ((Binding)lhsElement).getElements()) {
					if (e instanceof Variable) { // argument is a Variable => can use on the RHS
						Variable v = (Variable)e;
						// save for later, for parsing RHS
						String name = v.getSymbol();
						Constant type = v.getType().typeTerm();
						lhsBindings.add(new Pair<String, Term>(name, type));
					    lhsArgTypes.add(type);
					    lhsArgNames.add(name);
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

			Element rhsElement;
			try {
				rhsElement = userWC.second.typecheck(ctx).computeClause(ctx, lhsNT);
			} catch (SASyLFError e) {
				continue nextUserClause;
			}
			
			Term rhsTerm = Term.wrapWithLambdas(rhsElement.asTerm(lhsBindings), lhsArgTypes, lhsArgNames);
			rhsTerm = rhsTerm.substitute(ctx.currentSub);
			rhsTerm = rhsTerm.substitute(result);
			
			
			FreeVar lhsVar = (FreeVar) lhsNT.asTerm();
			if (result.getSubstituted(lhsVar) != null) {
				ErrorHandler.recoverableError("Where clause for " + lhsVar + " already written.", userWC.first);
			}
			
			if (lhsVar.equals(rhsTerm)) { // NOP substitution
				continue nextUserClause;
			}
			
			if (rhsTerm.getFreeVariables().contains(lhsVar)) {
				ErrorHandler.recoverableError("Where clause for " + lhsVar + " includes this same variable", userWC.second);
			}
			
			result.add(lhsVar, rhsTerm);
		}
		
		return computed=result;
	}
	
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
	public void checkWhereClauses(
			Context ctx, Term cas, Term rcc, Substitution su,
			Span errorSpan) {
		List<Pair<Element,Clause>> userWhereClauses = clauses;
		
		Substitution userSub = typecheck(ctx);
		
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
		
		Set<FreeVar> fvCAS = cas.getFreeVariables();
		
		Set<FreeVar> domain = new HashSet<FreeVar>(su.getDomain());
		for (FreeVar fv : domain) {
			if (fv.isGenerated() || !fvCAS.contains(fv)) su.remove(fv);
		}
		
		if (su.equals(userSub)) return; // everything matches!
		if (userSub.isEmpty() && !Util.COMP_WHERE) return;
		// ErrorHandler.warning("user sub " + userSub + " doesn't exactly match " + su, errorSpan);
		Util.debug("IW: cas = ",cas,"\n    rcc = ", rcc, "\n    su = ",su);
		
		
		// create map of vars to check clauses for, from s_u
		// used to check for missing clauses
		Map<FreeVar, Boolean> checked = new HashMap<FreeVar, Boolean>();
		
		int clausesNeeded = 0;
		for (FreeVar v : su.getMap().keySet()) {
			checked.put(v, false);
			Util.debug("where clause needed for ", v);
			clausesNeeded++;
		}

		// check if no clauses are needed, but the user wrote some
		if (clausesNeeded == 0 && !userWhereClauses.isEmpty()) {
			ErrorHandler.recoverableError(
				"No where clauses are needed here.", userWhereClauses.get(0).first
			);
			return;
		}
		
		// loop over the where clauses, for good error placement
		nextUserClause:
		for (Pair<Element, Clause> userWC : userWhereClauses) {

			// parse the LHS; should either be a NonTerminal or a Binding
			Element lhsElement = userWC.first.typecheck(ctx);
			
			// make sure the LHS isn't a judgment (probably would have failed already)
			if (!(lhsElement instanceof NonTerminal) && !(lhsElement instanceof Binding)) {
				ErrorHandler.recoverableError(
					"The left-hand side of this where clause is not the correct form: " +
						lhsElement, userWC.first);
				continue nextUserClause;
			}

			NonTerminal lhsNT;
			if (lhsElement instanceof NonTerminal) lhsNT = (NonTerminal)lhsElement;
			else lhsNT = ((Binding)lhsElement).getNonTerminal();
			
			String lhsVarName = lhsNT.toString();
			
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
					"Where clause for " + lhsVarName + " already written.", userWC.first
				);
			}
			checked.put(matchingVar, true);
			
			Term rhsUser = userSub.getSubstituted(matchingVar);
			Util.debug("For " + matchingVar + ", Correct = ",rhsCorrect,",User = ",rhsUser);
			if (rhsUser == null) rhsUser = matchingVar; // can happen if we remove a NOP subst from rhsUser

			int userLambdas = rhsUser.countLambdas();
			int correctLambdas = rhsCorrect.countLambdas();
			if (userLambdas != correctLambdas) {
				// generate helpful error message:
				String error;
				if (correctLambdas == 0) {
					error = "Arguments not needed for first-order where clause. " +
							"Left-hand side should be " + matchingVar;
						
				} else if (userLambdas == 0) {
					error = "Arguments needed for variable with 'holes'. " +
							"Left-hand side should be " + makeLhsString(matchingVar,rhsCorrect);
				} else {
					error = " arguments bound on the left-hand side of the clause. " +
							"Should have " + correctLambdas + " binding" + (correctLambdas > 1 ? "s" : "") +
									", not " + userLambdas;
					if (correctLambdas < userLambdas) {
						error = "Too many" + error;
					} else {
						error = "Not enough" + error;
					}
				}
				ErrorHandler.recoverableError(error, userWC.first);
				continue nextUserClause;
			}

			// Handle a generated variable in rhsCorrect
			// replaced with a fresh variable in rhsUser
			try {
				Set<FreeVar> freeVars = rhsUser.getFreeVariables();
				Set<FreeVar> newVars = new HashSet<FreeVar>();
				Substitution unifyingSub = rhsUser.unify(rhsCorrect);
				Set<FreeVar> needSpecifics = unifyingSub.selectUnavoidable(freeVars);
				Util.debug("unifyingSub = ",unifyingSub);
				Set<FreeVar> correctVars = rhsCorrect.getFreeVariables();
				Util.debug(" free are ", correctVars);
				if (needSpecifics.size() > 0) {
					ErrorHandler.recoverableError("replacement too general, perhaps these variables should be replaced with something specific: " + needSpecifics, userWC.second);
					continue nextUserClause;
				}
				for (Map.Entry<FreeVar, Term> e : unifyingSub.getMap().entrySet()) {
					if (!e.getKey().isGenerated()) {
						ErrorHandler.recoverableError("replacement too specific, imposes constraint on free variable " + e.getKey(), userWC.second);
						continue nextUserClause;
					}
					if (!correctVars.contains(e.getKey())) {
						// this variable is irrelevant
						continue;
					}
					FreeVar fv = e.getValue().getEtaPermutedEquivFreeVar(null, null);
					if (fv == null || ctx.isKnown(fv.getName())) {
						Term body = Term.getWrappingAbstractions(e.getValue(), null);
						if (body instanceof Application) {
							body = ((Application)body).getFunction();
						}
						String vals = TermPrinter.toString(ctx, null, userWC.second.getLocation(), body, false);
						ErrorHandler.recoverableError("Replacement too specific, probably needs a fresh variable instead of " + vals, userWC.second);
						continue nextUserClause;
					}
					if (!newVars.add(fv)) {
						ErrorHandler.recoverableError("New variable " + fv + " should only be used once.", userWC.second);
						continue nextUserClause;
					}
				}
				rhsCorrect = rhsCorrect.substitute(unifyingSub);
				ctx.composeSub(unifyingSub);
			} catch (UnificationFailed e) {
				// can't handle discrepancy
			}
			
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
				String suggestion;
				try {
					suggestion = termPrinter.asElement(rhsCorrect).toString();
				} catch (RuntimeException ex) {
					ErrorHandler.recoverableError(Errors.INTERNAL_ERROR, ": " + ex.toString(), this);
					suggestion = "(interal) " + rhsCorrect.toString();
				}
				int i = suggestion.indexOf("assumes"); // don't print "assumes ..."
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
					missing.add(makeLhsString(lhs,su.getSubstituted(lhs)));
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
	
	/**
	 * Make a string of the form t[x,x'] to represent a lhs
	 * possibly with bindings.
	 * @param lhsVar variable being mapped
	 * @param rhs term it's being mapped to (to know what bindings to require)
	 * @return string for error message.
	 */
	private static String makeLhsString(FreeVar lhsVar, Term rhs) {
		StringBuilder sb = new StringBuilder(lhsVar.toString());
		
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
		return sb.toString();
	}
	
	/**
	 * Create an absent where clause
	 */
	public WhereClause() {
		super((Location)null);
		clauses = Collections.emptyList();
	}

	/**
	 * Return true if there are no user-supplied where clauses.
	 * @return whether this is an absent where clause
	 */
	public boolean isEmpty() {
		return clauses.isEmpty();
	}
}
