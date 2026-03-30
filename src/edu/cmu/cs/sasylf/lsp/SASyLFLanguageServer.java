package edu.cmu.cs.sasylf.lsp;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.ServerInfo;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

public class SASyLFLanguageServer implements LanguageServer, LanguageClientAware {

	private LanguageClient client;
	private final DocumentManager documentManager = new DocumentManager();
	private final SASyLFTextDocumentService textDocumentService;
	private final SASyLFWorkspaceService workspaceService;
	private String workspaceRoot;

	public SASyLFLanguageServer() {
		textDocumentService = new SASyLFTextDocumentService(this);
		workspaceService = new SASyLFWorkspaceService(this);
	}

	public LanguageClient getClient() { return client; }
	public DocumentManager getDocumentManager() { return documentManager; }
	public String getWorkspaceRoot() { return workspaceRoot; }

	@Override
	public void connect(LanguageClient client) {
		this.client = client;
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		if (params.getRootUri() != null) {
			workspaceRoot = params.getRootUri();
		}

		ServerCapabilities caps = new ServerCapabilities();
		caps.setTextDocumentSync(TextDocumentSyncKind.Full);
		caps.setHoverProvider(true);
		CompletionOptions completionOptions = new CompletionOptions();
		completionOptions.setTriggerCharacters(Arrays.asList("."));
		caps.setCompletionProvider(completionOptions);
		caps.setDefinitionProvider(true);
		caps.setDocumentSymbolProvider(true);

		InitializeResult result = new InitializeResult(caps);
		result.setServerInfo(new ServerInfo("SASyLF Language Server", "0.1.0"));
		return CompletableFuture.completedFuture(result);
	}

	@Override
	public CompletableFuture<Object> shutdown() {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public void exit() {
		System.exit(0);
	}

	@Override
	public TextDocumentService getTextDocumentService() { return textDocumentService; }

	@Override
	public WorkspaceService getWorkspaceService() { return workspaceService; }
}
