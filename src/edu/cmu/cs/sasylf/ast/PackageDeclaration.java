package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;

public class PackageDeclaration extends Node {
	private String[] pieces;

	public PackageDeclaration(Location l, List<String> p, Location endL) {
		super(l,endL);
		pieces = p.toArray(new String[p.size()]);
	}

	public void typecheck(String[] expected) {
		if (Arrays.equals(pieces, expected)) return;
		if (pieces.length == 0) {
			ErrorHandler.warning(Errors.WRONG_PACKAGE, this, "\npackage " + toString(expected) + ";");
		} else if (expected.length == 0) {
			ErrorHandler.warning(Errors.WRONG_PACKAGE, this, "package " + toString(pieces) + ";\n");
		} else {
			ErrorHandler.warning(Errors.WRONG_PACKAGE, this,  toString(pieces) + "\n" + toString(expected)); 
		}
	}

	@Override
	public void prettyPrint(PrintWriter out) {
		if (pieces.length == 0) return;
		out.print("package ");
		out.print(toString(pieces));
		out.println(";");
	}

	public String[] getPieces() {
		return pieces;
	}

	public static String toString(String[] pieces) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i < pieces.length; ++i) {
			if (i > 0) sb.append(".");
			sb.append(pieces[i]);
		}
		return sb.toString();
	}
}
