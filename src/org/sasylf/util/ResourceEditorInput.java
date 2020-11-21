package org.sasylf.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PlatformUI;

/**
 * Editor input connected with a resource
 */
public class ResourceEditorInput extends PlatformObject implements IEditorInput {
	private final ResourceStorage storage;
	
	public ResourceEditorInput(ResourceStorage st) {
		storage = st;
	}
	
	@Override
	public int hashCode() {
		return storage.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ResourceEditorInput)) return false;
		ResourceEditorInput other = (ResourceEditorInput)obj;
		return storage.equals(other.storage);
	}

	@Override
	public String toString() {
		return "ResourceEditorInput(" + storage + ")";
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter.isInstance(storage)) return adapter.cast(storage);
		return super.getAdapter(adapter);
	}

	@Override
	public boolean exists() {
		return storage.exists();
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		IEditorRegistry registry= PlatformUI.getWorkbench().getEditorRegistry();
		return registry.getImageDescriptor(storage.getFullPath().getFileExtension());
	}

	@Override
	public String getName() {
		return storage.getName();
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return storage.getResourceString();
	}
	
	/**
	 * Return the project associated with this editor input
	 * @return project associated with this editor input
	 */
	public IProject getProject() {
		return storage.getProject();
	}
}