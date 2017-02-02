package org.sasylf.refactor;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IContainer;
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
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.sasylf.Proof;
import org.sasylf.project.ProofBuilder;

import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.ast.PackageDeclaration;
import edu.cmu.cs.sasylf.util.Location;

public class MoveProofFile extends MoveParticipant {

	private IFile proofFile;
	private IContainer newFolder;
	private IFile newProofFile;
	private Change change;

	public MoveProofFile() { }

	@Override
	protected boolean initialize(Object element) {
		MoveArguments args = super.getArguments();
		Object destination = args.getDestination();
		if (element instanceof IFile && destination instanceof IContainer) {
			proofFile = (IFile)element;
			// System.out.println("Participating in moving " + element);
			newFolder = (IContainer)destination;
			newProofFile = newFolder.getFile(new Path(proofFile.getName()));
			return true;
		} else {
			// System.out.println("Cannot participate, element is " + element + ", dest = " + destination);
			return false;
		}
	}


	@Override
	public String getName() {
		return "Move SASyLF Proof File";
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
		TextFileChange result = new TextFileChange("move package", newProofFile);
		ITextFileBufferManager manager = null;
		boolean connected = false;
		pm.beginTask("Check Move Proof File", 100);
		try {
			IContainer oldFolder = proofFile.getParent();
			IPath oldPath = ProofBuilder.getProofFolderRelativePath(oldFolder);
			IPath newPath = ProofBuilder.getProofFolderRelativePath(newFolder);
			String oldPackage = PackageDeclaration.toString(oldPath.segments());
			String newPackage = PackageDeclaration.toString(newPath.segments());
			pm.worked(10);
			manager = FileBuffers.getTextFileBufferManager();
			manager.connect(fullPath, LocationKind.IFILE, new SubProgressMonitor(pm, 25));
			connected = true;
			IDocument document = manager.getTextFileBuffer(fullPath, LocationKind.IFILE).getDocument();
			createPackageReplaceChange(document,result,status,oldPackage,newPackage);
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

	protected void createPackageReplaceChange(IDocument doc, TextFileChange change, RefactoringStatus status, String oldPackage, String newPackage) 
			throws CoreException, BadLocationException {
		// first see if we can get a CompUnit:
		CompUnit cu = Proof.getCompUnit(proofFile);
		if (cu == null) {
			status.addWarning("proof file is not syntactically legal; package declaration may be mislocated");
		}
		IRegion packageLoc = null;
		if (cu != null) {
			PackageDeclaration pd = cu.getPackage();
			if (pd.getPieces().length > 0) {
				Location loc = pd.getLocation();
				IRegion line = doc.getLineInformation(loc.getLine()-1);
				packageLoc = new Region(line.getOffset()+loc.getColumn()-1,7);
				String text = doc.get(packageLoc.getOffset(), packageLoc.getLength());
				if (!text.equals("package")) {
					// System.out.println("expected 'package', found '" + text + "'");
					status.addWarning("proof file is out of sync with build");
					packageLoc = null;
				}
			}
		} 
		if (packageLoc == null && !oldPackage.isEmpty()){
			packageLoc = new FindReplaceDocumentAdapter(doc).find(0,"package",true,true,true,false);
		}
		IRegion nameLoc = null;
		if (packageLoc != null) {
			int offset = packageLoc.getOffset()+packageLoc.getLength();
			while (Character.isWhitespace(doc.getChar(offset))) {
				++offset;
			}
			int length = 0;
			while (doc.getChar(offset+length) != ';') {
				++length;
			}
			nameLoc = new Region(offset,length);
			if (doc.get(offset, length).equals(newPackage)) {
				// no change needed
				return;
			}
			// System.out.println("Replacing " + doc.get(offset, length) + " with " + newPackage);
		}

		TextEdit edit;

		if (packageLoc == null) {
			if (newPackage.length() == 0) return; // no change needed.
			edit = new InsertEdit(0,"package "+newPackage+";"+doc.getLineDelimiter(0));
		} else {
			if (newPackage.length() == 0) {
				// everything is removed up through the semicolon after the name
				edit = new DeleteEdit(packageLoc.getOffset(),nameLoc.getOffset()-packageLoc.getOffset()+nameLoc.getLength()+1);
			} else {
				edit = new ReplaceEdit(nameLoc.getOffset(),nameLoc.getLength(),newPackage);
			}
		}
		// System.out.println("edit is " + edit);
		change.setEdit(edit);
	}
}
