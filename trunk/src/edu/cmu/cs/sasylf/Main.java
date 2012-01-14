package edu.cmu.cs.sasylf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

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
		if (args.length >= 1 && args[0].equals("--version")) {
		  // Rather than include the version here, we look for README.TXT
		  // This is rather more complex than I hoped; if we are packed up in a JAR,
		  // it's easy to find, but otherwise, we have to go hunting.
		  InputStream s = Main.class.getClassLoader().getResourceAsStream("README.TXT");
		  if (s == null) {
		    URL execdir = Main.class.getClassLoader().getResource(".");
		    URI uri;
		    try {
		      uri = execdir.toURI();
		    } catch (URISyntaxException e) {
		      e.printStackTrace();
		      return;
		    }
		    if (uri.getScheme().equals("file")) {
		      File dir = new File(uri.getPath());
		      File rfile = new File(dir.getParentFile(),"README.TXT");
		      try {
		        s = new FileInputStream(rfile);
		      } catch (FileNotFoundException ex) {
		        // muffle
		      }
		    }
		    if (s == null) {
		      System.out.println("SASyLF version ???");
		      return;
		    }
		  }
		  BufferedReader br = new BufferedReader(new InputStreamReader(s));
		  System.out.println(br.readLine());
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
