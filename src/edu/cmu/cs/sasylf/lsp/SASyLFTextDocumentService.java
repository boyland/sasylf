package edu.cmu.cs.sasylf.lsp;

import java.io.File;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

import edu.cmu.cs.sasylf.Proof;
import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.ast.Judgment;
import edu.cmu.cs.sasylf.ast.Node;
import edu.cmu.cs.sasylf.ast.Rule;
import edu.cmu.cs.sasylf.ast.RuleLike;
import edu.cmu.cs.sasylf.ast.SyntaxDeclaration;
import edu.cmu.cs.sasylf.ast.Theorem;
import edu.cmu.cs.sasylf.module.ModuleId;
import edu.cmu.cs.sasylf.util.ErrorReport;
import edu.cmu.cs.sasylf.util.Report;
import edu.cmu.cs.sasylf.util.Span;

public class SASyLFTextDocumentService implements TextDocumentService {

	private final SASyLFLanguageServer server;
	private final Map<String, CompUnit> cachedUnits = new HashMap<>();

	public SASyLFTextDocumentService(SASyLFLanguageServer server) {
		this.server = server;
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		TextDocumentItem doc = params.getTextDocument();
		server.getDocumentManager().open(doc.getUri(), doc.getText());
		validateAndPublish(doc.getUri());
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		VersionedTextDocumentIdentifier doc = params.getTextDocument();
		List<TextDocumentContentChangeEvent> changes = params.getContentChanges();
		if (!changes.isEmpty()) {
			String text = changes.get(changes.size() - 1).getText();
			server.getDocumentManager().update(doc.getUri(), text);
		}
		validateAndPublish(doc.getUri());
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		String uri = params.getTextDocument().getUri();
		server.getDocumentManager().close(uri);
		cachedUnits.remove(uri);
		server.getClient().publishDiagnostics(
				new PublishDiagnosticsParams(uri, Collections.emptyList()));
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		validateAndPublish(params.getTextDocument().getUri());
	}

	private void validateAndPublish(String uri) {
		List<Diagnostic> diagnostics = new ArrayList<>();
		try {
			String text = server.getDocumentManager().get(uri);
			if (text == null) return;

			String filename = uriToFilename(uri);
			LspModuleFinder mf = new LspModuleFinder(server.getDocumentManager(), filename);
			ModuleId id = null;
			try { id = new ModuleId(filename); } catch (Exception e) { /* ignore */ }

			Proof proof = new Proof(filename, id);
			proof.parseAndCheck(mf, new StringReader(text));
			cachedUnits.put(uri, proof.getCompilationUnit());

			for (Report r : proof.getReports()) {
				if (!(r instanceof ErrorReport)) continue;
				ErrorReport er = (ErrorReport) r;
				Diagnostic d = new Diagnostic();
				d.setSeverity(er.isError() ? DiagnosticSeverity.Error : DiagnosticSeverity.Warning);
				d.setMessage(er.getMessage());
				d.setSource("sasylf");
				d.setRange(spanToRange(er.getSpan()));
				diagnostics.add(d);
			}
		} catch (Throwable t) {
			// never crash the server
		}
		server.getClient().publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
	}

	// --- Hover ---

	@Override
	public CompletableFuture<Hover> hover(HoverParams params) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				String uri = params.getTextDocument().getUri();
				CompUnit cu = getCompUnit(uri);
				if (cu == null) return null;

				Position pos = params.getPosition();
				int line = pos.getLine() + 1;
				Node node = findNodeAt(cu, line);
				if (node == null) return null;

