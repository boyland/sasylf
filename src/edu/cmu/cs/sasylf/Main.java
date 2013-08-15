package edu.cmu.cs.sasylf;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

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
	 * @throws IOException 
	 */
	public static void main(String[] args) throws ParseException, IOException {
		if (args.length == 0 || (args.length >= 1 && args[0].equals("--help"))) {
			System.err.println("usage: sasylf file1.slf ...");
			System.err.println("   or: sasylf --version (print version)");
			System.err.println("   or: sasylf --help (print this message)");
			System.err.println("   or: sasylf --verbose file1.slf ... (prints out theorem names as it checks them)");
			System.err.println("   or: sasylf --LF file1.slf ... (extra info about LF terms in certain error messages)");
			return;
		}
		int oldErrorCount = 0;
		int oldWarnings = 0;
		if (args.length >= 1 && args[0].equals("--version")) {
      System.out.println(Version.getInstance());
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
				int newWarnings = ErrorHandler.getWarningCount() - oldWarnings;
				oldErrorCount = ErrorHandler.getErrorCount();
				oldWarnings = ErrorHandler.getWarningCount();
				@SuppressWarnings("resource")
        PrintStream ps = (newErrorCount == 0) ? System.out : System.err;
				if (newErrorCount == 0)
					ps.print(filename + ": No errors");
				else if (newErrorCount == 1)
					ps.print(filename + ": 1 error");
				else
					ps.print(filename + ": "+ newErrorCount +" errors");
				if (newWarnings > 0) {
				  if (newWarnings > 1) {
				    ps.print(" and " + newWarnings + " warnings");
				  } else {
				    ps.print(" and 1 warning");
				  }
				}
				ps.println(" reported.");
			}
		}
	}

}
