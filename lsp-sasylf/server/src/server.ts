import { spawnSync } from "child_process";
import { TextDocument } from "vscode-languageserver-textdocument";
import { EOL } from "os";
import {
	createConnection,
	Diagnostic,
	DiagnosticSeverity,
	DidChangeConfigurationNotification,
	InitializeParams,
	InitializeResult,
	ProposedFeatures,
	TextDocuments,
	TextDocumentSyncKind,
	DocumentSymbol,
	SymbolKind,
	Range,
	CodeAction,
} from "vscode-languageserver/node";
import { ast, parsedData } from "./types";
import {
	getLineRange,
	isBarChar,
	findRule,
	getLineRangeFromOffset,
	search,
} from "./utils";

// Create a connection for the server, using Node's IPC as a transport.
// Also include all preview / proposed LSP features.
const connection = createConnection(ProposedFeatures.all);
// Create a simple text document manager.
const documents: TextDocuments<TextDocument> = new TextDocuments(TextDocument);

let hasConfigurationCapability = false;
let hasWorkspaceFolderCapability = false;
let hasDiagnosticRelatedInformationCapability = false;

connection.onInitialize((params: InitializeParams) => {
	const capabilities = params.capabilities;

	// Does the client support the `workspace/configuration` request?
	// If not, we fall back using global settings.
	hasConfigurationCapability = !!(
		capabilities.workspace && !!capabilities.workspace.configuration
	);
	hasWorkspaceFolderCapability = !!(
		capabilities.workspace && !!capabilities.workspace.workspaceFolders
	);
	hasDiagnosticRelatedInformationCapability = !!(
		capabilities.textDocument &&
		capabilities.textDocument.publishDiagnostics &&
		capabilities.textDocument.publishDiagnostics.relatedInformation
	);

	const result: InitializeResult = {
		capabilities: {
			textDocumentSync: TextDocumentSyncKind.Incremental,
			codeActionProvider: { resolveProvider: true },
			documentSymbolProvider: true,
			definitionProvider: true,
		},
	};
	if (hasWorkspaceFolderCapability) {
		result.capabilities.workspace = {
			workspaceFolders: { supported: true },
		};
	}
	return result;
});

connection.onInitialized(() => {
	if (hasConfigurationCapability) {
		// Register for all configuration changes.
		connection.client.register(
			DidChangeConfigurationNotification.type,
			undefined,
		);
	}
	if (hasWorkspaceFolderCapability) {
		connection.workspace.onDidChangeWorkspaceFolders((_event) => {
			connection.console.log("Workspace folder change event received.");
		});
	}
});

// The example settings
interface ExampleSettings {
	maxNumberOfProblems: number;
}

// The global settings, used when the `workspace/configuration` request is not
// supported by the client. Please note that this is not the case when using
// this server with the client provided in this example but could happen with
// other clients.
const defaultSettings: ExampleSettings = {
	maxNumberOfProblems: 1000,
};
let globalSettings: ExampleSettings = defaultSettings;

// Cache the settings of all open documents
const documentSettings: Map<string, Thenable<ExampleSettings>> = new Map();

connection.onDidChangeConfiguration((change) => {
	if (hasConfigurationCapability) {
		// Reset all cached document settings
		documentSettings.clear();
	} else {
		globalSettings = <ExampleSettings>(
			(change.settings.languageServerExample || defaultSettings)
		);
	}

	// Revalidate all open text documents
	documents.all().forEach(validateTextDocument);
});

function getDocumentSettings(resource: string): Thenable<ExampleSettings> {
	if (!hasConfigurationCapability) {
		return Promise.resolve(globalSettings);
	}
	let result = documentSettings.get(resource);
	if (!result) {
		result = connection.workspace.getConfiguration({
			scopeUri: resource,
			section: "languageServerExample",
		});
		documentSettings.set(resource, result);
	}
	return result;
}

// Only keep settings for open documents
documents.onDidClose((e) => {
	documentSettings.delete(e.document.uri);
});

// Map that contains diagnostics encoded with line numbers and error message and
// their corresponding quickfixes
const quickfixes: Map<string | number | undefined, any> = new Map();

// Stores the abstract syntax tree
let compUnit: ast;

// The content of a text document has changed. This event is emitted
// when the text document first opened or when its content has changed.
documents.onDidChangeContent((change) => {
	validateTextDocument(change.document);
});

