package editor.editors;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import editor.actions.SasylfMarker;
import editor.editors.propertyOutline.PropertyOutlinePage;

//import org.eclipse.editors.EditorsLog;

public class PropertiesEditor extends MultiPageEditorPart {

	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		if (!(input instanceof IFileEditorInput))
			throw new PartInitException(
					"Invalid Input: Must be IFileEditorInput");
		super.init(site, input);
	}

	private SASyLFTextEditor textEditor;

	// private TreeViewer treeViewer;

	protected void createPages() {
		// createPropertiesPage();
		createSourcePage();

		updateTitle();

		// initTreeContent();
		// initTreeEditors();
	}

	void createSourcePage() {
		try {
			textEditor = new SASyLFTextEditor();
			int index = addPage(textEditor, getEditorInput());
			setPageText(index, "Source");
		} catch (PartInitException e) {
			// FavoritesLog.logError("Error creating nested text editor", e);
		}

	}

	private Composite parent(int ops, Transfer[] transfers,
			TextEditorDropAdapter textEditorDropAdapter) {
		// TODO Auto-generated method stub
		return null;
	}

	// protected void handlePropertyChange(int propertyId) {
	// if (propertyId == IEditorPart.PROP_DIRTY)
	// isPageModified = isDirty();
	// super.handlePropertyChange(propertyId);
	// }

	void updateTitle() {
		IEditorInput input = getEditorInput();
		setPartName(input.getName());
		setTitleToolTip(input.getToolTipText());
	}

	// public void setFocus() {
	// switch (getActivePage()) {
	// case 0:
	// treeViewer.getTree().setFocus();
	// break;
	// case 1:
	// textEditor.setFocus();
	// break;
	// }
	// }

	public void gotoMarker(IMarker marker) {
		setActivePage(1);
		((IGotoMarker) textEditor.getAdapter(IGotoMarker.class))
				.gotoMarker(marker);
	}

	public boolean isSaveAsAllowed() {
		return true;
	}

	public void doSave(IProgressMonitor monitor) {
		// if (getActivePage() == 0 && isPageModified)
		// updateTextEditorFromTree();
		// isPageModified = false;
		textEditor.doSave(monitor);
		IEditorInput iei = this.getEditorInput();
		IFileEditorInput ifi = (IFileEditorInput) iei;
		if (iei instanceof IFileEditorInput) {
			SasylfMarker.analyzeSlf(ifi.getFile());
		}
	}

	public void doSaveAs() {
		// if (getActivePage() == 0 && isPageModified)
		// updateTextEditorFromTree();
		// isPageModified = false;
		textEditor.doSaveAs();
		setInput(textEditor.getEditorInput());
		updateTitle();
	}

	private PropertyOutlinePage propertyOutlinePage;

