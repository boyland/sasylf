package org.sasylf.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

public class ProjectPropertyPage extends PropertyPage {

	private static final String PATH_TITLE = "Path:";
	private static final String BUILD_PATH_TITLE = "&Proof Folder:";

	private static final int TEXT_FIELD_WIDTH = 50;

	private Text buildPathText;

	public ProjectPropertyPage() {
		super();
	}

	private void addFirstSection(Composite parent) {
		Composite composite = createDefaultComposite(parent);

		//Label for path field
		Label pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText(PATH_TITLE);

		// Path text field
		Text pathValueText = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
		pathValueText.setText(((IResource) getElement()).getFullPath().toString());
	}

	private void addSeparator(Composite parent) {
		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		separator.setLayoutData(gridData);
	}

	private void addSecondSection(Composite parent) {
		Composite composite = createDefaultComposite(parent);

		// Label for build path field
		Label buildPathLabel = new Label(composite, SWT.NONE);
		buildPathLabel.setText(BUILD_PATH_TITLE);

		// Build path text field
		buildPathText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData();
		gd.widthHint = convertWidthInCharsToPixels(TEXT_FIELD_WIDTH);
		buildPathText.setLayoutData(gd);

		// Populate build path text field
		String buildPath;
		try {
			buildPath = ProjectProperties.getBuildPath((IProject)getElement());
		} catch (CoreException e) {
			buildPath = ProjectProperties.getDefaultBuildPath();
		}
		buildPathText.setText(buildPath);
	}

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL);
		data.grabExcessHorizontalSpace = true;
		composite.setLayoutData(data);

		addFirstSection(composite);
		addSeparator(composite);
		addSecondSection(composite);
		return composite;
	}

	private Composite createDefaultComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);

		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);

		return composite;
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		// Populate the build path text field with the default value
		String defaultBuildPath = ProjectProperties.getDefaultBuildPath();
		buildPathText.setText(defaultBuildPath);
	}

	@Override
	public boolean performOk() {
		// store the value in the build path text field
		try {
			ProjectProperties.setBuildPath((IProject) getElement(), buildPathText.getText());
		} catch (CoreException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}