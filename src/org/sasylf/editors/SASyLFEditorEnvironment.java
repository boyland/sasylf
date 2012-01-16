package org.sasylf.editors;

import org.eclipse.jface.text.rules.RuleBasedScanner;


public class SASyLFEditorEnvironment {
	private static SASyLFColorProvider _colorProvider;
    private static SASyLFCodeScanner _codeScanner;
    
    public static SASyLFColorProvider getSASyLFColorProvider () {
        if (_colorProvider == null) {
            _colorProvider = new SASyLFColorProvider ();
        }
        return _colorProvider;  
    }
    
    public static RuleBasedScanner getSASyLFCodeScanner () {
        if (_codeScanner == null) {
            _codeScanner = new SASyLFCodeScanner (getSASyLFColorProvider ());
        }
        return _codeScanner;
    }
}
