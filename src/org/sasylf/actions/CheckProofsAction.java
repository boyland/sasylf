package org.sasylf.actions;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.texteditor.ITextEditor;
import org.osgi.framework.Bundle;
import org.sasylf.Activator;
import org.sasylf.Marker;
import org.sasylf.editors.MarkerResolutionGenerator;
import org.sasylf.project.ProofBuilder;
import org.sasylf.util.EclipseUtil;

import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.ast.Errors;
import edu.cmu.cs.sasylf.ast.Location;
import edu.cmu.cs.sasylf.parser.DSLToolkitParser;
import edu.cmu.cs.sasylf.parser.ParseException;
import edu.cmu.cs.sasylf.parser.TokenMgrError;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.ErrorReport;
import edu.cmu.cs.sasylf.util.SASyLFError;

/**
 * Check SASyLF Proofs
 * @see IWorkbenchWindowActionDelegate
 */
public class CheckProofsAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;

	/**
	 * The constructor.
	 */
	public CheckProofsAction() {
	}

	// console message output
	// private static final String CONSOLE_NAME = "";

	// private MessageConsole findConsole(String name) {
	// ConsolePlugin plugin = ConsolePlugin.getDefault();
	// IConsoleManager conMan = plugin.getConsoleManager();
	// IConsole[] existing = conMan.getConsoles();
	// for (int i = 0; i < existing.length; i++)
	// if (name.equals(existing[i].getName()))
	// return (MessageConsole) existing[i];
	// // no console found, so create a new one
	// MessageConsole myConsole = new MessageConsole(name, null);
	// conMan.addConsoles(new IConsole[] { myConsole });
	// return myConsole;
	// }
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

	public static String analyzeSlf(IResource res) {
	  IDocument doc = EclipseUtil.getDocumentFromResource(res);
	  IFile f = (IFile)res.getAdapter(IFile.class);
	  if (doc == null && f == null) {
	    System.out.println("cannot get contents of resource");
	    return "cannot get contents of resource";
	  } else {
	    try {
	      if (doc != null)
          return analyzeSlf(res, doc);
        else return analyzeSlf(res,new InputStreamReader(f.getContents(),"UTF-8"));
      } catch (UnsupportedEncodingException e) {
        return e.toString();
      } catch (CoreException e) {
        return e.toString();
      }
	  }
	}
	
  /**
   * @param ite
   */
  public static void analyzeSlf(ITextEditor ite) {
    analyzeSlf(ResourceUtil.getResource(ite.getEditorInput()),ite);
  }

  public static String analyzeSlf(IResource res, IEditorPart editor) {
    if (editor == null) return analyzeSlf(res);
	  if (!(editor instanceof ITextEditor)) return "cannot open non-text file";
	  ITextEditor ite = (ITextEditor)editor;
	  IDocument doc = ite.getDocumentProvider().getDocument(ite.getEditorInput());
	  return analyzeSlf(res, doc);
	}

  /**
   * Check proofs for resource currently being edited as a document. 
   * @param res resource to which to attach error and warning markers.
   * Must not be null
   * @param doc document holding content.  If null, we will search
   * for an editor for the resource.
   * @return error strings. Not sure why.  May go away.
   */
  public static String analyzeSlf(IResource res, IDocument doc) {
    if (res == null) throw new NullPointerException("resource cannot be null");
    if (doc == null) return analyzeSlf(res);
    return analyzeSlf(res, new StringReader(doc.get()));
  }
	
  public static Location lexicalErrorAsLocation(String file, String error) {
    try {
      int lind = error.indexOf("line ");
      int cind = error.indexOf(", column ");
      int eind = error.indexOf(".", cind+1);
      int line = Integer.parseInt(error.substring(lind+5, cind));
      int column = Integer.parseInt(error.substring(cind+9, eind));
      return new Location(file,line,column);
    } catch (RuntimeException e) {
      return new Location(file,0,0);
    }
  }
  
  public static String getProofFolderRelativePathString(IResource res) {
    IPath rpath = ProofBuilder.getProofFolderRelativePath(res);
    return rpath.toOSString();
  }

  public static String analyzeSlf(IResource res, Reader contents) {
    StringBuilder sb = new StringBuilder(); //XXX: WHy do this?
    
    // int oldErrorCount = 0;

    try {
      CompUnit cu = null;
      try {
        cu = DSLToolkitParser.read(res.getName(),contents);
      } catch (ParseException e) {
        ErrorHandler.report(null, e.getMessage(), new Location(
            e.currentToken.next), null, true, false);
      } catch (TokenMgrError e) {
        Location loc = lexicalErrorAsLocation(res.getName(),e.getMessage());
        ErrorHandler.report(null, e.getMessage(), loc, null, true, false);
      }
      if (cu != null) cu.typecheck(getProofFolderRelativePathString(res));

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
				sb.append(er.getMessage() + "\n");
				reportProblem(er, res);
			}
			ErrorHandler.clearAll();
		}
		return sb.toString();
	}

	/**
	 * The action has been activated. The argument of the method represents the
	 * 'real' action sitting in the workbench UI.
	 * 
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		IEditorPart activeEditorPart = window /*PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow()*/.getActivePage().getActiveEditor();
		ITextEditor ite = (ITextEditor)activeEditorPart.getAdapter(ITextEditor.class);
		if (ite == null) {
		  // some sort of warning?
		  return;
		}
		analyzeSlf(ite);
		
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


	/**
	 * Selection in the workbench has been changed. We can change the state of
	 * the 'real' action here if we want, but this can only happen after the
	 * delegate has been created.
	 * 
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * We can use this method to dispose of any system resources we previously
	 * allocated.
	 * 
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 * 
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}