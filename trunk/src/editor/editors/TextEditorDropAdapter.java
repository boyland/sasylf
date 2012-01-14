package editor.editors;

import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.editors.text.TextEditor;

public class TextEditorDropAdapter {

	public TextEditorDropAdapter(TextEditor textEditor) {
		// TODO Auto-generated constructor stub
		
		/*Control textControl = TextEditor.getAdaptor(Control.class);
		int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;
		Transfer[] types = new Transfer[] {TextTransfer.getInstance()};
		DropTarget target = new DropTarget(textControl, operations);
		target.setTransfer(types);
		target.addDropListener (new DropTargetListener() {
				public void dragEnter(DropTargetEvent event) {};
				public void dragOver(DropTargetEvent event) {};
				public void dragLeave(DropTargetEvent event) {};
				public void dragOperationChanged(DropTargetEvent event) {};
				public void dropAccept(DropTargetEvent event) {}
				public void drop(DropTargetEvent event) {
					// A drop has occurred, copy over the data
					if (event.data == null) { // no data to copy, indicate failure in event.detail
						event.detail = DND.DROP_NONE;
						return;
					}
					// DO SOMETHING HERE
					 * 
					// label.setText ((String) event.data); // data copied to label text
				}
		 	});
		
		*/
	}

}
