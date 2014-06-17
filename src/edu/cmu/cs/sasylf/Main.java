package edu.cmu.cs.sasylf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.parser.DSLToolkitParser;
import edu.cmu.cs.sasylf.parser.ParseException;
import edu.cmu.cs.sasylf.parser.TokenMgrError;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.SASyLFError;

public class Main {

	/**
	 * @param args the files to parse and typecheck
	 * @throws ParseException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws ParseException, IOException {
	  //System.setOut(new PrintStream(System.out, true, "UTF-8"));
	  //System.setErr(new PrintStream(System.err, true, "UTF-8"));
	  if (args.length == 0 || (args.length >= 1 && args[0].equals("--help"))) {
			System.err.println("usage: sasylf [options] file1.slf ...");
			System.err.println("Options include:");
			System.err.println("   --version   print version");
			System.err.println("   --help      print this message");
			System.err.println("   --verbose   prints out theorem names as it checks them");
			System.err.println("   --LF        extra info about LF terms in certain error messages");
			System.err.println("   --root=dir  use the given directory for package/module checking.");
			return;
		}
		if (args.length >= 1 && args[0].equals("--version")) {
      System.out.println(Version.getInstance());
		  return;
		}
		String dir = null;
		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("--LF")) {
				edu.cmu.cs.sasylf.util.Util.EXTRA_ERROR_INFO = true;
				continue;
			}
			if (args[i].equals("--verbose")) {
				edu.cmu.cs.sasylf.util.Util.VERBOSE = true;
				continue;
			}
			if (args[i].equals("--debug")) {
			  edu.cmu.cs.sasylf.util.Util.DEBUG = true;
			  continue;
			}
			if (args[i].startsWith("--root=")) {
			  dir = args[i].substring(7);
        File root = new File(dir); 
			  if (!root.isDirectory()) {
			    if (root.exists()) {
			      System.err.println("Not a directory: "+ dir);
			      System.exit(-1);
			    } else {
			      System.err.println("No such file or directory: " + dir);
			      System.exit(-1);
			    }
			  }
			  continue;
			}
			String filename = args[i];
			File file;
			if (dir == null) {
			  file = new File(filename);
			} else {
			  file = new File(dir,filename);
			}
			if (!file.canRead()) {
				System.err.println("Could not open file " + filename);
				System.exit(-1);
				return;
			}
			try {
				CompUnit cu = null;
				try {
					cu = DSLToolkitParser.read(filename,new FileInputStream(file));
				} catch (ParseException e) {
					ErrorHandler.report(null, e.getMessage(), new Location(e.currentToken.next), null, true, true);
				} catch (TokenMgrError e) {
				  ErrorHandler.report(null, e.getMessage(), ErrorHandler.lexicalErrorAsLocation(filename, e.getMessage()), null, true, true);
				} catch (FileNotFoundException ex) {
				  System.err.println("Could not open file " + filename);
				  System.exit(-1);
				}
				if (dir == null) cu.typecheck();
				else cu.typecheck(filename);
			} catch (SASyLFError e) {
				// ignore the error; it has already been reported
				//e.printStackTrace();
			} catch (RuntimeException e) {
				System.err.println("Internal SASyLF error analyzing " + filename + " !");
				e.printStackTrace(); // unexpected exception
			} finally {
				int newErrorCount = ErrorHandler.getErrorCount();
				int newWarnings = ErrorHandler.getWarningCount();
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
			ErrorHandler.clearAll();
		}
	}

}
