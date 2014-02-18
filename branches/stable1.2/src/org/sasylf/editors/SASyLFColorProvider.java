package org.sasylf.editors;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class SASyLFColorProvider {
	
	public static final RGB MULTI_LINE_COMMENT = new RGB(0, 128, 0);
	public static final RGB SINGLE_LINE_COMMENT = new RGB(0, 128, 0);
	public static final RGB KEYWORD = new RGB(0, 0, 128);
	public static final RGB DEFAULT = new RGB(0, 0, 0);
	public static final RGB BACKGROUND = new RGB(255, 255, 255);
	public static final RGB RULE = new RGB(127, 0, 85);
	
	protected Map<RGB, Color> _colorTable = new HashMap<RGB, Color>(10);
	
	public void dispose(){
		Iterator<Color> e = _colorTable.values().iterator();
		while(e.hasNext()){
			e.next().dispose();
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
