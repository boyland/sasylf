package org.sasylf.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.sasylf.ProofChecker;
import org.sasylf.handlers.QuickFixHandler;
import org.sasylf.project.ProofBuilder;
import org.sasylf.util.DocumentUtil;
import org.sasylf.views.ProofOutline;

import edu.cmu.cs.sasylf.ast.Case;
import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.ast.Derivation;
import edu.cmu.cs.sasylf.ast.DerivationByAnalysis;
import edu.cmu.cs.sasylf.ast.Judgment;
import edu.cmu.cs.sasylf.ast.Theorem;


public class ProofEditor extends TextEditor implements ProofChecker.Listener {
  public ProofEditor() {
    ProofChecker.getInstance().addListener(this);
  }
  
  public static final String SASYLF_PROOF_CONTEXT = "org.sasylf.context";
  
	
	@Override
	public void doSave(IProgressMonitor progressMonitor) {
		super.doSave(progressMonitor);
		IEditorInput iei = getEditorInput();
		getProofOutline().setInput(iei);
		if (iei instanceof IFileEditorInput) {
		  IFile f = ((IFileEditorInput)iei).getFile();
		  if (f != null) {
		    IProject p = f.getProject();
		    if (ProofBuilder.getProofBuilder(p) != null) return;
		    // no proof builder to automatically parse the file, so we do it ourselves:
		    ProofChecker.analyzeSlf(f, this);
		  }
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

  @Override
  protected ISourceViewer createSourceViewer(Composite parent,
      IVerticalRuler ruler, int styles) {
    ISourceViewer viewer = new ProjectionViewer(parent, ruler,
        getOverviewRuler(), isOverviewRulerVisible(), styles);

    // ensure decoration support has been created and configured.
    // Eclipse help suggests doing this.
    getSourceViewerDecorationSupport(viewer);

    return viewer;
  }

  
  @Override
  public void createPartControl(Composite parent) {
    super.createPartControl(parent);
    ProjectionViewer viewer =(ProjectionViewer)getSourceViewer();

    // NB: we may need to declare this as a field.
    ProjectionSupport projectionSupport = new ProjectionSupport(viewer,getAnnotationAccess(),getSharedColors());
    projectionSupport.install();

    //turn projection mode on
    viewer.doOperation(ProjectionViewer.TOGGLE);

  }

  private static final String PROOF_PROJECTION_CATEGORY = "org.sasylf.category";

  public void updateFoldingStructure(List<Position> positions)
  {
    ProjectionAnnotationModel annotationModel = getProjectionAnnotationModel();
    IDocument doc = getDocument();
    
    if (!doc.containsPositionCategory(PROOF_PROJECTION_CATEGORY)) {
      doc.addPositionCategory(PROOF_PROJECTION_CATEGORY);       
    }

    // normally we cannot use Positions in maps, but since we are in charge
    // of adjusting them and we will not adjust them during this method, we can use
    // hashing.
    Map<Position,ProjectionAnnotation> oldAnnos = new HashMap<Position,ProjectionAnnotation>();
    Map<ProjectionAnnotation,Position> newAnnos = new HashMap<ProjectionAnnotation,Position>();
    
    for (Iterator<?> it = annotationModel.getAnnotationIterator(); it.hasNext();) {
      Object x = it.next();
      if (x instanceof ProjectionAnnotation) {
        ProjectionAnnotation anno = (ProjectionAnnotation)x;
        oldAnnos.put(annotationModel.getPosition(anno), anno);
      }
    }
    
    Annotation[] removeAnnos;

    try {
      for (Position p : positions) {
        if (oldAnnos.remove(p) == null) {
          ProjectionAnnotation annotation = new ProjectionAnnotation();
          doc.addPosition(PROOF_PROJECTION_CATEGORY, p);
          newAnnos.put(annotation, p);
        }
      }

      removeAnnos = oldAnnos.values().toArray(new Annotation[oldAnnos.size()]);

      for (Position p : oldAnnos.keySet()) {
        doc.removePosition(PROOF_PROJECTION_CATEGORY, p);
      }
    } catch (BadLocationException e) {
      // shouldn't happen
      e.printStackTrace();
      return;
    } catch (BadPositionCategoryException e) {
      // shouldn't happen
      e.printStackTrace();
      return;
    }

    annotationModel.modifyAnnotations(removeAnnos, newAnnos,null);
  }

  /**
   * @return
   */
  public ProjectionAnnotationModel getProjectionAnnotationModel() {
    ProjectionViewer viewer = (ProjectionViewer)getSourceViewer();
    ProjectionAnnotationModel annotationModel = viewer.getProjectionAnnotationModel();
    return annotationModel;
  }
  
  
  @Override
  public void proofChecked(IFile file, CompUnit cu) {
    if (file == null || cu == null) return;
    if (getEditorInput().getAdapter(IFile.class) == file) {
      IDocument doc = getDocument();
      List<Position> positions = new ArrayList<Position>();
      
      try {
        for (Judgment j : cu.getJudgments()) {
          Position p = DocumentUtil.getPositionToNextLine(j, doc);
          positions.add(p);
        }
        for (Theorem th : cu.getTheorems()) {
          Position p = DocumentUtil.getPosition(th, doc);
          positions.add(p);
          findFoldable(doc,th.getDerivations(),positions);
        }
      } catch (BadLocationException e) {
        e.printStackTrace();
      }

      updateFoldingStructure(positions);
    }
  }

  private void findFoldable(IDocument document, List<Derivation> ds, List<Position> positions) throws BadLocationException {
    for (Derivation d : ds) {
      if (d instanceof DerivationByAnalysis) {
        DerivationByAnalysis dba = (DerivationByAnalysis)d;
        if (dba.getCases().isEmpty()) continue; // no reason to fold
        positions.add(DocumentUtil.getPosition(d, document));
        for (Case c : dba.getCases()) {
          positions.add(DocumentUtil.getPosition(c, document));
          findFoldable(document,c.getDerivations(),positions);
        }
      }
    }
  }
  
  public void doSetInput(IEditorInput input) throws CoreException {
    super.doSetInput(input);
		if (fOutlinePage != null)
			fOutlinePage.setInput(input);
  }
  
  
  public void dispose() {
    super.dispose();
    ProofChecker.getInstance().removeListener(this);
  }
  
}
