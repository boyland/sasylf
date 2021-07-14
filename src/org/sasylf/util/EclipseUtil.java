package org.sasylf.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.DocumentProviderRegistry;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class EclipseUtil {
	public static final Boolean ensureLoaded = true;
	
	/**
	 * Get the document associated with a given resource.
	 * @param res resource to find document for
	 * @return document associated with resource
	 */
	public static IDocument getDocumentFromResource(IResource resource) {
		return getDocumentFromResource(resource, false);
	}

	/**
	 * Get the document associated with a given resource.
	 * @param res resource to find document for
	 * @return document associated with resource
	 * @param logReason if true then print reasons for a null result
	 * @return document (or null) for this resource
	 */
	public static IDocument getDocumentFromResource(IResource resource,
			boolean logReason) {
		if (resource == null) {
			if (logReason) System.out.println("Cannot get Document for null");
			return null;
		}
		IFile f = resource.getAdapter(IFile.class);
		if (f == null) {
			if (logReason) System.out.println("Cannot get Document for non-file " + resource);
			return null;
		}
		IEditorInput editorInput = new FileEditorInput(f);
		IDocumentProvider dp = DocumentProviderRegistry.getDefault().getDocumentProvider(editorInput);
		if (dp == null) {
			if (logReason) System.out.println("Cannot get document since no Document provider for " + editorInput);
			return null;
		}
		final IDocument document = dp.getDocument(editorInput);
		if (document == null && logReason) {
			System.out.println("Cannot get document since document provider can't do it: " + dp);
		}
		return document;
	}
}
