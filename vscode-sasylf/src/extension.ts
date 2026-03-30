import * as path from 'path';
import * as fs from 'fs';
import { workspace, ExtensionContext, window } from 'vscode';
import {
    LanguageClient,
    LanguageClientOptions,
    ServerOptions,
    TransportKind
} from 'vscode-languageclient/node';

let client: LanguageClient;

export function activate(context: ExtensionContext) {
    const config = workspace.getConfiguration('sasylf');
    const javaHome = config.get<string>('java.home', '');
    const javaCmd = javaHome ? path.join(javaHome, 'bin', 'java') : 'java';

    const serverJar = context.asAbsolutePath(path.join('server', 'sasylf-lsp.jar'));

    if (!fs.existsSync(serverJar)) {
        window.showErrorMessage(
            'SASyLF Language Server JAR not found. Expected at: ' + serverJar
        );
        return;
    }

    const serverOptions: ServerOptions = {
        command: javaCmd,
        args: ['-jar', serverJar],
        transport: TransportKind.stdio
    };

    const traceLevel = config.get<string>('trace.server', 'off');

    const clientOptions: LanguageClientOptions = {
        documentSelector: [{ scheme: 'file', language: 'sasylf' }],
        synchronize: {
            fileEvents: workspace.createFileSystemWatcher('**/*.slf')
        }
    };

    client = new LanguageClient(
        'sasylf',
        'SASyLF Language Server',
        serverOptions,
        clientOptions
    );

    client.start();
}

export function deactivate(): Thenable<void> | undefined {
    if (!client) {
        return undefined;
    }
    return client.stop();
}
