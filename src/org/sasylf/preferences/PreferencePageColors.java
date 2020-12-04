package org.sasylf.preferences;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.sasylf.Activator;
import org.sasylf.editors.SASyLFColorProvider;

public class PreferencePageColors
extends FieldEditorPreferencePage
implements IWorkbenchPreferencePage {

	public PreferencePageColors() {
		super(GRID);
	}

	private Button darkButton;
	private ColorFieldEditor[] fieldEditors;
	private static final RGB[] darkColors = { // must be in same order as field editors
			SASyLFColorProvider.DARK_DEFAULT,
			SASyLFColorProvider.DARK_KEYWORD,
			SASyLFColorProvider.DARK_RULE,
			SASyLFColorProvider.DARK_BACKGROUND,
			SASyLFColorProvider.DARK_MULTI_LINE_COMMENT,
			SASyLFColorProvider.DARK_SINGLE_LINE_COMMENT
	};
	
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
		fieldEditors = new ColorFieldEditor[] {def,key,rule,bg,ml,sl};
	}

	public void performDark() {
		for (int i=0; i < fieldEditors.length; ++i) {
			fieldEditors[i].getColorSelector().setColorValue(darkColors[i]);
		}
	}
		
	@Override
	protected void contributeButtons(Composite buttonBar) {
		String label = JFaceResources.getString("Dark"); //$NON-NLS-1$
		darkButton = new Button(buttonBar, SWT.PUSH);
		darkButton.setText(label);
		Dialog.applyDialogFont(darkButton);
		Point minButtonSize = darkButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		data.widthHint = Math.max(widthHint, minButtonSize.x);
		darkButton.setLayoutData(data);
		darkButton.addSelectionListener(widgetSelectedAdapter(e -> performDark()));
		((GridLayout)buttonBar.getLayout()).numColumns++;
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("SASyLF Syntax Highlighting Colors (require reload to apply)");
	}
}