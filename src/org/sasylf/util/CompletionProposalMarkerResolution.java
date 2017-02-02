package org.sasylf.util;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;

/**
 * Adapt a ICompletionProposal to an IMarkerResolution
 *
 */
public class CompletionProposalMarkerResolution implements IMarkerResolution, IMarkerResolution2 {

	private final IDocument document;
	private final ICompletionProposal proposal;

	public CompletionProposalMarkerResolution(IDocument doc, ICompletionProposal p) {
		document = doc;
		proposal = p;
	}

	@Override
	public String getLabel() {
		return proposal.getDisplayString();
	}

	@Override
	public void run(IMarker marker) {
		proposal.apply(document);
	}

	@Override
	public String getDescription() {
		return proposal.getAdditionalProposalInfo();
	}

	@Override
	public Image getImage() {
		return proposal.getImage();
	}

}