// Gives diagonistics based on output of sasylf cli
async function validateTextDocument(textDocument: TextDocument): Promise<void> {
	// Parses json output from sasylf core and turns them into corresponding
	// diagnostics and quickfixes
	const settings = await getDocumentSettings(textDocument.uri);

	const diagnostics: Diagnostic[] = [];
	const text = textDocument.getText();
	const command = spawnSync(
		`java -jar ${__dirname}/../src/SASyLF.jar`,
		["--lsp", "--stdin"],
		{
			input: text,
			shell: true,
		},
	);

	let parsedJson: parsedData;

	try {
		parsedJson = JSON.parse(command.stdout.toString());
	} catch (e) {
		console.log("Error during parsing: ", e);
		return;
	}

	const output = parsedJson.quickfixes;
	compUnit = parsedJson.ast;

	quickfixes.clear();
	for (
		let i = 0;
		i < output.length && i < settings.maxNumberOfProblems - 1;
		++i
	) {
		const element = output[i];
		const severity = element["severity"];
		if (severity == "info") continue;
		const start = {
			line: element["begin_line"] - 1,
			character: element["begin_column"] - 1,
		};
		const end = {
			line: element["end_line"] - 1,
			character: element["end_column"] - 1,
		};
		const message = element["error_message"];
		const range = { start: start, end: end };
		if (severity == "error") {
			quickfixes.set(i, {
				error_severity: severity,
				error_type: element["error_type"],
				error_info: element["error_info"],
				range: range,
			});
		}
		const diagnostic: Diagnostic = {
			severity:
				severity == "warning"
					? DiagnosticSeverity.Warning
					: DiagnosticSeverity.Error,
			range: range,
			message: message,
			source: "sasylf",
			code: i,
		};
		diagnostics.push(diagnostic);
	}

	// Send the computed diagnostics to VSCode.
	connection.sendDiagnostics({ uri: textDocument.uri, diagnostics });
}

// Implements go to definition
connection.onDefinition((params) => {
	const doc = documents.get(params.textDocument.uri);

	if (doc == null) return undefined;

	const badCharacters: String[] = [" ", "\t", "\n", "\r", "\f", "\v", "(", ")"];
	const text = doc.getText();
	const offset = doc.offsetAt(params.position);
	let wordStart = offset;
	let wordEnd = offset;

	// Finds the word underneath the cursor
	while (
		!badCharacters.includes(text[wordStart]) ||
		!badCharacters.includes(text[wordEnd])
	) {
		if (!badCharacters.includes(text[wordStart])) --wordStart;
		if (!badCharacters.includes(text[wordEnd])) ++wordEnd;
	}

	const word = text.slice(wordStart + 1, wordEnd);

	if (word.includes(".")) {
		const result = search(word, compUnit, params.textDocument.uri, connection);
		return result;
	}

	// Checks if it is a theorem or lemma
	const theoremNames = compUnit.theorems.map((theorem) => theorem.name);

	if (theoremNames.includes(word)) {
		const theorem = compUnit.theorems[theoremNames.indexOf(word)];

		return {
			uri: params.textDocument.uri,
			range: {
				start: { line: theorem.line - 1, character: theorem.column - 1 },
				end: { line: theorem.line - 1, character: theorem.column - 1 },
			},
		};
	}

	// Checks if it is a rule
	const rules = compUnit.judgments.map((judgment) => judgment.rules).flat();
	const ruleNames = rules.map((rule) => rule.name);

	if (ruleNames.includes(word)) {
		const rule = rules[ruleNames.indexOf(word)];

		if (rule.in_file) {
			return {
				uri: params.textDocument.uri,
				range: {
					start: { line: rule.line - 1, character: rule.column - 1 },
					end: { line: rule.line - 1, character: rule.column - 1 },
				},
			};
		}
	}

	return undefined;
});

