package edu.cmu.cs.sasylf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;

import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.module.Module;
import edu.cmu.cs.sasylf.module.ModuleFinder;
import edu.cmu.cs.sasylf.module.ModuleId;
import edu.cmu.cs.sasylf.module.RootModuleFinder;
import edu.cmu.cs.sasylf.parser.DSLToolkitParser;
import edu.cmu.cs.sasylf.parser.ParseException;
import edu.cmu.cs.sasylf.parser.TokenMgrError;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.TokenSpan;

public class Main {

	/**
	 * @param args the files to parse and typecheck
	 * @throws ParseException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws ParseException, IOException {
		//System.setOut(new PrintStream(System.out, true, "UTF-8"));
		//System.setErr(new PrintStream(System.err, true, "UTF-8"));
		int exitCode = 0;
		if (args.length == 0 || (args.length >= 1 && args[0].equals("--help"))) {
			System.err.println("usage: sasylf [options] file1.slf ...");
			System.err.println("Options include:");
			System.err.println("   --version     print version");
			System.err.println("   --help        print this message");
			System.err.println("   --compwhere   makes where clauses compulsory (will check them even if not)");
			System.err.println("   --verbose     prints out theorem names as it checks them");
			System.err.println("   --LF          extra info about LF terms in certain error messages");
			System.err.println("   --root=dir    use the given directory for package/module checking.");
			return;
		}
		if (args.length >= 1 && args[0].equals("--version")) {
			System.out.println(Version.getInstance());
			return;
		}
		String dir = null;
		ModuleFinder mf = null;
		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("--compwhere")) {
				edu.cmu.cs.sasylf.util.Util.COMP_WHERE = true;
				continue;
			}
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
			if (args[i].equals("--XContextIsSyntax")) {
				edu.cmu.cs.sasylf.util.Util.X_CONTEXT_IS_SYNTAX = true;
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
				mf = new RootModuleFinder(root);
				continue;
			}
			String filename = args[i];
			File file;
			ModuleId id = null;
			if (dir == null) {
				file = new File(filename);
			} else {
				file = new File(dir,filename);
				try {
					id = new ModuleId(filename);
				} catch (RuntimeException ex) {
					System.err.println(ex.getMessage());
					exitCode = -1;
					continue;
				}
			}
			if (!file.canRead()) {
				System.err.println("Could not open file " + filename);
				exitCode = -1;
				continue;
			}
			Reader r;
			try {
				r = new InputStreamReader(new FileInputStream(file),"UTF-8");
			} catch(FileNotFoundException ex) {
				System.err.println("Could not open file " + filename);
				exitCode = -1;
				continue;
			}
			try {
				/* long start = System.nanoTime(); */
				@SuppressWarnings("unused")
				Module cu = parseAndCheck(mf, filename, id, r);
				/*
				long mid = System.nanoTime();
				if (cu != null) {
				  check(mf,id,cu);
				  long end = System.nanoTime();
				  System.out.println("Parse and check: " + (mid - start));
				  System.out.println("        recheck: " + (end-mid));
				}
				 */
			} catch (SASyLFError e) {
				// ignore the error; it has already been reported
				//e.printStackTrace();
			} catch (RuntimeException e) {
				// System.err.println("Internal SASyLF error analyzing " + filename + " !");
				e.printStackTrace(); // unexpected exception
				ErrorHandler.recoverableError("Internal error: " + e.toString(), null); // "recoverable" = "don't throw"
			} finally {
				r.close();
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
				if (newErrorCount > 0) exitCode = -1;
			}
			ErrorHandler.clearAll();
		}
		System.exit(exitCode);
	}

	/**
	 * Analyze the SASyLF code in the reader and return a compilation unit
	 * if no (unrecoverable) errors are found. 
	 * @param mf may be4 null
	 * @param filename must not be null
	 * @param id may be null only if mf is null
	 * @param r contexts: must not be null
	 * @throws SASyLFError is an error is found.
	 */
	public static CompUnit parseAndCheck(ModuleFinder mf, String filename,
			ModuleId id, Reader r) {
		CompUnit cu = null;
		try {
			cu = DSLToolkitParser.read(filename,r);
		} catch (ParseException e) {
			ErrorHandler.report(null, e.getMessage(), new TokenSpan(e.currentToken.next), null, true, true);
		} catch (TokenMgrError e) {
			ErrorHandler.report(null, e.getMessage(), ErrorHandler.lexicalErrorAsLocation(filename, e.getMessage()), null, true, true);
		}
		check(mf, id, cu);
		return cu;
	}

	/**
	 * @param mf
	 * @param id
	 * @param cu
	 */
	public static void check(ModuleFinder mf, ModuleId id, CompUnit cu) {
		if (mf == null) cu.typecheck();
		else {
			mf.setCurrentPackage(id == null ? ModuleFinder.EMPTY_PACKAGE : id.packageName);
			cu.typecheck(mf,id);
		}
	}

}
