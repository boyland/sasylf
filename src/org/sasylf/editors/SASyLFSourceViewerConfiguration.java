package org.sasylf.editors;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;



public class SASyLFSourceViewerConfiguration extends TextSourceViewerConfiguration {

	static class SingleTokenScanner extends BufferedRuleBasedScanner {
        public SingleTokenScanner (TextAttribute attribute) {
            setDefaultReturnToken(new Token (attribute));
        }   
    }
	
	public SASyLFSourceViewerConfiguration (){
	}
	
	public IPresentationReconciler getPresentationReconciler (ISourceViewer sourceViewer) {
		SASyLFColorProvider provider = SASyLFEditorEnvironment.getSASyLFColorProvider();
		PresentationReconciler reconciler = new PresentationReconciler();
		DefaultDamagerRepairer dr = new DefaultDamagerRepairer (SASyLFEditorEnvironment.getSASyLFCodeScanner ());
        reconciler.setDamager (dr, IDocument.DEFAULT_CONTENT_TYPE);
        reconciler.setRepairer (dr, IDocument.DEFAULT_CONTENT_TYPE);
        
        dr = new DefaultDamagerRepairer (new SingleTokenScanner(new TextAttribute (provider.getColor(SASyLFColorProvider.MULTI_LINE_COMMENT))));
        reconciler.setDamager (dr, "__java_multiline_comment");
        reconciler.setRepairer (dr, "__java_multiline_comment");
        return reconciler;
	}

}
