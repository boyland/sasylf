import * as path from "path";
import {
	workspace,
	ExtensionContext,
	TextDocumentContentProvider,
	Uri,
	window,
	commands,
} from "vscode";

import {
	LanguageClient,
	LanguageClientOptions,
	ServerOptions,
	TransportKind,
} from "vscode-languageclient/node";

import { readFileSync } from "fs";
import { spawn } from "child_process";

let client: LanguageClient;

function validateHandler(_: any[]) {
	const file = window.activeTextEditor?.document.uri.toString();

	if (!file) return;

	client.sendNotification("custom/validateTextDocument", file);
}

function appHandler(_: any[]) {
	const text = window.activeTextEditor?.document.getText();

	if (!text) return;

	client.sendRequest("custom/getAST").then((ast) => {
		var spawn_env = JSON.parse(JSON.stringify(process.env));
		delete spawn_env.ATOM_SHELL_INTERNAL_RUN_AS_NODE;
		delete spawn_env.ELECTRON_RUN_AS_NODE;

		process.chdir(`${__dirname}/../../electron_app`);

		spawn(
			"npm",
			[
				"run",
				"start",
				"--",
				"--",
				JSON.stringify(ast),
				window.activeTextEditor?.document.fileName || "",
			],
			{ env: spawn_env, detached: true },
		);
	});
}

export function activate(context: ExtensionContext) {
	// The server is implemented in node
	const serverModule = context.asAbsolutePath(
		path.join("server", "out", "server.js"),
	);

	// If the extension is launched in debug mode then the debug server options are used
	// Otherwise the run options are used
	const serverOptions: ServerOptions = {
		run: { module: serverModule, transport: TransportKind.ipc },
		debug: {
			module: serverModule,
			transport: TransportKind.ipc,
		},
	};

	// Options to control the language client
	const clientOptions: LanguageClientOptions = {
		// Register the server for plain text documents
		documentSelector: [{ scheme: "file", language: "sasylf" }],
		synchronize: {
			// Notify the server about file changes to '.clientrc files contained in the workspace
			fileEvents: workspace.createFileSystemWatcher("**/.clientrc"),
		},
	};

	const myScheme = "temporary";
	const myProvider = new (class implements TextDocumentContentProvider {
		provideTextDocumentContent(uri: Uri): string {
			return readFileSync(uri.fsPath, "utf8");
		}
	})();

	context.subscriptions.push(
		workspace.registerTextDocumentContentProvider(myScheme, myProvider),
	);

	context.subscriptions.push(
		commands.registerCommand("sasylf.Validate", validateHandler),
	);

	context.subscriptions.push(
		commands.registerCommand("sasylf.App", appHandler),
	);

	// Create the language client and start the client.
	client = new LanguageClient(
		"languageServerExample",
		"Language Server Example",
		serverOptions,
		clientOptions,
	);

	console.log("Starting client");
	client.start();
}

export function deactivate(): Thenable<void> | undefined {
	if (!client) {
		return undefined;
	}
	return client.stop();
}
