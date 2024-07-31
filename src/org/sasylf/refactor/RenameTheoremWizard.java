package org.sasylf.refactor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditProcessor;
import org.eclipse.ui.PlatformUI;
import org.sasylf.IDEProof;
import org.sasylf.project.ProjectModuleFinder;
import org.sasylf.project.ProofBuilder;
import org.sasylf.util.DocumentUtil;

import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.ast.Node;
import edu.cmu.cs.sasylf.ast.QualName;
import edu.cmu.cs.sasylf.ast.RuleLike;
import edu.cmu.cs.sasylf.ast.Theorem;
import edu.cmu.cs.sasylf.module.ModuleId;
import edu.cmu.cs.sasylf.util.Location;

public class RenameTheoremWizard extends Wizard implements IWizard {
	private RenameTheoremWizardPage page;
	private RefactoringContext context;
	private Map<IDocument, TextEdit> changes;

	public RenameTheoremWizard(RefactoringContext context) {
		super();
		setNeedsProgressMonitor(true);
		this.context = context;
		changes = new HashMap<IDocument, TextEdit>();
	}

	@Override
	public void addPages() {
		page = new RenameTheoremWizardPage(context);
		addPage(page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	@Override
	public boolean performFinish() {
		try {
			getOriginalFileChange();
			collectReferencedChanges();
			makeChanges();
			PlatformUI.getWorkbench().saveAllEditors(false);	// save all changed editors
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private void getOriginalFileChange() throws BadLocationException {
		IDocument doc = context.getProofDocument();
		MultiTextEdit edit = new MultiTextEdit();
		IRegion moduleLoc = null;
		String newName = page.getTheoremName();

		Map<String, RuleLike> map = new HashMap<>();
		context.getCompUnit().collectRuleLike(map);
		
		Object ruleLike = map.get(context.getOldName());
		
		// If object is not a Theorem, return without doing anything
		if (!(ruleLike instanceof Theorem)) {
			return;
		}
		
		Theorem theorem = (Theorem)ruleLike;

		moduleLoc = getModuleLocation(doc, theorem);

		int offset = moduleLoc.getOffset();
		int length = moduleLoc.getLength();
		
		System.out.println("Replacing " + doc.get(offset, length) + " with " + newName);
		edit.addChild(new ReplaceEdit(offset, length, newName));

		List<QualName> qualNames = new ArrayList<>();
		Consumer<QualName> consumer = name -> {			
			if (name.getLastSegment().equals(context.getOldName())) {
				if (name.resolve(null) instanceof Theorem) {
					qualNames.add(name);
				}
			}
			
		};

		context.collectQualNames(consumer);

		// work backwards
		for (int i = qualNames.size() - 1; i >= 0; --i) {
			QualName name = qualNames.get(i);

			moduleLoc = getModuleLocation(doc, name);

			offset = moduleLoc.getOffset();
			length = moduleLoc.getLength();
			
			System.out.println("Replacing " + doc.get(offset, length) + " with " + newName);
			edit.addChild(new ReplaceEdit(offset, length, newName));
		}
		changes.put(doc, edit);
	}

	// get the change in the specified file
	private void getFileChange(IFile file, IDocument doc) throws BadLocationException {
		CompUnit cu = IDEProof.getCompUnit(file);
		if (cu == null) return;
		MultiTextEdit edit = new MultiTextEdit();
		IRegion moduleLoc = null;
		String newName = page.getTheoremName();
		IContainer proofFolder = file.getParent();
		IPath proofPath = ProofBuilder.getProofFolderRelativePath(proofFolder);
		String[] proofPackage = proofPath.segments();
		List<QualName> qualNames = new ArrayList<>();
		Consumer<QualName> consumer = name -> {
			if (name.getLastSegment().equals(context.getOldName())) {
				QualName source = name.getSource();
				if (source != null) {
					Object o = source.resolve(null);
					if (o instanceof CompUnit && ((CompUnit)o).equals(context.getCompUnit())) {
						qualNames.add(name);
					} else if (o instanceof String[] && Arrays.equals(proofPackage, (String[]) o)) {
						qualNames.add(name);
					}
				}
			}
		};

		cu.collectQualNames(consumer);

		// work backwards
		for (int i = qualNames.size() - 1; i >= 0; --i) {  
			QualName name = qualNames.get(i);

			moduleLoc = getModuleLocation(doc, name);

			int offset = moduleLoc.getOffset();
			int length = moduleLoc.getLength();

			if (doc.get(offset, length).equals(newName)) {
				// no change needed
				continue;
			}
			
			System.out.println("Replacing " + doc.get(offset, length) + " with " + newName + " in file " + file.getName());
			edit.addChild(new ReplaceEdit(offset, length, newName));
		}
		changes.put(doc, edit);

	}

	// collect all referenced changes
	private void collectReferencedChanges() throws BadLocationException, CoreException {
		IProject p = context.getProofFile().getProject();
		ProofBuilder pb = ProofBuilder.getProofBuilder(p);
		ProjectModuleFinder moduleFinder = pb.getModuleFinder();
		ModuleId id = ProofBuilder.getId(context.getProofFile());

		Set<ModuleId> dependencies = moduleFinder.getDependencies(id);

		for (ModuleId dependency : dependencies) {
			IFile file = pb.getResource(dependency);
			IPath fullPath = file.getFullPath();
			ITextFileBufferManager manager = null;
			boolean connected = false;
			try {
				manager = FileBuffers.getTextFileBufferManager();
				manager.connect(fullPath, LocationKind.IFILE, null);
				connected = true;
				IDocument document = manager.getTextFileBuffer(fullPath, LocationKind.IFILE).getDocument();
				getFileChange(file, document);
				document.getLineInformation(1);
			} catch (BadLocationException e) {
				e.printStackTrace();
			} finally {
				if (connected) {
					manager.disconnect(fullPath, LocationKind.IFILE, null);
				}
			}
			
		}
	}

	/**
	 * Make all the changes to all documents affected by this refactoring.
	 * @return whether the changes were able to be made or not
	 */
	private boolean makeChanges() {
		boolean changeGood = true;
		for (Map.Entry<IDocument,TextEdit> change : changes.entrySet()) {
			IDocument doc = change.getKey();
			TextEdit edits = change.getValue();
			try {
				new TextEditProcessor(doc, edits, TextEdit.NONE).performEdits();
			} catch (MalformedTreeException e) {
				e.printStackTrace();
				changeGood = false;
			} catch (BadLocationException e) {
				e.printStackTrace();
				changeGood = false;
			} finally {
				if (!changeGood) break;
			}
		}
		return changeGood;
	}

	private IRegion getModuleLocation(IDocument doc, Node node) throws BadLocationException {
		if ((node instanceof QualName)) {
			QualName qn = (QualName) node;
			Location loc = qn.getEndLocation();

			int length = qn.getLastSegment().length();
			int offset = DocumentUtil.getOffset(loc, doc) - length;

			return new Region(offset, length);
		} else {
			Theorem t = (Theorem) node;
			return new FindReplaceDocumentAdapter(doc).find(0,t.getName(),true,true,true,false);
		}
	}

}