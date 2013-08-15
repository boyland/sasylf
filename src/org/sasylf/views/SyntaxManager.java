package org.sasylf.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

public class SyntaxManager {
	private static SyntaxManager manager;
	private Collection<ISyntaxItem> Syntax;

	private SyntaxManager() {
	}

	public static SyntaxManager getManager() {
		if (manager == null)
			manager = new SyntaxManager();
		return manager;
	}

	public ISyntaxItem[] getSyntax() {
		if (Syntax == null)
			loadSyntax();
		return Syntax.toArray(new ISyntaxItem[Syntax.size()]);
	}

	private void loadSyntax() {
	      // temporary implementation
	      // to prepopulate list with projects
	      IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
	            .getProjects();
	      Syntax = new HashSet<ISyntaxItem>(projects.length);
	      for (int i = 0; i < projects.length; i++)
	         Syntax.add(new SyntaxResource(
	               SyntaxItemType.WORKBENCH_PROJECT, projects[i]));
	   }

	// The manager needs to look up existing Syntax objects and create new ones.

	public ISyntaxItem newSyntaxFor(Object obj) {
		SyntaxItemType[] types = SyntaxItemType.getTypes();
		for (int i = 0; i < types.length; i++) {
			ISyntaxItem item = types[i].newSyntax(obj);
			if (item != null)
				return item;
		}
		return null;
	}

	public ISyntaxItem[] newSyntaxFor(Iterator<?> iter) {
		if (iter == null)
			return ISyntaxItem.NONE;
		Collection<ISyntaxItem> items = new HashSet<ISyntaxItem>(20);
		while (iter.hasNext()) {
			ISyntaxItem item = newSyntaxFor(iter.next());
			if (item != null)
				items.add(item);
		}
		return items.toArray(new ISyntaxItem[items.size()]);
	}

	public ISyntaxItem[] newSyntaxFor(Object[] objects) {
		if (objects == null)
			return ISyntaxItem.NONE;
		return newSyntaxFor(Arrays.asList(objects).iterator());
	}

	public ISyntaxItem existingSyntaxFor(Object obj) {
		if (obj == null)
			return null;
		Iterator<ISyntaxItem> iter = Syntax.iterator();
		while (iter.hasNext()) {
			ISyntaxItem item = iter.next();
			if (item.isSyntaxFor(obj))
		          return item;
		}
		return null;
	}

	public ISyntaxItem[] existingSyntaxFor(Iterator<?> iter) {
		List<ISyntaxItem> result = new ArrayList<ISyntaxItem>(10);
		while (iter.hasNext()) {
			ISyntaxItem item = existingSyntaxFor(iter.next());

			if (item != null)
				result.add(item);
		}
		return result.toArray(new ISyntaxItem[result.size()]);
	}

	public void addSyntax(ISyntaxItem[] items) {
		if (Syntax == null)
			loadSyntax();
		if (Syntax.addAll(Arrays.asList(items)))
			fireSyntaxChanged(items, ISyntaxItem.NONE);
	}

	public void removeSyntax(ISyntaxItem[] items) {
		if (Syntax == null)
			loadSyntax();
		if (Syntax.removeAll(Arrays.asList(items)))
			fireSyntaxChanged(items, ISyntaxItem.NONE);
	}

	private List<SyntaxManagerListener> listeners = new ArrayList<SyntaxManagerListener>();

	public void addSyntaxManagerListener(SyntaxManagerListener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);
	}

	public void removeSyntaxManagerListener(SyntaxManagerListener listener) {
		listeners.remove(listener);
	}

	private void fireSyntaxChanged(ISyntaxItem[] itemsAdded,
			ISyntaxItem[] itemsRemoved) {
		SyntaxManagerEvent event = new SyntaxManagerEvent(this,
				itemsAdded, itemsRemoved);
		for (Iterator<SyntaxManagerListener> iter = listeners.iterator(); iter.hasNext();)
			iter.next().SyntaxChanged(event);
	}

}
