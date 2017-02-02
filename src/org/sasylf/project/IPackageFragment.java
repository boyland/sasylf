package org.sasylf.project;

import java.util.Collection;

import org.eclipse.core.resources.IResource;

/**
 * Collection of resources in a specific package from a particular place.
 * @see FolderPackageFragment
 */
public interface IPackageFragment {

	/**
	 * Return the name of the package -- a sequence of strings.
	 * @return array of names, perhaps empty, never null.
	 */
	public String[] getName();

	/**
	 * Place all the contents of this package (not sub-packages) into the parameter
	 * @param into collection of resources in this package fragment; new elements added at end.
	 */
	public void getElements(Collection<IResource> into);

	/**
	 * Return true if this package has elements.
	 * @return true if there is at least one element.
	 */
	public boolean hasElements();

	/**
	 * Place this package and all packages nested in this one into the parameter
	 * @param into collection of package fragments; new elements added at end.
	 */
	public void getSubpackages(Collection<IPackageFragment> into);

	/**
	 * Return true if this package has sub folders but no elements.
	 * @return true if the package is normally not shown.
	 */
	public boolean isInessential();

	/**
	 * Get the base object for this package fragment, or null
	 * if no object underlying.
	 * @return
	 */
	public Object getBaseObject();

	/**
	 * Get the logical parent of this package fragment.
	 * @return logical parent, perhaps the project proof folder.
	 */
	public IResource getParent();
}
