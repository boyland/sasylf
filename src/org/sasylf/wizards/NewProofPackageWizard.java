package org.sasylf.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.progress.IProgressService;
import org.sasylf.project.ProofBuilder;

/**
 * This is a new proof file wizard. It was copied
 * from the sample provided helpfully by the Eclipse team. 
 * Its role is to create a new file 
 * resource in the provided container. If the container resource
 * (a folder or a project) is selected in the workspace 
 * when the wizard is opened, it will accept it as the target
 * container. The wizard creates one file with the extension
 * "slf". Then it will use the proof editor to open it.
 */

public class NewProofPackageWizard extends Wizard implements INewWizard {
	private NewProofPackageWizardPage page;
	private ISelection selection;

	/**
	 * Constructor for SampleNewWizard.
	 */
	public NewProofPackageWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	/**
	 * Adding the page to the wizard.
	 */

	@Override
	public void addPages() {
		page = new NewProofPackageWizardPage(selection);
		addPage(page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	@Override
	public boolean performFinish() {
		final IProject project = page.getProject();
		final String packageName = page.getPackageName();
		IRunnableWithProgress op = new WorkspaceModifyOperation() {
			@Override
			public void execute(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(project, packageName, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
			progressService.busyCursorWhile(op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * The worker method. It will find the container, create the
	 * file if missing or just replace its contents, and open
	 * the editor on the newly created file.
	 */

	private static final int END_CREATION_SIZE = 1;

	private void doFinish(
			IProject project,
			String packageName,
			IProgressMonitor monitor)
					throws CoreException {
		String[] segs = packageName.split("\\.");
		monitor.beginTask("Creating package " + packageName, segs.length+END_CREATION_SIZE);
		IContainer con = ProofBuilder.getProofFolder(project);
		for (int i=0; i < segs.length; ++i) {
			IFolder f = con.getFolder(new Path(segs[i]));
			if (!f.exists()) {
				f.create(false, true, monitor);
			}
			con = f;
			monitor.worked(1);
		}
		monitor.worked(END_CREATION_SIZE);
	}

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}