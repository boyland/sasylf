package org.sasylf.project;

import java.util.Collection;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;

public class FolderPackageFragment extends PlatformObject implements IPackageFragment, IAdaptable {
	private String[] name;
	private IFolder folder;

	public FolderPackageFragment(IFolder f) {
		folder = f;
		IPath p = ProofBuilder.getProofFolderRelativePath(f);
		name = p.segments();
	}

	public FolderPackageFragment(String[] n, IFolder f) {
		name = n;
		folder = f;
	}

	@Override
	public boolean equals(Object x) {
		if (x == null) return false;
		if (!(x instanceof FolderPackageFragment)) return false;
		return folder.equals(((FolderPackageFragment)x).folder);
	}

	@Override
	public int hashCode() {
		return folder.hashCode();
	}

	@Override
	public String[] getName() {
		return name;
	}

	@Override
	public void getElements(Collection<IResource> into) {
		try {
			for (IResource child : folder.members()) {
				if (!(child instanceof IFolder)) {
					into.add(child);
				}
			}
		} catch (CoreException e) {
			return;
		}
	}

	@Override
	public boolean hasElements() {
		try {
			for (IResource child : folder.members()) {
				if (!(child instanceof IFolder)) {
					return true;
				}
			}
		} catch (CoreException ex) {
			// ignore
		}
		return false;
	}

	@Override
	public void getSubpackages(Collection<IPackageFragment> into) {
		into.add(this);
		try {
			for (IResource child : folder.members()) {
				if (child instanceof IFolder) {
					String[] newName = new String[name.length+1];
					System.arraycopy(name, 0, newName, 0, name.length);
					newName[name.length] = child.getName();
					new FolderPackageFragment(newName,(IFolder)child).getSubpackages(into);
				}
			}
		} catch (CoreException e) {
			return;
		}
	}

	@Override
	public boolean isInessential() {
		try {
			IResource[] members = folder.members();
			for (IResource child : members) {
				if (!(child instanceof IFolder)) {
					return false;
				}
			}
			return members.length > 0;
		} catch (CoreException ex) {
			// ignore
		}
		return false;
	}

	@Override
	public Object getBaseObject() {
		return folder;
	}

	@Override
	public IResource getParent() {
		IResource res = folder;
		for (int i=0; i < name.length; ++i) {
			res = res.getParent();
		}
		return res;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IFolder.class || adapter == IContainer.class || adapter == IResource.class)
			return adapter.cast(folder);
		return super.getAdapter(adapter);
	}

	@Override
	public String toString() {
		if (name.length == 0) return "(default package)";
		StringBuilder sb = new StringBuilder();
		for (int i=0; i < name.length; ++i) {
			if (i != 0) sb.append(".");
			sb.append(name[i]);
		}
		return sb.toString();
	}

}
