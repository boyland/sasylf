package org.sasylf.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.sasylf.Activator;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(PreferenceConstants.FORMATTER_INDENT_SIZE, 4);
		store.setDefault(PreferenceConstants.EDITOR_MATCHING_BRACKETS, true);
		store.setDefault(PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR, "127,0,85");
		store.setDefault(PreferenceConstants.PROOF_FOLDER_NAME, "slf");
		store.setDefault(PreferenceConstants.COMPULSORY_WHERE_CLAUSES, true);
		store.setDefault(PreferenceConstants.EXPERIMENTAL_FEATURES, "");
		initializeEditorPreferences(store);
	}
	
	public static void initializeEditorPreferences(IPreferenceStore store) {
		store.setDefault(PreferenceConstants.EDITOR_MATCHING_BRACKETS, true);
		store.setDefault(PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR, "127,0,85");		
	}

}
