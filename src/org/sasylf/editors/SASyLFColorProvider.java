package org.sasylf.editors;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.sasylf.Activator;
import org.sasylf.preferences.PreferenceConstants;

public class SASyLFColorProvider {

	static enum Fragments { 
		Default, Keyword,
		Background, Rule,
		MultiLineComment, SingleLineComment };

	public RGB MULTI_LINE_COMMENT;
	public RGB SINGLE_LINE_COMMENT;
	public RGB KEYWORD;
	public RGB DEFAULT;
	public RGB BACKGROUND;
	public RGB RULE;

	public static final RGB DEF_MULTI_LINE_COMMENT = new RGB(0, 128, 0);
	public static final RGB DEF_SINGLE_LINE_COMMENT = new RGB(0, 128, 0);
	public static final RGB DEF_KEYWORD = new RGB(0, 0, 128);
	public static final RGB DEF_DEFAULT = new RGB(0, 0, 0);
	public static final RGB DEF_BACKGROUND = new RGB(255, 255, 255);
	public static final RGB DEF_RULE = new RGB(127, 0, 85);

	protected Map<RGB, Color> _colorTable = new HashMap<RGB, Color>(10);

	public SASyLFColorProvider() {
		IPreferenceStore pStore = Activator.getDefault().getPreferenceStore();
		DEFAULT = PreferenceConverter.getColor(pStore, PreferenceConstants.PREF_COLOR_DEFAULT);
		KEYWORD = PreferenceConverter.getColor(pStore, PreferenceConstants.PREF_COLOR_KEYWORD);
		RULE    = PreferenceConverter.getColor(pStore, PreferenceConstants.PREF_COLOR_RULE);
		BACKGROUND = PreferenceConverter.getColor(pStore, PreferenceConstants.PREF_COLOR_BACKGROUND);
		MULTI_LINE_COMMENT  = PreferenceConverter.getColor(pStore, PreferenceConstants.PREF_COLOR_ML_COMMENT);
		SINGLE_LINE_COMMENT = PreferenceConverter.getColor(pStore, PreferenceConstants.PREF_COLOR_SL_COMMENT);
	}

	public void dispose(){
		Iterator<Color> e = _colorTable.values().iterator();
		while(e.hasNext()){
			e.next().dispose();
		}
	}

	public Color getColor(Fragments f){
		switch (f) {
		case Default: return getColor(DEFAULT);
		case Keyword: return getColor(KEYWORD);
		case Rule:    return getColor(RULE);
		case Background:        return getColor(BACKGROUND);
		case MultiLineComment:  return getColor(MULTI_LINE_COMMENT);
		case SingleLineComment: return getColor(SINGLE_LINE_COMMENT);
		default: throw new IllegalArgumentException("Unknown fragment to colorize: " + f);
		}
	}

	public Color getColor(RGB rgb){
		Color color = _colorTable.get(rgb);
		if(color==null){
			color = new Color(Display.getCurrent(),rgb);
			_colorTable.put(rgb, color);
		}
		return color;
	}

}
