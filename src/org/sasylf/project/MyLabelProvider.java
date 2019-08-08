package org.sasylf.project;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.ui.ProblemsLabelDecorator;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.sasylf.Activator;

public class MyLabelProvider extends LabelProvider {

	protected Image emptyPackageIcon;
	protected Image packageIcon;

	protected ILabelDecorator problemDecorator = new ProblemsLabelDecorator();

	@Override
	public void dispose() {
		// We don't dispose the main icons -- they are managed elsewhere.
		emptyPackageIcon = null;
		packageIcon = null;
		if (problemDecorator != null) {
			problemDecorator.dispose();
			problemDecorator = null;
		}
	}

	protected Image getEmptyPackageIcon() {
		if (emptyPackageIcon == null) {
			emptyPackageIcon = Activator.getDefault().getImage("icons/empty_pack_obj.png");
		}
		return emptyPackageIcon;
	}

	protected Image getPackageIcon() {
		if (packageIcon == null) {
			packageIcon = Activator.getDefault().getImage("icons/package_obj.png");
		}
		return packageIcon;
	}

	@Override
	public Image getImage(Object element) {
		Image result = null;
		Object baseObject = null;
		if (element instanceof IPackageFragment) {
			Collection<IResource> resources = new ArrayList<IResource>();
			IPackageFragment pack = (IPackageFragment)element;
			pack.getElements(resources);
			if (!pack.hasElements()) {
				result = getEmptyPackageIcon();
			} else {
				result = getPackageIcon();
			}
			baseObject = pack.getBaseObject();
		} else if (ProofBuilder.isProofFolder(element)) {
			result = Activator.getDefault().getImage("icons/packagefolder_obj.png");
			baseObject = element;
		}
		if (result != null && baseObject != null) {
			Image image = problemDecorator.decorateImage(result, baseObject);
			if (image != null) result = image;      
		}
		return result;
	}

	@Override
	public String getText(Object element) {
		String result = null;
		if (element instanceof IResource) {
			result = ((IResource)element).getName();
		} else if (element instanceof IPackageFragment) {
			IPackageFragment pack = (IPackageFragment)element;
			result = pack.toString();
		} else {
			result = element.toString();
		}
		return result;
	}
}
