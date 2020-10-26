package org.sasylf.util;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;

/**
 * An adapter class that lets us treat an IFile as a project storage object.
 */
public class IFileAdapter extends PlatformObject implements IProjectStorage {

	private final IFile file;
	
	/**
	 * Create an adapter for the given file
	 * @param f file to adapt, must not be null
	 */
	public IFileAdapter(IFile f) {
		file = f;
	}

	@Override
	public int hashCode() {
		return file.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof IFileAdapter)) return false;
		return file.equals(((IFileAdapter)obj).file);
	}

	@Override
	public String toString() {
		return "IFileAdapter(" + file + ")";
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter.isInstance(this)) return adapter.cast(this);
		if (IProject.class.equals(adapter)) return adapter.cast(file.getProject());
		if (adapter.isInstance(file)) return adapter.cast(file);
		return file.getAdapter(adapter);
	}

	@Override
	public InputStream getContents() throws CoreException {
		return file.getContents();
	}

	@Override
	public IPath getFullPath() {
		return file.getFullPath();
	}

	@Override
	public String getName() {
		return file.getName();
	}

	@Override
	public boolean isReadOnly() {
		return file.isReadOnly();
	}

	@Override
	public String getCharset() throws CoreException {
		return file.getCharset();
	}

	@Override
	public IProject getProject() {
		return file.getProject();
	}
	
	/** Create a wrapper for the file argument
	 * @param f file to create an adapter for
	 * @return an adapted file, must not be null
	 */
	public static IFileAdapter create(IFile f) { return new IFileAdapter(f); }
	
	/**
	 * Cast the argument as a project storage if possible or adapt as a file
	 * and then create a file adapter for it.
	 * @param obj object to test, may be null
	 * @return null if cannot adapt
	 */
	public static IProjectStorage adapt(Object obj) {
		IProjectStorage result = null;
		if (obj instanceof IAdaptable) {
			final IAdaptable pobj = (IAdaptable)obj;
			result = pobj.getAdapter(IProjectStorage.class);
			if (result == null) {
				final IFile file = pobj.getAdapter(IFile.class);
				if (file != null) result = create(file);
			}
		}
		return result;
	}
}
