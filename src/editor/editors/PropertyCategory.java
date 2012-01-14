package editor.editors;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class PropertyCategory extends PropertyElement {

	private String name;
	private List<PropertyEntry> entries;

	public PropertyCategory(PropertyFile parent, LineNumberReader reader) throws IOException {
		super(parent);

		// Determine the category name from comments.
		while (true) {
			reader.mark(1);
			int ch = reader.read();
			if (ch == -1)
				break;
			reader.reset();
			if (ch != '#')
				break;
			String line = reader.readLine();
			if (name == null) {
				line = line.replace('#', ' ').trim();
				if (line.length() > 0)
					name = line;
			}
		}
		if (name == null)
			name = "";

		// Determine the properties in this category.
		entries = new ArrayList<PropertyEntry>();
		while (true) {
			reader.mark(1);
			int ch = reader.read();
			if (ch == -1)
				break;
			reader.reset();
			if (ch == '#')
				break;
			String line = reader.readLine();
			int index = line.indexOf('=');
			if (index != -1) {
				String key = line.substring(0, index).trim();
				String value = line.substring(index + 1).trim();
				entries.add(new PropertyEntry(this, key, value));
			}
		}
	}

	public String getName() {
		return name;
	}

	public Collection<PropertyEntry> getEntries() {
		return entries;
	}

	public PropertyElement[] getChildren() {
		return (PropertyElement[]) entries.toArray(new PropertyElement[entries.size()]);
	}

	public void setName(String text) {
		if (name.equals(text))
			return;
		name = text;
		((PropertyFile) getParent()).nameChanged(this);
	}

	public void addEntry(PropertyEntry entry) {
		if (!entries.contains(entry)) {
			entries.add(entry);
			((PropertyFile) getParent()).entryAdded(this, entry);
		}
	}

	public void removeEntry(PropertyEntry entry) {
		if (entries.remove(entry))
			((PropertyFile) getParent()).entryRemoved(this, entry);
	}

	public void removeFromParent() {
		((PropertyFile) getParent()).removeCategory(this);
	}

	public void keyChanged(PropertyEntry entry) {
		((PropertyFile) getParent()).keyChanged(this, entry);
	}

	public void valueChanged(PropertyEntry entry) {
		((PropertyFile) getParent()).valueChanged(this, entry);
	}

	public void appendText(PrintWriter writer) {
		if (name.length() > 0) {
			writer.print("# ");
			writer.println(name);
		}
		Iterator iter = entries.iterator();
		while (iter.hasNext())
			((PropertyEntry) iter.next()).appendText(writer);
	}

}
