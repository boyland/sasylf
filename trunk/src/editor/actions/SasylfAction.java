package editor.actions;

import java.io.File;
import java.io.FileNotFoundException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.ast.Location;
import edu.cmu.cs.sasylf.parser.DSLToolkitParser;
import edu.cmu.cs.sasylf.parser.ParseException;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.ErrorReport;
import edu.cmu.cs.sasylf.util.SASyLFError;

/**
 * Our sample action implements workbench action delegate. The action proxy will
 * be created by the workbench and shown in the UI. When the user tries to use
 * the action, this delegate will be created and execution will be delegated to
 * it.
 * 
 * @see IWorkbenchWindowActionDelegate
 */
public class SasylfAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;

	/**
	 * The constructor.
	 */
	public SasylfAction() {
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
	private static final String MARKER_ID = "com.cmu.hci.slfmarker";

	private void reportProblem(ErrorReport report, IFile file) {
		IMarker marker;
		try {
			marker = file.createMarker(MARKER_ID);
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

	public static void deleteAuditMarkers(IProject project) {
		try {
			project.deleteMarkers(MARKER_ID, false, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private String analyzeSlf(IFile file) {
		String filename = file.getLocationURI().getPath().replaceFirst("/", "");

		StringBuilder sb = new StringBuilder();

		// int oldErrorCount = 0;

		try {
			CompUnit cu = null;
			try {
				cu = DSLToolkitParser.read(new File(filename));
			} catch (ParseException e) {
				ErrorHandler.report(null, e.getMessage(), new Location(
						e.currentToken.next), null, true, false);
			} catch (FileNotFoundException e) {
				sb.append(filename + "is not found.\n");
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
			deleteAuditMarkers(file.getProject());
			for (ErrorReport er : ErrorHandler.getReports()) {
				sb.append(er.getMessage() + "\n");
				reportProblem(er, file);
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
		IEditorPart activeEditorPart = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		IEditorInput iei = activeEditorPart.getEditorInput();
		IFileEditorInput ifi = (IFileEditorInput) iei;
		// MessageConsole myConsole = findConsole(CONSOLE_NAME);
		// MessageConsoleStream out = myConsole.newMessageStream();
		// System.setOut(new PrintStream(new BufferedOutputStream(out)));

		if (iei instanceof IFileEditorInput) {
			// IFileEditorInput ifi = (IFileEditorInput)iei;

			analyzeSlf(ifi.getFile());
			// IWorkbenchPage page = PlatformUI.getWorkbench()
			// .getActiveWorkbenchWindow().getActivePage();// obtain the
			// // active page
			// String id = IConsoleConstants.ID_CONSOLE_VIEW;
			// IConsoleView view = null;
			// try {
			// view = (IConsoleView) page.showView(id);
			// } catch (PartInitException e) {
			//
			// e.printStackTrace();
			// }
			// view.display(myConsole);
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