package edu.cmu.cs.sasylf.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.sasylf.ast.Clause;
import edu.cmu.cs.sasylf.ast.Judgment;
import edu.cmu.cs.sasylf.ast.ModulePart;
import edu.cmu.cs.sasylf.ast.NonTerminal;
import edu.cmu.cs.sasylf.ast.Rule;
import edu.cmu.cs.sasylf.ast.Syntax;
import edu.cmu.cs.sasylf.ast.SyntaxDeclaration;
import edu.cmu.cs.sasylf.ast.Theorem;
import edu.cmu.cs.sasylf.term.FreeVar;


public class ErrorHandler {
	/**
	 * Report an error or warning.
	 * <p>
	 * In the description of the parameters, "default" means the value to be used
	 * if "null" is passed for the parameter.
	 * @param errorType error type, defaults to UNSPECIFIED
	 * @param msg extra information associated with error, defaults to "".
	 * This text should be language independent to aid (future) internationalization,
	 * unless this is an {@link Errors#INTERNAL_ERROR}.
	 * Usually it is redundant to include text from within the span covered by this report.
	 * @param loc location of the error, defaults to the last span.
	 * The default should be avoided, since very few places in the code base update the last span.
	 * @param debugInfo extra information used for --LF flag, also used for quick fixes, optionally null.
	 * This information need not be language independent.
	 * @param isError whether an error (or just a warning)
	 * @param throwable whether this method should throw a SASyLF error after reporting the error
	 */
	public static void report(Errors errorType, String msg, Span loc, String debugInfo, boolean isError, boolean throwable) {
		if (errorType == null) errorType = Errors.UNSPECIFIED;
		if (msg == null) msg = "";
		if (loc == null) loc = lastSpan.get();
		ErrorReport rep = new ErrorReport(errorType, msg, loc, debugInfo, isError);
		report(rep);
		if (throwable) {
			throw new SASyLFError(rep);
		}
	}

	/**
	 * Report something to be logged and perhaps printed.
	 * @param r report to report, must not be null
	 */
	public static void report(Report r) {
		logReport(r);
		if (r.shouldPrint()) {
			System.err.println(r.formatMessage());
			String extra = r.getExtraInformation();
			if (extra != null && Util.EXTRA_ERROR_INFO) {
				System.err.println(extra);
			}
		}
	}
	
	public static void recoverableError(Errors x, Span obj) {
		recoverableError(x,null,obj,null);
	}

	public static void recoverableError(Errors x, String msg, Span obj) {
		recoverableError(x,msg,obj,null);
	}

	public static void recoverableError(Errors x, Span obj, String info) {
		recoverableError(x,null,obj,info);
	}

	public static void recoverableError(Errors errorType, String msg, Span obj, String debugInfo) {
		report(errorType, msg, obj, debugInfo, true, false);
	}


	public static void warning(Errors errorType, Span obj) {
		warning(errorType, null, obj, null);
	}

	public static void warning(Errors errorType, String added, Span obj) {
		warning(errorType, added, obj, null);
	}
	
	public static void warning(Errors errorType, Span obj, String fixInfo) {
		warning(errorType, null, obj, fixInfo);
	}

	public static void warning(Errors errorType, String msg, Span obj, String debugInfo) {
		report(errorType, msg, obj, debugInfo, false, false);
	}
	

	public static void error(Errors errorType, Span obj) {
		error(errorType, null, obj, null);
	}

	public static void error(Errors errorType, Span obj, String debugInfo) {
		error(errorType, null, obj, debugInfo);
	}

	public static void error(Errors errorType, String msg, Span obj) {
		error(errorType, msg, obj, null);
	}

	public static void error(Errors errorType, String msg, Span obj, String debugInfo) {
		report(errorType, msg, obj, debugInfo, true, true);
	}
	

	private static void logReport(Report r) {
		reports.get().add(r);
	}
	
	public static List<Report> getReports() { return reports.get(); }
	
	/**
	 * Run the given action with a new set of error reports.
	 * The current reports are preserved unchanged.
	 * @param r action to perform, must not be null
	 * @return reports that occurred before a successful end.
	 */
	public static List<Report> withFreshReports(Runnable r) {
		List<Report> saved = reports.get();
		reports.remove();
		List<Report> result = reports.get();
		try {
			r.run();
		} finally {
			reports.set(saved);
		}
		return result;
	}
	
