package org.sasylf.project;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.module.ModuleChangedEvent;
import edu.cmu.cs.sasylf.module.ModuleChangedEvent.EventType;
import edu.cmu.cs.sasylf.module.ModuleFinder;
import edu.cmu.cs.sasylf.module.ModuleId;
import edu.cmu.cs.sasylf.module.RootModuleProvider;
import edu.cmu.cs.sasylf.util.Span;

class ProjectModuleProvider extends RootModuleProvider implements IResourceChangeListener {
	private final IProject project;

	ProjectModuleProvider(IProject p) {
		super(ProofBuilder.getProofFolder(p).getLocation().toFile());
		project = p;
		project.getWorkspace().addResourceChangeListener(this);
	}

	@Override
	protected CompUnit parseAndCheck(ModuleFinder mf, File f, ModuleId id, Span loc) {
		return ((ProjectModuleFinder)mf).parseAndCheck(f, id, loc);
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
					ProjectModuleProvider.this.fireModuleEvent(new ModuleChangedEvent(getNewModuleId(resource), e));
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

	private ModuleId getNewModuleId(IResource resource) {
		IPath path = resource.getProjectRelativePath();
		
		// XXX: check project has proof folder or not
		// if it does not, don't remove first segment
		
		// gives warning if package is not "correct" -- does not seem to allow 
		return new ModuleId(path.removeFirstSegments(1).toFile());
	}
}