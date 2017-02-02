package org.sasylf.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.sasylf.Preferences;

public class NewProjectWizardPage extends WizardNewProjectCreationPage {

	public NewProjectWizardPage(String pageName) {
		super(pageName);
		// TODO Auto-generated constructor stub
	}

	private Text proofFolderNameField;

	@Override
	protected void setControl(Control parent) {
		// this is our last chance to add something to the Wizard Page
		if (proofFolderNameField == null) {
			initialize((Composite)parent);
		}
		super.setControl(parent);
	}

	protected void initialize(Composite parent) {
		System.out.println("initializing");
		createProofFolderName(parent);
	}

	@Override
	protected boolean validatePage() {    
		boolean result = super.validatePage();
		if (!result) return false;
		if (proofFolderNameField.getText().isEmpty()) return true;
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		IStatus status = ws.validateName(proofFolderNameField.getText(), IResource.FOLDER);
		if (!status.isOK()) {
			setErrorMessage(status.getMessage());
			return false;
		}
		return true;
	}

	private static final int PROOF_FOLDER_NAME_FIELD_WIDTH = 100;

	private Listener modifyListener = new Listener() {
		@Override
		public void handleEvent(Event e) {
			boolean valid = validatePage();
			setPageComplete(valid);

		}
	};

	private final void createProofFolderName(Composite parent) {

		Composite group = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// new project label
		Label projectLabel = new Label(group, SWT.NONE);
		projectLabel.setText("Proof Folder");
		projectLabel.setFont(parent.getFont());

		// new project name entry field
		proofFolderNameField = new Text(group, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = PROOF_FOLDER_NAME_FIELD_WIDTH;
		proofFolderNameField.setLayoutData(data);
		proofFolderNameField.setFont(parent.getFont());

		proofFolderNameField.setText(Preferences.getProofFolderName());
		proofFolderNameField.addListener(SWT.Modify, modifyListener);

	}

	public String getProofFolderName() {
		return proofFolderNameField.getText();
	}
}