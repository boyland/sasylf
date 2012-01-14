package editor.actions;

import java.io.File;
import java.io.FileNotFoundException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.ast.Location;
import edu.cmu.cs.sasylf.parser.DSLToolkitParser;
import edu.cmu.cs.sasylf.parser.ParseException;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.ErrorReport;
import edu.cmu.cs.sasylf.util.SASyLFError;

public class SasylfMarker {
	
	private static final String MARKER_ID = "com.cmu.hci.slfmarker";

	private static void reportProblem(ErrorReport report, IFile file) {
		IMarker marker;
		try {
			marker = file.createMarker(MARKER_ID);
			marker.setAttribute(IMarker.MESSAGE, report.getShortMessage());
			marker.setAttribute(IMarker.LINE_NUMBER, report.loc.getLine());
			//marker.setAttribute(IMarker.CHAR_START, report.loc.getColumn());
			marker.setAttribute(IMarker.SEVERITY,
					report.isError ? IMarker.SEVERITY_ERROR
							: IMarker.SEVERITY_WARNING);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private static void reportWarning(ErrorReport report, IFile file) {
		IMarker marker;
		try {
			marker = file.createMarker(MARKER_ID);
			marker.setAttribute(IMarker.MESSAGE, report.getShortMessage());
			marker.setAttribute(IMarker.LINE_NUMBER, report.loc.getLine());
			//marker.setAttribute(IMarker.CHAR_START, report.loc.getColumn());
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private static void reportNoneWarning(IFile file) {
		IMarker marker;
		try {
			marker = file.createMarker(MARKER_ID);
			marker.setAttribute(IMarker.MESSAGE, "0 Warning");
			//marker.setAttribute(IMarker.CHAR_START, report.loc.getColumn());
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	private static void reportError(ErrorReport report, IFile file) {
		IMarker marker;
		try {
			marker = file.createMarker(MARKER_ID);
			marker.setAttribute(IMarker.MESSAGE, report.getShortMessage());
			marker.setAttribute(IMarker.LINE_NUMBER, report.loc.getLine());
			//marker.setAttribute(IMarker.CHAR_START, report.loc.getColumn());
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	private static void reportNoneError(IFile file) {
		IMarker marker;
		try {
			marker = file.createMarker(MARKER_ID);
			marker.setAttribute(IMarker.MESSAGE, "0 Error");
			//marker.setAttribute(IMarker.CHAR_START, report.loc.getColumn());
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
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
	
	public static String analyzeSlf(IFile file) {
		StringBuilder sb = new StringBuilder();

		// int oldErrorCount = 0;

		try {
			CompUnit cu = null;
			try {
				cu = DSLToolkitParser.read(file.getName(),file.getContents());
			} catch (ParseException e) {
				ErrorHandler.report(null, e.getMessage(), new Location(
						e.currentToken.next), null, true, false);
			} catch (CoreException e) {
				sb.append(file.getName() + "is not found.\n");
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
			
			//deleteAuditMarkers(IResource);
			deleteAuditMarkers(file.getProject());
			int errCnt = 0;	//Error Count
			int wrnCnt = 0;	//Warning Count
			for (ErrorReport er : ErrorHandler.getReports()) {
				sb.append(er.getMessage() + "\n");
//				if(er.isError) {
//					reportError(er, file);
//					errCnt++;
//				} else {
//					reportWarning(er, file);
//					wrnCnt++;
//				}
				reportProblem(er, file);
			}
//			if(errCnt == 0) {
//				reportNoneError(file);
//			}
//			if(wrnCnt == 0 ) {
//				reportNoneWarning(file);
//			}
			ErrorHandler.clearAll();
		}
		return sb.toString();
	}
}
