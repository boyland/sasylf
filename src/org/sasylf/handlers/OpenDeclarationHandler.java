package org.sasylf.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.sasylf.IDEProof;
import org.sasylf.editors.ProofEditor;
import org.sasylf.project.ProofBuilder;
import org.sasylf.util.IProjectStorage;
import org.sasylf.views.ProofElement;
import org.sasylf.views.ProofOutline;

import edu.cmu.cs.sasylf.ast.ModulePart;
import edu.cmu.cs.sasylf.ast.Named;
import edu.cmu.cs.sasylf.ast.Node;
import edu.cmu.cs.sasylf.ast.QualName;
import edu.cmu.cs.sasylf.module.ModuleId;
import edu.cmu.cs.sasylf.util.Pair;

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
		IProjectStorage st = IProjectStorage.Adapter.adapt(editor.getEditorInput());
		if (st == null) return null;
		IDEProof pf = IDEProof.getProof(proofEditor.getEditorInput());
		if (pf == null) return null;
		Pair<IDEProof,Node> resolution = resolve(st.getProject(),pf,name);
		if (resolution == null || (resolution.first == pf && resolution.second == null)) {
			MessageDialog.openError(HandlerUtil.getActiveShell(event), "Open Declaration", "Could not find declaration for '"+ name +"'");
			return null;
		}
		if (resolution.first != pf) { // need to get a new editor
			IEditorInput input = resolution.first.getStorage().getAdapter(IEditorInput.class);
			if (input == null) {
				System.out.println("Cannot find editor input for " + resolution.first.toString());
				MessageDialog.openError(HandlerUtil.getActiveShell(event), "Open Declaration", "Could not find editor for '"+ name +"'");
				return null;
			}
			IWorkbenchPage page = editor.getSite().getWorkbenchWindow().getActivePage();
			try {
				editor = IDE.openEditor(page, input, ProofEditor.ID);
			} catch (PartInitException e) {
				MessageDialog.openError(HandlerUtil.getActiveShell(event), "Open Declaration", "Could not open editor for '"+ name +"'");
				return null;
			}
			if (!(editor instanceof ProofEditor)) {
				resolution.second = null; // no way to move to declaration
			} else proofEditor = (ProofEditor)editor;
		}
		IRegion region = null;
		if (resolution.second != null) {
			try {
				region = resolution.first.convert(resolution.second);
			} catch (BadLocationException e) {
				System.out.println("Failed to find " +((Named)resolution.second).getName() + " in " + resolution.first);
				MessageDialog.openError(HandlerUtil.getActiveShell(event), "Open Declaration", "Could not find declaration for '"+ name +"' any more");
				return null;
			}
			// the outline is more up to date: try it if available
			ProofOutline outline = proofEditor.getProofOutline();
			if (outline != null && resolution.second instanceof Named) {
				String localName = ((Named)resolution.second).getName();
				ProofElement element = outline.findProofElementByName(localName);
				if (element != null) {
					Position p = element.getPosition();
					if (p != null) {
						region = new Region(p.getOffset(),p.getLength());
					}
				}
			}
		}
		if (region != null) {
			ISourceViewer sourceViewer = proofEditor.getPublicSourceViewer();
			sourceViewer.setRangeIndication(region.getOffset(), region.getLength(), true);
		}
		/*
		ProofOutline outline = proofEditor.getProofOutline();
		ProofElement element = outline.findProofElementByName(name);
		if (element == null) {
			MessageDialog.openError(HandlerUtil.getActiveShell(event), "Open Declaration", "No judgment or theorem '"+ name +"' found");
		} else {
			Position pos = element.getPosition();
			//TODO: later we may need to open an editor:
			ISourceViewer sourceViewer = proofEditor.getPublicSourceViewer();
			sourceViewer.setRangeIndication(pos.getOffset(), pos.getLength(), true);
		}*/
		return null;
	}

	/**
	 * Resolve a name within a proof as a declaration with some proof (perhaps 
	 * a different one due to modules)
	 * @param proj project in which use occurs
	 * @param pf proof to search
	 * @param name to look for in it
	 * @return a pair or null (if not found).  
	 * The first part of the pair will never be null, but the second part is null if
	 * the resolution refers to the whole proof.
	 */
	protected Pair<IDEProof,Node> resolve(IProject proj, IDEProof pf, String name) {
		ProofBuilder pb = ProofBuilder.getProofBuilder(proj);
		boolean hasDot = name.contains(".");
		Node resolved = null;
		if (hasDot) {
			int last = name.lastIndexOf('.');
			IDEProof next = resolveModule(pb, pf, name.substring(0,last));
			final String lastPart = name.substring(last+1);
			if (next != null) {
				resolved = (Node)next.findDeclarationByName(lastPart);
				if (resolved == null) {
					resolved = next.findRuleLikeByName(lastPart);
				}
			}
			if (resolved != null) {
				pf = next;
			} else {
				ModuleId id = new ModuleId(name.split("[.]"));
				IProjectStorage st = pb.getStorage(id);
				if (st == null) return null;
				pf = IDEProof.getProof(st);
				if (pf == null) return null;
			}
		} else {
			resolved = (Node)pf.findDeclarationByName(name);
			if (resolved == null) resolved = pf.findRuleLikeByName(name);
			if (resolved == null) return null;
		}
		return Pair.create(pf, resolved);
	}
	
	/**
	 * See if given name can be resolved to a (already loaded) module.
	 * @param pb proof builder, must not be null
	 * @param pf context in which we look up the name
	 * @param name name of module.  It may have dots (module id) or not (module renaming)
	 * @return proof for the module
	 */
	protected IDEProof resolveModule(ProofBuilder pb, IDEProof pf, String name) {
		if (name.contains(".")) {
			ModuleId id = new ModuleId(name.split("[.]"));
			IProjectStorage st = pb.getStorage(id);
			if (st != null) return IDEProof.getProof(st);
			// We don't want to permit x.y.z to be as interpreted as global y.z
			// And so we lookup z in x.y, not y.z in x
			String initial = name.substring(0, name.lastIndexOf('.'));
			IDEProof next = resolveModule(pb, pf, initial);
			if (next == null) return null; // give up
			return resolveModule(pb,next,name.substring(name.lastIndexOf('.')+1));
		} else {
			Named decl = pf.findDeclarationByName(name);
			if (decl instanceof ModulePart) {
				ModulePart mpart = (ModulePart)decl;
				if (mpart.getName().equals(name)) {
					QualName mq = mpart.getModule();
					return resolveModule(pb, pf, mq.toString());
				}
			}
			return null;
		}
	}
}
