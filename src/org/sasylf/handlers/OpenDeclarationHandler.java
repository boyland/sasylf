package org.sasylf.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.sasylf.editors.ProofEditor;
import org.sasylf.editors.propertyOutline.ProofOutline;

/**
 * Context menu handler for "Open Declaration" 
 */
public class OpenDeclarationHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
    if (!(currentSelection instanceof ITextSelection)) return null;
    ITextSelection sel = (ITextSelection)currentSelection;
    String name = sel.getText();
    IEditorPart editor = HandlerUtil.getActiveEditor(event);
    if (!(editor instanceof ProofEditor)) return null;
    ProofEditor proofEditor = (ProofEditor)editor;
    ProofOutline outline = proofEditor.getProofOutline();
    Position pos = outline.findProofElementByName(name);
    if (pos == null) {
      MessageDialog.openError(HandlerUtil.getActiveShell(event), "Open Declaration", "No judgment or theorem '"+ name +"' found");
    } else {
      //TODO: later we may need to open an editor:
      ISourceViewer sourceViewer = proofEditor.getSourceViweer();
      sourceViewer.setRangeIndication(pos.getOffset(), pos.getLength(), true);
    }
    return null;
  }

}
