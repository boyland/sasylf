package org.sasylf.project;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.sasylf.IDEProof;
import org.sasylf.ProofChecker;

import edu.cmu.cs.sasylf.Proof;
import edu.cmu.cs.sasylf.module.ModuleChangedEvent;
import edu.cmu.cs.sasylf.module.ModuleChangedEvent.EventType;
import edu.cmu.cs.sasylf.module.ModuleFinder;
import edu.cmu.cs.sasylf.module.ModuleId;
import edu.cmu.cs.sasylf.module.RootModuleProvider;
import edu.cmu.cs.sasylf.util.Span;

public class ProjectModuleProvider extends RootModuleProvider implements IResourceChangeListener {
	private final IProject project;

	ProjectModuleProvider(IProject p) {
		super(ProofBuilder.getProofFolder(p).getLocation().toFile());
		project = p;
		project.getWorkspace().addResourceChangeListener(this);
	}
	
	public IProject getProject() {
		return project;
	}

	public IFile getFileFromModuleId(ModuleId id) {
		File file = super.getFile(id);
		// XXX: reconsider design

		IPath path = Path.fromOSString(file.getAbsolutePath());
		return project.getWorkspace().getRoot().getFileForLocation(path);
	}

	@Override
	protected Proof parseAndCheck(ModuleFinder mf, File f, ModuleId id, Span loc) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IPath path = Path.fromOSString(f.getAbsolutePath());
		IResource res = workspace.getRoot().getFileForLocation(path);
		ProofChecker.analyzeSlf(mf, id, res);
		return IDEProof.getProof(getFileFromModuleId(id));
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		// if this resource is in our project, then inform our own listeners
		if (event.getType() != IResourceChangeEvent.POST_CHANGE)
			return;

		IResourceDelta projDelta = event.getDelta().findMember(project.getFullPath());

		if (projDelta == null) return;
		//System.out.println("got relevant change for project " + project);

		IResourceDeltaVisitor visitor = new IResourceDeltaVisitor() {
			@Override
			public boolean visit(IResourceDelta delta) {
				IResource resource = delta.getResource();
				
				// we're only interested in files with the "slf" extension
				if (resource.getType() == IResource.FILE && 
						"slf".equalsIgnoreCase(resource.getFileExtension())) {
					
					EventType e = null;
					
					// determine which kind of change has occurred
					switch(delta.getKind()) {
					case IResourceDelta.CHANGED:
						e = EventType.CHANGED;
						break;
					case IResourceDelta.ADDED:
						e = EventType.ADDED;
						break;
					case IResourceDelta.REMOVED:
						e = EventType.REMOVED;
						break;
					}   
					
					// shouldn't happen, but just in case...
					if (e == null) throw new NullPointerException("Event should not be null!");
					
					// fire the module even with the module id 
					ProjectModuleProvider.this.fireModuleEvent(new ModuleChangedEvent(
							ProofBuilder.getId(resource), e));
				}

				return true;
			}
		};
		try {
			projDelta.accept(visitor);
		} catch (CoreException e) {
			// open error dialog with syncExec or print to plugin log file
		}
	}
}