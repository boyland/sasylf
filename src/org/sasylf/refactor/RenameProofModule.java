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
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.sasylf.IDEProof;
import org.sasylf.project.ProjectModuleFinder;
import org.sasylf.project.ProofBuilder;

import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.ast.Context;
import edu.cmu.cs.sasylf.ast.Node;
import edu.cmu.cs.sasylf.ast.QualName;
import edu.cmu.cs.sasylf.module.ModuleId;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.ParseUtil;

public class RenameProofModule extends RenameParticipant {

	IFile proofFile, newProofFile;
	String oldName, newName;
	CompositeChange change;

	public RenameProofModule() { }

	@Override
	protected boolean initialize(Object element) {
		RenameArguments args = super.getArguments();
		newName = args.getNewName();
		change = new CompositeChange("Rename Proof Module");
		if (element instanceof IFile) {
			proofFile = (IFile)element;
			String name = proofFile.getName();
			oldName = name.substring(0, name.length() - 4);
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
			change.add(this.createChange(pm, status, proofFile));
			checkDependencies(pm, status);
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

	protected Change createChange(IProgressMonitor pm, RefactoringStatus status, IFile file) 
			throws OperationCanceledException, CoreException
	{
		IPath fullPath = file.getFullPath();
		TextFileChange result = new TextFileChange("rename module", newProofFile);
		SubMonitor sub = SubMonitor.convert(pm);
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
			manager.connect(fullPath, LocationKind.IFILE, sub);
			connected = true;
			IDocument document = manager.getTextFileBuffer(fullPath, LocationKind.IFILE).getDocument();
			createModuleRenameChange(file,newModuleName,document,result, status);
			document.getLineInformation(1);
			pm.worked(40);
		} catch (BadLocationException e) {
			status.addWarning("couldn't change package info: internal error " + e.getMessage());
		} finally {
			if (connected) {
				manager.disconnect(fullPath, LocationKind.IFILE, sub);
			}
		}
		return result; 
	}

	private void createModuleRenameChange(IFile file, String newModuleName, IDocument doc, TextFileChange result, RefactoringStatus status) throws BadLocationException {
		// first see if we can get a CompUnit:
		CompUnit cu = IDEProof.getCompUnit(file);
		if (cu == null) {
			status.addWarning("proof file is not syntactically legal; module name may be mislocated");
		}
		IRegion moduleLoc = null;
		if (cu != null) {
			moduleLoc = getModuleLocation(doc, cu);
			String text = doc.get(moduleLoc.getOffset(), moduleLoc.getLength());
			if (!text.equals("module")) {	// nothing to change within document
				//System.out.println("expected 'module', found '" + text + "'");
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

		String name = doc.get(offset, length);

		if (name.equals(newModuleName)) {
			// no change needed
			return;
		}
		System.out.println("Replacing " + name + " with " + newModuleName);

		createEdit(newModuleName, result, nameLoc);
	}

	protected void createDependencyChanges(IFile file, IProgressMonitor pm, RefactoringStatus status) 
			throws OperationCanceledException, CoreException
	{
		if (file == null) return;
		IPath fullPath = file.getFullPath();
		ITextFileBufferManager manager = null;
		boolean connected = false;
		SubMonitor sub = SubMonitor.convert(pm);
		try {
			pm.worked(10);
			manager = FileBuffers.getTextFileBufferManager();
			manager.connect(fullPath, LocationKind.IFILE, sub);
			connected = true;
			IDocument document = manager.getTextFileBuffer(fullPath, LocationKind.IFILE).getDocument();
			createModulePartRenameChange(file,document,status);
			document.getLineInformation(1);
			pm.worked(10);
		} catch (BadLocationException e) {
			status.addWarning("couldn't change package info: internal error " + e.getMessage());
		} finally {
			if (connected) {
				manager.disconnect(fullPath, LocationKind.IFILE, sub);
			}
		}
	}

	private String getNewName() {
		return newName.substring(0, newName.length()-4);
	}

	private void createModulePartRenameChange(IFile file, IDocument doc, 
			RefactoringStatus status) throws BadLocationException {
		// first see if we can get a CompUnit:
		CompUnit cu = IDEProof.getCompUnit(file);
		if (cu == null) {
			status.addWarning("proof file is not syntactically legal; module name may be mislocated");
		}
		IContainer proofFolder = proofFile.getParent();
		IPath proofPath = ProofBuilder.getProofFolderRelativePath(proofFolder);
		String[] proofPackage = proofPath.segments();
		IRegion moduleLoc = null;
		String newModuleName = getNewName();
		if (cu != null) {
			List<QualName> qualNames = new ArrayList<>();
			Consumer<QualName> consumer = name -> {
				if (name.getLastSegment().equals(oldName)) {
					QualName source = name.getSource();
					if (source != null) {
						Object o = source.resolve(null);
						if (o instanceof String[] && Arrays.equals(proofPackage, (String[]) o)) {
							qualNames.add(name);
						}
					}
				}
			};

			cu.collectQualNames(consumer);

			for (int i = qualNames.size() - 1; i >= 0; --i) {
				QualName name = qualNames.get(i);
				TextFileChange result = new TextFileChange("rename module part", file);
				moduleLoc = getModuleLocation(doc, name);

				int offset = moduleLoc.getOffset();
				int length = moduleLoc.getLength();

				if (doc.get(offset, length).equals(newModuleName)) {
					// no change needed
					continue;
				}
				System.out.println("Replacing " + doc.get(offset, length) + " with " + newModuleName);

				createEdit(newModuleName, result, moduleLoc);
				change.add(result);
			}
		}
	}

	private void createEdit(String newModuleName, TextFileChange result,
			IRegion moduleLoc) {
		TextEdit edit = new ReplaceEdit(moduleLoc.getOffset(),moduleLoc.getLength(),newModuleName);
		Context.updateVersion();

		result.setEdit(edit);  
	}

	private IRegion getModuleLocation(IDocument doc, Node node) throws BadLocationException {
		Location loc = node.getLocation();
		IRegion line = doc.getLineInformation(loc.getLine()-1);

		if (!(node instanceof QualName)) {
			return new Region(line.getOffset()+loc.getColumn()-1,6);
		} 

		QualName qn = (QualName) node;
		loc = qn.getEndLocation();
		line = doc.getLineInformation(loc.getLine()-1);

		int length = qn.getLastSegment().length();
		int offset = line.getOffset() + loc.getColumn() - length - 1;

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
			createDependencyChanges(pb.getResource(dependency), pm, status);
		}

		pm.done();
	}
}