	/**
	 * Start a new check session.
	 * This starts the reports on a fresh list and clears the
	 * free variables. Mixing two functions like this is a bad idea.
	 * It also doesn't give a way to protect existing problems
	 * @deprecated use {@link #withFreshReports(Runnable)} and 
	 * call {@link FreeVar.reinit} directly.
	 */
	@Deprecated
	public static void clearAll() {
		reports.remove();
		FreeVar.reinit();
	}
	
	public static int getErrorCount() {
		int errorCount = 0;
		for (Report r : reports.get()) {
			if (r.isError()) ++errorCount;
		}
		return errorCount;
	}
	public static int getWarningCount() { 
		int warnCount = 0;
		for (Report r : reports.get()) {
			if (r.isError()) continue;
			if (!(r instanceof ErrorReport)) continue;
			++warnCount;
		}
		return warnCount;
	}

	public static void recordLastSpan(Span s) {
		if (s != null) {
			lastSpan.set(s);
		}
	}

	private static ThreadLocal<Span> lastSpan = new ThreadLocal<Span>();

	private static ThreadLocal<List<Report>> reports = new ThreadLocal<List<Report>>(){
		@Override
		protected List<Report> initialValue() {
			return new ArrayList<Report>();
		}
	};

	public static Location lexicalErrorAsLocation(String file, String error) {
		try {
			int lind = error.indexOf("line ");
			int cind = error.indexOf(", column ");
			int eind = error.indexOf(".", cind+1);
			int line = Integer.parseInt(error.substring(lind+5, cind));
			int column = Integer.parseInt(error.substring(cind+9, eind));
			return new Location(file,line,column);
		} catch (RuntimeException e) {
			return new Location(file,0,0);
		}
	}

	public static void modArgInvalid(String argType, ModulePart modulePart) {

		String errorMessage = "A module argument must be a syntax, judgment, rule, or theorem, but " + argType + " was provided.";

		ErrorHandler.error(Errors.MOD_ARG_INVALID, errorMessage, modulePart);

	}

	public static void modArgMismatchSyntax(Syntax argSyntax, Syntax paramSyntax, Syntax whatParamIsBoundTo, ModulePart modulePart) {
		String errorMessage = "Syntax " + argSyntax + " is being assigned to " + paramSyntax + ", but " 
			+ paramSyntax + " is already bound to " + whatParamIsBoundTo + ".";

		ErrorHandler.error(Errors.MOD_ARG_MISMATCH_SYNTAX, errorMessage, modulePart);
	}

	public static void modArgSyntaxWrongNumProductions(SyntaxDeclaration argSyntax, SyntaxDeclaration paramSyntax, ModulePart modulePart) {
		int argNumProductions = argSyntax.getClauses().size();
		int paramNumProductions = paramSyntax.getClauses().size();
		String errorMessage = "Module syntax argument " + argSyntax + " has " + argNumProductions 
			+ " productions, but " + paramNumProductions + " productions are expected.";

		ErrorHandler.error(Errors.MOD_ARG_SYNTAX_WRONG_NUM_PRODUCTIONS, errorMessage, modulePart);

	}

	public static void modArgumentJudgmentWrongNumRules(Judgment argJudgment, Judgment paramJudgment, ModulePart modulePart) {
		int argNumRules = argJudgment.getRules().size();
		int paramNumRules = paramJudgment.getRules().size();

		String errorMessage = "Module argument judgment " + argJudgment + " has " + argNumRules 
			+ " rules, but " + paramNumRules + " rules are expected.";

		ErrorHandler.error(Errors.MOD_ARG_JUDGMENT_WRONG_NUM_RULES, errorMessage, modulePart);
	}

	public static void modArgMismatchJudgment(Judgment argJudgment, Judgment paramJudgment, Judgment whatParamIsBoundTo, ModulePart modulePart) {
		String errorMessage = "Judgment " + argJudgment + " is being assigned to " 
			+ paramJudgment + ", but " + paramJudgment + " is already bound to " + whatParamIsBoundTo + ".";

		ErrorHandler.error(Errors.MOD_ARG_MISMATCH_JUDGMENT, errorMessage, modulePart);
	}

