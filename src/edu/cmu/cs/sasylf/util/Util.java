package edu.cmu.cs.sasylf.util;

import java.util.*;
import java.io.*;


public class Util {
	public static final boolean DEBUG_PARSE = false;
	public static final boolean DEBUG = false;
	public static final boolean DEBUG2 = false;
	public static boolean EXTRA_ERROR_INFO = false;
	public static boolean VERBOSE = false;
    public static void debug_parse(String s) {
    	if (DEBUG_PARSE)
    		System.out.println(s);
    }
    public static void debug(String s) {
    	if (DEBUG)
    		System.out.println(s);
    }
    // temporary debug (used to show a particular debug statement without setting the global flag
    public static void tdebug(String s) {
		System.out.println(s);
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
	public static String stripId(String id) {
		if (Character.isDigit(id.charAt(0)))
			return id;
		int newLength = id.length();
		char ch = id.charAt(newLength-1);
		while (ch == '\'' || Character.isDigit(ch)) {
			newLength--;
			ch = id.charAt(newLength-1);
		}
		return id.substring(0, newLength);
	}
}
