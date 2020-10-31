package org.sasylf.util;

import java.util.Arrays;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * Automate the transfer of properties from one store to another.
 * Typically the first store is specific to this plugin and the other
 * is a preference store for another plugin.
 */
public class PreferenceTransfer implements IPropertyChangeListener {

	private final IPreferenceStore source;
	private final IPreferenceStore sink;
	private final String[] keys;
	
	public PreferenceTransfer(IPreferenceStore from, IPreferenceStore to, String[] ks) {
		source = from;
		sink = to;
		keys = ks;
		
	}
	
	@Override
	public int hashCode() {
		return source.hashCode() + (13 * sink.hashCode()) + (19*Arrays.hashCode(keys));
	}
	
	@Override
	public boolean equals(Object x) {
		if (!(x instanceof PreferenceTransfer)) return false;
		PreferenceTransfer t = (PreferenceTransfer)x;
		return source.equals(t.source) && sink.equals(t.sink) && Arrays.equals(keys, t.keys);
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		String key = event.getProperty();
		for (int i=0; i < keys.length; ++i) {
			if (keys[i].equals(key)) {
				sink.setValue(key, event.getNewValue().toString());
			}
		}
	}
	
	/**
	 * Copy all (non-null, non-default) preferences with the given keys from one store to another,
	 * and set up so that any later changes are transferred too.
	 * @param from source of values
	 * @param to destination of non-default values
	 * @param keys preferences to copy
	 */
	public static void copy(IPreferenceStore from, IPreferenceStore to, String... keys) {
		for (String k : keys) {
			final String defaultValue = to.getDefaultString(k);
			final String value = from.getString(k);
			if (value != defaultValue && (value != null && !value.equals(defaultValue))) {
				to.setValue(k, value);
			}
		}
		from.addPropertyChangeListener(new PreferenceTransfer(from,to,keys));
	}

}
