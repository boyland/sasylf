package org.sasylf.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import edu.cmu.cs.sasylf.Version;

public class AboutAction implements IWorkbenchWindowActionDelegate {

  private IWorkbenchWindow window;
  
  @Override
  public void run(IAction action) {
    MessageDialog.openInformation(window.getShell(), "About SASyLF", 
        Version.getInstance() + "\nTool to check proofs.\n" +
        "See http://www.cs.cmu.edu/~aldrich/SASyLF/\n" +
        "and http://code.google.com/p/sasylf/wiki/Main");
  }

  @Override
  public void selectionChanged(IAction action, ISelection selection) {
  }

  @Override
  public void dispose() {
    window = null;
  }

  
  @Override
  public void init(IWorkbenchWindow window) {
    this.window = window;
  }

}
