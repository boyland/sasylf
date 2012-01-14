package editor.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.*;
import org.eclipse.gef.dnd.TemplateTransfer; //import org.eclipse.gef.dnd.GadgetTransfer;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.*;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.SWT;

import editor.editors.SASyLFColorProvider;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */

public class SyntaxView extends ViewPart {
	private TableViewer viewer;
	private Action action1;
	private Action action2;
	private Action doubleClickAction;

	// private TableItem codeBlock1;
	// private TableItem codeBlock2;

	private TableColumn contentColumn;

	// private TableColumn locationColumn;

	/*
	 * The content provider class is responsible for providing objects to the
	 * view. It can wrap existing objects in adapters or simply return objects
	 * as-is. These objects may be sensitive to the current input of the view,
	 * or ignore it and always show the same content (like Task List, for
	 * example).
	 */

	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {

		}

		public void dispose() {
		}

		public Object[] getElements(Object parent) {
			return new String[] { /* "theorem proof", "case analysis", "induction" */
//					"case <name>\n\n" + "is\n\n" + "<proof name>:\n\n"
//							+ "<proof name>:\n\n" + "end case\n\n",
//
//					"case rule\n\n" + "------------ <rule name>\n\n"
//							+ "<proof name>:\n\n" + "is\n\n"
//							+ "<proof name>:\n\n" + "<proof name>:\n\n"
//							+ "end case\n\n\n ",
//					"theorem <   > : forall <> exists <> \n\n\n"
//							+ "d1: <>	by rule <rule name> \n\n "
//							+ "d2: <>  	by rule <rule name> \n\n\n "
//							+ "end theorem\n\n\n",

					// new template

					"theorem NAME : forall NAME : JUDGMENT exists JUDGMENT\n\n"+"NAME : JUDGMENT by JUSTIFICATION\n\n"+"end theorem\n",
					"case rule\n\n"+"NAME : PREMISE\n"+  "----------------- RULENAME\n"+"NAME : CONCLUSION\n\n"+"is\n\n"+"NAME : JUDGMENT by JUSTIFICATION\n\n"+"end case\n",
					"by rule NAME on NAME\n",
					"by unproved\n",
					"by theorem NAME on NAME\n",
					"by case analysis on NAME\n",
					"by induction on NAME\n",
					"case SYNTAX is\n\n"+"NAME : DERIVATION by JUSTIFICATION\n\n"+"end case\n",
					"by induction hypothesis on NAME\n",
					"by substitution on NAME, NAME\n",
					"by weakening on NAME\n",
					"by exchange on NAME\n",

			};
		}
	}

	class ViewLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return getText(obj);
			
		}
		
		public void ViewCodeScanner(SASyLFColorProvider provider)	{
			TextAttribute kwAtt = new TextAttribute (provider.getColor(SASyLFColorProvider.KEYWORD), null,SWT.BOLD);
			Token keyword = new Token (kwAtt);
			
			IToken comment = new Token (new TextAttribute (provider.getColor(SASyLFColorProvider.SINGLE_LINE_COMMENT)));
			IToken other = new Token (new TextAttribute (provider.getColor(SASyLFColorProvider.DEFAULT)));
			IToken multiLineComment = new Token (new TextAttribute (provider.getColor(SASyLFColorProvider.MULTI_LINE_COMMENT)));
			IToken rule = new Token (new TextAttribute (provider.getColor(SASyLFColorProvider.RULE)));
			
			List rules = new ArrayList ();
			rules.add (new EndOfLineRule ("//", comment));
			rules.add (new MultiLineRule ("/*", "*/", multiLineComment));
			rules.add (new SingleLineRule ("<",">",comment));
			rules.add (new EndOfLineRule ("---", rule));
		}

		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}

		// public Image getImage(Object obj) {
		// return PlatformUI.getWorkbench().getSharedImages().getImage(
		// ISharedImages.IMG_OBJ_FILE);
		// }
	}

	class NameSorter extends ViewerSorter {
	}

	/**
	 * The constructor.
	 */

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.MULTI | SWT.FULL_SELECTION);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		// viewer.setSorter(new NameSorter());
		// viewer.setInput(getViewSite());
		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(),
				"Editor.viewer");
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();

		final Table table = viewer.getTable();
		table.setHeaderVisible(false);
		table.setLinesVisible(true);

		// codes block

		final TableEditor editor = new TableEditor(table);

		editor.horizontalAlignment = SWT.CENTER;
		//editor.verticalAlignment = SWT.CENTER;
		
		editor.grabHorizontal = true;
		final int TEXT_MARGIN = 1;
		table.addListener(SWT.MeasureItem, new Listener() {
			public void handleEvent(Event event) {
				TableItem item = (TableItem) event.item;
				String text = item.getText(event.index);
				Point size = event.gc.textExtent(text);
				event.width = size.x + TEXT_MARGIN;
				event.height = Math.max(event.height, size.y + TEXT_MARGIN);
			}
		});
		table.addListener(SWT.EraseItem, new Listener() {
			public void handleEvent(Event event) {
				event.detail &= ~SWT.FOREGROUND;
			}
		});
		table.addListener(SWT.PaintItem, new Listener() {
			public void handleEvent(Event event) {
				TableItem item = (TableItem) event.item;
				String text = item.getText(event.index);
				/* center column 1 vertically */
				int yOffset = 0;
				if (event.index == 1) {
					Point size = event.gc.textExtent(text);
					yOffset = Math.max(0, (event.height - size.y) / 2);
				}
				event.gc.drawText(text, event.x - TEXT_MARGIN, event.y
						+ yOffset, true);
			}
		});

		// editable

		// table.addListener(SWT.MouseDown, new Listener() {
		// public void handleEvent(Event event) {
		// Rectangle clientArea = table.getClientArea();
		// Point pt = new Point(event.x, event.y);
		// int index = table.getTopIndex();
		// while (index < table.getItemCount()) {
		// boolean visible = false;
		// final TableItem item = table.getItem(index);
		// for (int i = 0; i < table.getColumnCount(); i++) {
		// Rectangle rect = item.getBounds(i);
		// if (rect.contains(pt)) {
		// final int column = i;
		// final Text text = new Text(table, SWT.NONE);
		// Listener textListener = new Listener() {
		// public void handleEvent(final Event e) {
		// switch (e.type) {
		// case SWT.FocusOut:
		// item.setText(column, text.getText());
		// text.dispose();
		// break;
		// case SWT.Traverse:
		// switch (e.detail) {
		// case SWT.TRAVERSE_RETURN:
		// item
		// .setText(column, text
		// .getText());
		// // FALL THROUGH
		// case SWT.TRAVERSE_ESCAPE:
		// text.dispose();
		// e.doit = false;
		// }
		// break;
		// }
		// }
		// };
		// text.addListener(SWT.FocusOut, textListener);
		// text.addListener(SWT.Traverse, textListener);
		// editor.setEditor(text, item, i);
		// text.setText(item.getText(i));
		// text.selectAll();
		// text.setFocus();
		// return;
		// }
		// if (!visible && rect.intersects(clientArea)) {
		// visible = true;
		//
		// }
		// }
		// if (!visible)
		// return;
		//
		// index++;
		// }
		//
		// //
		// }
		// });

		contentColumn = new TableColumn(table, SWT.LEFT);
		contentColumn.setWidth(250);
		contentColumn.setText("Name");
		// contentColumn.setWidth(150);

		// locationColumn = new TableColumn(table, SWT.LEFT);
		// locationColumn.setText("Location");
		// locationColumn.setWidth(200);

		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setInput(getViewSite());

		// drag support
		int ops = DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_DEFAULT;
		Transfer[] transfers = new Transfer[] { TextTransfer.getInstance() };

		viewer.addDragSupport(ops, transfers, new GadgetDragListener(viewer));

		// multiple-line layout

	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				SyntaxView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		// manager.add(action1);
		// manager.add(new Separator());
		// manager.add(action2);
	}

	private void fillContextMenu(IMenuManager manager) {
		// manager.add(action1);
		// manager.add(action2);
		// // Other plug-ins can contribute there actions here
		// manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillLocalToolBar(IToolBarManager manager) {

		manager.add(action1);
		// manager.add(action2);
	}

	private void makeActions() {
		action1 = new Action() {
			public void run() {

				showMessage("Drag the template you want and drop into the right place in editor\n\n"
			);
			}
		};
		action1.setText("how to use template");
		action1.setToolTipText("how to use template");
		action1.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

		action2 = new Action() {
			// public void run() {
			// showMessage("Action 2 executed");
			// }
		};
		action2.setText("Action 2");
		action2.setToolTipText("Action 2 tooltip");
		action2.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

		doubleClickAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection) selection)
						.getFirstElement();
				showMessage(obj.toString());

			}
		};
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	private void showMessage(String message) {
		MessageDialog.openInformation(viewer.getControl().getShell(),
				"Template", message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	/*
	 * 
	 * this.viewer.addDragSupport( DND.DROP_COPY,
	 * 
	 * new Transfer[] { TemplateTransfer.getInstance() },
	 * 
	 * new DragSourceAdapter() {
	 * 
	 * public void dragSetData(DragSourceEvent event) {
	 * 
	 * event.data =
	 * ((StructuredSelection)View.this.viewer.getSelection()).getFirstElement();
	 * 
	 * }
	 * 
	 * 
	 * 
	 * public void dragStart(DragSourceEvent event) {
	 * 
	 * DragHelper.data =
	 * ((StructuredSelection)View.this.viewer.getSelection()).getFirstElement();
	 * 
	 * DragHelper.sameObject = false;
	 * 
	 * }
	 * 
	 * 
	 * 
	 * });
	 */

}