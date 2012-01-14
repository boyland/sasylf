package editor.views;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;

import editor.views.Rules.TreeObject;

public class RulesDragListener implements DragSourceListener {
	private TreeViewer viewer;

	public RulesDragListener(TreeViewer viewer) {
		// TODO Auto-generated constructor stub

		this.viewer = viewer;
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

			event.data = ((TreeObject) selection.getFirstElement()).getName();
		}

	}

	@Override
	public void dragStart(DragSourceEvent event) {
		// TODO Auto-generated method stub

	}

}
