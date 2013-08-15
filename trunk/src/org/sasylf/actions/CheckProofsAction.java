package org.sasylf.actions;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.ast.Location;
import edu.cmu.cs.sasylf.parser.DSLToolkitParser;
import edu.cmu.cs.sasylf.parser.ParseException;
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
	private static final String MARKER_ID = SasylfMarker.MARKER_ID;

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
	  IFile f = (IFile)res.getAdapter(IFile.class);
	  if (f == null) {
	    System.out.println("cannot get contents of resource");
	    return "cannot get contents of resource";
	  } else {
	    try {
        return analyzeSlf(res,new InputStreamReader(f.getContents(),"UTF-8"));
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
	  if (!(editor instanceof ITextEditor)) return "cannot open non-text file";
	  ITextEditor ite = (ITextEditor)editor;
	  IDocument doc = ite.getDocumentProvider().getDocument(ite.getEditorInput());
	  return analyzeSlf(res, new StringReader(doc.get()));
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
      }
      cu.typecheck();

		} catch (SASyLFError e) {
			// ignore the error; it has already been reported
			// sb.append(e.getMessage() + "\n");
		} catch (RuntimeException e) {
			sb.append("Internal SASyLF error!\n" + e.getMessage() + "\n");

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