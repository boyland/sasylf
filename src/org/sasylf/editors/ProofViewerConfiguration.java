package org.sasylf.editors;

import org.eclipse.jface.text.DefaultTextHover;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.ContentFormatter;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;



public class ProofViewerConfiguration extends TextSourceViewerConfiguration {

	static class SingleTokenScanner extends BufferedRuleBasedScanner {
		public SingleTokenScanner (Token defToken) {
			setDefaultReturnToken(defToken);
		}   
	}

	private final ProofEditor editor;

	public ProofViewerConfiguration (ProofEditor ed){
		editor = ed;
	}

	@Override
	public IPresentationReconciler getPresentationReconciler (ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		DefaultDamagerRepairer dr = new DefaultDamagerRepairer (SASyLFEditorEnvironment.getSASyLFCodeScanner ());
		reconciler.setDamager (dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer (dr, IDocument.DEFAULT_CONTENT_TYPE);
		return reconciler;
	}

	@Override
	public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer,
			String contentType) {
		// System.out.println("getAutoEditStrategies: contentType =  " + contentType);
		return new IAutoEditStrategy[]{ new SASyLFAutoIndentStrategy() };
	}

	@Override
	public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
		ContentFormatter result = new ContentFormatter();
		// result.setFormattingStrategy(new ProofFormattingStrategy(), IDocument.DEFAULT_CONTENT_TYPE);
		return result;
	}

	@Override
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		ContentAssistant assist = new ContentAssistant();
		assist.setContentAssistProcessor(new ProofContentAssistProcessor(editor,assist), IDocument.DEFAULT_CONTENT_TYPE);
		assist.setInformationControlCreator(getInformationControlCreator(sourceViewer));
		assist.enableAutoActivation(true);

		return assist;
	}

	@Override
	public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
		QuickAssistAssistant assist = new QuickAssistAssistant();
		assist.setQuickAssistProcessor(new ProofQuickFixProcessor(editor.getDocument()));
		return assist;
	}

	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		return new DefaultTextHover(sourceViewer);
	}
}
