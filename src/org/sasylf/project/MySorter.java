package org.sasylf.project;

import java.text.Collator;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

public class MySorter extends ViewerSorter {
	private final Collator collator;

	public MySorter() {
		// TODO Auto-generated constructor stub
		collator = Collator.getInstance();
	}

	public MySorter(Collator coll) {
		super(coll);
		if (coll == null) coll = Collator.getInstance();
		this.collator = coll;
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (e1 instanceof IPackageFragment && e2 instanceof IPackageFragment) {
			return collator.compare(e1.toString(), e2.toString());
		}
		return super.compare(viewer, e1, e2);
	}


}
