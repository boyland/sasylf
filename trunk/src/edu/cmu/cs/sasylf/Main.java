package edu.cmu.cs.sasylf;

import java.io.File;
import java.io.FileNotFoundException;

import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.ast.Location;
import edu.cmu.cs.sasylf.parser.DSLToolkitParser;
import edu.cmu.cs.sasylf.parser.ParseException;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.SASyLFError;

public class Main {

	/**
	 * @param args the files to parse and typecheck
	 * @throws ParseException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, ParseException {
		if (args.length == 0 || (args.length == 1 && args[0].equals("--help"))) {
			System.err.println("usage: sasylf file1.slf");
			System.err.println("   or: sasylf --version");
			System.err.println("   or: sasylf --help");
			System.err.println("   or: sasylf --verbose (prints out theorem names as it checks them)");
			System.err.println("   or: sasylf --LF file1.slf (extra info about LF terms in certain error messages)");
			return;
		}
		int oldErrorCount = 0;
		if (args.length == 1 && args[0].equals("--version")) {
			System.out.println("SASyLF version 1.0.2 (uwm 13)");
			return;
		}
		// TODO: may want to add command line argument for explicit error messages on case analysis -- see Rule.getErrorDescription()
		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("--LF")) {
				edu.cmu.cs.sasylf.util.Util.EXTRA_ERROR_INFO = true;
				continue;
			}
			if (args[i].equals("--verbose")) {
				edu.cmu.cs.sasylf.util.Util.VERBOSE = true;
				continue;
			}
			String filename = args[i];
			File file= new File(filename);
			if (!file.canRead()) {
				System.err.println("Could not open file " + filename);
				return;
			}
			try {
				CompUnit cu = null;
				try {
					cu = DSLToolkitParser.read(file);
				} catch (ParseException e) {
					ErrorHandler.report(null, e.getMessage(), new Location(e.currentToken.next), null, true, false);
				}
				cu.typecheck();
			} catch (SASyLFError e) {
				// ignore the error; it has already been reported
				//e.printStackTrace();
			} catch (RuntimeException e) {
				System.err.println("Internal SASyLF error!");
				e.printStackTrace(); // unexpected exception
			} finally {
				int newErrorCount = ErrorHandler.getErrorCount() - oldErrorCount;
				oldErrorCount = ErrorHandler.getErrorCount();
				if (newErrorCount == 0)
					System.out.println(filename + ": No errors found.");
				else if (newErrorCount == 1)
					System.err.println(filename + ": 1 error found");
				else
					System.err.println(filename + ": "+ newErrorCount +" errors found");
			}
		}
	}

}
