package org.sasylf;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.osgi.framework.Bundle;
import org.sasylf.editors.MarkerResolutionGenerator;
import org.sasylf.project.MyNature;
import org.sasylf.project.ProofBuilder;
import org.sasylf.util.EclipseUtil;

import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.ast.Location;
import edu.cmu.cs.sasylf.parser.DSLToolkitParser;
import edu.cmu.cs.sasylf.parser.ParseException;
import edu.cmu.cs.sasylf.parser.TokenMgrError;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.ErrorReport;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.SASyLFError;

/**
 * Check SASyLF Proofs
 */
public class ProofChecker  {

  public static interface Listener {
    public void proofChecked(IFile file, CompUnit cu);
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
  
  protected void informListeners(IFile source, CompUnit cu) {
    for (Listener l : listeners) {
      l.proofChecked(source, cu);
    }
  }
  

	private static final String MARKER_ID = Marker.MARKER_ID;

	private static void reportProblem(ErrorReport report, IResource res) {
		IMarker marker;
		try {
			marker = res.createMarker(MARKER_ID);
			marker.setAttribute(IMarker.MESSAGE, report.getShortMessage());
			marker.setAttribute(IMarker.LINE_NUMBER, report.loc.getLine());
			// marker.setAttribute(IMarker.CHAR_START, report.loc.getColumn());
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

	public static CompUnit analyzeSlf(IResource res, IEditorPart editor) {
    if (editor == null || !(editor instanceof ITextEditor)) {
      return analyzeSlf(res);
    }
    ITextEditor ite = (ITextEditor)editor;
    IDocument doc = ite.getDocumentProvider().getDocument(ite.getEditorInput());
    return analyzeSlf(res, doc);
  }

  public static CompUnit analyzeSlf(IResource res) {
	  IDocument doc = EclipseUtil.getDocumentFromResource(res);
	  IFile f = (IFile)res.getAdapter(IFile.class);
	  if (doc == null && f == null) {
	    System.out.println("cannot get contents of resource");
	    return null;
	  } else {
	    try {
	      if (doc != null)
          return analyzeSlf(res, doc);
        else return analyzeSlf(res,new InputStreamReader(f.getContents(),"UTF-8"));
      } catch (UnsupportedEncodingException e) {
        return null;
      } catch (CoreException e) {
        return null;
      }
	  }
	}
	
  /**
   * Check proofs for resource currently being edited as a document. 
   * @param res resource to which to attach error and warning markers.
   * Must not be null
   * @param doc document holding content.  Must not be null.
   * @return compilation unit of the parse, or null (if a serious error)
   */
  public static CompUnit analyzeSlf(IResource res, IDocument doc) {
    if (res == null) throw new NullPointerException("resource cannot be null");
    return analyzeSlf(res, new StringReader(doc.get()));
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

  private static CompUnit analyzeSlf(IResource res, Reader contents) {
    CompUnit result = null;
    // int oldErrorCount = 0;

    try {
      try {
        result = DSLToolkitParser.read(res.getName(),contents);
      } catch (ParseException e) {
        ErrorHandler.report(null, e.getMessage(), new Location(
            e.currentToken.next), null, true, false);
      } catch (TokenMgrError e) {
        Location loc = ErrorHandler.lexicalErrorAsLocation(res.getName(),e.getMessage());
        ErrorHandler.report(null, e.getMessage(), loc, null, true, false);
      }
      if (result != null) {
        String location = getProofFolderRelativePathString(res);
        result.typecheck(location);
      }

		} catch (SASyLFError e) {
			// ignore the error; it has already been reported
			// sb.append(e.getMessage() + "\n");
		} catch (RuntimeException e) {
		  Bundle myBundle = Platform.getBundle(Activator.PLUGIN_ID);
		  if (myBundle != null) {
		    Platform.getLog(myBundle).log(new Status(Status.ERROR,Activator.PLUGIN_ID,"Internal error",e));
		  }
		  ErrorHandler.recoverableError(Errors.INTERNAL_ERROR, new Location(res.getName(),1,1));
		  e.printStackTrace();

			// unexpected exception
		} finally {
			// int newErrorCount = ErrorHandler.getErrorCount() - oldErrorCount;
			// oldErrorCount = ErrorHandler.getErrorCount();
			// if (newErrorCount == 0)
			// sb.append(filename + ": No errors found.\n");
			// else {
			// if (newErrorCount == 1)
			// sb.append(filename + ": 1 error found\n");
			// else
			// sb.append(filename + ": " + newErrorCount
			// + " errors found\n");

			// deleteAuditMarkers(IResource);
			deleteAuditMarkers(res);
			for (ErrorReport er : ErrorHandler.getReports()) {
				reportProblem(er, res);
			}
			ErrorHandler.clearAll();
		}
    
    if (result != null && res instanceof IFile) getInstance().informListeners((IFile)res,result);
    
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
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}