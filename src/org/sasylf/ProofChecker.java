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
import org.eclipse.core.runtime.IStatus;
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
import org.sasylf.util.ImmutableDocument;
import org.sasylf.util.TrackDirtyRegions.IDirtyRegion;

import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.module.Module;
import edu.cmu.cs.sasylf.module.ModuleFinder;
import edu.cmu.cs.sasylf.module.ModuleId;
import edu.cmu.cs.sasylf.util.ErrorReport;
import edu.cmu.cs.sasylf.util.Report;
import edu.cmu.cs.sasylf.util.TaskReport;
import edu.cmu.cs.sasylf.util.Util;

/**
 * Check SASyLF Proofs
 */
public class ProofChecker  {

	public static interface Listener {
		/**
		 * We attempted a check of this file.
		 * @param file file checked (perhaps in an editor that hasn't saved yet)
		 * @param proof proof structure, will not be null
		 * @param errors number of errors found.
		 */
		public void proofChecked(IFile file, IDEProof proof, int errors);
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

	protected void informListeners(IFile source, IDEProof proof, int errors) {
		for (Listener l : listeners) {
			l.proofChecked(source, proof, errors);
		}
	}


	private static void report(Report report, IDocument doc, IResource res) {
		IMarker marker;
		String markerId;
		if (report instanceof ErrorReport) markerId = Marker.ERROR_MARKER_ID;
		else if (report instanceof TaskReport) markerId = Marker.TASK_MARKER_ID;
		else markerId = Marker.UNKNOWN_MARKER_ID;
		try {
			marker = res.createMarker(markerId);
			marker.setAttribute(IMarker.MESSAGE, report.getMessage());
			if (report.getSpan() == null || report.getSpan().getLocation() == null) {
				System.err.println("Bad report: " + report);
			} else {
				marker.setAttribute(IMarker.LINE_NUMBER, report.getSpan().getLocation().getLine());
				try {
					Position p = DocumentUtil.getPosition(report.getSpan(), doc);
					marker.setAttribute(IMarker.CHAR_START, p.offset);
					marker.setAttribute(IMarker.CHAR_END, p.offset+p.length);
				} catch (BadLocationException e) {
					System.err.println("bad location? " + e);
				}
			}
			if (report instanceof ErrorReport) {
				ErrorReport er = (ErrorReport)report;
				marker.setAttribute(IMarker.SEVERITY,
						report.isError() ? IMarker.SEVERITY_ERROR
								: IMarker.SEVERITY_WARNING);
				if (er.errorType != null) {
					marker.setAttribute(Marker.SASYLF_ERROR_TYPE, er.errorType.toString());
				}
				marker.setAttribute(Marker.SASYLF_ERROR_INFO, er.getExtraInformation());
				if (MarkerResolutionGenerator.hasProposals(marker)) {
					marker.setAttribute(Marker.HAS_QUICK_FIX, true);
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public static void deleteAuditMarkers(IResource project) {
		try {
			project.deleteMarkers(Marker.ERROR_MARKER_ID, false, IResource.DEPTH_INFINITE);
			project.deleteMarkers(Marker.TASK_MARKER_ID, false, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Check proofs for a resource bound in an editor.
	 * @param res resource (must not be null)
	 * @param editor editor for resource, may be null if not known
	 * @return compilation unit (inf any) resulting from the analysis
	 */
	public static Module analyzeSlf(IResource res, IEditorPart editor) {
		if (editor == null || !(editor instanceof ITextEditor)) {
			return analyzeSlf(null, null, res);
		}
		ITextEditor ite = (ITextEditor)editor;
		IDocument doc = ite.getDocumentProvider().getDocument(ite.getEditorInput());
		return analyzeSlf(res, doc);
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
		return analyzeSlf(null, null, res, doc, new StringReader(doc.get()));
	}

	/**
	 * Analyze a resource as found with the given module finder using the id.
	 * @param mf module finder used to find resource (may be null)
	 * @param id identifier used to find resource (may be null)
	 * @param res resource, must not be null and must either have
	 * a document associated with it and/or a file.
	 * @return compilation unit that was read, or null if there were problems.
	 */
	public static CompUnit analyzeSlf(ModuleFinder mf, ModuleId id, IResource res) {
		IDocument doc = EclipseUtil.getDocumentFromResource(res);
		IFile f = res.getAdapter(IFile.class);
		if (doc == null && f == null) {
			System.out.println("cannot get contents of resource");
			return null;
		} else {
			Reader r;
			try {
				if (doc != null)
					r = new StringReader(doc.get());
				else r = new InputStreamReader(f.getContents(),"UTF-8");
			} catch (UnsupportedEncodingException e) {
				return null;
			} catch (CoreException e) {
				return null;
			}
			return analyzeSlf(mf, id, res, doc, r);
		}
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
		IDEProof oldProof = IDEProof.getProof(res);
		
		// try to correct some nulls.
		
		if (mf == null) {
			ProofBuilder pb = ProofBuilder.getProofBuilder(res.getProject());
			if (pb != null) {
				mf = pb.getModuleFinder();
			} else {
				// OK: This means that the file is outside of a proof folder
				// and we won't do module/package checks.
			}
		}
		if (doc == null) {
			try {
				doc = new ImmutableDocument(res);
			} catch (RuntimeException ex) {
				// not good, but we'll muddle along after logging our problem
				Bundle myBundle = Platform.getBundle(Activator.PLUGIN_ID);
				if (myBundle != null) {
					Platform.getLog(myBundle).log(new Status(IStatus.ERROR,Activator.PLUGIN_ID,"Internal error",ex));
				}
				ex.printStackTrace();
			}
		}
		if (id == null) {
			id = ProofBuilder.getId(res);
		}
		
		IDEProof newProof = new IDEProof(res.toString(), id, res, doc);

		// System.out.println("Reparsing...");

		// TODO: eventually we want to do incremental checking.
		List<IDirtyRegion> dirtyRegions = oldProof == null ? null : oldProof.getChanges(doc);
		if (dirtyRegions != null) {
			try {
				if (Util.DEBUG) { // or perhaps a debug flag specifically for incrementality
					for (IDirtyRegion dr : dirtyRegions) {
						String newText = doc.get(dr.getOffset(), dr.getLength());
						System.out.println("Replacing '" + dr.getOldText() + "' with '" + newText + "'");
					}
				}
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
		
		Util.PRINT_ERRORS = false; // not needed for IDE
		Util.PRINT_SOLVE = false;
		Util.COMP_WHERE = Preferences.isWhereCompulsory();
		Util.X_CONTEXT_IS_SYNTAX = Preferences.experimentalfeature("ContextIsSyntax");
		newProof.parseAndCheck(mf, contents);
		
		int errors = 0;
		deleteAuditMarkers(res);
		for (Report r : newProof.getReports()) {
			report(r, doc, res);
			if (r instanceof ErrorReport) ++errors;
		}
		
		if (!IDEProof.changeProof(oldProof, newProof)) {
			System.out.println("Concurrent compile got there ahead of us for " + res);
		} else {
			if (res instanceof IFile) getInstance().informListeners((IFile)res, newProof, errors);
			else System.out.println("Can't inform listeners since not IFile: " + res);
		}

		return newProof.getCompilation();
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