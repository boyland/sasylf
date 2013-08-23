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
import org.sasylf.editors.SASyLFCorrectIndentStrategy;

/**
 * The default handler for the Correct Indentation handler.
 */
public class CorrectIndentationHandler extends AbstractHandler {

  private SASyLFCorrectIndentStrategy doIndent;
  
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IEditorPart editor = HandlerUtil.getActiveEditor(event);
    if (!(editor instanceof ProofEditor)) return null;
    ProofEditor proofEditor = (ProofEditor)editor;
    IDocument doc = proofEditor.getDocument();
    if (doc == null) return null;
    ISelection sel = HandlerUtil.getCurrentSelection(event);
    if (!(sel instanceof ITextSelection)) return null;
    ITextSelection textSel = (ITextSelection)sel;
    if (doIndent == null) doIndent = new SASyLFCorrectIndentStrategy();
    final int first = textSel.getStartLine();
    final int count = textSel.getEndLine() - first + 1;
    TextEdit edits = doIndent.correctIndentation(doc, first, count);
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
