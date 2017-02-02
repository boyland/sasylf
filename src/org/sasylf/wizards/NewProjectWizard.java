package org.sasylf.wizards;

import java.net.URI;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.sasylf.project.MyNature;

public class NewProjectWizard extends Wizard implements INewWizard {

	private NewProjectWizardPage page;

	public NewProjectWizard() {
		this.setWindowTitle("New SASyLF Project Wizard");
	}


	@Override
	public void addPages() {
		super.addPages();
		page = new NewProjectWizardPage("New SASyLF Project Wizard");
		page.setTitle("New SASyLF Project Wizard (1)");
		page.setDescription("Create a Project with SASyLF Nature to Hold SASyLF Proofs");

		addPage(page);
	}


	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean performFinish() {
		String name = page.getProjectName();
		URI location = null;
		if (!page.useDefaults()) {
			location = page.getLocationURI();
		}
		String proofFolderName = page.getProofFolderName();
		MyNature.createSASyLFProject(name, location, proofFolderName);
		return true;
	}

}
