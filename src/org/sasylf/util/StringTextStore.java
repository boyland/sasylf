package org.sasylf.util;

import org.eclipse.jface.text.ITextStore;

class StringTextStore implements ITextStore {
	private final String contents;
	
	StringTextStore(String contents) {
		this.contents = contents;
	}

	@Override
	public char get(int offset) {
		return contents.charAt(offset);
	}

	@Override
	public String get(int offset, int length) {
		return contents.substring(offset, offset+length);
	}

	@Override
	public int getLength() {
		return contents.length();
	}

	@Override
	public void replace(int offset, int length, String text) {
		throw new UnsupportedOperationException("Document is immutable");
	}

	@Override
	public void set(String text) {
		throw new UnsupportedOperationException("Document is immutable");
	}
	
}