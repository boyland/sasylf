package org.sasylf;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import edu.cmu.cs.sasylf.parser.DSLToolkitParser;
import edu.cmu.cs.sasylf.util.TaskReport;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.sasylf";

	// The shared instance
	private static Activator plugin;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	protected void performInitializations() {
		DSLToolkitParser.addListener(TaskReport.commentListener);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		System.out.println("SASyLF plugin activated.");
		plugin = this;
		performInitializations();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		DSLToolkitParser.remListener(TaskReport.commentListener);
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		if (plugin == null) {
			// Eclipse documentation claims that that "start" method
			// will be called before any code in the plugin, but this
			// is evidently not true.
			System.out.println("SASyLF Plugin activated manually.");
			plugin = new Activator();
			plugin.performInitializations();
		}
		return plugin;
	}

	/**
	 * Return image from plugin.
	 * @param path
	 * @return
	 * @author VonC @ stackoverflow
	 */
	public Image getImage(String path) {
		Image image = Activator.getDefault().getImageRegistry().get(path);
		if (image == null) {
			getImageRegistry().put(path, AbstractUIPlugin.
					imageDescriptorFromPlugin(PLUGIN_ID, path));
			image = getImageRegistry().get(path);
		}
		return image;
	}

	public ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
