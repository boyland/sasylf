package org.sasylf;

import org.eclipse.jface.preference.IPreferenceStore;
import org.sasylf.preferences.PreferenceConstants;

public class Preferences {

  public static IPreferenceStore get() {
    return Activator.getDefault().getPreferenceStore();
  }
  
  public static int getFormatterIndentSize() {
    return get().getInt(PreferenceConstants.FORMATTER_INDENT_SIZE);
  }
}
