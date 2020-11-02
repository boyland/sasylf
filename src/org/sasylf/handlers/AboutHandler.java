package org.sasylf.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import edu.cmu.cs.sasylf.Version;


public class AboutHandler extends AbstractHandler {

	public AboutHandler() {}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		new MyDialog(
			window.getShell(),
			"About SASyLF",
			null,
			Version.getInstance() + "\nTool to check proofs.",
			MessageDialog.INFORMATION,
			0,
			new String[] { "OK" }
			).open();
		return null;
	}
}

class MyDialog extends MessageDialog {
	public MyDialog(
			Shell parentShell,
			String dialogTitle,
			Image dialogTitleImage,
			String dialogMessage,
			int dialogImageType,
			int defaultIndex,
			String[] dialogButtonLabels) {
		super(parentShell, dialogTitle, dialogTitleImage, dialogMessage,
				dialogImageType, defaultIndex, dialogButtonLabels);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Control createCustomArea(Composite parent) {
		Link link = new Link(parent, SWT.WRAP);
		link.setText(
				"<a>http://www.cs.cmu.edu/~aldrich/SASyLF/</a>\n" +
				"<a>https://github.com/boyland/sasylf/wiki/Main</a>"
				);
		link.addSelectionListener(new SelectionAdapter()  {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Program.launch(e.text);
			}
		});
		return link;
	}
}
