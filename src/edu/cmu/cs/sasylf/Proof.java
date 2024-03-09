package edu.cmu.cs.sasylf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.Reader;
import java.io.StringWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import edu.cmu.cs.sasylf.ast.Clause;
import edu.cmu.cs.sasylf.ast.Context;
import edu.cmu.cs.sasylf.ast.Element;
import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.ast.Fact;
import edu.cmu.cs.sasylf.ast.Judgment;
import edu.cmu.cs.sasylf.ast.ModulePart;
import edu.cmu.cs.sasylf.ast.Node;
import edu.cmu.cs.sasylf.ast.Rule;
import edu.cmu.cs.sasylf.ast.RuleLike;
import edu.cmu.cs.sasylf.ast.Sugar;
import edu.cmu.cs.sasylf.ast.SyntaxDeclaration;
import edu.cmu.cs.sasylf.ast.TermPrinter;
import edu.cmu.cs.sasylf.ast.Theorem;
import edu.cmu.cs.sasylf.module.ModuleFinder;
import edu.cmu.cs.sasylf.module.Module;
import edu.cmu.cs.sasylf.module.ModuleId;
import edu.cmu.cs.sasylf.module.ResourceModuleFinder;
import edu.cmu.cs.sasylf.parser.DSLToolkitParser;
import edu.cmu.cs.sasylf.parser.ParseException;
import edu.cmu.cs.sasylf.parser.TokenMgrError;
import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.ErrorReport;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Report;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.Span;
import edu.cmu.cs.sasylf.util.TokenSpan;

/**
 * The results of parsing and checking a SASyLF source unit.
 */
public class Proof {
	private final String filename;
	private final ModuleId id;
	private CompUnit syntaxTree;
	private List<Report> reports;
	private int duringParse;

	/**
	 * Prepare for some results to come later.
	 * @see #doParseAndCheck(ModuleFinder, Reader)
	 * @param name name of file/resource to read (must not be null)
	 * @param id module id (may be null)
	 */
	public Proof(String name, ModuleId id) {
		if (name == null) throw new NullPointerException("filename is null");
		this.filename = name;
		this.id = id;
	}

	/**
	 * Construct a result for the given AST and counted number of
	 * reports during parsing
	 * @param filename the name of the unit parsed and checked
	 * @param id module ID used to fetch the unit (may be null)
	 * @param cu abstract syntax tree resulted from parsing;
	 * may be null if there are errors during parsing
	 * @param parseReports number of reports that arose during parsing
	 * must not be negative or more than the number of reports.
	 */
	public Proof(String filename, ModuleId id, CompUnit cu, int parseReports) {
		this(filename, id);
		syntaxTree = cu;
		reports = new ArrayList<>(ErrorHandler.getReports());
		duringParse = parseReports;
		if (parseReports < 0 || parseReports > reports.size()) {
			throw new IllegalArgumentException("parseReports wrong: " + parseReports);
		}
		if (cu == null) {
			boolean foundParseError = false;
			for (int i = 0; i < parseReports; ++i) {
				Report r = reports.get(i);
				if (r instanceof ErrorReport && ((ErrorReport)r).isError()) {
					foundParseError = true;
					break;
				}
			}
			if (!foundParseError) {
				throw new IllegalArgumentException("null AST but no parse errors?");
			}
		}
	}

	public String getFilename() { return filename; }

	public ModuleId getModuleId() { return id; }

	/**
	 * Return the AST that was parsed.
	 */
	public CompUnit getCompilationUnit() { return syntaxTree; }

	/**
	 * Return the reports that occurred during processing.
	 * @return list of report objects (unmodifiable)
	 * @throws IllegalStateException if not parsed yet
	 */
	public List<Report> getReports() {
		if (reports == null) throw new IllegalStateException("not parsed yet");
		return Collections.unmodifiableList(reports);
	}

