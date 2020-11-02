package org.sasylf.project;

import java.text.Collator;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

/**
 * XXX: This is supposed to sort packages in a SLF package view
 * alphabetically, but strangely puts "default package" in a random place.
 */
public class MySorter extends ViewerComparator {
	public MySorter() {
		super();
	}

	public MySorter(Collator coll) {
		super(coll);
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (e1 instanceof IPackageFragment && e2 instanceof IPackageFragment) {
			return super.compare(viewer, e1.toString(), e2.toString());
		}
		return super.compare(viewer, e1, e2);
	}


}
