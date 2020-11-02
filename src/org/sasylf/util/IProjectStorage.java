package org.sasylf.util;

import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IProject;

/**
 * A storage that has a project associated with it.
 */
public interface IProjectStorage extends IEncodedStorage {
	/**
	 * Return the project associated with this storage.
	 * @return project associate with this storage
	 */
	public IProject getProject();
}
