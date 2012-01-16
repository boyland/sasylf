package org.sasylf.editors;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class PropertiesEditorContentProvider implements ITreeContentProvider

{
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	public Object[] getElements(Object element) {
		return getChildren(element);
	}

	public Object[] getChildren(Object element) {
		if (element instanceof PropertyElement)
			return ((PropertyElement) element).getChildren();
		return null;
	}

	public Object getParent(Object element) {
		if (element instanceof PropertyElement)
			return ((PropertyElement) element).getParent();
		return null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof PropertyElement)
			return ((PropertyElement) element).getChildren().length > 0;
		return false;
	}

	public void dispose() {
	}

}
