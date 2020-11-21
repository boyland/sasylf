package org.sasylf.util;

import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;

/**
 * A storage that has a project associated with it.
 */
public interface IProjectStorage extends IEncodedStorage {
	/**
	 * Return the project associated with this storage.
	 * @return project associate with this storage
	 */
	public IProject getProject();

	public static class Adapter {
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
					if (file != null) result = IFileAdapter.create(file);
				}
			}
			return result;
		}
	}
}
