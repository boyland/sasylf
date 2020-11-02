package org.sasylf.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.text.Position;
import org.sasylf.util.PositionComparator;

public class ProofElement implements Comparable<ProofElement>, Cloneable {
	private String category;
	private String content;
	private String lexicalInfo;
	private ProofElement parentElement;
	private List<ProofElement> children;
	private Position position;

	public ProofElement(String category, String content) {
		this.category = category;
		this.content = content;
	}

	ProofElement(Position p) {
		category = "<none>";
		content = "<none>";
		lexicalInfo = "";
		this.position = p;
	}


	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getLexicalInfo() {
		return lexicalInfo;
	}

	public void setLexicalInfo(String info) {
		lexicalInfo = info;
	}

	public void addChild(ProofElement element) {
		if(this.children == null) {
			this.children = new ArrayList<ProofElement>();
		}
		this.children.add(element);
	}

	public List<ProofElement> getChildren() {
		return children;
	}

	public void setChildren(List<ProofElement> children) {
		this.children = children;
	}

	public ProofElement getParentElement() {
		return parentElement;
	}

	public void setParentElement(ProofElement parentElement) {
		this.parentElement = parentElement;
	}	

	public boolean hasChildren() {
		if(this.children == null || this.children.size() == 0) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return this.category + " " + this.content;
	}

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}



	@Override
	public int compareTo(ProofElement arg0) {
		return PositionComparator.getDefault().compare(position,arg0.position);
	}

	/**
	 * Return true if these proof elements are equal in all their contents,
	 * although not necessarily in their positions
	 * @param other another proof element, may be null (in which case this methods returns false)
	 * @return whether the two proof elements have the same structure.
	 */
	public boolean deepEquals(ProofElement other) {
		if (other == null) return false;
		if (Objects.equals(this.getCategory(),other.category) &&
				Objects.equals(this.content, other.content) &&
				Objects.equals(this.lexicalInfo, other.lexicalInfo)) {
			if (children == null) return other.children == null || other.children.size() == 0;
			if (other.children == null) return children.size() == 0;
			if (children.size() != other.children.size()) return false;
			Iterator<ProofElement> it1 = children.iterator();
			Iterator<ProofElement> it2 = other.children.iterator();
			while (it1.hasNext()) { // same length so only check needed
				ProofElement pe1 = it1.next();
				ProofElement pe2 = it2.next();
				if (pe1 == null && pe2 == null) continue;
				if (pe1 == null || pe2 == null) return false;
				if (!pe1.deepEquals(pe2)) return false;
			}
			return true;
		} else return false;
	}
	
	/**
	 * Return a deep copy of this proof element
	 */
	@Override
	public ProofElement clone() {
		ProofElement result;
		try {
			result = (ProofElement) super.clone();
		} catch (CloneNotSupportedException ex) {
			// Won't happen
			throw new AssertionError("We are cloneable!");
		}
		if (children == null) return result;
		result.children = new ArrayList<ProofElement>(children.size());
		for (ProofElement pe : children) {
			if (pe == null) result.children.add(null);
			else {
				final ProofElement pec = pe.clone();
				result.children.add(pec);
				pec.parentElement = result;
			}
		}
		return result;
	}
	
	/**
	 * Modify this proof element so that it is deep equal to the argument.
	 * It attempts to reuse as much structure as possible.
	 * @param newer argument to modify this proof element to, must not be null.
	 */
	public void updateTo(ProofElement newer) {
		this.category = newer.category;
		this.content = newer.content;
		this.lexicalInfo = newer.lexicalInfo;
		this.position = newer.position;
		if (newer.children == null || newer.children.size() == 0) {
			children = null;
			return;
		}
		List<ProofElement> oldChildren = this.children;
		if (oldChildren == null) oldChildren = Collections.emptyList();
		children = new ArrayList<ProofElement>(newer.children.size());
		for (ProofElement newCh : newer.children) {
			ProofElement oldCh = null;
			if (newCh != null) {
				for (ProofElement ch : oldChildren) {
					if (ch != null && ch.toString().equals(newCh.toString())) {
						oldCh = ch;
						break;
					}
				}
				if (oldCh == null) {
					oldCh = newCh.clone();
					oldCh.parentElement = this;
				} else {
					oldCh.updateTo(newCh);
				}
			}
			children.add(oldCh);
			oldChildren.remove(oldCh);
		}
	}
}
