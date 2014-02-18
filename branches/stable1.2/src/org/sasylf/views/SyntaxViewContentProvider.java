package org.sasylf.views;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;

class SyntaxViewContentProvider implements IStructuredContentProvider,
		SyntaxManagerListener {
	private TableViewer viewer;
	private SyntaxManager manager;

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = (TableViewer) viewer;
		if (manager != null)
			manager.removeSyntaxManagerListener(this);
		manager = (SyntaxManager) newInput;
		if (manager != null)
			manager.addSyntaxManagerListener(this);
	}

	public void dispose() {
	}

	public Object[] getElements(Object parent) {
		return manager.getSyntax();
	}

	public void SyntaxChanged(SyntaxManagerEvent event) {
		viewer.getTable().setRedraw(false);
		try {
			viewer.remove(event.getItemsRemoved());
			viewer.add(event.getItemsAdded());
		} finally {
			viewer.getTable().setRedraw(true);
		}
	}

	@Override
	public void favoritesChanged(SyntaxManagerEvent event) {
		// TODO Auto-generated method stub
		
	}
}
