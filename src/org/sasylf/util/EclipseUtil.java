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
		if (resource == null) return null;
		IFile f = resource.getAdapter(IFile.class);
		if (f == null) return null;
		IEditorInput editorInput = new FileEditorInput(f);
		IDocumentProvider dp = DocumentProviderRegistry.getDefault().getDocumentProvider(editorInput);
		if (dp == null) return null;
		return dp.getDocument(editorInput);
	}
}
