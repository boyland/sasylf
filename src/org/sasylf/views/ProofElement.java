package org.sasylf.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.Position;
import org.sasylf.util.PositionComparator;

public class ProofElement implements Comparable<ProofElement> {
	private String category;
	private String content;
	private String lexicalInfo;
	private ProofElement parentElement;
	private List<ProofElement> children;
	private int start;
	private int end;
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

	public String toString() {
		return this.category + " " + this.content;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
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
	
	
}
