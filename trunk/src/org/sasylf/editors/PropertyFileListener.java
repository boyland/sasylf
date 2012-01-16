package org.sasylf.editors;

public interface PropertyFileListener {

	void keyChanged(PropertyCategory category, PropertyEntry entry);

	void valueChanged(PropertyCategory category, PropertyEntry entry);

	void nameChanged(PropertyCategory category);

	void entryAdded(PropertyCategory category, PropertyEntry entry);

	void entryRemoved(PropertyCategory category, PropertyEntry entry);

	void categoryAdded(PropertyCategory category);

	void categoryRemoved(PropertyCategory category);

}
