package org.sasylf.wizards;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.sasylf.project.MyNature;
import org.sasylf.project.ProofBuilder;

/**
 * The "New" proof wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name
 * with the extension that matches the expected one (slf).
 */

public class NewProofPackageWizardPage extends WizardPage {
	private IProject project;
	private Text packageNameText;

	private ISelection selection;

	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public NewProofPackageWizardPage(ISelection selection) {
		super("proofPackageWizardPage");
		setTitle("SASyLF Proof Package");
		setDescription("This wizard creates a new proof package.");
		this.selection = selection;
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		Label label = new Label(container, SWT.NULL);
		label = new Label(container, SWT.NULL);
		label.setText("&Package name:");

		packageNameText = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		packageNameText.setLayoutData(gd);
		packageNameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		initialize();
		dialogChanged();
		setControl(container);
	}

	/**
	 * Tests if the current workbench selection is a suitable container to use.
	 */

	private void initialize() {
		if (selection != null && selection.isEmpty() == false
				&& selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() > 1)
				return;
			Object obj = ssel.getFirstElement();
			IContainer container = null;
			if (obj instanceof IResource) {
				if (obj instanceof IContainer)
					container = (IContainer) obj;
				else
					container = ((IResource) obj).getParent();
			} else if (obj instanceof IAdaptable) {
				container = (IContainer)((IAdaptable)obj).getAdapter(IContainer.class);
			}
			if (container != null) {
				project = container.getProject();
				IPath ip = ProofBuilder.getProofFolderRelativePath(container);
				StringBuilder sb = new StringBuilder();
				String[] segs = ip.segments();
				for (int i=0; i < segs.length; ++i) {
					if (i != 0) sb.append('.');
					sb.append(segs[i]);
				}
				packageNameText.setText(sb.toString());
			}
		}
	}

	/**
	 * Ensures that both text fields are set.
	 */

	private void dialogChanged() {
		String packageName = getPackageName();

		if (project == null) {
			updateStatus("Can't find project.  Can't create SASyLF Proof package");
			return;
		}

		if (!project.isAccessible()) {
			updateStatus("Project is closed.  Can't create SASyLF Proof package");
			return;
		}

		try {
			if (!project.hasNature(MyNature.NATURE_ID)) {
				updateStatus("Can only create proof packages in SASyLF Projects");
				return;
			}
		} catch (CoreException e) {
			// should never happen
			e.printStackTrace();
			updateStatus(e.toString());
			return;
		}

		if (packageName.length() == 0) {
			updateStatus("Need to specify package name");
			return;
		}

		if (packageName.startsWith(".")) {
			updateStatus("Package name cannot start with a dot.");
			return;
		}

		if (packageName.endsWith(".")) {
			updateStatus("Package name cannot end with a dot.");
			return;
		}

		IWorkspace ws = ResourcesPlugin.getWorkspace();
		String[] segs = packageName.split("\\.");
		IContainer f = ProofBuilder.getProofFolder(project);
		for (int i=0; i < segs.length; ++i) {
			if (!ws.validateName(segs[i], IResource.FOLDER).isOK()) {
				updateStatus("Package name has illegal segment '" + segs[i] + "'");
				return;
			}
			f = f.getFolder(new Path(segs[i]));
		}

		if (f.exists()) {
			updateStatus("Package already exists.");
			return;
		}    

		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public IProject getProject() {
		return project;
	}

	public String getPackageName() {
		return packageNameText.getText();
	}
}