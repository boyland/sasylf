/**
 * 
 */
package org.sasylf.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditProcessor;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.eclipse.text.undo.IDocumentUndoManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.sasylf.editors.ProofEditor;
import org.sasylf.editors.SASyLFAddRemoveCommentStrategy;

/**
 * The default handler for the Remove Comment handler.
 */
public class RemoveCommentHandler extends AbstractHandler {

	private SASyLFAddRemoveCommentStrategy doWork;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editor = HandlerUtil.getActiveEditor(event);
		if (!(editor instanceof ProofEditor)) return null;
		ProofEditor proofEditor = (ProofEditor)editor;
		IDocument doc = proofEditor.getDocument();
		if (doc == null) return null;
		ISelection // = HandlerUtil.getCurrentSelection(event);
		sel = proofEditor.getSelectionProvider().getSelection();
		final int first;
		final int count;
		if (sel == null || sel.isEmpty()) {
			int offset = proofEditor.getCursorOffset();
			try {
				first = doc.getLineOfOffset(offset);
			} catch (BadLocationException e) {
				return null;
			}
			count = 1;
		} else if (!(sel instanceof ITextSelection)) {
			return null;
		} else {
			ITextSelection textSel = (ITextSelection)sel;
			first = textSel.getStartLine();
			count = textSel.getEndLine() - first + 1;
		}
		if (doWork == null) doWork = new SASyLFAddRemoveCommentStrategy(false);
		TextEdit edits = doWork.changeComment(doc, first, count);
		if (edits == null) return null;
		IDocumentUndoManager undoManager = DocumentUndoManagerRegistry.getDocumentUndoManager(doc);
		undoManager.beginCompoundChange();
		try {
			new TextEditProcessor(doc, edits, TextEdit.NONE).performEdits();
		} catch (MalformedTreeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			undoManager.endCompoundChange();
		}
		return null;
	}

}
