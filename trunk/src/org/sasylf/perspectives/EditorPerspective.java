package org.sasylf.perspectives;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.jdt.ui.JavaUI;

/**
 * This class is meant to serve as an example for how various contributions are
 * made to a perspective. Note that some of the extension point id's are
 * referred to as API constants while others are hardcoded and may be subject to
 * change.
 */
public class EditorPerspective implements IPerspectiveFactory {

	private IPageLayout factory;
	// private static String EDITOR_ID = "editor.editors.PropertiesEditor";
	private static String RULES_ID = "editor.views.Rules";
	private static String LEMMAS_ID = "editor.views.Lemmas";
	private static String EXAMPLEVIEW_ID = "editor.views.ExampleView";
	private static String SYNTAXVIEW_ID = "editor.views.SyntaxView";

	// private static String ID_OUTLINE =
	// "editor.editors.propertyOutline.propertyOutlinePage";
	// private static String PEDITOR_ID = "editor.editors.PropertiesEditor";

	/*
	 * public void createInitialLayout(IPageLayout layout) { // Get the editor
	 * area. String editorArea = layout.getEditorArea();
	 * 
	 * // Put the Outline view on the left. layout.addView(
	 * IPageLayout.ID_OUTLINE, IPageLayout.RIGHT, 0.25f, editorArea);
	 * 
	 * // Put the Favorites view on the bottom with // the Tasks view.
	 * IFolderLayout bottom = layout.createFolder( "bottom", IPageLayout.BOTTOM,
	 * 0.66f, editorArea); bottom.addView(editorArea);
	 * bottom.addView(IPageLayout.ID_TASK_LIST);
	 * bottom.addPlaceholder(IPageLayout.ID_PROBLEM_VIEW);
	 * 
	 * // Add the Favorites action set. layout.addActionSet(editorArea); }
	 */

	// public EditorPerspective() {
	// super();
	// }
	public void createInitialLayout(IPageLayout factory) {
		this.factory = factory;
		addViews();
		addActionSets();
		addNewWizardShortcuts();
		addPerspectiveShortcuts();
		addViewShortcuts();
	}

	private void addViews() {
		// Creates the overall folder layout.
		// Note that each new Folder uses a percentage of the remaining
		// EditorArea.

		IFolderLayout Left = factory.createFolder("Left", // NON-NLS-1
				IPageLayout.LEFT, 0.20f, factory.getEditorArea());

		// factory.getEditorArea();

		Left.addView(IPageLayout.ID_RES_NAV);
		Left.addView(SYNTAXVIEW_ID);

		IFolderLayout Right =
		factory.createFolder(
		"Right",
		IPageLayout.RIGHT,
		0.80f,
		factory.getEditorArea());
		

		Right.addView(IPageLayout.ID_OUTLINE);
		// Right.addView(LEMMAS_ID);

		IFolderLayout bottom = factory.createFolder("bottom", // NON-NLS-1
				IPageLayout.BOTTOM, 0.75f, factory.getEditorArea());
		bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
		// bottom.addView("org.eclipse.jdt.junit.ResultView"); //NON-NLS-1
		// bottom.addView("org.eclipse.ui.console.ConsoleView");
		// bottom.addView("org.eclipse.team.ui.GenericHistoryView"); //NON-NLS-1

		factory.addFastView("org.eclipse.team.ccvs.ui.RepositoriesView", 0.50f); // NON-NLS-1
		factory.addFastView("org.eclipse.jdt.junit.ResultView", 0.50f); // NON-NLS-1
	}

	private void addActionSets() {
		factory.addActionSet("org.eclipse.debug.ui.launchActionSet"); // NON-NLS-1
		factory.addActionSet("org.eclipse.debug.ui.debugActionSet"); // NON-NLS-1
		factory.addActionSet("org.eclipse.debug.ui.profileActionSet"); // NON-NLS-1
		factory.addActionSet("org.eclipse.jdt.debug.ui.JDTDebugActionSet"); // NON-NLS-1
		factory.addActionSet("org.eclipse.jdt.junit.JUnitActionSet"); // NON-NLS-1
		factory.addActionSet("org.eclipse.team.ui.actionSet"); // NON-NLS-1
		factory.addActionSet("org.eclipse.team.cvs.ui.CVSActionSet"); // NON-NLS-1
		factory.addActionSet("org.eclipse.ant.ui.actionSet.presentation"); // NON-NLS-1
		factory.addActionSet(JavaUI.ID_ACTION_SET);
		factory.addActionSet(JavaUI.ID_ELEMENT_CREATION_ACTION_SET);
		factory.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET); // NON-NLS-1
	}

	private void addPerspectiveShortcuts() {
		factory
				.addPerspectiveShortcut("org.eclipse.team.ui.TeamSynchronizingPerspective"); // NON-NLS-1
		factory
				.addPerspectiveShortcut("org.eclipse.team.cvs.ui.cvsPerspective"); // NON-NLS-1
		factory.addPerspectiveShortcut("org.eclipse.ui.resourcePerspective"); // NON-NLS-1
	}

	private void addNewWizardShortcuts() {
		factory
				.addNewWizardShortcut("org.eclipse.team.cvs.ui.newProjectCheckout");// NON-NLS-1
		factory.addNewWizardShortcut("org.eclipse.ui.wizards.new.folder");// NON-NLS-1
		factory.addNewWizardShortcut("org.eclipse.ui.wizards.new.file");// NON-NLS-1
	}

	private void addViewShortcuts() {
		factory.addShowViewShortcut("org.eclipse.ant.ui.views.AntView"); // NON-NLS-1
		factory.addShowViewShortcut("org.eclipse.team.ccvs.ui.AnnotateView"); // NON-NLS-1
		factory.addShowViewShortcut("org.eclipse.pde.ui.DependenciesView"); // NON-NLS-1
		factory.addShowViewShortcut("org.eclipse.jdt.junit.ResultView"); // NON-NLS-1
		factory.addShowViewShortcut("org.eclipse.team.ui.GenericHistoryView"); // NON-NLS-1
		factory.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);
		factory.addShowViewShortcut(JavaUI.ID_PACKAGES);
		factory.addShowViewShortcut(IPageLayout.ID_RES_NAV);
		factory.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
		factory.addShowViewShortcut(IPageLayout.ID_OUTLINE);
	}

}
