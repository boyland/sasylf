package org.sasylf.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.IPipelinedTreeContentProvider2;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.navigator.PipelinedViewerUpdate;
import org.sasylf.Activator;
import org.sasylf.preferences.PreferenceConstants;

public class MyContentProvider implements ITreeContentProvider, IPipelinedTreeContentProvider2 {

	StructuredViewer viewer;

	Map<IProject,IFolder> proofFolders = new HashMap<IProject,IFolder>();

	private IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (event.getProperty().equals(PreferenceConstants.PROOF_FOLDER_NAME)) {
				Display.getDefault().asyncExec(() -> viewer.refresh());
			} else if (event.getProperty().equals(ProjectProperties.PROJECT_BUILD_PATH_FULL_NAME)) {
				Display.getDefault().asyncExec(() -> viewer.refresh(event.getSource()));
			}
		}
	};

	private IResourceChangeListener resourceChangeListener = new IResourceChangeListener() {
		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
				if (viewer != null) refreshDelta(event.getDelta());
			}
		}
	};


	public MyContentProvider() {
		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(propertyChangeListener);
		ProjectProperties.getInstance().addListener(propertyChangeListener);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener,IResourceChangeEvent.POST_CHANGE);
	}


	@Override
	public void dispose() {
		proofFolders.clear();
		Activator.getDefault().getPreferenceStore().removePropertyChangeListener(propertyChangeListener);
		ProjectProperties.getInstance().removeListener(propertyChangeListener);
		viewer = null;
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
	}

	@Override
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		if (v instanceof StructuredViewer) {
			viewer = (StructuredViewer)v;
		}
	}

	private static Object[] EMPTY_ARRAY = new Object[0];
	private static String[] EMPTY_NAME = new String[0];

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IPackageFragment) {
			Collection<IResource> files = new ArrayList<IResource>();
			((IPackageFragment)parentElement).getElements(files);
			return files.toArray();
		} else if (ProofBuilder.isProofFolder(parentElement)) {
			Collection<IPackageFragment> packs = new ArrayList<IPackageFragment>();
			new FolderPackageFragment(EMPTY_NAME,(IFolder)parentElement).getSubpackages(packs);
			for (Iterator<IPackageFragment> it = packs.iterator(); it.hasNext();) {
				if (it.next().isInessential()) {
					it.remove();
				}
			}
			return packs.toArray();
		}
		return EMPTY_ARRAY;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof IPackageFragment) {
			return ((IPackageFragment)element).getParent();
		} else if (element instanceof IResource) {
			IResource res = (IResource)element;
			IContainer proofFolder = ProofBuilder.getProofFolder(res.getProject());
			if (proofFolder != null) {
				if (element == proofFolder) return res.getProject();
				IResource parent = res.getParent();
				if (proofFolder.getProjectRelativePath().isPrefixOf(parent.getProjectRelativePath())) {
					return new FolderPackageFragment((IFolder) parent);
				}
			}
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		try {
			if (element instanceof IPackageFragment) {
				Collection<IResource> contents = new ArrayList<IResource>();
				((IPackageFragment)element).getElements(contents);
				return !contents.isEmpty();
			} else if (element instanceof IFolder) {
				IFolder f = (IFolder)element;
				return (f.members().length > 0);
			}
		} catch (CoreException e) {
			// ignore
		}
		return false;
	}

	@Override
	public void init(ICommonContentExtensionSite aConfig) {
		// TODO Auto-generated method stub

	}

	@Override
	public void restoreState(IMemento aMemento) {
		// TODO Auto-generated method stub

	}

	@Override
	public void saveState(IMemento aMemento) {
		// TODO Auto-generated method stub

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void getPipelinedChildren(Object aParent, Set theCurrentChildren) {
		// System.out.println("pipelining from " + theCurrentChildren + " of " + theCurrentChildren.getClass());
		Object[] children = getChildren(aParent);  
		// we only return EMPTY_ARRAY when we can't compute anything -- defer to current
		if (children == EMPTY_ARRAY) return;

		// now go to some effort to not get rid of everything and start from scratch.
		HashSet<Object> newSet = new HashSet<Object>();
		for (Object o : children) {
			newSet.add(o);
		}
		theCurrentChildren.retainAll(newSet);
		theCurrentChildren.addAll(newSet);
		// System.out.println("    to " + theCurrentChildren);
	}

	@Override
	public boolean hasPipelinedChildren(Object anInput, boolean currentHasChildren) {
		if (getChildren(anInput) == EMPTY_ARRAY) return currentHasChildren;
		return hasChildren(anInput);
	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	public void getPipelinedElements(Object anInput, Set theCurrentElements) {
		getPipelinedChildren(anInput, theCurrentElements);
	}

	@Override
	public Object getPipelinedParent(Object anObject, Object aSuggestedParent) {
		Object parent = getParent(anObject);
		if (parent == null) return aSuggestedParent;
		return aSuggestedParent;
	}

	@SuppressWarnings("unchecked")
	protected void convertShapeModification(PipelinedShapeModification mod) {
		Object parent = mod.getParent();
		Collection<Object> newChildren = new ArrayList<Object>();
		if (ProofBuilder.isProofFolder(parent)) { // must have been added directly to proof folder
			for (Iterator<?> chit = mod.getChildren().iterator(); chit.hasNext();) {
				Object child = chit.next();
				if (child instanceof IFolder) {
					// System.out.println("Converting folder child: " + child);
					IPackageFragment pf = new FolderPackageFragment((IFolder)child);
					chit.remove();
					newChildren.add(pf);
				} else if (child instanceof IFile) {
					// System.out.println("Discarding child: " + child);
					// the file should be in the default package, not here.
					// Unfortunately, we can't split the modification into two:
					// one for default package and one for the proof folder.
					// IPackageFragment defaultPackage = new FolderPackageFragment((IFolder)parent,(IFolder)parent);
					// mod.setParent(defaultPackage);
					chit.remove();
				}
			}
		} else if (parent instanceof IPackageFragment) {
			for (Iterator<?> chit = mod.getChildren().iterator(); chit.hasNext();) {
				Object child = chit.next();
				if (child instanceof IFolder) {
					// System.out.println("Discarding folder: " + child);
					// this should be a package at the top level.
					// As above, just drop it
					chit.remove();
				} 
			}
		}
		mod.getChildren().addAll(newChildren);
	}

	@Override
	public PipelinedShapeModification interceptAdd(
			PipelinedShapeModification anAddModification) {
		convertShapeModification(anAddModification);
		return anAddModification;
	}

	@Override
	public PipelinedShapeModification interceptRemove(
			PipelinedShapeModification aRemoveModification) {
		convertShapeModification(aRemoveModification);
		return aRemoveModification;
	}

	protected boolean convertRefreshSet(Set<Object> refreshSet) {
		Collection<Object> newElements = new ArrayList<Object>();
		for (Iterator<Object> it = refreshSet.iterator(); it.hasNext();) {
			Object obj = it.next();
			if (obj instanceof IFolder && !ProofBuilder.isProofFolder(obj)) {
				IFolder f = (IFolder)obj;
				IContainer pf = ProofBuilder.getProofFolder(f.getProject());
				if (pf != null && pf.getProjectRelativePath().isPrefixOf(f.getProjectRelativePath())) {
					// System.out.println("Converting refresh " + f);
					it.remove();
					newElements.add(new FolderPackageFragment(f));
				}
			}
		}
		refreshSet.addAll(newElements);
		return !newElements.isEmpty();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean interceptRefresh(PipelinedViewerUpdate aRefreshSynchronization) {
		return convertRefreshSet(aRefreshSynchronization.getRefreshTargets());
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean interceptUpdate(PipelinedViewerUpdate anUpdateSynchronization) {
		return convertRefreshSet(anUpdateSynchronization.getRefreshTargets());
	}

	private class ProofFolderDeltaVisitor implements IResourceDeltaVisitor {

		private final IContainer proofFolder;

		public ProofFolderDeltaVisitor(IContainer pf) {
			proofFolder = pf;
		}

		private boolean foundPackagesChange = false;

		private boolean foundFilesChange = false;

		private boolean visitChildrenOnly = false;

		private Collection<Object> refreshObjects = new ArrayList<Object>();

		public void doRefresh() {
			if (foundPackagesChange) {
				refreshObjects.clear();
				refreshObjects.add(proofFolder);
			}
			if (refreshObjects.isEmpty()) return;
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					// just doing the following misses packages that become
					// inessential or stop being inessential.  Doing this properly
					// would require keeping track of which packages are visible or not.
					/*for (Object x : refreshObjects) {
            viewer.refresh(x);
          }*/
					viewer.refresh(proofFolder);
				}
			});
		}

		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			if (foundPackagesChange) return false; // no point continuing
			if (visitChildrenOnly) {
				visitChildrenOnly = false;
				return true;
			}
			IResource res = delta.getResource();
			// System.out.println("Visiting " + res + " with " + delta.getKind());
			if (res instanceof IFolder) {
				IFolder f = (IFolder)res;
				if ((delta.getKind() & (IResourceDelta.ADDED|IResourceDelta.REMOVED)) != 0) {
					// System.out.println("  folder change found!");
					foundPackagesChange = true;
					return false;
				}
				boolean savedFilesChange = foundFilesChange;
				foundFilesChange = false;
				visitChildrenOnly = true;
				delta.accept(this);
				if (foundFilesChange && !foundPackagesChange) {
					// System.out.println("Found internal change to " + res);
					refreshObjects.add(new FolderPackageFragment(f));
				}
				foundFilesChange = savedFilesChange;
			} else {
				if ((delta.getKind() & (IResourceDelta.ADDED|IResourceDelta.REMOVED)) != 0) {
					// System.out.println("  file change found!");
					foundFilesChange = true;
				}
			}
			return false;
		}

	}

	public void refreshDelta(IResourceDelta d) {
		IResource res = d.getResource();
		if (res instanceof IWorkspaceRoot) {
			IResourceDelta[] projectDeltas = d.getAffectedChildren(IResourceDelta.CHANGED);
			for (IResourceDelta pd : projectDeltas) {
				res = pd.getResource();
				if (res instanceof IProject) {
					IProject project = (IProject)res;
					// System.out.println("Saw change " +pd.getFlags() + " on " + project);
					try {
						// projects opening or closing are handled already
						if (project.isOpen() && (pd.getFlags()&IResourceDelta.OPEN) == 0 &&
								project.hasNature(MyNature.NATURE_ID)) {
							IContainer pf = ProofBuilder.getProofFolder(project);
							IResourceDelta pfd = pd.findMember(pf.getProjectRelativePath());
							if (pfd != null) {
								// System.out.println("Found change on PF " + pf);
								ProofFolderDeltaVisitor visitor = new ProofFolderDeltaVisitor(pf);
								pfd.accept(visitor);
								visitor.doRefresh();
							}
						}
					} catch (CoreException e) {
						// Apparently not;
					}
				} else System.out.println("Child of workspace not a project? " + res);
			}
		} else System.out.println("not a workspace?: " + res + " of " + res.getClass());
	}

}
