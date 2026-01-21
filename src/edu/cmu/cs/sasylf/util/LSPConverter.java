package edu.cmu.cs.sasylf.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.cmu.cs.sasylf.ast.Clause;
import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.ast.Fact;
import edu.cmu.cs.sasylf.ast.Judgment;
import edu.cmu.cs.sasylf.ast.ModulePart;
import edu.cmu.cs.sasylf.ast.Node;
import edu.cmu.cs.sasylf.ast.Rule;
import edu.cmu.cs.sasylf.ast.Sugar;
import edu.cmu.cs.sasylf.ast.SyntaxDeclaration;
import edu.cmu.cs.sasylf.ast.Theorem;
import edu.cmu.cs.sasylf.module.Module;

public class LSPConverter {
	private final ObjectMapper objectMapper = new ObjectMapper();
	public ObjectMapper getObjectMapper() { return objectMapper; }

	private final ObjectNode json = objectMapper.createObjectNode();
	public ObjectNode getJson() { return json; }

    private boolean lsp;
    public boolean getLsp() {
        return lsp;
    }
    public void setLsp(boolean lsp) {
        this.lsp = lsp;
    }

    public String errorReportsToJson(List<Report> reports) {
		ObjectNode res = objectMapper.createObjectNode();
		ArrayNode errorReports = objectMapper.createArrayNode();

		res.put("errors", errorReports);

		for (Report r : reports) {
			if (r instanceof ErrorReport) {
				ErrorReport er = (ErrorReport)r;
				if (er.isError()) errorReports.add(er.toString());
			}
		}

		return res.toString();
    }

    public void reportsToJSON(Module module, List<Report> reports, String filename) {
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

			json.put("ast", moduleToJSON(module, filename));
		}
    }

	private ObjectNode moduleToJSON(Module module, String filename) {
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
				moduleNode.put("ast", moduleToJSON(m, filename));
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
}
