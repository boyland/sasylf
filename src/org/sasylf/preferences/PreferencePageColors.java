package org.sasylf.preferences;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.sasylf.Activator;

public class PreferencePageColors
extends FieldEditorPreferencePage
implements IWorkbenchPreferencePage {

	public PreferencePageColors() {
		super(GRID);
	}

	@Override
	public void createFieldEditors() {
		ColorFieldEditor def = new ColorFieldEditor(PreferenceConstants.PREF_COLOR_DEFAULT, "Default text", getFieldEditorParent());
		addField(def);
		ColorFieldEditor key = new ColorFieldEditor(PreferenceConstants.PREF_COLOR_KEYWORD, "Keyword text", getFieldEditorParent());
		addField(key);
		ColorFieldEditor rule = new ColorFieldEditor(PreferenceConstants.PREF_COLOR_RULE, "Rule text", getFieldEditorParent());
		addField(rule);
		ColorFieldEditor bg = new ColorFieldEditor(PreferenceConstants.PREF_COLOR_BACKGROUND, "Background", getFieldEditorParent());
		addField(bg);
		ColorFieldEditor ml = new ColorFieldEditor(PreferenceConstants.PREF_COLOR_ML_COMMENT, "Multi-line comments", getFieldEditorParent());
		addField(ml);
		ColorFieldEditor sl = new ColorFieldEditor(PreferenceConstants.PREF_COLOR_SL_COMMENT, "Single-line comments", getFieldEditorParent());
		addField(sl);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("SASyLF Syntax Highlighting Colors (require reload to apply)");
	}
}