package org.sasylf;

import java.util.HashMap;
import java.util.Map;


/**
 * Class for plugin preferences.
 */
public class Options extends HashMap<String,String> {
  /**
   * serializable version
   */
  private static final long serialVersionUID = 1L;

  private Options() { }
  private Options(Options old) { super(old); }

  
  /**
   * Get SASyLF default options.
   * The map may be safely changed; each call gets a fresh map.
   * @return the default options for everyone.
   */
  public static Options getDefaultOptions() {
    Options result = new Options();
    result.initializeDefaults();
    return result;
  }
  
  private static volatile Options currentOptions;
  private static Options getMutableOptions() {
    if (currentOptions == null) {
      Options options = getDefaultOptions();
      synchronized (Options.class) {
        if (currentOptions == null) currentOptions = options;
      }
    }
    return currentOptions;  
  }
  
  /**
   * This returns a copy of the current options.
   * @return a copy of the current option settings.
   */
  public static Options getOptions() {
    return new Options(getMutableOptions());
  }
  
  /**
   * Get the current value of the given option key.
   * @param key key to observer
   * @return mapping, or null
   */
  public static String getOption(String key) {
    return getOptions().get(key);
  }
  
  /**
   * Return the integer value of an option, or if the option is not a
   * valid integer string, return the default value of the options as
   * as integer.  If the default is not a valid integer string, this method throws
   * an number format exception.
   * @param key
   * @throws NumberFormatException if the current option value is not a valid integer string,
   * nor is the default value.
   * @return
   */
  public static int getIntOption(String key) throws NumberFormatException {
      String key2 = key;
      String sval = Options.getOption(key2);
      boolean optionValid = false;
      int result = 42; // this value will never be seen
      try {
        if (sval != null) {
          result = Integer.parseInt(sval);
          optionValid = true;
        }
      } catch (NumberFormatException e) {
        // ignore
      }
      if (!optionValid) {
        sval = Options.getDefaultOptions().get(key2);
        result = Integer.parseInt(sval); // if fails, then caller chose a bad option.
        // Options.setOption(key2, sval);
      }
      return result;
  }
  
  /**
   * Get the value of the option after first checking the preferences map.
   * @param preferences preferences map to consult first.
   * @param key
   * @return value of option in the preferences, or in the current option map.
   */
  public static String getOption(Map<?,?> preferences, String key) {
    Object result = preferences.get(key);
    if (result instanceof String) return (String)result;
    return getOptions().get(key);
  }
  
  public static synchronized void setOption(String key, String value) {
    getOptions().put(key, value);
  }
  
  public static synchronized void merge(Map<?,?> preferences) {
    Options options = getOptions();
    for (Map.Entry<String, String> e : options.entrySet()) {
      Object x = preferences.get(e.getKey());
      if (x instanceof String) {
        e.setValue((String)x);
      }
    }
  }
  
  
  /// OPTIONS
  
  public static String FORMATTER_INDENT_SIZE = "org.sasylf.formatter.indent.size";
  
  private void initializeDefaults() {
    super.put(FORMATTER_INDENT_SIZE,"4");
    // .. etc.
  }
}
