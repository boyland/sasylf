package org.sasylf.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.AbstractDocumentProvider;
import org.eclipse.ui.texteditor.DocumentProviderRegistry;

/**
 * A document provider for temporary document
 */
public class ResourceDocumentProvider extends AbstractDocumentProvider {

	public static final String ID = "org.sasylf.editors.ResourceDocumentProvider";
	
	public static ResourceDocumentProvider getInstance(IEditorInput sample) {
		final DocumentProviderRegistry registry = DocumentProviderRegistry.getDefault();
		ResourceDocumentProvider result = (ResourceDocumentProvider)registry.getDocumentProvider(sample);
		if (result == null) {
			result = new ResourceDocumentProvider();
			System.out.println("Warning: Registry didn't have document provider.");
		}
		return result;
	}
	
	@Override
	protected IDocument createDocument(Object element) throws CoreException {
		// System.out.println(index + ": creating document for " + element);
		IProjectStorage st = IProjectStorage.Adapter.adapt(element);
		if (st == null) return null;
		return new ImmutableDocument(st);
	}

	@Override
	protected IAnnotationModel createAnnotationModel(Object element)
			throws CoreException {
		return new AnnotationModel();
	}

	@Override
	protected void doSaveDocument(IProgressMonitor monitor, Object element,
			IDocument document, boolean overwrite) throws CoreException {
	}

	@Override
	protected IRunnableContext getOperationRunner(IProgressMonitor monitor) {
		return null;
	}

}
