package org.sasylf.util;

import java.io.InputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IEditorInput;
import org.sasylf.Activator;

/**
 * A class to adapt a SASyLF library file from Java resource to an Eclipse IStorage
 */
public class ResourceStorage extends PlatformObject implements IProjectStorage {

	private final String resourceString;
	private final IProject project;
	
	/**
	 * Create an IStorage for the given Java resource string
	 * @param resString resource string
	 * @param p project associated with this storage, may be null
	 */
	public ResourceStorage(String resString, IProject p) {
		this.resourceString = resString;
		project = p;
	}
	
	@Override
	public int hashCode() {
		int h = resourceString.hashCode();
		if (project == null) return h;
		else return h ^ project.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ResourceStorage)) return false;
		ResourceStorage rs = (ResourceStorage)obj;
		if (project == null && rs.project != null) return false;
		if (project != null && rs.project == null) return false;
		return resourceString.equals(rs.resourceString) && project.equals(rs.project);
	}

	@Override
	public String toString() {
		return project + "#" + resourceString;
	}

	/**
	 * Return resource string for this resource
	 * @return resource string
	 */
	public String getResourceString() {
		return resourceString;
	}
	
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (IEditorInput.class.equals(adapter)) {
			return adapter.cast(asEditorInput());
		}
		if (IProject.class.equals(adapter)) {
			return adapter.cast(project);
		}
		return super.getAdapter(adapter);
	}

	@Override
	public InputStream getContents() throws CoreException {
		final InputStream input = getClass().getResourceAsStream(resourceString);
		
		if (input != null) return input;
		
		throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Resource not found: " + resourceString));
	}

	@Override
	public IPath getFullPath() {
		return Path.forPosix(resourceString);
	}

	@Override
	public String getName() {
		return getFullPath().lastSegment();
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	/**
	 * Determine whether the named resource exists.
	 * @return whether it exists
	 */
	public boolean exists() {
		return getClass().getResource(resourceString) != null;
	}
	
	/**
	 * Return project that asked for creation of this storage.
	 * @return
	 */
	@Override
	public IProject getProject() {
		return project;
	}
	
	/**
	 * Create an editor input for this resource.
	 * @return a readonly editor input for this resource
	 */
	public IEditorInput asEditorInput() {
		return new ResourceEditorInput(this);
	}

	@Override
	public String getCharset() throws CoreException {
		return "UTF-8"; // specific to SASyLF
	}
}
