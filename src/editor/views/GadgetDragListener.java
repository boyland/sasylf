package editor.views;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;
//import org.eclipse.swt.widgets.TableColumn;


public class GadgetDragListener implements DragSourceListener {

	private TableViewer viewer;
	//private TableColumn contentColumn;
	//private TreeViewer viewer1;
	
	
	public GadgetDragListener(TableViewer viewer) {
		// TODO Auto-generated constructor stub
		this.viewer = (TableViewer) viewer;
//		this.contentColumn = contentColumn;
		
	}
	
	

	public GadgetDragListener(TreeViewer viewer) {
		// TODO Auto-generated constructor stub
		//this.viewer1 = viewer;
	}



	@Override
	public void dragFinished(DragSourceEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dragSetData(DragSourceEvent event) {
		// TODO Auto-generated method stub

		/*
		 * if (TextTransfer.getInstance().isSupportedType(event.dataType)){
		 * event.data = "DRAGGED_TEXT";
		 * 
		 * if (event.data == null) { // no data to copy, indicate failure in
		 * event.detail event.detail = DND.DROP_NONE; return; }
		 * 
		 * 
		 * }
		 */
		
		
		
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		
		if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
			//event.data = viewer.getData(null);
			
			event.data = (String) selection.getFirstElement();
		}
	}

	@Override
	public void dragStart(DragSourceEvent event) {
		// TODO Auto-generated method stub
//		if (viewer.getInput().length() == 0) {
//			event.doit = false;
//		}
	}

}
