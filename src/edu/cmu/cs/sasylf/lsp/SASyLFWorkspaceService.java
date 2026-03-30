package edu.cmu.cs.sasylf.lsp;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.services.WorkspaceService;

public class SASyLFWorkspaceService implements WorkspaceService {

	private final SASyLFLanguageServer server;

	public SASyLFWorkspaceService(SASyLFLanguageServer server) {
		this.server = server;
	}

	@Override
	public void didChangeConfiguration(DidChangeConfigurationParams params) {
	}

	@Override
	public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
	}
}
