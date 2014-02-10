package org.sasylf;

import org.eclipse.jface.preference.IPreferenceStore;
import org.sasylf.preferences.PreferenceConstants;

public class Preferences {

  public static IPreferenceStore get() {
    return Activator.getDefault().getPreferenceStore();
  }
  
  public static int getFormatterIndentSize() {
    IPreferenceStore store = get();
    if (store == null) {
      System.err.println("ERROR: preference store is null!");
      new RuntimeException("for trace").printStackTrace();
      return 4;
    }
    return store.getInt(PreferenceConstants.FORMATTER_INDENT_SIZE);
  }
  
  public static String getProofFolderName() {
    IPreferenceStore store = get();
    return store.getString(PreferenceConstants.PROOF_FOLDER_NAME);
  }
}
