package editor.editors;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import edu.cmu.cs.sasylf.parser.ParseException;

/**
 * Manages the installation/deinstallation of global actions for multi-page
 * editors. Responsible for the redirection of global actions to the active
 * editor. Multi-page contributor replaces the contributors for the individual
 * editors in the multi-page editor.
 */
public class PropertiesEditorContributor extends
		MultiPageEditorActionBarContributor {
	private IEditorPart activeEditorPart;
	private Action Run;

	/**
	 * Creates a multi-page contributor.
	 */
	public PropertiesEditorContributor() {
		super();
		createActions();
	}

	/**
	 * Returns the action registed with the given text editor.
	 * 
	 * @return IAction or null if editor is null.
	 */
	protected IAction getAction(ITextEditor editor, String actionID) {
		return (editor == null ? null : editor.getAction(actionID));
	}

	/*
	 * (non-JavaDoc) Method declared in
	 * AbstractMultiPageEditorActionBarContributor.
	 */

	public void setActivePage(IEditorPart part) {
		if (activeEditorPart == part)
			return;

		activeEditorPart = part;

		IActionBars actionBars = getActionBars();
		if (actionBars != null) {

			ITextEditor editor = (part instanceof ITextEditor) ? (ITextEditor) part
					: null;

			actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(),
					getAction(editor, ITextEditorActionConstants.DELETE));
			actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(),
					getAction(editor, ITextEditorActionConstants.UNDO));
			actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(),
					getAction(editor, ITextEditorActionConstants.REDO));
			actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(),
					getAction(editor, ITextEditorActionConstants.CUT));
			actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(),
					getAction(editor, ITextEditorActionConstants.COPY));
			actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(),
					getAction(editor, ITextEditorActionConstants.PASTE));
			actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(),
					getAction(editor, ITextEditorActionConstants.SELECT_ALL));
			actionBars.setGlobalActionHandler(ActionFactory.FIND.getId(),
					getAction(editor, ITextEditorActionConstants.FIND));
			actionBars.setGlobalActionHandler(
					IDEActionFactory.BOOKMARK.getId(), getAction(editor,
							IDEActionFactory.BOOKMARK.getId()));
			actionBars.updateActionBars();
		}
	}

	// console message output
	private static final String CONSOLE_NAME = "";

	private MessageConsole findConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++)
			if (name.equals(existing[i].getName()))
				return (MessageConsole) existing[i];
		// no console found, so create a new one
		MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { myConsole });
		return myConsole;
	}

	private void createActions() {
		Run = new Action() {
			public void run() {
				// MessageDialog.openInformation(null, "SASyLF proof tool",
				// "Code Testing Executed");

				MessageConsole myConsole = findConsole(CONSOLE_NAME);
				MessageConsoleStream out = myConsole.newMessageStream();
				IEditorInput iei = activeEditorPart.getEditorInput();
				if (iei instanceof IFileEditorInput) {
					IFileEditorInput ifi = (IFileEditorInput)iei;
					
					try {
						edu.cmu.cs.sasylf.Main.main(new String[] { ifi.getFile()
								.getLocationURI().getPath() });
					} catch (IOException e) {
						out.println(e.getMessage());
					} catch (ParseException e) {
						out.println(e.getMessage());
					}
					
					
					//out.println(ifi.getFile().getLocationURI().getPath());
					//out.println("Hello from Generic console sample action");
				}

//				IWorkbenchPage page = IWorkbenchWindow.getActivePage();
//				String id = IConsoleConstants.ID_CONSOLE_VIEW;
//				IConsoleView view = (IConsoleView) page.showView(id);
//				view.display(myConsole);
				
				 IWorkbenchPage page = PlatformUI.getWorkbench()
				 .getActiveWorkbenchWindow().getActivePage();// obtain the
				 // active page
				 String id = IConsoleConstants.ID_CONSOLE_VIEW;
				 IConsoleView view = null;	 
				 view.display(myConsole);

			}
		};

		Run.setText("Run");
		Run.setToolTipText("testing the codes");
		Run.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(IDE.SharedImages.IMG_OBJS_TASK_TSK));
	}

	public void contributeToMenu(IMenuManager manager) {
//		IMenuManager menu = new MenuManager("SASyLF &Menu");
//		manager.prependToGroup(IWorkbenchActionConstants.MB_ADDITIONS, menu);
//		menu.add(Run);
	}

	public void contributeToToolBar(IToolBarManager manager) {
		manager.add(new Separator());
		manager.add(Run);
	}
}
