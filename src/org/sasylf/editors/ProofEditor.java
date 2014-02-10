package org.sasylf.editors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.sasylf.editors.propertyOutline.ProofOutline;
import org.sasylf.handlers.QuickFixHandler;


public class ProofEditor extends TextEditor {
  public ProofEditor() {}
  
  public static final String SASYLF_PROOF_CONTEXT = "org.sasylf.context";
  
	
	@Override
	public void doSave(IProgressMonitor progressMonitor) {
		super.doSave(progressMonitor);
		IEditorInput iei = getEditorInput();
		getProofOutline().setInput(iei);
		// CheckProofsAction.analyzeSlf(this);
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
	  if (getDocumentProvider() == null) return null;
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

	/*
	 * Staging of the various initializations:
	 * BEGIN: initializeEditor
   * END: initializeEditor
   * BEGIN: init
   * BEGIN: doSetInput
   * END: doSetInput
   * END: init
   * BEGIN: createVerticalRuler
   * END: createVerticalRuler
	 */
	
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
	
	protected IVerticalRuler createVerticalRuler() {
	   final IVerticalRuler result = super.createVerticalRuler();
	   Display.getDefault().asyncExec(new Runnable(){
	     public void run() {
	   	  result.getControl().addMouseListener(new MouseListener() {

          @Override
          public void mouseDoubleClick(MouseEvent e) {
            int line = result.getLineOfLastMouseButtonActivity();
            IAnnotationModel model = result.getModel();
            Iterator<?> annos = model.getAnnotationIterator();
            List<IMarker> selected = new ArrayList<IMarker>();
            while (annos.hasNext()) {
              Object anno = annos.next();
              if (anno instanceof MarkerAnnotation) {
                IMarker marker = ((MarkerAnnotation)anno).getMarker();
                if (marker.getAttribute(IMarker.LINE_NUMBER, 0) == line+1 &&
                    MarkerResolutionGenerator.hasProposals(marker)) {
                  selected.add(marker);
                }
              }
            }
            if (selected.size() > 0) {
              QuickFixHandler.showQuickFixes(getDocument(), getSite(), selected.toArray(new IMarker[selected.size()]));
            } else {
              System.out.println("no markers found at line " + line);
            }
          }

          @Override
          public void mouseDown(MouseEvent e) {
            // TODO Auto-generated method stub
            
          }

          @Override
          public void mouseUp(MouseEvent e) {
            // TODO Auto-generated method stub
            
          }
	   	    
	   	  });
	     }});
	   return result;
	}

  public void doSetInput(IEditorInput input) throws CoreException {
    super.doSetInput(input);
		if (fOutlinePage != null)
			fOutlinePage.setInput(input);
  }
}
