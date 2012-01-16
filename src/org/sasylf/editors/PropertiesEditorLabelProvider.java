package org.sasylf.editors;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;

public class PropertiesEditorLabelProvider extends LabelProvider implements
		ITableLabelProvider {
	public org.eclipse.swt.graphics.Image getColumnImage(Object element,
			int columnIndex) {
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof PropertyCategory) {
			PropertyCategory category = (PropertyCategory) element;
			switch (columnIndex) {
			case 0:
				return category.getName();
			case 1:
				return "";
			}
		}
		if (element instanceof PropertyEntry) {
			PropertyEntry entry = (PropertyEntry) element;
			switch (columnIndex) {
			case 0:
				return entry.getKey();
			case 1:
				return entry.getValue();
			}
		}

		if (element == null)
			return "<null>";
		return element.toString();
	}

	
}
