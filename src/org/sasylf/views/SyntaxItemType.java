package org.sasylf.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public abstract class SyntaxItemType implements Comparable<SyntaxItemType> {
	private static final ISharedImages PLATFORM_IMAGES = PlatformUI
			.getWorkbench().getSharedImages();

	public static SyntaxItemType WORKBENCH_PROJECT;

	private static SyntaxItemType JAVA_PACKAGE;

	private static SyntaxItemType JAVA_PROJECT;

	private static SyntaxItemType JAVA_PACKAGE_ROOT;

	private static SyntaxItemType JAVA_CLASS_FILE;

	private static SyntaxItemType JAVA_COMP_UNIT;

	private static SyntaxItemType JAVA_CLASS;

	private static SyntaxItemType JAVA_INTERFACE;

	private final String id;
	private final String printName;
	private final int ordinal;

	private SyntaxItemType(String id, String name, int position) {
		this.id = id;
		this.ordinal = position;
		this.printName = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return printName;
	}

	public abstract Image getImage();

	public abstract ISyntaxItem newSyntax(Object obj);

	public abstract ISyntaxItem loadSyntax(String info);

	public int compareTo(SyntaxItemType arg) {
		return this.ordinal - arg.ordinal;
	}

	public static final SyntaxItemType UNKNOWN = new SyntaxItemType(
			"Unknown", "Unknown", 0) {
		public Image getImage() {
			return null;
		}

		public ISyntaxItem newSyntax(Object obj) {
			return null;
		}

		public ISyntaxItem loadSyntax(String info) {
			return null;
		}
	};

	public static final SyntaxItemType WORKBENCH_FILE = new SyntaxItemType(
			"WBFile", "Workbench File", 1) {
		public Image getImage() {
			return PLATFORM_IMAGES
					.getImage(org.eclipse.ui.ISharedImages.IMG_OBJ_FILE);
		}

		public ISyntaxItem newSyntax(Object obj) {
			if (!(obj instanceof IFile))
				return null;
			return new SyntaxResource(this, (IFile) obj);
		}

		public ISyntaxItem loadSyntax(String info) {
			return SyntaxResource.loadSyntax(this, info);
		}
	};

	public static final SyntaxItemType WORKBENCH_FOLDER = new SyntaxItemType(
			"WBFolder", "Workbench Folder", 2) {
		public Image getImage() {
			return PLATFORM_IMAGES
					.getImage(org.eclipse.ui.ISharedImages.IMG_OBJ_FOLDER);
		}

		public ISyntaxItem newSyntax(Object obj) {
			if (!(obj instanceof IFolder))
				return null;
			return new SyntaxResource(this, (IFolder) obj);
		}

		public ISyntaxItem loadSyntax(String info) {
			return SyntaxResource.loadSyntax(this, info);
		}
	};
	
	
	private static final SyntaxItemType[] TYPES = {
		   UNKNOWN, WORKBENCH_FILE, WORKBENCH_FOLDER, WORKBENCH_PROJECT,
		   JAVA_PROJECT, JAVA_PACKAGE_ROOT, JAVA_PACKAGE,
		   JAVA_CLASS_FILE, JAVA_COMP_UNIT, JAVA_INTERFACE, JAVA_CLASS};

		public static SyntaxItemType[] getTypes() {
		   return TYPES;
		}



}
