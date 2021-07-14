package org.sasylf.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;

/**
 * An immutable document for a (resource) storage
 */
public class ImmutableDocument extends AbstractDocument {

	/**
	 * Create a temporary document for a storage.
	 * @param st source of data, must not be null
	 */
	public ImmutableDocument(IEncodedStorage st) {
		final String contents = getContentsAsString(st);
		super.setTextStore(new StringTextStore(contents));
		final DefaultLineTracker tracker = new DefaultLineTracker();
		super.setLineTracker(tracker);
		tracker.set(contents);
		super.completeInitialization();
	}
	
	/**
	 * Create a temporary document for a resource.
	 * @param res source of data, must not be null. Should be a file or related.
	 */
	public ImmutableDocument(IResource res) {
		this(IProjectStorage.Adapter.adapt(res));
	}

	@Override
	public int getLineOfOffset(int pos) throws BadLocationException {
		return super.getLineOfOffset(pos);
	}

	/**
	 * Get the contents of an encoded storage as a string.
	 * @param st source of characters
	 * @return a string, perhaps with a message about error included
	 */
	protected static String getContentsAsString(IEncodedStorage st) {
		StringBuilder sb = new StringBuilder();
		try {
			Reader r = new InputStreamReader(st.getContents(),st.getCharset());
			final Reader in = r;
			BufferedReader br = new BufferedReader(in);
			String s;
			while ((s = br.readLine()) != null) {
				sb.append(s);
				sb.append('\n');
			}
		} catch (IOException | CoreException e) {
			sb.append(e.getLocalizedMessage());
			sb.append('\n');
		}
		final String contents = sb.toString();
		return contents;
	}
}
