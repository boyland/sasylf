package org.sasylf.refactor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.sasylf.editors.ProofEditor;

/**
 * Handler to refactor a theorem or lemma.
 * @author rodenbe4
 */
public class RenameTheoremHandler extends AbstractHandler {	
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// first get the relevant info into variables
		RefactoringContext context = getInfo(event);
		if (!context.areAllValuesSet()) return null;	// not enough info obtained to refactor
		
		// use wizard to get new name from user and create the changes needed
		RenameTheoremWizard wizard = new RenameTheoremWizard(context);
		Shell shell = HandlerUtil.getActiveShell(event);
		WizardDialog dialog = new WizardDialog(shell,wizard);
		dialog.open();
		
		return null;
	}
	
	/**
	 * Get all information needed to refactor.
	 * @param event contains all information needed about the state of application
	 * @return the resulting {@link RefactoringContext}
	 */
	private RefactoringContext getInfo(ExecutionEvent event) {
		// adapted from OpenDeclarationHandler
		RefactoringContext context = new RefactoringContext();
		
		ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
		if (!(currentSelection instanceof ITextSelection)) return context;
		ITextSelection sel = (ITextSelection)currentSelection;
		context.setOldName(sel.getText());
		IEditorPart editor = HandlerUtil.getActiveEditor(event);
		if (!(editor instanceof ProofEditor)) return context;
		context.setProofEditor((ProofEditor)editor);
//		IProjectStorage st = IProjectStorage.Adapter.adapt(editor.getEditorInput());
//		if (st == null) return false;
		
		return context;
	}
}
