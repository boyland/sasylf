package org.sasylf.project;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.sasylf.Activator;
import org.sasylf.IDEProof;
import org.sasylf.Marker;
import org.sasylf.util.IProjectStorage;
import org.sasylf.util.ResourceStorage;

import edu.cmu.cs.sasylf.module.ModuleId;
import edu.cmu.cs.sasylf.module.ModuleProvider;
import edu.cmu.cs.sasylf.module.ResourceModuleProvider;

public class ProofBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = "org.sasylf.proofBuilder";

	/// Registration of the builder with the project
	// For some reason Eclipse doesn't make this information easy to get, 
	// so we do it ourselves:
	private static ConcurrentMap<IProject,ProofBuilder> builders = new ConcurrentHashMap<>();

	@Override
	protected void startupOnInitialize() {
		// System.out.println("Registering proof builder for " + this.getProject());
		super.startupOnInitialize();
		ProofBuilder old = builders.put(this.getProject(), this);
		if (old instanceof ProofBuilderProxy) {
			// System.out.println("Replaced proxy for " + this.getProject());
		} else {
			forceInitialBuild(getProject());
		}
	}

	/**
	 * Make sure that the project has been built at some time in the past
	 * or is in the process of being built.  In particular, if doWait
	 * is set, it ensures that 
	 * if the project has SASyLF nature, that a proof builder is created for it.
	 * @param project project to require building of.
	 */
	public static void ensureBuilding(final IProject project, boolean doWait) {
		try {
			if (!project.hasNature(MyNature.NATURE_ID)) return;
		} catch (CoreException e1) {
			e1.printStackTrace();
			return;
		}
		ProofBuilder pb = builders.get(project);
		if (pb != null) return;
		
		// The use of proxy obviates "doWait"
		// The old implementation was liable to deadlock.
		if (builders.putIfAbsent(project, new ProofBuilderProxy(project)) == null) {
			// System.out.println("Using a proxy for " + project);
			forceInitialBuild(project);
		} else {
			// Race because we just checked and a builder wasn't
			// registered for the project yet, but
			// Harmless because we created a proxy that will never be used
			// and will be immediately garbage.
			System.out.println("Harmless race condition finding builder for " + project);
		}
	}

	/**
	 * We ask Eclipse to do a build for this project.
	 * @param project project to force a build for
	 */
	public static void forceInitialBuild(final IProject project) {
		Job initialBuild = new Job("initial SASyLF build for " + project) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				// System.out.println("initial build for " + project + " starting...");
				try {
					project.build(FULL_BUILD, monitor);
					// System.out.println("Completed initial build for " + project);
				} catch (CoreException e) {
					e.printStackTrace();
					return e.getStatus();
				} catch (OperationCanceledException ex) {
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};
		initialBuild.schedule();
	}

	/**
	 * Return the instance for this project that has been created,
	 * creating it if necessary.
	 * Warning: because the proof can only be created indirectly, 
	 * and the code (in Eclipse) that leads up to the initialization
	 * performs some locks, this code can lead to a deadlock
	 * if the caller is involved in resource changes.
	 * @param p
	 * @return
	 */
	public static ProofBuilder getProofBuilder(IProject p) {
		// System.out.println("getProofBuilder(" + p + ")");
		ensureBuilding(p, true);
		ProofBuilder pb = builders.get(p);
		// System.out.println("pb = " + pb);
		return pb;
	}

	public void dispose() {
		builders.remove(this.getProject(),this);
	}

	protected ProjectModuleFinder moduleFinder;
	public ProjectModuleFinder getModuleFinder() {
		if (moduleFinder == null) {
			moduleFinder = new ProjectModuleFinder(this.getProject());
		}
		return moduleFinder;
	}

	class ProofDeltaVisitor implements IResourceDeltaVisitor {
		ProjectModuleFinder mf = getModuleFinder();
		//TODO: Instead of immediately checking proofs, put into a (priority?)queue
		// and place into the queue also everything that is dependent on this thing.
		// Make sure the dependencies stored are never cyclic.
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
		 */
		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			ModuleId id = getId(resource);
			if (id != null) {
				// System.out.println("incremental for " + resource);
				switch (delta.getKind()) {
				case IResourceDelta.CHANGED:
				case IResourceDelta.ADDED:
					// handle added resource
					mf.recheckNeeded(id);
					break;
				case IResourceDelta.REMOVED:
					// handle removed resource
					IDEProof.removeProof(resource);
					break;
				}
			}
			//return true to continue visiting children.
			return true;
		}
	}

	class ProofResourceVisitor implements IResourceVisitor {
		ProjectModuleFinder mf = getModuleFinder();
		@Override
		public boolean visit(IResource resource) {
			ModuleId id = getId(resource);
			if (id != null) {
				mf.recheckNeeded(id);
			}
			//return true to continue visiting children.
			return true;
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
	 *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IProject[] build(int kind, Map<String,String> args, IProgressMonitor monitor)
			throws CoreException {
		// we pass on the monitor to the the builder to use.
		if (monitor == null) monitor = new NullProgressMonitor();
		if (kind == CLEAN_BUILD) {
			getModuleFinder().clear();
			kind = FULL_BUILD;
		}
		if (kind == FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}
		return null;
	}

	/**
	 * Indicate that the proof associated with the resource
	 * needs to be re-checked.  This can take time.
	 * @param res resource for the proof.
	 */
	public void forceBuild(IResource res) {
		ModuleId id = getId(res);
		if (id == null) return;
		getModuleFinder().recheckNeeded(id);
		getModuleFinder().recheckAll(null);
	}

	protected void fullBuild(final IProgressMonitor monitor)
			throws CoreException {
		SubMonitor sub = SubMonitor.convert(monitor, "Full Build", 1000);
		
		getModuleFinder().clear();
		sub.worked(10);
		try {
			getProject().accept(new ProofResourceVisitor());
		} catch (CoreException e) {
		}
		sub.worked(90);
		getModuleFinder().recheckAll(sub.split(900));
	}

	protected void incrementalBuild(IResourceDelta delta,
			IProgressMonitor monitor) throws CoreException {
		SubMonitor sub = SubMonitor.convert(monitor, "Incremental Build", 1000);

		delta.accept(new ProofDeltaVisitor());
		sub.worked(100);
		getModuleFinder().recheckAll(sub.split(900));
	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		super.clean(null);
		if (monitor == null) monitor = new NullProgressMonitor();
		monitor.beginTask("clean", 100);
		try {
			getProject().deleteMarkers(Marker.ERROR_MARKER_ID, true, IResource.DEPTH_INFINITE);
			monitor.worked(90);
			getModuleFinder().clear();
			monitor.worked(10);
		} finally {
			monitor.done();
		}
	}

	public static ModuleId getId(IAdaptable res) {
		IPath path = getProofFolderRelativePath(res);
		if ("slf".equals(path.getFileExtension())) {
			path = path.removeFileExtension();
			int n = path.segmentCount();
			String[] pkg = new String[n-1];
			for (int i=0; i < n-1; ++i) {
				pkg[i] = path.segment(i);
			}
			return new ModuleId(pkg,path.lastSegment());
		} else return null;
	}
	
	/**
	 * Get the file associated with this module ID, if any.
	 * There are two reasons why a module ID might not be associated with a file:
	 * (1) There could be nothing in the project that has the ID, or
	 * (2) The module ID could be associated with a entry from a JAR or a library file.
	 * @param id module ID to look up, must not be null
	 * @return file associated with this ID or null if no file is associated.
	 */
	public IFile getResource(ModuleId id) {
		IProjectStorage st = getStorage(id);
		if (st != null) return st.getAdapter(IFile.class);
		/*
		ProjectModuleFinder f = getModuleFinder();
		
		ModuleProvider provider = f.lookupModule(id);
		
		if (provider instanceof ProjectModuleProvider) {
			return ((ProjectModuleProvider) provider).getFileFromModuleId(id);			
		}
		*/
		
		return null;
	}
	
	/**
	 * Get the storage associated with this module ID, if any.
	 * If nothing in the project is associated with the ID, return null.
	 * @param id module ID to look up, must not be null
	 * @return storage associated with this ID or null if none is associated.
	 */
	public IProjectStorage getStorage(ModuleId id) {
		ProjectModuleFinder f = getModuleFinder();
		ModuleProvider provider = f.lookupModule(id);
		if (provider instanceof ProjectModuleProvider) {
			final ProjectModuleProvider pmp = (ProjectModuleProvider)provider;
			return IProjectStorage.Adapter.adapt(pmp.getFileFromModuleId(id));
		} else if (provider instanceof ResourceModuleProvider) {
			final ResourceModuleProvider rmp = (ResourceModuleProvider)provider;
			String resourceString = rmp.asResourceString(id);
			return new ResourceStorage(resourceString,getProject());
		}
		return null;
	}

	/**
	 * Return the path from the path folder to this resource.
	 * If the resource is not in the proof folder, we instead
	 * return the relative path from the root of the project.
	 * @param src resource to return path for, must be adaptable as a project storage or a resource
	 * @return relative path from proof folder or project to this resource.
	 */
	public static IPath getProofFolderRelativePath(IAdaptable src) {
		IProject proj;
		IPath p;
		if (src instanceof IResource) {
			IResource resource = (IResource)src;
			proj = resource.getProject();
			p = resource.getProjectRelativePath();
		} else {
			IProjectStorage st = IProjectStorage.Adapter.adapt(src);
			if (st == null) throw new IllegalArgumentException("Not a proof storage: " + src);
			proj = st.getProject();
			p = st.getFullPath();
		}
		IPath base = proj.getProjectRelativePath();
		IContainer pf = getProofFolder(proj.getProject());
		if (pf != null) base = pf.getProjectRelativePath();
		IPath rpath = p.makeRelativeTo(base);
		return rpath;
	}

	public static boolean isProofFolder(Object x) {
		if (x instanceof IContainer) {
			IContainer f = (IContainer)x;
			IProject p = f.getProject();
			return f.equals(getProofFolder(p));
		}
		return false;
	}

	public static String makeDefaultBuildPath(String pfn) {
		return pfn;
	}

	public static IContainer getProofFolder(IProject project) {
		if (project == null || !project.exists() || !project.isOpen()) return null;
		String buildPath;
		try {
			buildPath = ProjectProperties.getBuildPath(project);
		} catch (CoreException e) {
			// Apparently not
			return null;
		}
		ensureBuilding(project,false);
		if (buildPath == null) return null;
		String[] pieces = buildPath.split(":");
		String proofFolderName = pieces[0];
		IContainer result;
		if (proofFolderName.isEmpty()) result = project;
		else result = project.getFolder(proofFolderName);
		// System.out.println("Proof folder is " + result);
		return result;
	}
}

class ProofBuilderProxy extends ProofBuilder {
	private final IProject project;
	ProofBuilderProxy(IProject p) {
		project = p;
	}
	
	ProofBuilder resolve() {
		ProofBuilder pb = ProofBuilder.getProofBuilder(project);
		if (pb instanceof ProofBuilderProxy) return null;
		return pb;
	}
	
	ProofBuilder resolveOrError() throws CoreException {
		ProofBuilder pb = resolve();
		if (pb == null) {
			throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, "No SASyLF Proof Builder created for " + project));
		}
		return pb;
	}
	
	@Override
	public ProjectModuleFinder getModuleFinder() {
		ProofBuilder pb = resolve();
		if (pb != null) return pb.getModuleFinder();
		// have to duplicate code here because getProject is final
		if (moduleFinder == null) {
			moduleFinder = new ProjectModuleFinder(project);
		}
		return moduleFinder;
	}
	
	@Override
	protected IProject[] build(int kind, Map<String, String> args,
			IProgressMonitor monitor) throws CoreException {
		return resolveOrError().build(kind,args,monitor);
	}
	
	@Override
	protected void fullBuild(IProgressMonitor monitor) throws CoreException {
		resolveOrError().fullBuild(monitor);
	}
	
	@Override
	protected void incrementalBuild(IResourceDelta delta,
			IProgressMonitor monitor) throws CoreException {
		resolveOrError().incrementalBuild(delta, monitor);
	}
	
	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		resolveOrError().clean(monitor);
	}

	@Override
	public void forceBuild(IResource res) {
		ProofBuilder pb = resolve();
		if (pb != null) {
			pb.forceBuild(res);
			return;
		}
		MessageDialog.openWarning(null, "SASyLF Proof Check", "This project's builder has not yet been set up.");
		super.forceBuild(res);
	}

	@Override
	public String toString() {
		return ("ProofBuilderProxy(" + project + ")");
	}

}
