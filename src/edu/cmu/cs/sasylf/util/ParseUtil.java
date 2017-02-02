package edu.cmu.cs.sasylf.util;

import java.io.StringReader;

import edu.cmu.cs.sasylf.parser.DSLToolkitParserConstants;
import edu.cmu.cs.sasylf.parser.DSLToolkitParserTokenManager;
import edu.cmu.cs.sasylf.parser.JavaCharStream;
import edu.cmu.cs.sasylf.parser.Token;

public class ParseUtil {

	/**
	 * Is the given string a legal SASyLF identifier?
	 * @param text string to examine, must not be null
	 * @return whether it yields a token &lt;IDENTIFIER&gt; in the SASyLF parser.
	 */
	public static boolean isLegalIdentifier(String text) {
		JavaCharStream s = new JavaCharStream(new StringReader(text),1,1);
		DSLToolkitParserTokenManager tm = new DSLToolkitParserTokenManager(s);
		Token t1 = tm.getNextToken();
		Token t2 = tm.getNextToken();
		return t1.kind == DSLToolkitParserConstants.IDENTIFIER &&
				t2.kind == DSLToolkitParserConstants.EOF;
	}

	public static boolean isBarChar(char ch) {
		return ch == '-' || ch == '\u2500' || ch == '\u2014' || ch == '\u2015';
	}

	public static void main(String[] args) {
		for (String a : args) {
			System.out.println(a + " is legal? " + isLegalIdentifier(a));
		}
	}
}
