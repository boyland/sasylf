package editor.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import editor.actions.SasylfMarker;
import editor.editors.propertyOutline.PropertyOutlinePage;
import editor.editors.propertyOutline.PropertyOutlinePage2;

public class PropertiesEditor2 extends TextEditor {
  public PropertiesEditor2() {}
	
	@Override
	public void doSave(IProgressMonitor progressMonitor) {
		super.doSave(progressMonitor);
		IEditorInput iei = getEditorInput();
		fOutlinePage.setInput(iei);
		IFileEditorInput ifi = (IFileEditorInput) iei;
		if (iei instanceof IFileEditorInput) {
			SasylfMarker.analyzeSlf(ifi.getFile());
		}
	}

	@Override
	public void doSaveAs() {
		super.doSaveAs();
		setInput(getEditorInput());
		updateTitle();
	}
	
	void updateTitle() {
		IEditorInput input = getEditorInput();
		setPartName(input.getName());
		setTitleToolTip(input.getToolTipText());
	}

	private PropertyOutlinePage2 fOutlinePage;
	
	@Override
	public Object getAdapter(Class adapter) {
		if (IContentOutlinePage.class.equals(adapter)) {
			if (fOutlinePage == null) {
				fOutlinePage= new PropertyOutlinePage2(getDocumentProvider(), this);
				if (getEditorInput() != null)
					fOutlinePage.setInput(getEditorInput());
			}
			return fOutlinePage;
		}
		return super.getAdapter(adapter);
	}

	@Override
	protected void initializeEditor() {
		super.initializeEditor();
		setSourceViewerConfiguration(new SASyLFSourceViewerConfiguration());
//		setDocumentProvider(new PropertyDocumentProvider());
	}
	
	public void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		if (fOutlinePage != null)
			fOutlinePage.setInput(input);
	}
}
