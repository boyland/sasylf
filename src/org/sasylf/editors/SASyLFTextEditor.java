package org.sasylf.editors;

import org.eclipse.ui.editors.text.TextEditor;



public class SASyLFTextEditor extends TextEditor {
	public SASyLFTextEditor () {
		super ();
		setSourceViewerConfiguration(new SASyLFSourceViewerConfiguration());
	}
}
