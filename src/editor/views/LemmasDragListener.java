package editor.views;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;

public class LemmasDragListener implements DragSourceListener {
	private TableViewer viewer;

	public LemmasDragListener(TableViewer viewer) {
		// TODO Auto-generated constructor stub

		this.viewer = (TableViewer) viewer;
	}

	@Override
	public void dragFinished(DragSourceEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dragSetData(DragSourceEvent event) {
		// TODO Auto-generated method stub

		IStructuredSelection selection = (IStructuredSelection) viewer
				.getSelection();

		if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
			// event.data = viewer.getData(null);

			event.data = (String) selection.getFirstElement();
		}
	}

	@Override
	public void dragStart(DragSourceEvent event) {
		// TODO Auto-generated method stub

	}

}
