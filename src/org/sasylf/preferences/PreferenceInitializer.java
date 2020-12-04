package org.sasylf.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.sasylf.Activator;
import org.sasylf.editors.SASyLFColorProvider;

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
		initializeColorPreferences(store);
	}
	
	public static void initializeEditorPreferences(IPreferenceStore store) {
		store.setDefault(PreferenceConstants.EDITOR_MATCHING_BRACKETS, true);
		store.setDefault(PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR, "127,0,85");		
	}

	public static void initializeColorPreferences(IPreferenceStore pStore) {
		PreferenceConverter.setDefault(pStore, PreferenceConstants.PREF_COLOR_DEFAULT, SASyLFColorProvider.DEF_DEFAULT);
		PreferenceConverter.setDefault(pStore, PreferenceConstants.PREF_COLOR_KEYWORD, SASyLFColorProvider.DEF_KEYWORD);
		PreferenceConverter.setDefault(pStore, PreferenceConstants.PREF_COLOR_RULE, SASyLFColorProvider.DEF_RULE);
		PreferenceConverter.setDefault(pStore, PreferenceConstants.PREF_COLOR_BACKGROUND, SASyLFColorProvider.DEF_BACKGROUND);
		PreferenceConverter.setDefault(pStore, PreferenceConstants.PREF_COLOR_ML_COMMENT, SASyLFColorProvider.DEF_MULTI_LINE_COMMENT);
		PreferenceConverter.setDefault(pStore, PreferenceConstants.PREF_COLOR_SL_COMMENT, SASyLFColorProvider.DEF_SINGLE_LINE_COMMENT);
	}
}
