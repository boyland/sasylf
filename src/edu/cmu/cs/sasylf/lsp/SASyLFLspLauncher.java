package edu.cmu.cs.sasylf.lsp;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;

public class SASyLFLspLauncher {
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		InputStream in = System.in;
		OutputStream out = System.out;
		SASyLFLanguageServer server = new SASyLFLanguageServer();
		Launcher<LanguageClient> launcher = Launcher.createLauncher(
				server, LanguageClient.class, in, out);
		LanguageClient client = launcher.getRemoteProxy();
		server.connect(client);
		Future<?> startListening = launcher.startListening();
		startListening.get();
	}
}
