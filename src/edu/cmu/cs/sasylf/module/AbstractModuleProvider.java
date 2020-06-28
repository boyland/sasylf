package edu.cmu.cs.sasylf.module;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractModuleProvider implements ModuleProvider {
	private List<ModuleEventListener> listeners = new ArrayList<ModuleEventListener>();
	
	@Override
	public void addModuleEventListener(ModuleEventListener listener) {
		if (listener == null) throw new NullPointerException("listener should not be null!");
		listeners.add(listener);
	}

	/**
	 * Alert all observers that a module has changed.
	 * @param e the module event in question
	 */
	protected void fireModuleEvent(ModuleChangedEvent e) {
		// make copy of listeners while iterating over them
		// so that we don't run into CME's
		List<ModuleEventListener> copy = new ArrayList<ModuleEventListener>();
		copy.addAll(listeners);
		
		for (ModuleEventListener l : copy) {
			l.moduleChanged(e);
		}
	}
}
