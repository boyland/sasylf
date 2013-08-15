package org.sasylf.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import edu.cmu.cs.sasylf.Version;

public class AboutHandler extends AbstractHandler {

  public AboutHandler() {}
  
  /**
   * the command has been executed, so extract extract the needed information
   * from the application context.
   */
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
    MessageDialog.openInformation(window.getShell(), "About SASyLF", 
        Version.getInstance() + "\nTool to check proofs.\n" +
        "See http://www.cs.cmu.edu/~aldrich/SASyLF/\n" +
        "and http://code.google.com/p/sasylf/wiki/Main");
    return null;
  }



}
