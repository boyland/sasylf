package org.sasylf.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.ResourceUtil;
import org.sasylf.actions.CheckProofsAction;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class CheckProofsHandler extends AbstractHandler {
	/**
	 * The constructor.
	 */
	public CheckProofsHandler() {
	}

	/**
	 * This is called when checking proofs is chosen from a menu, the
	 * toolbar or a context menu.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
	  IWorkbenchPage page = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
	  ISelection sel = HandlerUtil.getCurrentSelection(event);
	  if (sel instanceof TreeSelection) {
	    TreeSelection tsel = (TreeSelection) sel;
      for (Object seg : tsel.toArray()) {
        if (seg instanceof IResource) {
          // System.out.println("Found resource: " + seg);
          IResource res = (IResource)seg;
          if ("slf".equals(res.getFileExtension())) {
            IFile f = (IFile)res.getAdapter(IFile.class);
            // System.out.println("  with correct extension");
            CheckProofsAction.analyzeSlf(res,ResourceUtil.findEditor(page, f));
          }
        }
        // System.out.println("Selected is " + seg + " of class " + (seg== null ? "<null>" : seg.getClass().toString()));
	    }
      return null;
	  }
	  IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
	  IResource res = (IResource)activeEditor.getEditorInput().getAdapter(IResource.class);
	  if (res != null) {
	    CheckProofsAction.analyzeSlf(res, activeEditor);
	  }  else {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		MessageDialog.openInformation(
				window.getShell(),
				"SASyLF Check Proofs ",
				"Cannot find resource for " + activeEditor);
	  }
		return null;
	}
}
