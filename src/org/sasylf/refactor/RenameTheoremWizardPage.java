package org.sasylf.refactor;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class RenameTheoremWizardPage extends WizardPage {
	private Text nameText;
	private RefactoringContext context;
	
	public RenameTheoremWizardPage(RefactoringContext context) {
		super("renameTheoremPage");
		setTitle("Rename Theorem");
		setDescription("Rename a theorem or a lemma.");
		this.context = context;
	}

	@Override
	public void createControl(Composite parent) {
		// TODO Auto-generated method stub
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 6;
		Label label = new Label(container, SWT.NULL);
		label.setText("&New theorem name:");
		nameText = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		nameText.setLayoutData(gd);
		nameText.setText(context.getOldName());
		nameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		
		dialogChanged();
		setControl(container);
	}
	
	@Override
	public void setVisible(boolean visible) {
		nameText.setFocus(); // https://www.eclipse.org/forums/index.php/t/163588/
		super.setVisible(visible);
	}
	
	/**
	 * Ensures that text field is set properly.
	 */
	private void dialogChanged() {
		if (context.getOldName().indexOf(".") != -1) {
			updateStatus("Theorem must be renamed within proof it's defined in!");
			return;
		}
		String newName = getTheoremName();

		if (newName.length() == 0) {
			updateStatus("Theorem name must be specified!");
			return;
		} else if (newName.indexOf(" ") != -1) {
			updateStatus("Theorem name cannot have spaces in it!");
			return;
		} 
		// XXX: check if characters are syntactically legal?
		// TODO: no other names of that name

		// otherwise, everything is good here!
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
	
	public String getTheoremName() {
		return nameText.getText();
	}
}
