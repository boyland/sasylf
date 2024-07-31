package org.sasylf.project;

import java.util.ArrayList;
import java.util.Collection;

import javax.swing.SwingUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.prefs.BackingStoreException;
import org.sasylf.Activator;
import org.sasylf.Preferences;

public class ProjectProperties {

	private static volatile ProjectProperties instance;

	private ProjectProperties() {  }

	public static ProjectProperties getInstance() {
		if (instance == null) {
			synchronized (ProjectProperties.class) {
				if (instance == null) {
					instance = new ProjectProperties();
				}
			}
		}
		return instance;
	}

	private Collection<IPropertyChangeListener> listeners = new ArrayList<IPropertyChangeListener>();

	public synchronized boolean addListener(IPropertyChangeListener l) {
		if (l == null) throw new NullPointerException("listener cannot be null");
		if (listeners.contains(l)) return false;
		return listeners.add(l);
	}

	public synchronized boolean removeListener(IPropertyChangeListener l) {
		return listeners.remove(l);
	}

	protected void informListeners(IProject project, String localName, Object oldValue, Object newValue) {
		final PropertyChangeEvent event = new PropertyChangeEvent(project,Activator.PLUGIN_ID+"."+localName,oldValue,newValue);
		final Collection<IPropertyChangeListener> listenersCopy;
		synchronized (this) {
			if (listeners.isEmpty()) return;
			listenersCopy = new ArrayList<IPropertyChangeListener>(listeners);
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				for (IPropertyChangeListener l : listenersCopy) {
					l.propertyChange(event);
				} 
			}
		});
	}

	private static final String PROJECT_BUILD_PATH_LOCAL_NAME = "project.buildPath";
	public static final String PROJECT_BUILD_PATH_FULL_NAME = Activator.PLUGIN_ID + "." + PROJECT_BUILD_PATH_LOCAL_NAME;

	/**
	 * Change the Build path for this project, and inform any listeners who may be listening.
	 * @param project project to apply change to, must not be null;
	 * @param buildPath build path, must not be null
	 * @throws CoreException if project isn't open or doesn't have SASyLF nature.
	 */
	public static void setBuildPath(IProject project, String buildPath)
			throws CoreException {
		if (buildPath == null) throw new NullPointerException("cannot set a null build path");
		IScopeContext scope = new ProjectScope(project);
		IEclipsePreferences node = scope.getNode(Activator.PLUGIN_ID);
		Object oldValue = node.get(PROJECT_BUILD_PATH_LOCAL_NAME, null);
		node.put(PROJECT_BUILD_PATH_LOCAL_NAME, buildPath);
		try {
			node.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
		getInstance().informListeners(project,PROJECT_BUILD_PATH_LOCAL_NAME,oldValue,buildPath);
	}

	/**
	 * @return
	 */
	public static String getDefaultBuildPath() {
		return ProofBuilder.makeDefaultBuildPath(Preferences.getProofFolderName());
	}

	/**
	 * Get the build path for the project.
	 * If it hasn't been set yet, set it to the default before returning.
	 * @param project to get SASyLF Build Path for, must not be null,
	 * and must have SASyLF Nature.
	 * @return build path, never null.
	 * @throws CoreException if the project is not open, or doesn't have nature
	 */
	public static String getBuildPath(final IProject project) throws CoreException {
		String buildPath;
		if (!project.hasNature(MyNature.NATURE_ID)) return null;
		IScopeContext scope = new ProjectScope(project);
		IEclipsePreferences node = scope.getNode(Activator.PLUGIN_ID);
		buildPath = node.get(PROJECT_BUILD_PATH_LOCAL_NAME, null);
		if (buildPath == null) {
			final String newBuildPath = getDefaultBuildPath();
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					try {
						setBuildPath(project,newBuildPath);
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			});
			buildPath = newBuildPath;
		}
		return buildPath;
	}
}
