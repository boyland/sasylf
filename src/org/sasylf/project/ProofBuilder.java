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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.sasylf.Marker;
import org.sasylf.Proof;
import org.sasylf.util.Cell;

import edu.cmu.cs.sasylf.module.ModuleId;
import edu.cmu.cs.sasylf.module.ModuleProvider;

public class ProofBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = "org.sasylf.proofBuilder";

	/// Registration of the builder with the project
	// For some reason Eclipse doesn't make this information easy to get, 
	// so we do it ourselves:
	private static ConcurrentMap<IProject,Object> builders = new ConcurrentHashMap<IProject,Object>();

	@Override
	protected void startupOnInitialize() {
		// System.out.println("Registering proof builder for " + this.getProject());
		super.startupOnInitialize();
		Object previous = builders.put(this.getProject(), this);
		if (previous instanceof Cell<?>) {
			// System.out.println("Oops, people are waiting...");
			synchronized(previous) {
				@SuppressWarnings("unchecked")
				Cell<Boolean> bcell = (Cell<Boolean>)previous;
				bcell.set(true);
				bcell.notifyAll();
			}
			// System.out.println("Everyone should be notified: " + this.getProject());
		} else {
			// System.out.println("Making sure we have an initial build");
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
		if (builders.putIfAbsent(project, new Cell<Boolean>(false)) == null) {
			forceInitialBuild(project);
		}
		if (!doWait) return;
		Object x;
		while ((x = builders.get(project)) instanceof Cell<?>) {
			// System.out.println("still a cell?");
			@SuppressWarnings("unchecked")
			Cell<Boolean> bcell = (Cell<Boolean>)x;
			// System.out.println("Synching on " + bcell);
			synchronized (bcell) {
				while (!bcell.get()) {
					try {
						// System.out.println("waiting ...");
						bcell.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
						Thread.currentThread().interrupt();
						return;
					}
				}
			}
		}
	}

	/**
	 * @param project
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
		ProofBuilder pb = (ProofBuilder)builders.get(p);
		// System.out.println("pb = " + pb);
		return pb;
	}

	public void dispose() {
		builders.remove(this.getProject());
	}

	private ProjectModuleFinder moduleFinder;
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
					Proof.removeProof(resource);
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

	public static ModuleId getId(IResource res) {
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
	
	public IFile getResource(ModuleId id) {
		ProjectModuleFinder f = getModuleFinder();
		
		ModuleProvider provider = f.lookupModule(id);
		
		if (provider instanceof ProjectModuleProvider) {
			return ((ProjectModuleProvider) provider).getFileFromModuleId(id);			
		}
		
		return null;
	}

	/**
	 * Return the path from the path folder to this resource.
	 * If the resource is not in the proof folder, we instead
	 * return the relative path from the root of the project.
	 * @param res resource to return path for, must not be null
	 * @return relative path from proof folder or project to this resource.
	 */
	public static IPath getProofFolderRelativePath(IResource res) {
		IPath p = res.getProjectRelativePath();
		IPath base = res.getProject().getProjectRelativePath();
		IContainer pf = getProofFolder(res.getProject());
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