connection.onDocumentSymbol((identifier) => {
	// Adds the module to the symbols
	const res: DocumentSymbol[] = [];
	const modules = compUnit.modules;

	for (const module of modules) {
		const moduleSymbol = DocumentSymbol.create(
			module.name,
			undefined,
			SymbolKind.Module,
			{
				start: {
					line: module.begin_line - 1,
					character: module.begin_column - 1,
				},
				end: {
					line: module.end_line - 1,
					character: module.end_column - 1,
				},
			},
			{
				start: {
					line: module.begin_line - 1,
					character: module.begin_column - 1,
				},
				end: {
					line: module.end_line - 1,
					character: module.end_column - 1,
				},
			},
		);

		res.push(moduleSymbol);
	}

	// Adds all the syntaxes declarations to the symbols
	const syntaxDeclarations = compUnit.syntax.syntax_declarations;

	for (const declaration of syntaxDeclarations) {
		const children: DocumentSymbol[] = [];

		for (const clause of declaration.clauses) {
			const clauseSymbol = DocumentSymbol.create(
				clause.name,
				undefined,
				SymbolKind.Key,
				{
					start: {
						line: clause.line - 1,
						character: clause.column - 1,
					},
					end: {
						line: clause.line - 1,
						character: clause.column - 1,
					},
				},
				{
					start: {
						line: clause.line - 1,
						character: clause.column - 1,
					},
					end: {
						line: clause.line - 1,
						character: clause.column - 1,
					},
				},
			);

			children.push(clauseSymbol);
		}

		const declarationSymbol = DocumentSymbol.create(
			declaration.name,
			undefined,
			SymbolKind.String,
			{
				start: {
					line: declaration.line - 1,
					character: declaration.column - 1,
				},
				end: {
					line: declaration.line - 1,
					character: declaration.column - 1,
				},
			},
			{
				start: {
					line: declaration.line - 1,
					character: declaration.column - 1,
				},
				end: {
					line: declaration.line - 1,
					character: declaration.column - 1,
				},
			},
			children,
		);

		res.push(declarationSymbol);
	}

	// Adds all the sugars to the symbols
	const sugars = compUnit.syntax.sugars;

	for (const sugar of sugars) {
		const sugarSymbol = DocumentSymbol.create(
			sugar.name.slice(0, -1), // Removes the newline
			undefined,
			SymbolKind.String,
			{
				start: {
					line: sugar.line - 1,
					character: sugar.column - 1,
				},
				end: {
					line: sugar.line - 1,
					character: sugar.column - 1,
				},
			},
			{
				start: {
					line: sugar.line - 1,
					character: sugar.column - 1,
				},
				end: {
					line: sugar.line - 1,
					character: sugar.column - 1,
				},
			},
		);

		res.push(sugarSymbol);
	}

	// Adds the theorems to the symbols
	const theorems = compUnit.theorems;

	for (const theorem of theorems) {
		let detail = "";

		for (const forall of theorem.foralls) {
			detail += "∀" + forall;
		}

		detail += "∃" + theorem.conclusion;

		const theoremSymbol = DocumentSymbol.create(
			theorem.name,
			detail,
			theorem.kind == "theorem" ? SymbolKind.Class : SymbolKind.Struct,
			{
				start: {
					line: theorem.line - 1,
					character: theorem.column - 1,
				},
				end: {
					line: theorem.line - 1,
					character: theorem.column - 1,
				},
			},
			{
				start: {
					line: theorem.line - 1,
					character: theorem.column - 1,
				},
				end: {
					line: theorem.line - 1,
					character: theorem.column - 1,
				},
			},
		);

		res.push(theoremSymbol);
	}

	// Adds the judgments to the symbols
	const judgments = compUnit.judgments;

	for (const judgment of judgments) {
		const children: DocumentSymbol[] = [];

		for (const rule of judgment.rules) {
			if (rule.in_file) {
				let detail = "";

				for (const premise of rule.premises) {
					detail += "∀" + premise;
				}

				detail += "∃" + rule.conclusion;

				const ruleSymbol = DocumentSymbol.create(
					rule.name,
					detail,
					SymbolKind.Property,
					{
						start: {
							line: rule.line - 1,
							character: rule.column - 1,
						},
						end: {
							line: rule.line - 1,
							character: rule.column - 1,
						},
					},
					{
						start: {
							line: rule.line - 1,
							character: rule.column - 1,
						},
						end: {
							line: rule.line - 1,
							character: rule.column - 1,
						},
					},
				);

				children.push(ruleSymbol);
			}
		}

		const judgmentSymbol = DocumentSymbol.create(
			judgment.name,
			judgment.form,
			SymbolKind.Variable,
			{
				start: {
					line: judgment.line - 1,
					character: judgment.column - 1,
				},
				end: {
					line: judgment.line - 1,
					character: judgment.column - 1,
				},
			},
			{
				start: {
					line: judgment.line - 1,
					character: judgment.column - 1,
				},
				end: {
					line: judgment.line - 1,
					character: judgment.column - 1,
				},
			},
			children,
		);

		res.push(judgmentSymbol);
	}

	return res;
});

