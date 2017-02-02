package org.sasylf.handlers;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.IProgressService;
import org.sasylf.Activator;
import org.sasylf.project.FolderPackageFragment;
import org.sasylf.project.IPackageFragment;
import org.sasylf.project.ProofBuilder;

public class DeletePackageHandler extends AbstractHandler {

	public DeletePackageHandler() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
		IStructuredSelection structuredSelection = null;
		final Collection<FolderPackageFragment> frags = new ArrayList<FolderPackageFragment>(); 
		if (currentSelection instanceof IStructuredSelection) {
			structuredSelection = (IStructuredSelection)currentSelection;
			for (Iterator<?> it = structuredSelection.iterator(); it.hasNext(); ) {
				Object x = it.next();
				if (x instanceof FolderPackageFragment) {
					frags.add((FolderPackageFragment) x);
				}
			}
		}
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		if (frags.isEmpty()) {
			MessageDialog.openError(window.getShell(), "Delete Proof Package", 
					"No proof packages selected");
			return null;
		}
		IRunnableWithProgress op = new WorkspaceModifyOperation() {
			@Override
			public void execute(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doDelete(frags, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		StringBuilder sb = new StringBuilder();
		for (IPackageFragment pf : frags) {
			sb.append("\n    ");
			sb.append(pf.toString());
		}
		if (!MessageDialog.openConfirm(window.getShell(), "Delete Packages", "OK to delete" + sb.toString() + "\n?")) {
			return null;
		}
		try {
			IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
			progressService.busyCursorWhile(op);
			// window.run(true, true, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(window.getShell(), "Error", realException.getMessage());
			return false;
		}
		return null;
	}

	private static class StringArrayComparator implements Comparator<String[]> {

		@Override
		public int compare(String[] arg0, String[] arg1) {
			int d;
			d = arg1.length - arg0.length; // long strings before short strings.
			if (d != 0) return d;
			for (int i=0; i < arg0.length; ++i) {
				d = arg0[i].compareTo(arg1[i]);
				if (d != 0) return d;
			}
			return 0;
		}

	}

	private static Comparator<String[]> stringArrayComparator = new StringArrayComparator();

	private void doDelete(Collection<FolderPackageFragment> frags, IProgressMonitor monitor) 
			throws CoreException
	{
		PriorityQueue<String[]> work = new PriorityQueue<String[]>(frags.size(),stringArrayComparator);
		int totalWork = 0;
		IContainer pf =null;
		for (FolderPackageFragment f : frags) {
			String[] name = f.getName();
			IContainer fres = (IContainer)f.getBaseObject();
			IContainer npf = ProofBuilder.getProofFolder(fres.getProject());
			if (pf == null) {
				pf = npf;
			} else if (!pf.equals(npf)) {
				throwCoreException("Can't delete packages in different folders.");
			}
			for (IResource m : fres.members()) {
				if (!(m instanceof IFolder)) {
					++totalWork; // for each file we try to delete
				}
			}
			if (!work.add(name)) {
				totalWork += name.length; // counting parent directories
			}
		}
		monitor.beginTask("Delete Packages", totalWork);
		while (!work.isEmpty()) {
			String[] packName = work.remove();
			// System.out.println("deleting: " + Arrays.toString(packName));
			IContainer f = pf;
			for (int i=0; i < packName.length; ++i) {
				f = f.getFolder(new Path(packName[i]));
			}
			if (!f.exists()) {
				// already deleted from a child
				monitor.worked(packName.length);
				continue;
			}
			boolean hasSubdir = false;
			for (IResource part : f.members()) {
				if (part instanceof IFolder) {
					hasSubdir = true;
				} else {
					part.delete(false, monitor);
					monitor.worked(1);
				}
			}
			// make sure to do this check before deleting this folder.
			// a package cannot be inessential if it has no children.
			if (packName.length > 1) {
				// check if parent packages should be deleted
				IPackageFragment parentFrag = new FolderPackageFragment((IFolder)f.getParent());
				if (parentFrag.isInessential()) {
					// this meets up with the monitor definition as long as "inEssential"
					// ensures we have no normal files.
					work.add(parentFrag.getName());
				} else {
					monitor.worked(packName.length-1); // skip these steps
				}
			}
			if (!hasSubdir) {
				f.delete(false, monitor);
			}
			monitor.worked(1);
		}
		monitor.done();
	}

	private void throwCoreException(String message) throws CoreException {
		IStatus status =
				new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.OK, message, null);
		throw new CoreException(status);
	}
}
