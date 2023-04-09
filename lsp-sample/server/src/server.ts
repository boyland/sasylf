/* --------------------------------------------------------------------------------------------
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 * ------------------------------------------------------------------------------------------
 */
import {spawnSync} from 'child_process';
import {rmSync, writeFileSync} from 'node:fs';
import {EOL} from 'os';
import {TextDocument} from 'vscode-languageserver-textdocument';
import {
  CompletionItem,
  CompletionItemKind,
  createConnection,
  Diagnostic,
  DiagnosticSeverity,
  DidChangeConfigurationNotification,
  InitializeParams,
  InitializeResult,
  ProposedFeatures,
  TextDocumentPositionParams,
  TextDocuments,
  TextDocumentSyncKind,
  TextEdit
} from 'vscode-languageserver/node';

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
  hasConfigurationCapability =
      !!(capabilities.workspace && !!capabilities.workspace.configuration);
  hasWorkspaceFolderCapability =
      !!(capabilities.workspace && !!capabilities.workspace.workspaceFolders);
  hasDiagnosticRelatedInformationCapability =
      !!(capabilities.textDocument &&
         capabilities.textDocument.publishDiagnostics &&
         capabilities.textDocument.publishDiagnostics.relatedInformation);

  const result: InitializeResult = {
    capabilities : {
      textDocumentSync : TextDocumentSyncKind.Incremental,
      // Tell the client that this server supports code completion.
      codeActionProvider : {resolveProvider : true},
      completionProvider : {resolveProvider : true},
    }
  };
  if (hasWorkspaceFolderCapability) {
    result.capabilities.workspace = {workspaceFolders : {supported : true}};
  }
  return result;
});

connection.onInitialized(() => {
  if (hasConfigurationCapability) {
    // Register for all configuration changes.
    connection.client.register(DidChangeConfigurationNotification.type,
                               undefined);
  }
  if (hasWorkspaceFolderCapability) {
    connection.workspace.onDidChangeWorkspaceFolders(_event => {
      connection.console.log('Workspace folder change event received.');
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
  maxNumberOfProblems : 1000
};
let globalSettings: ExampleSettings = defaultSettings;

// Cache the settings of all open documents
const documentSettings: Map<string, Thenable<ExampleSettings>> = new Map();

connection.onDidChangeConfiguration(change => {
  if (hasConfigurationCapability) {
    // Reset all cached document settings
    documentSettings.clear();
  } else {
    globalSettings = <ExampleSettings>(
        (change.settings.languageServerExample || defaultSettings));
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
    result = connection.workspace.getConfiguration(
        {scopeUri : resource, section : 'languageServerExample'});
    documentSettings.set(resource, result);
  }
  return result;
}

// Only keep settings for open documents
documents.onDidClose(e => { documentSettings.delete(e.document.uri); });

// Map that contains diagnostics encoded with line numbers and error message and
// their corresponding quickfixes
const quickfixes: Map<string, any> = new Map();

// The content of a text document has changed. This event is emitted
// when the text document first opened or when its content has changed.
documents.onDidChangeContent(
    change => { validateTextDocument(change.document); });

// Gives diagonistics based on output of sasylf cli
async function validateTextDocument(textDocument: TextDocument): Promise<void> {
  // Parses json output from sasylf core and turns them into corresponding
  // diagnostics and quickfixes
  const settings = await getDocumentSettings(textDocument.uri);

  const eolSetting =
      await connection.workspace.getConfiguration({section : "files"});
  const eol = (eolSetting.eol == "auto") ? EOL : eolSetting.eol;

  const indentSetting =
      await connection.workspace.getConfiguration({section : "editor"});
  const indentSize = (indentSetting.indentSize === "tabSize")
                         ? indentSetting.tabSize
                         : indentSetting.indentSize;

  const diagnostics: Diagnostic[] = [];
  const text = textDocument.getText();
  const text_lines = text.split("\n");
  const command = spawnSync(
      'sasylf',
      [ `--indentAmount=${indentSize}`, `--eol=${eol}`, '--lsp', '--stdin' ],
      {input : text});

  console.log(command.stdout.toString());

  const output = JSON.parse(command.stdout.toString());

  quickfixes.clear();

  for (let i = 0; i < output.length && i < settings.maxNumberOfProblems - 1;
       ++i) {
    const element = output[i];
    const line = element['Line'] - 1;
    const quickfix = element['Quickfix'];
    const start = {
      line : line,
      character : text_lines[line].length - text_lines[line].trimStart().length
    };
    const end = {line : line, character : text_lines[line].length};
    const message = element['Error Message'];
    const severity = element['Severity'];
    if (severity == "info")
      continue;
    const diagnostic: Diagnostic = {
      severity : (severity == "warning") ? DiagnosticSeverity.Warning
                                         : DiagnosticSeverity.Error,
      range : {start : start, end : end},
      message : message,
      source : 'sasylf',
    };
    quickfixes.set(`${line}${message}`, quickfix);
    diagnostics.push(diagnostic);
  }

  // Send the computed diagnostics to VSCode.
  connection.sendDiagnostics({uri : textDocument.uri, diagnostics});
}

connection.onDidChangeWatchedFiles(change => {
  // Monitored files have change in VSCode
  connection.console.log('We received an file change event');
  console.log('We received an file change event');
});

// This handler provides the initial list of the completion items.
connection.onCompletion(
    (_textDocumentPosition: TextDocumentPositionParams): CompletionItem[] => {
      // The pass parameter contains the position of the text document in
      // which code complete got requested. For the example we ignore this
      // info and always provide the same completion items.
      return [
        {label : 'TypeScript', kind : CompletionItemKind.Text, data : 1},
        {label : 'JavaScript', kind : CompletionItemKind.Text, data : 2}
      ];
    });

// This handler resolves additional information for the item selected in
// the completion list.
connection.onCompletionResolve((item: CompletionItem): CompletionItem => {
  if (item.data === 1) {
    item.detail = 'TypeScript details';
    item.documentation = 'TypeScript documentation';
  } else if (item.data === 2) {
    item.detail = 'JavaScript details';
    item.documentation = 'JavaScript documentation';
  }
  return item;
});

// Looks for quickfixes in the `quickfixes` map and returns them if they exist
connection.onCodeAction(async (params) => {
  const textDocument: TextDocument|undefined =
      documents.get(params.textDocument.uri);

  if (textDocument == null)
    return;

  const codeActions = [];

  // For each diagnostic, try to find a quick fix
  for (const diagnostic of params.context.diagnostics) {
    const index = `${diagnostic.range.start.line}${diagnostic.message}`;
    if (!quickfixes.has(index))
      continue;
    const quickfix = quickfixes.get(index);
    if (quickfix == null)
      continue;
    const charStart = quickfix['charStart'];
    const charEnd = quickfix['charEnd'];
    const start = textDocument.positionAt(charStart);
    const end = textDocument.positionAt(charEnd);
    codeActions.push({
      title : 'Code Action',
      kind : 'quickfix',
      diagnostics : [ diagnostic ],
      edit : {
        changes : {
          [textDocument.uri] : [
            {range : {start : start, end : end}, newText : quickfix.newText}
          ]
        }
      }
    });
  }

  return codeActions;
});

// Make the text document manager listen on the connection
// for open, change and close text document events
documents.listen(connection);

// Listen on the connection
connection.listen();