connection.onDidChangeWatchedFiles((change) => {
	// Monitored files have change in VSCode
	connection.console.log("We received an file change event");
});

// Looks for quickfixes in the `quickfixes` map and returns them if they exist
connection.onCodeAction(async (params) => {
	const textDocument: TextDocument | undefined = documents.get(
		params.textDocument.uri,
	);
	if (textDocument == null) return [];

	const codeActions: CodeAction[] = [];

	const eolSetting = await connection.workspace.getConfiguration({
		section: "files",
	});
	const nl = eolSetting.eol == "auto" ? EOL : eolSetting.eol;

	// For each diagnostic, try to find a quick fix
	for (const diagnostic of params.context.diagnostics) {
		const code: string | number | undefined = diagnostic.code;
		if (code == null) continue;
		if (!quickfixes.has(code)) continue;
		const quickfix = quickfixes.get(code);
		const errorType: string = quickfix.error_type;
		const errorInfo: string = quickfix.error_info;
		const range = quickfix.range;
		const line = range.start.line;
		let extraIndent = "";
		if (errorType == null || errorInfo == null || line == 0) continue;
		if (range.start.line != range.end.line) {
			continue;
		}
		// Range that includes the entire line at line number `line`, 0 indexed
		const lineInfo: Range = getLineRange(line);

		const lineText: string = textDocument.getText(lineInfo);

		const split = errorInfo.split(/\r?\n/, -1);

		let lineIndent: string;
		{
			let i;
			for (i = 0; i < lineText.length; ++i) {
				const ch = lineText.charAt(i);
				if (ch == " " || ch == "\t") continue;
				break;
			}
			lineIndent = lineText.substring(0, i);
		}
		const indentAmount =
			textDocument.getText(getLineRange(line)).length -
			textDocument.getText(getLineRange(line)).trimStart().length;

		let indent = "    ";
		if (indentAmount >= 0 && indentAmount <= 8) {
			indent = "        ".substring(0, indentAmount);
		}

		const ind = textDocument
			.getText(getLineRange(line, Number.MAX_VALUE, 0, Number.MAX_VALUE))
			.indexOf(split[0]);

		let old: Range | null = null;

		if (ind != -1) {
			old = getLineRangeFromOffset(
				ind + textDocument.offsetAt(lineInfo.start),
				split[0].length,
				textDocument,
			);
		}

		if (old == null) {
			if (split[0] == lineText) {
				old = lineInfo;
			}
		}

		switch (errorType) {
			case "MISSING_CASE":
				let newText = "";
				if (errorInfo.indexOf("\n\n") == -1) {
					// syntax case
					const n = split.length - 1;
					for (let i = 0; i < n; ++i) {
						newText = newText.concat(lineIndent);
						newText = newText.concat(indent);
						newText = newText.concat("case ");
						newText = newText.concat(split[i]);
						newText = newText.concat(" is");
						newText = newText.concat(nl);
						newText = newText.concat(lineIndent);
						newText = newText.concat(indent);
						newText = newText.concat(indent);
						newText = newText.concat("proof by unproved");
						newText = newText.concat(nl);
						newText = newText.concat(lineIndent);
						newText = newText.concat(indent);
						newText = newText.concat("end case");
						newText = newText.concat(nl);
						newText = newText.concat(nl);
					}
				} else {
					let startCase = true;
					const n = split.length - 1; // extra line at end
					for (let i = 0; i < n; ++i) {
						if (startCase) {
							newText = newText.concat(lineIndent);
							newText = newText.concat(indent);
							newText = newText.concat("case rule");
							newText = newText.concat(nl);
							startCase = false;
						}
						if (split[i].length == 0) {
							newText = newText.concat(lineIndent);
							newText = newText.concat(indent);
							newText = newText.concat("is");
							newText = newText.concat(nl);
							newText = newText.concat(lineIndent);
							newText = newText.concat(indent);
							newText = newText.concat(indent);
							newText = newText.concat("proof by unproved");
							newText = newText.concat(nl);
							newText = newText.concat(lineIndent);
							newText = newText.concat(indent);
							newText = newText.concat("end case");
							newText = newText.concat(nl);
							newText = newText.concat(nl);
							startCase = true;
							continue;
						}
						newText = newText.concat(lineIndent);
						newText = newText.concat(indent);
						newText = newText.concat(indent);
						if (split[i].startsWith("---")) {
							const ruleName = split[i].split(" ")[1];
							const rule = findRule(compUnit, ruleName);
							if (rule !== null) {
								let ruleText = textDocument.getText(getLineRange(rule.line));
								let lexicalInfo = "";
								for (const c of ruleText) {
									if (isBarChar(c)) {
										lexicalInfo += c;
									} else {
										break;
									}
								}
								if (lexicalInfo.length >= 3) {
									split[i] =
										lexicalInfo.substring(0, 3) +
										lexicalInfo.substring(0, 3) +
										lexicalInfo +
										" " +
										ruleName;
								}
							}
						} else {
							newText = newText.concat("_: ");
						}
						newText = newText.concat(split[i]);
						newText = newText.concat(nl);
					}
				}
				if (
					lineText.includes("by contradiction on") &&
					!lineText.includes("by case analysis on")
				) {
					// XXX: Could be confused by a comment
					let lo = lineText.indexOf("contradiction");
					let parts = lineText.split("\\s+");
					const l = parts.length;
					// try to avoid dangerous changes
					if (l > 3 && parts[l - 2] === "on") {
						let derivName = parts[l - 1];
						newText =
							"case analysis on " +
							derivName +
							":" +
							nl +
							newText +
							lineIndent +
							"end case analysis";
						let offset = textDocument.offsetAt(lineInfo.start) + lo;
						codeActions.push({
							title: "convert to case analysis with missing case(s)",
							kind: "quickfix",
							diagnostics: [diagnostic],
							edit: {
								changes: {
									[textDocument.uri]: [
										{
											range: {
												start: textDocument.positionAt(offset),
												end: textDocument.positionAt(
													offset + lineText.length - lo,
												),
											},
											newText: newText,
										},
									],
								},
							},
						});
					}
				} else {
					codeActions.push({
						title: "insert missing case(s)",
						kind: "quickfix",
						diagnostics: [diagnostic],
						edit: {
							changes: {
								[textDocument.uri]: [
									{
										range: getLineRange(
											lineInfo.start.line + 1,
											lineInfo.end.line + 1,
											0,
											0,
										),
										newText: newText,
									},
								],
							},
						},
					});
				}
				break;
			case "ABSTRACT_NOT_PERMITTED_HERE":
			case "ILLEGAL_ASSUMES":
			case "EXTRANEOUS_ASSUMES":
				if (old != null) {
					codeActions.push({
						title: `remove '${split[0]}'`,
						kind: "quickfix",
						diagnostics: [diagnostic],
						edit: {
							changes: {
								[textDocument.uri]: [{ range: old, newText: "" }],
							},
						},
					});
				}
			/* falls through */
			case "RULE_NOT_THEOREM":
			case "THEOREM_NOT_RULE":
			case "THEOREM_KIND_WRONG":
			case "THEOREM_KIND_MISSING":
			case "INDUCTION_REPEAT":
			case "WRONG_END":
			case "WRONG_MODULE_NAME":
			case "PARTIAL_CASE_ANALYSIS":
				if (old != null) {
					if (split.length > 1 && split[1].length > 0) {
						codeActions.push({
							title: `replace '${split[0]}' with '${split[1]}'`,
							kind: "quickfix",
							diagnostics: [diagnostic],
							edit: {
								changes: {
									[textDocument.uri]: [{ range: old, newText: split[1] }],
								},
							},
						});
					}
				}
				break;
			case "WRONG_PACKAGE":
				if (split[0].length == 0) {
					codeActions.push({
						title: `insert '${split[1]}'`,
						kind: "quickfix",
						diagnostics: [diagnostic],
						edit: {
							changes: {
								[textDocument.uri]: [
									{
										range: getLineRange(
											lineInfo.start.line,
											lineInfo.end.line,
											Number.MAX_VALUE,
											Number.MAX_VALUE,
										),
										newText: split[1] + nl,
									},
								],
							},
						},
					});
				}
				if (old != null && split.length > 1) {
					if (split[1].length == 0) {
						codeActions.push({
							title: `remove '${split[0]}'`,
							kind: "quickfix",
							diagnostics: [diagnostic],
							edit: {
								changes: {
									[textDocument.uri]: [{ range: old, newText: "" }],
								},
							},
						});
					} else {
						codeActions.push({
							title: `replace '${split[0]}' with '${split[1]}'`,
							kind: "quickfix",
							diagnostics: [diagnostic],
							edit: {
								changes: {
									[textDocument.uri]: [{ range: old, newText: split[1] }],
								},
							},
						});
					}
				}
				break;
			case "ASSUMED_ASSUMES":
				extraIndent = indent;
			/* falls through */
			case "MISSING_ASSUMES":
				codeActions.push({
					title: `insert '${errorInfo}'`,
					kind: "quickfix",
					diagnostics: [diagnostic],
					edit: {
						changes: {
							[textDocument.uri]: [
								{
									range: getLineRange(
										lineInfo.start.line + 1,
										lineInfo.end.line + 1,
										Number.MAX_VALUE,
										Number.MAX_VALUE,
									),
									newText: lineIndent + extraIndent + errorInfo + nl,
								},
							],
						},
					},
				});

				break;
			case "CASE_REDUNDANT":
			case "CASE_UNNECESSARY":
				break;
			case "OTHER_JUSTIFIED":
				if (lineInfo != null) {
					const holeStart = split[0].indexOf("...");
					const startPat = split[0].substring(0, holeStart);
					const endPat = split[0].substring(holeStart + 3);
					const findStart = lineText.indexOf(startPat);
					if (findStart < 0) break;
					const findEnd = lineText.indexOf(endPat, findStart);
					if (findEnd < 0) break;
					const oldStart = findStart + startPat.length;
					const oldText = lineText.substring(oldStart, findEnd);
					codeActions.push({
						title: `replace '${oldText}' with '${" " + split[1]}'`,
						kind: "quickfix",
						diagnostics: [diagnostic],
						edit: {
							changes: {
								[textDocument.uri]: [
									{
										range: getLineRangeFromOffset(
											oldStart + textDocument.offsetAt(lineInfo.start),
											oldText.length,
											textDocument,
										),
										newText: " " + split[1],
									},
								],
							},
						},
					});
				}
				break;
			case "RULE_CONCLUSION_CONTRADICTION":
				if (lineInfo != null) {
					const findBy = lineText.indexOf(" by ");
					if (findBy >= lineIndent.length) {
						const oldText = lineText.substring(lineIndent.length, findBy);
						codeActions.push({
							title: `replace '${oldText}' with '_: contradiction'`,
							kind: "quickfix",
							diagnostics: [diagnostic],
							edit: {
								changes: {
									[textDocument.uri]: [
										{
											range: getLineRangeFromOffset(
												lineIndent.length +
													textDocument.offsetAt(lineInfo.start),
												oldText.length,
												textDocument,
											),
											newText: "_: contradiction",
										},
									],
								},
							},
						});
					}
				}
				break;
			case "DERIVATION_NOT_FOUND": {
				const colon = split[0].indexOf(":");
				const useName = textDocument.getText(diagnostic.range);
				let defName;
				if (colon >= 0) {
					defName = split[0].substring(0, colon);
				} else {
					defName = split[0];
					codeActions.push({
						title: `replace '${lineText}' with '${defName}'`,
						kind: "quickfix",
						diagnostics: [diagnostic],
						edit: {
							changes: {
								[textDocument.uri]: [{ range: lineInfo, newText: defName }],
							},
						},
					});
					break;
				}

				const newText = nl + indent + split[0] + " by unproved";
				let extra = "";

				const changes = [
					{
						range: getLineRange(
							lineInfo.start.line,
							lineInfo.start.line,
							Number.MAX_VALUE,
							Number.MAX_VALUE,
						),
						newText: newText,
					},
				];

				if (defName != useName) {
					if (useName == "_") {
						extra = `, and replace '_' with '${defName}'`;
						changes.push({
							range: diagnostic.range,
							newText: defName,
						});
					}
				}

				codeActions.push({
					title: `insert '${split[0]} by unproved' before this line ${extra}`,
					kind: "quickfix",
					diagnostics: [diagnostic],
					edit: {
						changes: {
							[textDocument.uri]: changes,
						},
					},
				});
				break;
			}
			default:
				break;
		}
	}
	return codeActions;
});

// Make the text document manager listen on the connection
// for open, change and close text document events
documents.listen(connection);

// Listen on the connection
connection.listen();
