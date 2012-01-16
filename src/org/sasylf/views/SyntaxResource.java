package org.sasylf.views;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

public class SyntaxResource implements ISyntaxItem {
	private SyntaxItemType type;
	private IResource resource;
	private String name;

	SyntaxResource(SyntaxItemType type, IResource resource) {
		this.type = type;
		this.resource = resource;
	}

	public static SyntaxResource loadFavorite(SyntaxItemType type,
			String info) {
		IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(
				new Path(info));
		if (res == null)
			return null;
		return new SyntaxResource(type, res);
	}

	public String getName() {
		if (name == null)
			name = resource.getName();
		return name;
	}

	public void setName(String newName) {
		name = newName;
	}

	public String getLocation() {
		IPath path = resource.getLocation().removeLastSegments(1);
		if (path.segmentCount() == 0)
			return "";
		return path.toString();
	}

	public boolean isFavoriteFor(Object obj) {
		return resource.equals(obj);
	}

	public SyntaxItemType getType() {
		return type;
	}

	public boolean equals(Object obj) {
		return this == obj
				|| ((obj instanceof SyntaxResource) && resource
						.equals(((SyntaxResource) obj).resource));
	}

	public int hashCode() {
		return resource.hashCode();
	}

	public Object getAdapter(Class adapter) {
		if (adapter.isInstance(resource))
			return resource;
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	public String getInfo() {
		return resource.getFullPath().toString();
	}

	public static ISyntaxItem loadSyntax(SyntaxItemType syntaxItemType,
			String info) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isSyntaxFor(Object obj) {
		// TODO Auto-generated method stub
		return false;
	}
}
