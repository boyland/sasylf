package org.sasylf.project;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
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
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.statushandlers.StatusManager;
import org.sasylf.Marker;
import org.sasylf.ProofChecker;
import org.sasylf.util.Cell;

import edu.cmu.cs.sasylf.ast.CompUnit;

public class ProofBuilder extends IncrementalProjectBuilder {

  public static final String BUILDER_ID = "org.sasylf.proofBuilder";
  
  /// Registration of the builder with the project
  // For some reason Eclipse doesn't make this information easy to get, 
  // so we do it ourselves:
  private static Map<IProject,ProofBuilder> builders = new HashMap<IProject,ProofBuilder>();
  
  @Override
  protected void startupOnInitialize() {
    super.startupOnInitialize();
    builders.put(this.getProject(), this);
  }

  public static void ensureBuilt(final IProject project) {
    if (builders.get(project) == null) {
      final Cell<Boolean> lock = new Cell<Boolean>(false);
      synchronized (lock) {
        Job initialBuild = new Job("initial SASyLF build for " + project) {
          @Override
          protected IStatus run(IProgressMonitor monitor) {
            synchronized (lock) {
              lock.set(true);
              lock.notify();
            }
            System.out.println("initial build for " + project + " starting...");
            try {
              project.build(FULL_BUILD, monitor);
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
        while (!lock.get()) {
          try {
            lock.wait();
          } catch (InterruptedException e) {
            StatusManager.getManager().handle(Status.CANCEL_STATUS);
            return;
          }
        }
      }
    }
  }
  
  public static ProofBuilder getProofBuilder(IProject p) {
    ensureBuilt(p);
    ProofBuilder pb = builders.get(p);
    if (pb == null) {
      System.out.println("No proof builder for " + p);
    }
    return pb;
  }
  
  public void dispose() {
    builtModules.clear();
    builders.remove(this.getProject());
  }
  
  private final Map<IFile,CompUnit> builtModules = new HashMap<IFile,CompUnit>();
  
  public static CompUnit getCompUnit(IFile f) {
    ProofBuilder pb = getProofBuilder(f.getProject());
    if (pb == null) return null;
    return pb.builtModules.get(f);
  }
  
  /*
   * TODO:
   * Define a per-project dictionary
   * Each dictionary should look up a module name to a CompUnit.
   * The Outline view should get its information in this way rather that from the editor.
   * The dictionary should have an order so that we see things in order. 
   */
  
	class ProofDeltaVisitor implements IResourceDeltaVisitor {
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
		 */
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			// System.out.println("incremental for " + resource);
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				// handle added resource
				checkProof(resource);
				break;
			case IResourceDelta.REMOVED:
				// handle removed resource
			  builtModules.remove(resource);
				break;
			case IResourceDelta.CHANGED:
				// handle changed resource
        builtModules.remove(resource);
				checkProof(resource);
				break;
			}
			//return true to continue visiting children.
			return true;
		}
	}

	class ProofResourceVisitor implements IResourceVisitor {
		public boolean visit(IResource resource) {
			checkProof(resource);
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

	void checkProof(IResource resource) {
		if (resource instanceof IFile && resource.getName().endsWith(".slf")) {
			CompUnit cu = ProofChecker.analyzeSlf(resource);
			if (cu != null) {
			  builtModules.put((IFile)resource,cu);
			}
		}
	}


	protected void fullBuild(final IProgressMonitor monitor)
			throws CoreException {
    System.out.println("Doing full build");
		try {
			getProject().accept(new ProofResourceVisitor());
		} catch (CoreException e) {
		}
	}


	protected void incrementalBuild(IResourceDelta delta,
			IProgressMonitor monitor) throws CoreException {
		// the visitor does the work.
		delta.accept(new ProofDeltaVisitor());
	}

	@Override
  protected void clean(IProgressMonitor monitor) throws CoreException {
	  super.clean(null);
	  if (monitor == null) monitor = new NullProgressMonitor();
	  monitor.beginTask("clean", 100);
    getProject().deleteMarkers(Marker.MARKER_ID, true, IResource.DEPTH_INFINITE);
    monitor.worked(90);
    builtModules.clear();
    monitor.worked(10);
    monitor.done();
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
      IContainer f = (IFolder)x;
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
    ensureBuilt(project);
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
