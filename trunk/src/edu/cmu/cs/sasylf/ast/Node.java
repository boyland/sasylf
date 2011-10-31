package edu.cmu.cs.sasylf.ast;

import java.util.*;
import java.io.*;


public abstract class Node {
	public Node() {}
	public Node(Location l) { location = l; }

	public Location getLocation() { return location; }

	public abstract void prettyPrint(PrintWriter out);

	public String toString() {
		StringWriter sw = new StringWriter();
		prettyPrint(new PrintWriter(sw));
		return sw.toString();
	}

	private Location location;
}
