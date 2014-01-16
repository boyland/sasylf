package org.sasylf.util;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.texteditor.ITextEditor;

@SuppressWarnings("restriction")
public class EclipseUtil {

  /**
   * Get the (active) document associated with a given resource.
   * Unfortunately Eclipse makes this very hard to find,
   * and "discourages" the only way I know how to get the information.
   * @param res resource to find document for
   * @return document associated with resource
   */
  public static IDocument getDocumentFromResource(IResource resource) {
    if (resource == null) return null;
    Workbench w = Workbench.getInstance();
    if (w == null) return null;
    for (IWorkbenchWindow window : w.getWorkbenchWindows()) {
      for (IWorkbenchPage p : window.getPages()) {
        IEditorPart editor = p.getActiveEditor();
        if (editor == null) continue;
        if (!(editor instanceof ITextEditor)) continue;
        ITextEditor textEd = (ITextEditor)editor;
        IEditorInput iei = editor.getEditorInput();
        IResource res = ResourceUtil.getResource(iei);
        if (res != resource) continue;
        IDocument result = textEd.getDocumentProvider().getDocument(iei);
        if (result != null) return result;
      }
    }
    return null;
  }
}
