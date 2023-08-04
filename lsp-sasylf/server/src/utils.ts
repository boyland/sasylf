import { TextDocument } from "vscode-languageserver-textdocument";
import {
	Range,
	CodeAction,
	Diagnostic,
	TextEdit,
} from "vscode-languageserver/node";
import { ast, ruleNode, moduleNode } from "./ast";

// Returns a range corresponding to line and char passed in
export function getLineRange(
	start_line: number,
	end_line: number | null = null,
	start_char: number | null = null,
	end_char: number | null = null,
): Range {
	if (end_line == null) end_line = start_line;
	if (start_char == null) start_char = -1;
	if (end_char == null) {
		end_char = start_char == -1 ? Number.MAX_VALUE : start_char;
	}
	return {
		start: { line: start_line, character: start_char },
		end: { line: end_line, character: end_char },
	};
}

export function getLineRangeFromOffset(
	offset: number,
	length: number,
	textDocument: TextDocument,
) {
	return {
		start: textDocument.positionAt(offset),
		end: textDocument.positionAt(offset + length),
	};
}

// Checks if a character is the bar char
export function isBarChar(ch: string): boolean {
	return ch === "-" || ch === "\u2500" || ch === "\u2014" || ch === "\u2015";
}

// Finds rule based on its name
export function findRule(compUnit: ast, ruleName: string): ruleNode | null {
	let ruleModule = null;
	let modulePath = null;
	if (ruleName.includes("'")) {
		ruleName = ruleName.split(".")[1];
		ruleModule = ruleName.split(".")[0];
	}
	if (ruleModule != null) {
		const modules: moduleNode[] = compUnit.Modules;
		for (const module of modules) {
			if (module.Name === ruleModule) {
				modulePath = module.File;
				break;
			}
		}
	}
	const judgments = compUnit.Judgments;
	for (const judgment of judgments) {
		for (const rule of judgment.Rules) {
			if (rule.File === modulePath && rule.Name === ruleName) {
				return rule;
			}
		}
	}
	return null;
}
