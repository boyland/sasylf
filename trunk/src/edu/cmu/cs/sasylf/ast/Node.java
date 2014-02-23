package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.io.StringWriter;


public abstract class Node {
	public Node() {}
	public Node(Location l) { 
	  location = l; 
	  endLocation = l;
	}
	public Node(Location l1, Location l2) {
	  location = l1;
	  endLocation = l2;
	}

	public Location getLocation() { return location; }
	public Location getEndLocation() { return endLocation; }
	
	public abstract void prettyPrint(PrintWriter out);

	public String toString() {
		StringWriter sw = new StringWriter();
		prettyPrint(new PrintWriter(sw));
		return sw.toString();
	}

	private Location location;
	private Location endLocation;
	
	protected void setLocation(Location l) {
	  location = l;
	  if (endLocation == null) endLocation = l;
	}
	
	public void setEndLocation(Location l) {
	  endLocation = l;
	}
}