	public static void modArgRuleWrongNumPremises(Rule argRule, Rule paramRule, ModulePart modulePart) {
		int argNumPremises = argRule.getPremises().size();
		int paramNumPremises = paramRule.getPremises().size();

		String errorMessage = "Module argument rule " + argRule + " has " + argNumPremises 
			+ ", but " + paramNumPremises + "premises are expected.";

		ErrorHandler.error(Errors.MOD_ARG_RULE_WRONG_NUM_PREMISES, errorMessage, modulePart);
	}
	
	public static void modArgTheoremWrongNumForalls(Theorem argTheorem, Theorem paramTheorem, ModulePart modulePart) {
		int argNumForalls = argTheorem.getForalls().size();
		int paramNumForalls = paramTheorem.getForalls().size();

		String errorMessage = "Module argument theorem " + argTheorem + " has " + argNumForalls 
			+ " foralls, but " + paramNumForalls + " foralls are expected.";

		ErrorHandler.error(Errors.MOD_ARG_THEOREM_WRONG_NUM_FORALLS, errorMessage, modulePart);
	}
	
	public static void modArgNonterminalMismatch(
		NonTerminal argNonterminal, 
		NonTerminal paramNonterminal, 
		NonTerminal whatParamIsBoundTo, 
		ModulePart modulePart) {
		
			String errorMessage = "Nonterminal " + argNonterminal + " is being assigned to " 
				+ paramNonterminal + ", but " + paramNonterminal + " is already bound to " + whatParamIsBoundTo + ".";

			ErrorHandler.error(Errors.MOD_ARG_NONTERMINAL_MISMATCH, errorMessage, modulePart);
	}

	// errors from Clause.checkClauseSameStructure

	public static void modArgClauseMismatchParamIsAndClauseButArgIsnt(Clause argClause, Clause paramClause, ModulePart modulePart) {

		String errorMessage = "Clause " + argClause + " doesn't match clause " + paramClause + " because "
			+ "the latter is an and clause, but the former isn't.";

		ErrorHandler.error(Errors.MOD_ARG_CLAUSE_CLASS_MISMATCH, errorMessage, modulePart);
	}

	public static void modArgClauseWrongNumNotTerminals(
		Clause argClause,
		Clause paramClause,
		ModulePart modulePart) {

			int argNumNotTerminals = argClause.withoutTerminals().size();
			int paramNumNotTerminals = paramClause.withoutTerminals().size();

			String errorMessage = "Clause " + argClause + " has " + argNumNotTerminals 
				+ " not-terminals, but " + paramNumNotTerminals + " not-terminals are expected.";

			ErrorHandler.error(Errors.MOD_ARG_CLAUSE_WRONG_NUM_NOT_TERMINALS, errorMessage, modulePart);
	}

	public static void modArgClauseNonterminalTypeMismatch(
		NonTerminal argNonterminal,
		NonTerminal paramNonterminal,
		Map<Syntax, Syntax> paramToArgSyntax,
		ModulePart modulePart) {

			String errorMessage = "Nonterminal " + argNonterminal + "has type " + argNonterminal.getType() + ", but is expected to have type "
				+ paramNonterminal.getType() + " because " + paramNonterminal + " is bound to " + paramToArgSyntax.get(paramNonterminal.getType().getOriginalDeclaration()) + ".";
			
			ErrorHandler.error(Errors.MOD_ARG_CLAUSE_NONTERMINAL_TYPE_MISMATCH, errorMessage, modulePart);
			
		}

	public static void modArgNonTerminalMismatch(
		NonTerminal argNonterminal,
		NonTerminal paramNonterminal,
		NonTerminal whatParamIsBoundTo,
		ModulePart modulePart)
		{
			String errorMessage = "Nonterminal " + argNonterminal + " is being assigned to " 
				+ paramNonterminal + ", but " + paramNonterminal + " is already bound to " + whatParamIsBoundTo + ".";

			ErrorHandler.error(Errors.MOD_ARG_NONTERMINAL_MISMATCH, errorMessage, modulePart);
		}

	public static void modArgClauseClassMismatch(
		Clause argClause,
		Clause paramClause,
		ModulePart modulePart)
		{
			String errorMessage = "Argument clause " + argClause + " and parameter clause"
				+ paramClause + " are not of the same class.";

			ErrorHandler.error(Errors.MOD_ARG_CLAUSE_CLASS_MISMATCH, errorMessage, modulePart);
		}


}
