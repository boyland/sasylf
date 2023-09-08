import { TextDocument } from "vscode-languageserver-textdocument";
import { Range } from "vscode-languageserver/node";
import { ast, ruleNode, moduleNode } from "./types";
import * as fs from 'fs';
import * as path from 'path';

// Returns a range corresponding to line and char passed in
// If only start_line is passed in, we get entire line
// end_line defaults to start_line
// start_char defaults to -1 (first character)
// end_char defaults to start_char if it is not -1, and Number.MAX_VALUE (last character) otherwise
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

// Gets range based off offset and length passed, essentially gets the range of textDocument.substring(offset, length)
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
    let ruleModule: null | string = null;
    let modulePath: null | string = null;
    if (ruleName.includes("'")) {
        ruleName = ruleName.split(".")[1];
        ruleModule = ruleName.split(".")[0];
    }
    if (ruleModule != null) {
        const modules: moduleNode[] = compUnit.modules;
        for (const module of modules) {
            if (module.name === ruleModule) {
                modulePath = module.file;
                break;
            }
        }
    }
    const judgments = compUnit.judgments;
    for (const judgment of judgments) {
        for (const rule of judgment.rules) {
            if (rule.file === modulePath && rule.name === ruleName) {
                return rule;
            }
        }
    }
    return null;
}

function createLogFile(logFile: string): string {
    // Get path to the log file
    const logFilePath = path.join(__dirname, "..", "..", "logs", logFile);

    // If it doesn't already exist, create it
    if (!fs.existsSync(logFilePath)) {
        fs.writeFileSync(logFilePath, '', { flag: 'w' });
    }

    return logFilePath;
}

export function logErrorToFile(errorMsg: string, file: string, logFile: string) {
    errorMsg += "\n";
    // Create the log file
    const logFilePath = createLogFile(logFile);

    // Append date and file location
    const date = new Date().toISOString();

    fs.appendFile(logFilePath, file + ": " + date + ": ", (err) => {
        if (err) {
            console.error('Error writing to the log file:', err);
        }
    });

    // Append the error message to the log file
    fs.appendFile(logFilePath, errorMsg, (err) => {
        if (err) {
            console.error('Error writing to the log file:', err);
        }
    });
}
