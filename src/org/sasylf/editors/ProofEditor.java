package org.sasylf.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.sasylf.actions.CheckProofsAction;
import org.sasylf.editors.propertyOutline.ProofOutline;


public class ProofEditor extends TextEditor {
  public ProofEditor() {}
  
  public static final String SASYLF_PROOF_CONTEXT = "org.sasylf.context";
  
	
	@Override
	public void doSave(IProgressMonitor progressMonitor) {
		super.doSave(progressMonitor);
		IEditorInput iei = getEditorInput();
		getProofOutline().setInput(iei);
		CheckProofsAction.analyzeSlf(this);
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

	public IDocument getDocument() {
	  return getDocumentProvider().getDocument(this.getEditorInput());
	}
	
	public ISourceViewer getSourceViweer() {
	  return super.getSourceViewer();
	}
	
	private ProofOutline fOutlinePage;
	
	public ProofOutline getProofOutline() {
    if (fOutlinePage == null) {
      fOutlinePage= new ProofOutline(getDocumentProvider(), this);
      if (getEditorInput() != null)
        fOutlinePage.setInput(getEditorInput());
    }
    return fOutlinePage;	  
	}
	
	@SuppressWarnings("rawtypes")
  @Override
	public Object getAdapter(Class adapter) {
		if (IContentOutlinePage.class.equals(adapter)) {
		  return getProofOutline();
		}
		return super.getAdapter(adapter);
	}

	@Override
	protected void initializeEditor() {
		super.initializeEditor();
		setSourceViewerConfiguration(new ProofViewerConfiguration(this));
	}
	
	@Override
  public void init(IEditorSite site, IEditorInput input)
      throws PartInitException {
    super.init(site, input);
    IContextService service = (IContextService) site.getService(IContextService.class);
    if (service == null) {
      System.err.println("can't find a context service");
    } else {
      service.activateContext(SASYLF_PROOF_CONTEXT);
    }    
  }

  public void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		if (fOutlinePage != null)
			fOutlinePage.setInput(input);
  }
	
	
}
