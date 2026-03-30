package edu.cmu.cs.sasylf.lsp;

import java.util.concurrent.ConcurrentHashMap;

public class DocumentManager {
	private final ConcurrentHashMap<String, String> documents = new ConcurrentHashMap<>();

	public void open(String uri, String text) {
		documents.put(uri, text);
	}

	public void update(String uri, String text) {
		documents.put(uri, text);
	}

	public void close(String uri) {
		documents.remove(uri);
	}

	public String get(String uri) {
		return documents.get(uri);
	}

	public boolean contains(String uri) {
		return documents.containsKey(uri);
	}
}