//	@Override
//	public Object getAdapter(Class required) {
//		if (IContentOutlinePage.class.equals(required)) {
//			if (propertyOutlinePage == null) {
////				propertyOutlinePage = new PropertyOutlinePage(getDocumentProvider(), this);
//				propertyOutlinePage.setInput(getEditorInput());
//			}
//			return propertyOutlinePage;
//		}
//		return super.getAdapter(required);
//	}

	public Object getAdapter(Class required) {
		if (IContentOutlinePage.class.equals(required)) {
			if (propertyOutlinePage == null) {
//				propertyOutlinePage= new PropertyContentProvider(getDocumentProvider(), this);
				if (getEditorInput() != null)
					propertyOutlinePage.setInput(getEditorInput());
			}
			return propertyOutlinePage;
		}
		
		return super.getAdapter(required);
	}
	
	// private PropertiesEditorContentProvider treeContentProvider;

	// void initTreeContent() {
	// treeContentProvider = new PropertiesEditorContentProvider();
	// treeViewer.setContentProvider(treeContentProvider);
	// treeViewer.setLabelProvider(new PropertiesEditorLabelProvider());
	//
	// // Reset the input from the text editor's content
	// // after the editor initialization has completed.
	// // treeViewer.setInput(new PropertyFile(""));
	// // treeViewer.getTree().getDisplay().asyncExec(new Runnable() {
	// // public void run() {
	// // updateTreeFromTextEditor();
	// // }
	// // });
	// // treeViewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
	//
	// // drag support
	// int ops = DND.DROP_COPY | DND.DROP_MOVE;
	// Transfer[] transfers = new Transfer[] { TextTransfer.getInstance() };
	// treeViewer.addDragSupport(ops, transfers, new GadgetDragListener(
	// treeViewer));
	// }
	//
	// public static final String VALUE_COLUMN_ID = "Value";
	// public static final String KEY_COLUMN_ID = "Key";

	// private boolean isAltPressed;
	//
	// void initTreeEditors() {
	// treeViewer.setColumnProperties(new String[] { KEY_COLUMN_ID,
	// VALUE_COLUMN_ID });
	// final TextCellEditor keyEditor = new TextCellEditor(treeViewer
	// .getTree());
	// final TextCellEditor valueEditor = new TextCellEditor(treeViewer
	// .getTree());
	// treeViewer.setCellEditors(new CellEditor[] { keyEditor, valueEditor });
	// treeViewer.setCellModifier(new PropertiesEditorCellModifier(this,
	// treeViewer));
	// keyEditor.setValidator(new ICellEditorValidator() {
	// public String isValid(Object value) {
	// if (((String) value).trim().length() == 0)
	// return "Key must not be empty string";
	// return null;
	// }
	// });

	// valueEditor.setValidator(new ICellEditorValidator() {
	// public String isValid(Object value) {
	// return null;
	// }
	// });
	//
	// keyEditor.addListener(new ICellEditorListener() {
	// public void applyEditorValue() {
	// setErrorMessage(null);
	// }
	//
	// public void cancelEditor() {
	// setErrorMessage(null);
	// }
	//
	// public void editorValueChanged(boolean oldValidState,
	// boolean newValidState) {
	// setErrorMessage(keyEditor.getErrorMessage());
	// }
	//
	// void setErrorMessage(String errorMessage) {
	// getEditorSite().getActionBars().getStatusLineManager()
	// .setErrorMessage(errorMessage);
	// }
	// });

	// treeViewer.getTree().addKeyListener(new KeyListener() {
	// public void keyPressed(KeyEvent e) {
	// if (e.keyCode == SWT.ALT)
	// isAltPressed = true;
	// }
	//
	// public void keyReleased(KeyEvent e) {
	// if (e.keyCode == SWT.ALT)
	// isAltPressed = false;
	// }
	// });
	// }

	// public boolean shouldEdit() {
	// if (!isAltPressed)
	// return false;
	// Must reset this value here because if an editor
	// is opened, we don't get the Alt key up event.
	// isAltPressed = false;
	// return true;
	// }

	// private final PropertyFileListener propertyFileListener = new
	// PropertyFileListener() {
	// public void keyChanged(PropertyCategory category, PropertyEntry entry) {
	// treeViewer.update(entry, new String[] { KEY_COLUMN_ID });
	// treeModified();
	// }
	//
	// public void valueChanged(PropertyCategory category, PropertyEntry entry)
	// {
	// treeViewer.update(entry, new String[] { VALUE_COLUMN_ID });
	// treeModified();
	// }
	//
	// public void nameChanged(PropertyCategory category) {
	// treeViewer.update(category, new String[] { KEY_COLUMN_ID });
	// treeModified();
	// }
	//
	// public void entryAdded(PropertyCategory category, PropertyEntry entry) {
	// treeViewer.refresh();
	// treeModified();
	// }
	//
	// public void entryRemoved(PropertyCategory category, PropertyEntry entry)
	// {
	// treeViewer.refresh();
	// treeModified();
	// }
	//
	// public void categoryAdded(PropertyCategory category) {
	// treeViewer.refresh();
	// treeModified();
	// }
	//
	// public void categoryRemoved(PropertyCategory category) {
	// treeViewer.refresh();
	// treeModified();
	// }
	// };
	//
	// void updateTreeFromTextEditor() {
	// PropertyFile propertyFile = (PropertyFile) treeViewer.getInput();
	// propertyFile.removePropertyFileListener(propertyFileListener);
	// propertyFile = new PropertyFile(textEditor.getDocumentProvider()
	// .getDocument(textEditor.getEditorInput()).get());
	// treeViewer.setInput(propertyFile);
	// propertyFile.addPropertyFileListener(propertyFileListener);
	//
	// }

	// private TreeColumn keyColumn;
	// private TreeColumn valueColumn;
	//
	// void createPropertiesPage() {
	// treeViewer = new TreeViewer(getContainer(), SWT.MULTI
	// | SWT.FULL_SELECTION);
	//
	// Tree tree = treeViewer.getTree();
	// tree.setHeaderVisible(true);
	//
	// keyColumn = new TreeColumn(tree, SWT.NONE);
	// keyColumn.setText("Key");
	// keyColumn.setWidth(150);
	//
	// valueColumn = new TreeColumn(tree, SWT.NONE);
	// valueColumn.setText("Value");
	// valueColumn.setWidth(150);
	//
	// int index = addPage(treeViewer.getControl());
	// setPageText(index, "Properties");
	//
	// }
	//
	// private boolean isPageModified;
	//
	// public void treeModified() {
	// boolean wasDirty = isDirty();
	// isPageModified = true;
	// if (!wasDirty)
	// firePropertyChange(IEditorPart.PROP_DIRTY);
	//
	// }

	// public boolean isDirty() {
	// return isPageModified || super.isDirty();
	// }

	// protected void pageChange(int newPageIndex) {
	// switch (newPageIndex) {
	// case 0:
	// if (isDirty())
	// updateTreeFromTextEditor();
	// break;
	// case 1:
	// if (isPageModified)
	// updateTextEditorFromTree();
	// break;
	// }
	// isPageModified = false;
	// super.pageChange(newPageIndex);
	// }

	// void updateTextEditorFromTree() {
	// textEditor.getDocumentProvider().getDocument(
	// textEditor.getEditorInput()).set(
	// ((PropertyFile) treeViewer.getInput()).asText());
	// }

}
