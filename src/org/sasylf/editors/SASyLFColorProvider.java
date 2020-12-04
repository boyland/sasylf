package org.sasylf.editors;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.sasylf.Activator;
import org.sasylf.preferences.PreferenceConstants;

public class SASyLFColorProvider {

	/**
	 * An enumeration of the classes of tokens
	 * so that (foreground) colors can be assigned for them.
	 */
	public static enum TokenColorClass {
		Default(PreferenceConstants.PREF_COLOR_DEFAULT), 
		Keyword(PreferenceConstants.PREF_COLOR_KEYWORD),
		Background(PreferenceConstants.PREF_COLOR_BACKGROUND), 
		Rule(PreferenceConstants.PREF_COLOR_RULE),
		MultiLineComment(PreferenceConstants.PREF_COLOR_ML_COMMENT), 
		SingleLineComment(PreferenceConstants.PREF_COLOR_SL_COMMENT);
		private final String prefName;
		private TokenColorClass (String s) {
			prefName = s;
		}
		public String getPreferenceName() {
			return prefName;
		}
	};

	public static final RGB DEF_MULTI_LINE_COMMENT = new RGB(0, 128, 0);
	public static final RGB DEF_SINGLE_LINE_COMMENT = new RGB(0, 128, 0);
	public static final RGB DEF_KEYWORD = new RGB(0, 0, 128);
	public static final RGB DEF_DEFAULT = new RGB(0, 0, 0);
	public static final RGB DEF_BACKGROUND = new RGB(255, 255, 255);
	public static final RGB DEF_RULE = new RGB(127, 0, 85);

	public static final RGB DARK_MULTI_LINE_COMMENT = new RGB(95,175,95);
	public static final RGB DARK_SINGLE_LINE_COMMENT = new RGB(95,175,95);
	public static final RGB DARK_KEYWORD = new RGB(127,175,225);
	public static final RGB DARK_DEFAULT = new RGB(222,222,222);
	public static final RGB DARK_BACKGROUND = new RGB(33,33,33);
	public static final RGB DARK_RULE = new RGB(175,95,127);
	
	protected Map<RGB, Color> colorTable = new HashMap<RGB, Color>(10);

	public void dispose(){
		Iterator<Color> e = colorTable.values().iterator();
		while(e.hasNext()){
			e.next().dispose();
		}
	}

	public Color getColor(RGB rgb){
		Color color = colorTable.get(rgb);
		if(color==null){
			color = new Color(Display.getCurrent(),rgb);
			colorTable.put(rgb, color);
		}
		return color;
	}

	/**
	 * Create a token with the given text attribute as its data, to be modified
	 * by the given foreground color class.
	 * @param attr text attribute (or null) to modify with the foreground color 
	 * @param fgClass the class of token to be used to set foreground color according to preferences
	 * @return new token with then given text attribute and foreground color for this class of tokens
	 */
	public Token createToken(TextAttribute attr, TokenColorClass fgClass) {
		TextAttribute modified = makeTextAttribute(attr, fgClass);
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		Token result = new Token(modified);
		store.addPropertyChangeListener((pce) -> updateToken(pce, result, attr, fgClass));
		return result;
	}

	private void updateToken(PropertyChangeEvent pce, Token token, TextAttribute attr, TokenColorClass fgClass) {
		if (pce.getProperty().equals(fgClass.getPreferenceName())) {
			Display.getDefault().asyncExec(() -> token.setData(makeTextAttribute(attr, fgClass)));
		}
	}
	
	/**
	 * Create a text attribute in which a base attribute is modified
	 * with the foreground for the token class specified
	 * @param attr attribute to modify, or null if no defaults
	 * @param fgClass class of token to get foreground for
	 * @return new text attribute with foreground color preference for given token class
	 */
	protected TextAttribute makeTextAttribute(TextAttribute attr,
			TokenColorClass fgClass) {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		RGB rgb = PreferenceConverter.getColor(store, fgClass.getPreferenceName());
		Color c= getColor(rgb);
		if (attr == null) return new TextAttribute(c);
		else return new TextAttribute(c,attr.getBackground(),attr.getStyle(),attr.getFont());
	}
}