	/**
	 * Return the sublist of the the reports that occurred during parsing.
	 * @return list of report objects (unmodifiable)
	 * @throws IllegalStateException if not parsed yet
	 */
	public List<Report> getParseReports() {
		if (reports == null) throw new IllegalStateException("not parsed yet");
		return Collections.unmodifiableList(reports.subList(0, duringParse));
	}

	/**
	 * Return the sublist of the reports that occurred after parsing was over.
	 * @return list of report objects (unmodifiable)
	 * @throws IllegalStateException if not parsed yet
	 */
	public List<Report> getAfterParseReports() {
		if (reports == null) throw new IllegalStateException("not parsed yet");
		return Collections.unmodifiableList(
				reports.subList(duringParse, reports.size()));
	}

	private int errors = -1;
	private int warnings = -1;

	protected void cacheErrorCount() {
		if (errors >= 0) return;
		int countErrors = 0;
		int countWarnings = 0;
		for (Report r : getReports()) {
			if (r instanceof ErrorReport) {
				ErrorReport er = (ErrorReport)r;
				if (er.isError()) ++countErrors;
				else ++countWarnings;
			}
		}
		errors = countErrors;
		warnings = countWarnings;
	}

	/**
	 * Return the number of errors reported
	 * @return number of errors
	 */
	public int getErrorCount() {
		cacheErrorCount();
		return errors;
	}

	/**
	 * Return the number of warnings reported
	 * @return number of warnings
	 */
	public int getWarningCount() {
		cacheErrorCount();
		return warnings;
	}

	/**
	 * Analyze the SASyLF code in the reader and return the results.
	 * @param mf may be null
	 * @param filename should not be null
	 * @param id may be null if not interested in checking package/module-name
	 *     errors
	 * @param r contents to parse; must not be null
	 * @returns Results object of results, never null
	 */
	private final static ObjectMapper objectMapper = new ObjectMapper();
	private static ObjectNode json = objectMapper.createObjectNode();

	private static boolean lsp = false;
	public static boolean getLsp() { return lsp; }
	public static void setLsp(boolean lsp) { Proof.lsp = lsp; }

	private static Clause c = null;
	public static void setClause(Clause c) { Proof.c = c; }
	public static Clause getClause() { return c; }

	private static String r = null;
	public static void setRule(String r) { Proof.r = r; }
	public static String getRule() { return r; }

	private static List<Clause> p = null;
	public static void setPremise(String p)
			throws JsonProcessingException, ParseException {

		Map<String, List<String>> map = objectMapper.readValue(p, Map.class);
		List<Clause> premises = new ArrayList<>();
		for (String s : map.get("premises")) {
			StringReader reader = new StringReader(s);
			DSLToolkitParser parser = new DSLToolkitParser(reader);
			premises.add(parser.ExprToNL());
		}
		Proof.p = premises;
	}
	public static List<Clause> getPremise() { return p; }

	public static Proof parseAndCheck(ModuleFinder mf, String filename,
																		ModuleId id, Reader r) {
		Proof result = new Proof(filename, id);
		result.parseAndCheck(mf, r);
		return result;
	}

	/**
	 * Analyze the SASyLF code in the reader and initialize remaining parts of
	 * results. This method can called just once.
	 * @param mf may be null
	 * @param r contents to parse; must not be null
	 */

	public static String getJSON() { return json.toString(); }

