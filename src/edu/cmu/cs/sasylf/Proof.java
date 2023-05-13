package edu.cmu.cs.sasylf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.module.ModuleFinder;
import edu.cmu.cs.sasylf.module.ModuleId;
import edu.cmu.cs.sasylf.module.ResourceModuleFinder;
import edu.cmu.cs.sasylf.parser.DSLToolkitParser;
import edu.cmu.cs.sasylf.parser.ParseException;
import edu.cmu.cs.sasylf.parser.TokenMgrError;
import edu.cmu.cs.sasylf.term.FreeVar;
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
	private static boolean lsp = false;
	public static boolean getLsp() { return lsp; }
	public static void setLsp(boolean lsp) { Proof.lsp = lsp; }

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
	private static ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
	public static ArrayNode getArrayNode() { return arrayNode; }
	public static void setArrayNode(ArrayNode arrayNode) {
		Proof.arrayNode = arrayNode;
	}

	public void parseAndCheck(ModuleFinder mf, Reader r) {
		if (reports != null) {
			throw new IllegalStateException("Results already determined");
		}
		// System.out.println(id + ".parseAndCheck()");
		reports = ErrorHandler.withFreshReports(() -> doParseAndCheck(mf, r));

		if (lsp) {
			ObjectMapper objectMapper = new ObjectMapper();

			for (Report rep : reports) {
				Span s = rep.getSpan();
				Location begin = s.getLocation();
				Location end = s.getEndLocation();

				String severity = "info";

				ObjectNode tmp = objectMapper.createObjectNode();

				if (rep instanceof ErrorReport) {
					ErrorReport report = (ErrorReport)(rep);

					tmp.put("error_type", report.getErrorType().name());
					tmp.put("error_info", report.getExtraInformation());

					severity = (report.isError()) ? "error" : "warning";
				}

				tmp.put("error_message", rep.getMessage());
				tmp.put("severity", severity);
				tmp.put("begin_line", begin.getLine());
				tmp.put("begin_column", begin.getColumn());
				tmp.put("end_line", end.getLine());
				tmp.put("end_column", end.getColumn());

				arrayNode.add(tmp);
			}
		}

		cacheErrorCount();
	}

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
		duringParse = ErrorHandler.getReports().size();
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
	}
}
