package org.sasylf.editors;

import java.io.PrintWriter;

public class PropertyEntry extends PropertyElement {

	String key;
	String value;

	public PropertyEntry(PropertyCategory parent, String key, String value) {
		super(parent);
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public PropertyElement[] getChildren() {
		return NO_CHILDREN;
	}

	public void setKey(String text) {
		if (key.equals(text))
			return;
		key = text;
		((PropertyCategory) getParent()).keyChanged(this);
	}

	public void setValue(String text) {
		if (value.equals(text))
			return;
		value = text;
		((PropertyCategory) getParent()).valueChanged(this);
	}

	public void removeFromParent() {
		((PropertyCategory) getParent()).removeEntry(this);
	}

	public void appendText(PrintWriter writer) {
		writer.print(key);
		writer.print(" = ");
		writer.println(value);
	}

}