	private ObjectNode moduleToJSON(Module module) {
		if (module == null) return null;

		ObjectNode astNode = objectMapper.createObjectNode();

		astNode.put("name", module.getName());

		ArrayNode theoremsNode = objectMapper.createArrayNode();
		ArrayNode modulesNode = objectMapper.createArrayNode();
		ObjectNode syntaxesNode = objectMapper.createObjectNode();
		ArrayNode judgmentsNode = objectMapper.createArrayNode();

		astNode.put("theorems", theoremsNode);
		astNode.put("modules", modulesNode);
		astNode.put("syntax", syntaxesNode);
		astNode.put("judgments", judgmentsNode);

		ArrayNode syntaxDeclarationsNode = objectMapper.createArrayNode();
		ArrayNode syntaxSugarsNode = objectMapper.createArrayNode();

		syntaxesNode.put("syntax_declarations", syntaxDeclarationsNode);
		syntaxesNode.put("sugars", syntaxSugarsNode);

		List<Node> pieces = new ArrayList<>();
		module.collectTopLevel(pieces);

		for (Node piece : pieces) {
			Location startLoc = piece.getLocation();
			Location endLoc = piece.getEndLocation();

			if (piece instanceof Theorem) {
				Theorem theorem = (Theorem)piece;
				ObjectNode theoremNode = objectMapper.createObjectNode();

				theoremsNode.add(theoremNode);

				theoremNode.put("name", theorem.getName());
				theoremNode.put("column", startLoc.getColumn());
				theoremNode.put("line", startLoc.getLine());
				theoremNode.put("file", startLoc.getFile());
				theoremNode.put("kind", theorem.getKind());

				ArrayNode forallsNode = objectMapper.createArrayNode();

				theoremNode.put("foralls", forallsNode);

				for (Fact forall : theorem.getForalls()) {
					forallsNode.add(forall.getElement().toString());
				}

				theoremNode.put("conclusion", theorem.getConclusion().getName());
			} else if (piece instanceof ModulePart) {
				ModulePart modulePart = (ModulePart)piece;
				ObjectNode moduleNode = objectMapper.createObjectNode();

				modulesNode.add(moduleNode);

				moduleNode.put("name", modulePart.getName() + ": " +
																	 modulePart.getModule().toString());
				moduleNode.put("begin_column", startLoc.getColumn());
				moduleNode.put("end_column", endLoc.getColumn());
				moduleNode.put("begin_line", startLoc.getLine());
				moduleNode.put("end_line", endLoc.getLine());

				// moduleNode.put("file", startLoc.getFile());

				Module m = (Module)modulePart.getModule().resolve(null);
				String file = m instanceof CompUnit
													? ((CompUnit)m).getLocation().getFile()
													: startLoc.getFile();

				if (file.charAt(0) == '/' || file.charAt(0) == '\\') {
					try (InputStream in = getClass().getResourceAsStream(file);
							 BufferedReader reader =
									 new BufferedReader(new InputStreamReader(in))) {
						StringBuilder stringBuilder = new StringBuilder();
						String line;

						while ((line = reader.readLine()) != null) {
							stringBuilder.append(line).append('\n');
						}

						String content = stringBuilder.toString();
						moduleNode.put("text", content);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				moduleNode.put("file", file);
				moduleNode.put("ast", moduleToJSON(m));
			} else if (piece instanceof SyntaxDeclaration) {
				SyntaxDeclaration syntax = (SyntaxDeclaration)piece;
				ObjectNode syntaxNode = objectMapper.createObjectNode();

				syntaxDeclarationsNode.add(syntaxNode);

				syntaxNode.put("name", syntax.getName());
				syntaxNode.put("column", startLoc.getColumn());
				syntaxNode.put("line", startLoc.getLine());
				syntaxNode.put("file", startLoc.getFile());

				ArrayNode clausesNode = objectMapper.createArrayNode();

				syntaxNode.put("clauses", clausesNode);

				List<Clause> clauses = syntax.getClauses();

				for (Clause clause : clauses) {
					ObjectNode clauseNode = objectMapper.createObjectNode();

					clausesNode.add(clauseNode);

					clauseNode.put("name", clause.getName());
					clauseNode.put("column", clause.getLocation().getColumn());
					clauseNode.put("line", clause.getLocation().getLine());
					clauseNode.put("file", clause.getLocation().getFile());
				}
			} else if (piece instanceof Sugar) {
				Sugar syntax = (Sugar)piece;
				ObjectNode syntaxNode = objectMapper.createObjectNode();

				syntaxSugarsNode.add(syntaxNode);

				syntaxNode.put("name", syntax.toString());
				syntaxNode.put("column", syntax.getLocation().getColumn());
				syntaxNode.put("line", syntax.getLocation().getLine());
				syntaxNode.put("file", syntax.getLocation().getFile());
			} else if (piece instanceof Judgment) {
				Judgment judgment = (Judgment)piece;

				ObjectNode judgmentNode = objectMapper.createObjectNode();

				judgmentsNode.add(judgmentNode);

				judgmentNode.put("name", judgment.getName());
				judgmentNode.put("column", judgment.getLocation().getColumn());
				judgmentNode.put("line", judgment.getLocation().getLine());
				judgmentNode.put("form", judgment.getForm().getName());
				judgmentNode.put("file", judgment.getLocation().getFile());

				List<Rule> rules = judgment.getRules();

				ArrayNode rulesNode = objectMapper.createArrayNode();

				judgmentNode.put("rules", rulesNode);

				for (Rule rule : rules) {
					ObjectNode ruleNode = objectMapper.createObjectNode();

					rulesNode.add(ruleNode);

					ArrayNode premisesNode = objectMapper.createArrayNode();

					ruleNode.put("premises", premisesNode);

					for (Clause clause : rule.getPremises()) {
						premisesNode.add(clause.getName());
					}

					ruleNode.put("name", rule.getName());
					ruleNode.put("conclusion", rule.getConclusion().getName());
					ruleNode.put("in_file",
											 rule.getLocation().getFile().equals(filename));
					ruleNode.put("column", rule.getLocation().getColumn());
					ruleNode.put("line", rule.getLocation().getLine());
					ruleNode.put("file", rule.getLocation().getFile());
				}
			}
		}

		return astNode;
	}

	public void parseAndCheck(ModuleFinder mf, Reader r) {
		if (reports != null) {
			throw new IllegalStateException("Results already determined");
		}
		// System.out.println(id + ".parseAndCheck()");
		reports = ErrorHandler.withFreshReports(() -> doParseAndCheck(mf, r));

		// If the lsp flag is set, gather the quickfixes and parse the syntax
		// tree, adding all the data to a json
		if (lsp) {
			ArrayNode qfArray = objectMapper.createArrayNode();

			json.put("quickfixes", qfArray);

			for (Report rep : reports) {
				Span s = rep.getSpan();
				Location begin = s.getLocation();
				Location end = s.getEndLocation();

				String severity = "info";

				ObjectNode qfNode = objectMapper.createObjectNode();

				qfArray.add(qfNode);

				if (rep instanceof ErrorReport) {
					ErrorReport report = (ErrorReport)(rep);

					qfNode.put("error_type", report.getErrorType().name());
					qfNode.put("error_info", report.getExtraInformation());

					severity = (report.isError()) ? "error" : "warning";
				}

				qfNode.put("error_message", rep.getMessage());
				qfNode.put("severity", severity);
				qfNode.put("begin_line", begin.getLine());
				qfNode.put("begin_column", begin.getColumn());
				qfNode.put("end_line", end.getLine());
				qfNode.put("end_column", end.getColumn());
			}

			json.put("ast", moduleToJSON((Module)syntaxTree));
		}

		cacheErrorCount();
	}

	private RuleLike findRule(String n) {
		List<Node> pieces = new ArrayList<>();
		syntaxTree.collectTopLevel(pieces);

		for (Node piece : pieces) {
			if (piece instanceof Theorem && ((Theorem)piece).getName().equals(n))
				return (RuleLike)piece;
			else if (piece instanceof Judgment) {
				Judgment judgment = (Judgment)piece;

				for (Rule rule : judgment.getRules()) {
					if (rule.getName().equals(n)) return (RuleLike)rule;
				}
			}
		}

		return null;
	}

	private static String oldVar = null;

	public static void setOldVar(String oldVar) { Proof.oldVar = oldVar; }

	private static String newVar = null;

	public static void setNewVar(String newVar) { Proof.newVar = newVar; }

	private static Clause sclause = null;

	public static Clause getSclause() { return sclause; }

	public static void setSclause(Clause substitutee) {
		Proof.sclause = substitutee;
	}

	private static String newClause = null;

	public static String getNewClause() { return newClause; }

	private static ObjectNode premises = objectMapper.createObjectNode();
	private static ObjectNode conclusions = objectMapper.createObjectNode();

	public static String getPremises() { return premises.toString(); }
	public static String getConclusions() { return conclusions.toString(); }

	private void doParseAndCheck(ModuleFinder mf, Reader r) {
		FreeVar.reinit();
		try {
			syntaxTree = DSLToolkitParser.read(filename, r);
		} catch (ParseException e) {
			final TokenSpan errorSpan = new TokenSpan(e.currentToken.next);
			if (e.expectedTokenSequences != null &&
					e.expectedTokenSequences.length == 1) {
				String expected = e.tokenImage[e.expectedTokenSequences[0][0]];
				ErrorHandler.recoverableError(Errors.PARSE_EXPECTED, expected,
																			errorSpan);
			} else {
				// do not use "e.getMessage()": not localized
				ErrorHandler.recoverableError(Errors.PARSE_ERROR, errorSpan);
			}
		} catch (TokenMgrError e) {
			ErrorHandler.recoverableError(
					Errors.LEXICAL_ERROR,
					ErrorHandler.lexicalErrorAsLocation(filename, e.getMessage()));
		} catch (RuntimeException e) {
			e.printStackTrace();
			final Span errorSpan = new Location(filename, 0, 0);
			ErrorHandler.recoverableError(Errors.INTERNAL_ERROR,
																		"Internal error during parsing: " + e,
																		errorSpan);
		}
		Context ctx = null;
		try {
			if (mf != null) ctx = new Context(new ResourceModuleFinder(), syntaxTree);
			else {
				mf.setCurrentPackage(id == null ? ModuleFinder.EMPTY_PACKAGE
																				: id.packageName);
				ctx = new Context(mf, syntaxTree);
			}
		} catch (SASyLFError ex) {
			// muffle: handled already
		} catch (RuntimeException ex) {
			ex.printStackTrace();
			ErrorHandler.recoverableError(Errors.INTERNAL_ERROR,
																		ex.getLocalizedMessage(), null);
		}
		if (syntaxTree != null) {
			try {
				if (mf == null) syntaxTree.typecheck(new ResourceModuleFinder(), null);
				else {
					mf.setCurrentPackage(id == null ? ModuleFinder.EMPTY_PACKAGE
																					: id.packageName);
					syntaxTree.typecheck(mf, id);
				}
			} catch (SASyLFError ex) {
				// muffle: handled already
			} catch (RuntimeException ex) {
				ex.printStackTrace();
				ErrorHandler.recoverableError(Errors.INTERNAL_ERROR,
																			ex.getLocalizedMessage(), null);
			}
		}

		// the substitution to get fresh variables
		Substitution freshSub = new Substitution();

		// the substitution to handle possible dependencies on the context
		// (this cannot be used until we have removed bindings of variable free
		// NTs)
		Substitution adaptSub = new Substitution();

		// the variable-free NTs that should not be adapted:
		Set<FreeVar> varFree = new HashSet<FreeVar>();

		// after adaptation, the free variables in the premises
		Set<FreeVar> freeVars = new HashSet<FreeVar>();

		List<Term> addedTypes = new ArrayList<Term>();

		if (sclause != null) {
			try {
				syntaxTree.typecheck(ctx, id);
				sclause.typecheck(ctx);

				Element e = sclause.computeClause(ctx);
				FreeVar oldTerm = null;

				for (FreeVar var : e.asTerm().getFreeVariables())
					if (var.getName().equals(oldVar)) {
						oldTerm = var;
						break;
					}

				if (oldTerm != null) {
					Substitution sub =
							new Substitution(Facade.FVar(newVar, oldTerm.getType()), oldTerm);
					Term t = e.asTerm().substitute(sub);
					TermPrinter tp =
							new TermPrinter(ctx, null, new Location(filename, 0, 0), false);

					if (ctx.inputVars == null) ctx.inputVars = new HashSet<FreeVar>();
					if (ctx.outputVars == null) ctx.outputVars = new HashSet<FreeVar>();
					if (ctx.recursiveTheorems == null)
						ctx.recursiveTheorems = new HashMap<String, Theorem>();

					newClause = tp.toString(t, true);
				}
			} catch (Exception exp) {
				newClause = "Err:" + exp.getMessage();
			}
		}

		if (c != null && r != null) {
			RuleLike ruleLike = findRule(Proof.r);
			syntaxTree.typecheck(ctx, id);
			c.typecheck(ctx);
			Element e = c.computeClause(ctx);
			List<Fact> inputs = new ArrayList<Fact>();
			List<Abstraction> addedContext = new ArrayList<Abstraction>();

			for (Element premise : ruleLike.getPremises()) {
				inputs.add(premise.asFact(ctx, ctx.assumedContext));
			}

			Term subject = ruleLike.checkApplication(
					ctx, inputs, e.asFact(ctx, ctx.assumedContext), addedContext, null,
					false, addedTypes, freshSub, adaptSub, varFree, true, false);

			Set<FreeVar> conclusionFreeVars = new HashSet<FreeVar>();
			Term pattern =
					ruleLike.getFreshAdaptedRuleTerm(addedContext, conclusionFreeVars);
			Substitution callSub = pattern.unify(subject);
			Set<FreeVar> vars = e.asTerm().getFreeVariables();
			callSub.avoid(vars);
			Term actual = subject.substitute(callSub);

			TermPrinter tp =
					new TermPrinter(ctx, null, ruleLike.getLocation(), false);
			if (ctx.inputVars == null) ctx.inputVars = new HashSet<FreeVar>();
			if (ctx.outputVars == null) ctx.outputVars = new HashSet<FreeVar>();
			if (ctx.recursiveTheorems == null)
				ctx.recursiveTheorems = new HashMap<String, Theorem>();

			ArrayNode premiseNode = objectMapper.createArrayNode();
			premises.put("arguments", premiseNode);
			for (Term arg : ((Application)actual).getArguments()) {
				premiseNode.add(tp.toString(tp.asClause(arg)));
			}
		}

		if (p != null && r != null) {
			RuleLike ruleLike = findRule(Proof.r);
			syntaxTree.typecheck(ctx, id);
			List<Fact> inputs = new ArrayList<Fact>();
			List<Abstraction> addedContext = new ArrayList<Abstraction>();

			Element conclusion = ruleLike.getConclusion();

			Set<FreeVar> vars = new HashSet<FreeVar>();

			for (Clause premise : p) {
				premise.typecheck(ctx);
			}
			for (Clause premise : p) {
				Element e = premise.computeClause(ctx);
				vars.addAll(e.asTerm().getFreeVariables());
				inputs.add(e.asFact(ctx, ctx.assumedContext));
			}

			Term subject = ruleLike.checkApplication(
					ctx, inputs, conclusion.asFact(ctx, ctx.assumedContext), addedContext,
					null, true, addedTypes, freshSub, adaptSub, varFree, false, true);

			Set<FreeVar> conclusionFreeVars = new HashSet<FreeVar>();
			Term pattern =
					ruleLike.getFreshAdaptedRuleTerm(addedContext, conclusionFreeVars);
			Substitution callSub = subject.unify(pattern);
			callSub.avoid(vars);
			Term actual = subject.substitute(callSub);

			TermPrinter tp =
					new TermPrinter(ctx, null, ruleLike.getLocation(), false);
			if (ctx.inputVars == null) ctx.inputVars = new HashSet<FreeVar>();
			if (ctx.outputVars == null) ctx.outputVars = new HashSet<FreeVar>();
			if (ctx.recursiveTheorems == null)
				ctx.recursiveTheorems = new HashMap<String, Theorem>();

			ArrayNode conclusionNode = objectMapper.createArrayNode();
			conclusions.put("conclusion", conclusionNode);
			conclusionNode.add(tp.toString(tp.asClause(
					((Application)actual)
							.getArguments()
							.get(((Application)actual).getArguments().size() - 1))));
		}
	}
}