				String text = buildHoverText(node);
				if (text == null) return null;
				return new Hover(new MarkupContent(MarkupKind.MARKDOWN, text));
			} catch (Exception e) {
				return null;
			}
		});
	}

	// --- Completion ---

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				String uri = params.getTextDocument().getUri();
				CompUnit cu = getCompUnit(uri);
				List<CompletionItem> items = new ArrayList<>();
				if (cu == null) return Either.forLeft(items);

				Collection<Node> things = new ArrayList<>();
				cu.collectTopLevel(things);
				Map<String, RuleLike> ruleLikes = new HashMap<>();
				cu.collectRuleLike(ruleLikes);

				for (Node n : things) {
					if (n instanceof Theorem) {
						CompletionItem item = new CompletionItem(((Theorem) n).getName());
						item.setKind(CompletionItemKind.Function);
						item.setDetail("theorem");
						items.add(item);
					} else if (n instanceof Judgment) {
						CompletionItem jItem = new CompletionItem(((Judgment) n).getName());
						jItem.setKind(CompletionItemKind.Interface);
						jItem.setDetail("judgment");
						items.add(jItem);
						for (Rule r : ((Judgment) n).getRules()) {
							CompletionItem rItem = new CompletionItem(r.getName());
							rItem.setKind(CompletionItemKind.Method);
							rItem.setDetail("rule");
							items.add(rItem);
						}
					} else if (n instanceof SyntaxDeclaration) {
						CompletionItem item = new CompletionItem(((SyntaxDeclaration) n).getName());
						item.setKind(CompletionItemKind.Class);
						item.setDetail("syntax");
						items.add(item);
					}
				}

				String[] keywords = {
					"theorem", "lemma", "judgment", "syntax", "terminals",
					"rule", "proof", "by", "forall", "exists", "end",
					"case", "analysis", "induction", "on", "inversion",
					"of", "substitution", "weakening", "exchange",
					"contradiction", "where", "module", "requires",
					"provides", "imports", "package", "abstract", "use"
				};
				for (String kw : keywords) {
					CompletionItem item = new CompletionItem(kw);
					item.setKind(CompletionItemKind.Keyword);
					items.add(item);
				}
				return Either.forLeft(items);
			} catch (Exception e) {
				return Either.forLeft(Collections.emptyList());
			}
		});
	}

	// --- Go to Definition ---

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams params) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				String uri = params.getTextDocument().getUri();
				CompUnit cu = getCompUnit(uri);
				if (cu == null) return Either.forLeft(Collections.emptyList());

				Position pos = params.getPosition();
				int line = pos.getLine() + 1;
				Node node = findNodeAt(cu, line);
				if (node == null) return Either.forLeft(Collections.emptyList());

				Range range = spanToRange(node);
				Location loc = new Location(uri, range);
				List<Location> result = new ArrayList<>();
				result.add(loc);
				return Either.forLeft(result);
			} catch (Exception e) {
				return Either.forLeft(Collections.emptyList());
			}
		});
	}

	// --- Document Symbols ---

	@Override
	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				String uri = params.getTextDocument().getUri();
				CompUnit cu = getCompUnit(uri);
				if (cu == null) return Collections.emptyList();

				List<Either<SymbolInformation, DocumentSymbol>> symbols = new ArrayList<>();
				Collection<Node> things = new ArrayList<>();
				cu.collectTopLevel(things);

				for (Node n : things) {
					DocumentSymbol sym = nodeToSymbol(n);
					if (sym != null) symbols.add(Either.forRight(sym));
				}
				return symbols;
			} catch (Exception e) {
				return Collections.emptyList();
			}
		});
	}

	// --- Helpers ---

	private CompUnit getCompUnit(String uri) {
		CompUnit cu = cachedUnits.get(uri);
		if (cu != null) return cu;
		String text = server.getDocumentManager().get(uri);
		if (text == null) return null;
		try {
			String filename = uriToFilename(uri);
			LspModuleFinder mf = new LspModuleFinder(server.getDocumentManager(), filename);
			ModuleId id = null;
			try { id = new ModuleId(filename); } catch (Exception e) { /* ignore */ }
			Proof proof = new Proof(filename, id);
			proof.parseAndCheck(mf, new StringReader(text));
			cu = proof.getCompilationUnit();
			if (cu != null) cachedUnits.put(uri, cu);
			return cu;
		} catch (Exception e) {
			return null;
		}
	}

	private Node findNodeAt(CompUnit cu, int line) {
		Collection<Node> things = new ArrayList<>();
		cu.collectTopLevel(things);
		Node best = null;
		int bestLine = -1;
		for (Node n : things) {
			edu.cmu.cs.sasylf.util.Location loc = n.getLocation();
			if (loc == null) continue;
			int nl = loc.getLine();
			edu.cmu.cs.sasylf.util.Location end = n.getEndLocation();
			int el = (end != null) ? end.getLine() : nl;
			if (nl <= line && el >= line && nl > bestLine) {
				best = n;
				bestLine = nl;
			}
		}
		return best;
	}

	private String buildHoverText(Node n) {
		if (n instanceof Theorem) {
			Theorem t = (Theorem) n;
			StringBuilder sb = new StringBuilder();
			sb.append("**").append(t.getKind()).append("** `").append(t.getName()).append("`");
			return sb.toString();
		} else if (n instanceof Judgment) {
			Judgment j = (Judgment) n;
			return "**judgment** `" + j.getName() + "`: " + j.getForm().toString();
		} else if (n instanceof SyntaxDeclaration) {
			SyntaxDeclaration sd = (SyntaxDeclaration) n;
			StringBuilder sb = new StringBuilder();
			sb.append("**syntax** `").append(sd.getName()).append("`");
			if (!sd.isAbstract()) {
				sb.append(" ::= ...");
			}
			return sb.toString();
		} else if (n instanceof Rule) {
			Rule r = (Rule) n;
			return "**rule** `" + r.getName() + "`";
		}
		return null;
	}

	private DocumentSymbol nodeToSymbol(Node n) {
		if (n instanceof Theorem) {
			Theorem t = (Theorem) n;
			Range r = spanToRange(t);
			return new DocumentSymbol(t.getName(), SymbolKind.Function, r, r);
		} else if (n instanceof Judgment) {
			Judgment j = (Judgment) n;
			Range r = spanToRange(j);
			DocumentSymbol sym = new DocumentSymbol(j.getName(), SymbolKind.Interface, r, r);
			List<DocumentSymbol> children = new ArrayList<>();
			for (Rule rule : j.getRules()) {
				Range rr = spanToRange(rule);
				children.add(new DocumentSymbol(rule.getName(), SymbolKind.Method, rr, rr));
			}
			sym.setChildren(children);
			return sym;
		} else if (n instanceof SyntaxDeclaration) {
			SyntaxDeclaration sd = (SyntaxDeclaration) n;
			Range r = spanToRange(sd);
			return new DocumentSymbol(sd.getName(), SymbolKind.Class, r, r);
		}
		return null;
	}

	private Range spanToRange(Span span) {
		if (span == null) return new Range(new Position(0, 0), new Position(0, 1));
		edu.cmu.cs.sasylf.util.Location loc = span.getLocation();
		edu.cmu.cs.sasylf.util.Location end = span.getEndLocation();
		if (loc == null) return new Range(new Position(0, 0), new Position(0, 1));
		int startLine = Math.max(0, loc.getLine() - 1);
		int startCol = Math.max(0, loc.getColumn() - 1);
		int endLine = (end != null && end.getLine() > 0) ? Math.max(0, end.getLine() - 1) : startLine;
		int endCol = (end != null && end.getColumn() > 0) ? Math.max(0, end.getColumn()) : startCol + 1;
		if (endLine < startLine) endLine = startLine;
		if (endLine == startLine && endCol <= startCol) endCol = startCol + 1;
		return new Range(new Position(startLine, startCol), new Position(endLine, endCol));
	}

	static String uriToFilename(String uri) {
		try {
			URI u = new URI(uri);
			File f = new File(u);
			return f.getAbsolutePath();
		} catch (Exception e) {
			if (uri.startsWith("file:///")) {
				String path = uri.substring(8);
				return path.replace('/', File.separatorChar);
			}
			return uri;
		}
	}
}
