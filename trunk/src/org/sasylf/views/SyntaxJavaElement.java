package org.sasylf.views;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

public class SyntaxJavaElement implements ISyntaxItem {
	private SyntaxItemType type;
	private IJavaElement element;
	private String name;

	public SyntaxJavaElement(SyntaxItemType type, IJavaElement element) {
		this.type = type;
		this.element = element;
	}

	public static SyntaxJavaElement loadFavorite(SyntaxItemType type,
			String info) {
		IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(
				new Path(info));
		if (res == null)
			return null;
		IJavaElement elem = JavaCore.create(res);
		if (elem == null)
			return null;
		return new SyntaxJavaElement(type, elem);
	}

	public String getName() {
		if (name == null)
			name = element.getElementName();
		return name;
	}

	public void setName(String newName) {
		name = newName;
	}

	public String getLocation() {
		IResource res = element.getUnderlyingResource();
		if (res != null) {
			IPath path = res.getLocation().removeLastSegments(1);
			if (path.segmentCount() == 0)
				return "";
			return path.toString();
		}
		return "";
	}


	public SyntaxItemType getType() {
		return type;
	}

	public boolean equals(Object obj) {
		return this == obj
				|| ((obj instanceof SyntaxJavaElement) && element
						.equals(((SyntaxJavaElement) obj).element));
	}

	public int hashCode() {
		return element.hashCode();
	}

	public Object getAdapter(Class adapter) {
		if (adapter.isInstance(element))
			return element;
		IResource resource = element.getUnderlyingResource();
		if (adapter.isInstance(resource))
			return resource;
		Class adapterType = null;
		Object adaptable;
		return Platform.getAdapterManager().getAdapter(this, adapter);

	}

	public String getInfo(IResource resource) {
	      return resource.getFullPath().toString();
	   }


	@Override
	public boolean isSyntaxFor(Object obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	
	
}
