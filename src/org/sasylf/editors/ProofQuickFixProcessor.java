package org.sasylf.editors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension2;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.MarkerAnnotation;

/**
 * Class to handle quick fixes in the project.
 * This class doesn't include the logic for the quick fixes, rather that
 * is done in {@link MarkerResolutionGenerator}.
 */
public class ProofQuickFixProcessor implements IQuickAssistProcessor {

	public ProofQuickFixProcessor(IDocument doc) {
		// doc.addDocumentListener(this);
	}

	private String errorMessage;

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public boolean canFix(Annotation annotation) {
		if (annotation instanceof MarkerAnnotation) {
			return MarkerResolutionGenerator.hasProposals(((MarkerAnnotation)annotation).getMarker());
		}
		return false;
	}

	@Override
	public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
		return false;
	}

	private static final ICompletionProposal[] NO_COMPLETION_PROPOSALS = new ICompletionProposal[0];

	@Override
	public ICompletionProposal[] computeQuickAssistProposals(
			IQuickAssistInvocationContext invocationContext) {
		ISourceViewer sourceViewer = invocationContext.getSourceViewer();
		IAnnotationModel model = sourceViewer.getAnnotationModel();
		IDocument document = sourceViewer.getDocument();
		if (!(model instanceof IAnnotationModelExtension2)) {
			errorMessage = "annotation model doesn't support extension2";
			return NO_COMPLETION_PROPOSALS;
		}
		IAnnotationModelExtension2 rmam = (IAnnotationModelExtension2)model;
		int offset = invocationContext.getOffset();
		int length = invocationContext.getLength();
		if (length < 0) try {
			length = 0; // in case we throw an exception
			int line = document.getLineOfOffset(offset);
			// System.out.println("line is " + line);
			offset = document.getLineOffset(line);
			length = document.getLineLength(line);
		} catch (BadLocationException ex) {
			System.err.println("bad location exception!");
			ex.printStackTrace();
			return NO_COMPLETION_PROPOSALS;
		}
		// System.out.println("offset = " + offset + ", length = " + length);
		Iterator<?> annos = rmam.getAnnotationIterator(offset, length, true, true);
		if (!annos.hasNext()) {
			errorMessage = "no annotations in context";
			return NO_COMPLETION_PROPOSALS;
		}
		List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		while (annos.hasNext()) {
			Object anno = annos.next();
			if (anno instanceof MarkerAnnotation) {
				IMarker marker = ((MarkerAnnotation)anno).getMarker();
				if (MarkerResolutionGenerator.hasProposals(marker)) {
					ICompletionProposal[] someProposals = MarkerResolutionGenerator.getProposals(document, marker);
					if (someProposals == null) continue;
					for (ICompletionProposal cp : someProposals) {
						proposals.add(cp);
					}
				}
			}
		}
		if (proposals.isEmpty()) {
			errorMessage = "no markers in context that has fixes";
			System.out.println(errorMessage);
			return NO_COMPLETION_PROPOSALS;
		}
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

}
