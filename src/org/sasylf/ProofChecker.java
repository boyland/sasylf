package org.sasylf;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.osgi.framework.Bundle;
import org.sasylf.editors.MarkerResolutionGenerator;
import org.sasylf.project.MyNature;
import org.sasylf.project.ProofBuilder;
import org.sasylf.util.DocumentUtil;
import org.sasylf.util.EclipseUtil;
import org.sasylf.util.ResourceDocument;
import org.sasylf.util.TrackDirtyRegions.IDirtyRegion;

import edu.cmu.cs.sasylf.Main;
import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.ast.ModuleFinder;
import edu.cmu.cs.sasylf.ast.ModuleId;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.ErrorReport;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.SASyLFError;

/**
 * Check SASyLF Proofs
 */
public class ProofChecker  {

  public static interface Listener {
    /**
     * We attempted a check of this file.
     * @param file file checked (perhaps in an editor that hasn't saved yet)
     * @param cu compilation unit, if any parsed (may be null).
     * @param errors number of errors found.
     */
    public void proofChecked(IFile file, CompUnit cu, int errors);
  }
  
	/**
	 * The constructor.
	 */
	private ProofChecker() { }
	
	private static ProofChecker instance;
	
	public static ProofChecker getInstance() {
	  synchronized (ProofChecker.class) {
	    if (instance == null) instance = new ProofChecker();
	    return instance;
	  }
	}
	
	private volatile Collection<Listener> listeners = Collections.emptyList();
	
	public boolean addListener(Listener l) {
	  synchronized (this) {
	    Collection<Listener> newListeners = new ArrayList<Listener>(listeners);
	    boolean result = newListeners.add(l);
	    listeners = newListeners;
	    return result;
	  }
	}
	
  public boolean removeListener(Listener l) {
    synchronized (this) {
      Collection<Listener> newListeners = new ArrayList<Listener>(listeners);
      boolean result = newListeners.remove(l);
      listeners = newListeners;
      return result;
    }
  }
  
  protected void informListeners(IFile source, CompUnit cu, int errors) {
    for (Listener l : listeners) {
      l.proofChecked(source, cu, errors);
    }
  }
  

	private static final String MARKER_ID = Marker.MARKER_ID;

