package edu.cmu.cs.sasylf.util;



public class Util {
	public static final boolean DEBUG_PARSE = false;
	public static boolean DEBUG = false;
	public static final boolean DEBUG2 = false;
	public static boolean EXTRA_ERROR_INFO = false;
	public static boolean VERBOSE = false;
	public static boolean COMP_WHERE = false;

	public static void debug_parse(Object o) {
		if (DEBUG_PARSE)
			System.out.println(o);
	}

	// for efficiency, we avoid the multiple argument version unless we have many (>3) args:

	public static void debug(Object o) {
		if (DEBUG)
			tdebug(o);
	}
	public static void debug(Object o1, Object o2) {
		if (DEBUG)
			tdebug(o1,o2);
	}
	public static void debug(Object o1, Object o2, Object o3) {
		if (DEBUG)
			tdebug(o1,o2,o3);
	}
	public static void debug(Object... o) {
		if (DEBUG)
			tdebug(o);
	}

	// temporary debug (used to show a particular debug statement without setting the global flag
	public static void tdebug(Object... os) {
		StringBuilder sb = new StringBuilder();
		for (Object o : os) {
			sb.append(o);
		}
		System.out.println(sb.toString());
	}

	public static void debug2(String s) {
		if (DEBUG2)
			System.out.println(s);
	}
	public static void verify(boolean invariant, String message) {
		if (!invariant) {
			System.err.println(message);
			throw new RuntimeException(message);
		}
	}

	public static boolean isNumber(char ch) {
		return Character.isDigit(ch) || Character.getType(ch) == Character.OTHER_NUMBER;
	}

	public static String stripId(String id) {
		if (isNumber(id.charAt(0)))
			return id;
		int newLength = id.length();
		char ch = id.charAt(newLength-1);
		while (ch == '\'' || isNumber(ch)) {
			newLength--;
			ch = id.charAt(newLength-1);
		}
		return id.substring(0, newLength);
	}

	public static void main(String[] args) {
		char c = '₁';
		System.out.println("type = " + Character.getType(c));
	}
}
