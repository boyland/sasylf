package org.sasylf.refactor;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.sasylf.Proof;

import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.ParseUtil;

public class RenameProofModule extends RenameParticipant {

	IFile proofFile, newProofFile;
	String newName;
	Change change;

	public RenameProofModule() { }

	@Override
	protected boolean initialize(Object element) {
		RenameArguments args = super.getArguments();
		newName = args.getNewName();
		if (element instanceof IFile) {
			proofFile = (IFile)element;
			// System.out.println("Participating in renaming " + element);
			newProofFile = proofFile.getParent().getFile(new Path(newName));
			return true;
		} else {
			// System.out.println("Cannot participate, element is " + element + ", new name = " + newName);
			return false;
		}
	}

	@Override
	public String getName() {
		return "Rename SASyLF Module";
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm,
			CheckConditionsContext context) throws OperationCanceledException {
		if (pm == null) pm = new NullProgressMonitor();
		RefactoringStatus status = new RefactoringStatus();
		try {
			change = this.createChange(pm, status);
		} catch (CoreException e) {
			status.addFatalError(e.getStatus().getMessage());
		}
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
	OperationCanceledException {
		return change;
	}

	protected Change createChange(IProgressMonitor pm, RefactoringStatus status) 
			throws OperationCanceledException, CoreException
	{
		IPath fullPath = proofFile.getFullPath();
		TextFileChange result = new TextFileChange("rename module", newProofFile);
		ITextFileBufferManager manager = null;
		boolean connected = false;
		pm.beginTask("Check Rename Module", 100);
		try {
			if (!newName.endsWith(".slf")) {
				status.addError("'" + newName + "' does not end in '.slf' as required.");
				return result;
			}
			String newModuleName = newName.substring(0, newName.length()-4);
			if (!ParseUtil.isLegalIdentifier(newModuleName)) {
				status.addError("'" + newModuleName + "' is not a legal SASyLF identifier");
				return result;
			}
			pm.worked(10);
			manager = FileBuffers.getTextFileBufferManager();
			manager.connect(fullPath, LocationKind.IFILE, new SubProgressMonitor(pm, 25));
			connected = true;
			IDocument document = manager.getTextFileBuffer(fullPath, LocationKind.IFILE).getDocument();
			createModuleRenameChange(newModuleName,document,result,status);
			document.getLineInformation(1);
			pm.worked(40);
		} catch (BadLocationException e) {
			status.addWarning("couldn't change package info: internal error " + e.getMessage());
		} finally {
			if (connected) {
				manager.disconnect(fullPath, LocationKind.IFILE, new SubProgressMonitor(pm, 25));
			}
			pm.done();
		}
		return result; 
	}

	private void createModuleRenameChange(String newModuleName, IDocument doc, TextFileChange result, RefactoringStatus status) throws BadLocationException {
		// first see if we can get a CompUnit:
		CompUnit cu = Proof.getCompUnit(proofFile);
		if (cu == null) {
			status.addWarning("proof file is not syntactically legal; module name may be mislocated");
		}
		IRegion moduleLoc = null;
		if (cu != null) {
			Location loc = cu.getLocation();
			IRegion line = doc.getLineInformation(loc.getLine()-1);
			moduleLoc = new Region(line.getOffset()+loc.getColumn()-1,6);
			String text = doc.get(moduleLoc.getOffset(), moduleLoc.getLength());
			if (!text.equals("module")) {
				// System.out.println("expected 'module', found '" + text + "'");
				return;
			}
		} else {
			moduleLoc = new FindReplaceDocumentAdapter(doc).find(0,"module",true,true,true,false);
			if (moduleLoc == null) {
				// System.out.println("Found no 'module' keyword");
				return;
			}
		}

		int offset = moduleLoc.getOffset()+moduleLoc.getLength();
		while (Character.isWhitespace(doc.getChar(offset))) {
			++offset;
		}
		int length = 0;
		char ch;
		while ((ch=doc.getChar(offset+length)) != ';' && !Character.isWhitespace(ch)) {
			++length;
		}
		IRegion nameLoc = new Region(offset,length);

		if (doc.get(offset, length).equals(newModuleName)) {
			// no change needed
			return;
		}
		System.out.println("Replacing " + doc.get(offset, length) + " with " + newModuleName);

		TextEdit edit = new ReplaceEdit(nameLoc.getOffset(),nameLoc.getLength(),newModuleName);

		// System.out.println("edit is " + edit);
		result.setEdit(edit);  

	}

}
