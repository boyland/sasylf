package org.sasylf.refactor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.sasylf.IDEProof;
import org.sasylf.project.ProjectModuleFinder;
import org.sasylf.project.ProofBuilder;

import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.ast.Context;
import edu.cmu.cs.sasylf.ast.PackageDeclaration;
import edu.cmu.cs.sasylf.ast.QualName;
import edu.cmu.cs.sasylf.module.ModuleId;
import edu.cmu.cs.sasylf.util.Location;

public class MoveProofFile extends MoveParticipant {

	private IFile proofFile;
	private IContainer newFolder;
	private IFile newProofFile;
	private CompositeChange change;
	private IPath newPath;
	private String newPackage;

	public MoveProofFile() { }

	@Override
	protected boolean initialize(Object element) {
		MoveArguments args = super.getArguments();
		Object destination = args.getDestination();
		change = new CompositeChange("Move Proof File");
		if (element instanceof IFile && destination instanceof IContainer) {
			proofFile = (IFile)element;
			// System.out.println("Participating in moving " + element);
			newFolder = (IContainer)destination;
			newProofFile = newFolder.getFile(new Path(proofFile.getName()));
			newPath = ProofBuilder.getProofFolderRelativePath(newFolder);
			newPackage = PackageDeclaration.toString(newPath.segments());
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
			
			if (newPackage.length() == 0) {
				IProject p = proofFile.getProject();
				ProofBuilder pb = ProofBuilder.getProofBuilder(p);
				ProjectModuleFinder moduleFinder = pb.getModuleFinder();
				ModuleId id = ProofBuilder.getId(proofFile);
				
				if (moduleFinder.getDependencies(id).size() > 0) {
					status.addWarning("Moving file with dependencies into default package");
				}
			}
			change.add(this.createChange(proofFile, pm, status));
		} catch (CoreException e) {
			status.addFatalError(e.getStatus().getMessage());
		}
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
	OperationCanceledException {
		//System.out.println("returning the change");
		return change;
	}

	protected Change createChange(IFile fileName, IProgressMonitor pm, RefactoringStatus status) 
			throws OperationCanceledException, CoreException
	{
		SubMonitor sub = SubMonitor.convert(pm);
		IPath fullPath = fileName.getFullPath();
		TextFileChange result = new TextFileChange("move package", newProofFile);
		ITextFileBufferManager manager = null;
		boolean connected = false;
		pm.beginTask("Check Move Proof File", 100);
		try {
			IContainer oldFolder = proofFile.getParent();
			IPath oldPath = ProofBuilder.getProofFolderRelativePath(oldFolder);
			String oldPackage = PackageDeclaration.toString(oldPath.segments());
			pm.worked(10);
			manager = FileBuffers.getTextFileBufferManager();
			manager.connect(fullPath, LocationKind.IFILE, sub);
			connected = true;
			IDocument document = manager.getTextFileBuffer(fullPath, LocationKind.IFILE).getDocument();
			createPackageReplaceChange(document,result,status,oldPackage);
			pm.worked(40);
			checkDependencies(pm, status);
		} catch (BadLocationException e) {
			status.addWarning("couldn't change package info: internal error " + e.getMessage());
		} finally {
			if (connected) {
				manager.disconnect(fullPath, LocationKind.IFILE, sub);
			}
			pm.done();
		}
		return result; 
	}

	protected void createPackageReplaceChange(IDocument doc, TextFileChange change, RefactoringStatus status, String oldPackage) 
			throws CoreException, BadLocationException {
		// first see if we can get a CompUnit:
		CompUnit cu = IDEProof.getCompUnit(proofFile);
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
		//System.out.println("edit is " + edit);
		change.setEdit(edit);
	}

	protected void createDependencyChanges(IFile fileName, IProgressMonitor pm, RefactoringStatus status) 
			throws OperationCanceledException, CoreException
	{
		IPath fullPath = fileName.getFullPath();
		ITextFileBufferManager manager = null;
		boolean connected = false;
		SubMonitor sub = SubMonitor.convert(pm);
		try {
			IContainer oldFolder = proofFile.getParent();
			IPath oldPath = ProofBuilder.getProofFolderRelativePath(oldFolder);
			String oldPackage = PackageDeclaration.toString(oldPath.segments());
			pm.worked(10);
			manager = FileBuffers.getTextFileBufferManager();
			manager.connect(fullPath, LocationKind.IFILE, sub);
			connected = true;
			IDocument document = manager.getTextFileBuffer(fullPath, LocationKind.IFILE).getDocument();
			createPackageReplaceChangeModule(fileName,document,status,oldPackage, oldPath);
			document.getLineInformation(1);
		} catch (BadLocationException e) {
			status.addWarning("couldn't change package info: internal error " + e.getMessage());
		} finally {
			if (connected) {
				manager.disconnect(fullPath, LocationKind.IFILE, sub);
			}
		}

	}

	private void createPackageReplaceChangeModule(IFile file, IDocument doc, 
			RefactoringStatus status, String oldPackage, IPath oldPath) throws BadLocationException {
		// first see if we can get a CompUnit:
		CompUnit cu = IDEProof.getCompUnit(file);
		if (cu == null) {
			status.addWarning("proof file is not syntactically legal; package declaration may be mislocated");
		}	
		IRegion moduleLoc = null;
		if (cu != null) {
			String fileName = proofFile.getName();
			String movedFile = fileName.substring(0, fileName.length() - 4);
			String[] old = oldPath.segments();
			
			List<QualName> qualNames = new ArrayList<>();
			Consumer<QualName> consumer = name -> {
				QualName source = name.getSource();
				if (source != null) {
					Object o = source.resolve(null);
					if (o instanceof String[] && name.getLastSegment().equals(movedFile)) {
						String[] p = (String[]) o;
						
						if (Arrays.equals(old,p)) {
							//System.out.println("Matched name " + name);
							qualNames.add(name);
						}
					}
				}
			};

			cu.collectQualNames(consumer);
			//System.out.println("Done finding qual names in this file!");
			for (int i = qualNames.size() - 1; i >= 0; --i) {
				QualName name = qualNames.get(i);
				TextFileChange result = new TextFileChange("move package", file);
				moduleLoc = getModuleLocation(oldPackage, doc, name);

				int offset = moduleLoc.getOffset();
				int length = moduleLoc.getLength();

				if (doc.get(offset, length).equals(newPackage)) {
					// no change needed
					continue;
				}
				System.out.println("Replacing " + doc.get(offset, length) + " with " + newPackage);

				createEdit(result, moduleLoc);
				change.add(result);
			}
		} else {
			moduleLoc = new FindReplaceDocumentAdapter(doc).find(0,"module",true,true,true,false);
			if (moduleLoc == null) {
				// System.out.println("Found no 'module' keyword");
				return;
			}
		}
	}

	private void createEdit(TextFileChange result, IRegion moduleLoc) {
		TextEdit edit = new ReplaceEdit(moduleLoc.getOffset(),moduleLoc.getLength(),newPackage);
		Context.updateVersion();	// added this to Context instead of QualName because QualName gets overwritten

		// System.out.println("edit is " + edit);
		result.setEdit(edit);  
	}

	private IRegion getModuleLocation(String oldPackage, IDocument doc, QualName node) throws BadLocationException {
		Location loc = node.getLocation();
		IRegion line = doc.getLineInformation(loc.getLine()-1);

		QualName qn = node;
		loc = qn.getLocation();
		line = doc.getLineInformation(loc.getLine()-1);

		int length = oldPackage.length();
		int offset = line.getOffset() + loc.getColumn() - 1;
		return new Region(offset, length);
	}

	private void checkDependencies(IProgressMonitor pm, RefactoringStatus status) 
			throws OperationCanceledException, CoreException {
		IProject p = proofFile.getProject();
		ProofBuilder pb = ProofBuilder.getProofBuilder(p);
		ProjectModuleFinder moduleFinder = pb.getModuleFinder();
		ModuleId id = ProofBuilder.getId(proofFile);

		Set<ModuleId> dependencies = moduleFinder.getDependencies(id);

		for (ModuleId dependency : dependencies) {
			//System.out.println("IFile: " + pb.getResource(dependency));
			createDependencyChanges(pb.getResource(dependency), pm, status);
		}

		pm.done();
	}
}