	private static void reportProblem(ErrorReport report, IDocument doc, IResource res) {
		IMarker marker;
		try {
			marker = res.createMarker(MARKER_ID);
			marker.setAttribute(IMarker.MESSAGE, report.getShortMessage());
			marker.setAttribute(IMarker.LINE_NUMBER, report.loc.getLocation().getLine());
			try {
			  Position p = DocumentUtil.getPosition(report.loc, doc);
			  marker.setAttribute(IMarker.CHAR_START, p.offset);
			  marker.setAttribute(IMarker.CHAR_END, p.offset+p.length);
			} catch (BadLocationException e) {
			  System.err.println("bad location? " + e);
			}

			marker.setAttribute(IMarker.SEVERITY,
					report.isError ? IMarker.SEVERITY_ERROR
							: IMarker.SEVERITY_WARNING);
			if (report.errorType != null) {
			  marker.setAttribute(Marker.SASYLF_ERROR_TYPE, report.errorType.toString());
			}
			marker.setAttribute(Marker.SASYLF_ERROR_INFO, report.debugInfo);
			if (MarkerResolutionGenerator.hasProposals(marker)) {
			  marker.setAttribute(Marker.HAS_QUICK_FIX, true);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public static void deleteAuditMarkers(IResource project) {
		try {
			project.deleteMarkers(MARKER_ID, false, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public static CompUnit analyzeSlf(ModuleFinder mf, ModuleId id, IResource res, IEditorPart editor) {
    if (editor == null || !(editor instanceof ITextEditor)) {
      return analyzeSlf(mf, id, res);
    }
    ITextEditor ite = (ITextEditor)editor;
    IDocument doc = ite.getDocumentProvider().getDocument(ite.getEditorInput());
    return analyzeSlf(mf, id, res, doc);
  }

  public static CompUnit analyzeSlf(ModuleFinder mf, ModuleId id, IResource res) {
	  IDocument doc = EclipseUtil.getDocumentFromResource(res);
	  IFile f = (IFile)res.getAdapter(IFile.class);
	  if (doc == null && f == null) {
	    System.out.println("cannot get contents of resource");
	    return null;
	  } else {
	    try {
	      if (doc != null)
          return analyzeSlf(mf, id, res, doc);
        else return analyzeSlf(mf,id,res, doc, new InputStreamReader(f.getContents(),"UTF-8"));
      } catch (UnsupportedEncodingException e) {
        return null;
      } catch (CoreException e) {
        return null;
      }
	  }
	}
	
  /**
   * Check proofs for resource currently being edited as a document. 
   * @param mf TODO
   * @param id TODO
   * @param res resource to which to attach error and warning markers.
   * Must not be null
   * @param doc document holding content.  Must not be null.
   * @return compilation unit of the parse, or null (if a serious error)
   */
  public static CompUnit analyzeSlf(ModuleFinder mf, ModuleId id, IResource res, IDocument doc) {
    if (res == null) throw new NullPointerException("resource cannot be null");
    return analyzeSlf(mf, id, res, doc, new StringReader(doc.get()));
  }
	
  public static String getProofFolderRelativePathString(IResource res) {
    IProject p = res.getProject();
    try {
      if (p.hasNature(MyNature.NATURE_ID)) {
        IPath rpath = ProofBuilder.getProofFolderRelativePath(res);
        return rpath.toOSString();
      }
    } catch (CoreException e) {
      // Apparently not.
    }
    return null;
  }

  private static CompUnit analyzeSlf(ModuleFinder mf, ModuleId id, IResource res, IDocument doc, Reader contents) {
    CompUnit result = null;
    Proof oldProof = Proof.getProof(res);
    Proof newProof = new Proof(res,doc);
    int errors = 0;
    
    try {
      if (doc == null) {
        doc = new ResourceDocument(res);
      }
      // eventually we want to do incremental checking.
      List<IDirtyRegion> dirtyRegions = oldProof == null ? null : oldProof.getChanges(doc);
      if (dirtyRegions != null) {
        try {
          for (IDirtyRegion dr : dirtyRegions) {
            String newText = doc.get(dr.getOffset(), dr.getLength());
            System.out.println("Replacing '" + dr.getOldText() + "' with '" + newText + "'");
          }
        } catch (BadLocationException e) {
          e.printStackTrace();
        }
      }
      result = Main.parseAndCheck(mf, res.getName(), id, contents);
      newProof.setCompilation(result);
		} catch (SASyLFError e) {
			// ignore the error; it has already been reported
		} catch (RuntimeException e) {
		  Bundle myBundle = Platform.getBundle(Activator.PLUGIN_ID);
		  if (myBundle != null) {
		    Platform.getLog(myBundle).log(new Status(Status.ERROR,Activator.PLUGIN_ID,"Internal error",e));
		  }
		  ErrorHandler.recoverableError(Errors.INTERNAL_ERROR, null);
		  e.printStackTrace();
			// unexpected exception
		} catch (CoreException e) {
      Bundle myBundle = Platform.getBundle(Activator.PLUGIN_ID);
      if (myBundle != null) {
        Platform.getLog(myBundle).log(new Status(Status.ERROR,Activator.PLUGIN_ID,"Internal error",e));
      }
      e.printStackTrace();
    } finally {
			deleteAuditMarkers(res);
			for (ErrorReport er : ErrorHandler.getReports()) {
				reportProblem(er, doc, res);
				++errors;
			}
			ErrorHandler.clearAll();
		}
    
    if (!Proof.changeProof(oldProof, newProof)) {
      System.out.println("Concurrent compile got there ahead of us for " + res);
    } else {
      if (res instanceof IFile) getInstance().informListeners((IFile)res,result, errors);
    }
    
		return result;
	}

  /**
   * @param res
   */
  public static void dumpMarkers(IResource res) {
    System.out.println("Printing all markers on " + res);
		try {
      for (IMarker m : res.findMarkers(null, true, IResource.DEPTH_INFINITE)) {
        System.out.println("Marker is subtype of problem marker? " + m.isSubtypeOf("org.eclipse.core.resources.problemmarker"));
        System.out.println("Marker found with message " + m.getAttribute(IMarker.MESSAGE, "<none>"));
      }
    } catch (CoreException e) {
      e.printStackTrace();
    }
  }

}