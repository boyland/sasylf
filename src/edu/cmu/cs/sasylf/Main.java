package edu.cmu.cs.sasylf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.module.ModuleFinder;
import edu.cmu.cs.sasylf.module.ModuleId;
import edu.cmu.cs.sasylf.module.PathModuleFinder;
import edu.cmu.cs.sasylf.module.RootModuleFinder;
import edu.cmu.cs.sasylf.parser.DSLToolkitParser;
import edu.cmu.cs.sasylf.parser.ParseException;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.TaskReport;

public class Main {

	/**
	 * @param args the files to parse and typecheck
	 * @throws ParseException
	 * @throws IOException
	 */
	public static void main(String[] args) throws ParseException, IOException {
		PrintStream out = new PrintStream(System.out, true, "UTF-8");
		PrintStream err = new PrintStream(System.err, true, "UTF-8");
		System.setOut(out);
		System.setErr(err);
		int exitCode = 0;
		if (args.length == 0 || (args.length >= 1 && args[0].equals("--help"))) {
			System.err.println("usage: sasylf [options] file1.slf ...");
			System.err.println("Options include:");
			System.err.println("   --version     print version");
			System.err.println("   --help        print this message");
			System.err.println(
					"   --compwhere   makes where clauses compulsory (will check them even if not)");
			System.err.println(
					"   --verbose     prints out theorem names as it checks them");
			System.err.println("   --task        print out task comments");
			System.err.println(
					"   --LF          extra info about LF terms in certain error messages");
			System.err.println("   --stdin       pass in slf file via stdin");
			System.err.println(
					"   --lsp         lsp interface for completions, quick fixes, etc. note: intended for lsp use only.");
			System.err.println(
					"   --debug       debug mode that does not redirect output in lsp mode");
			System.err.println(
					"   --path=dir... use the given directories for package/module checking.");
			return;
		}
		if (args.length >= 1 && args[0].equals("--version")) {
			System.out.println(Version.getInstance());
			return;
		}
		String dir = null;
		PathModuleFinder mf = null;
		PathModuleFinder defaultMF = new PathModuleFinder("");
		boolean debug = false;
		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("--debug")) {
				debug = true;
				// If debug flag is set after the lsp flag, then we need to reset out
				// and err
				if (Proof.getLsp()) {
					System.setOut(out);
					System.setErr(err);
				}
				continue;
			}
			if (args[i].equals("--lsp")) {
				if (!debug) {
					System.setOut(new PrintStream(OutputStream.nullOutputStream()));
					System.setErr(new PrintStream(OutputStream.nullOutputStream()));
				}
				Proof.setLsp(true);
				continue;
			}
			if (args[i].equals("--compwhere")) {
				edu.cmu.cs.sasylf.util.Util.COMP_WHERE = true;
				continue;
			}
			if (args[i].equals("--LF")) {
				edu.cmu.cs.sasylf.util.Util.EXTRA_ERROR_INFO = true;
				continue;
			}
			if (args[i].equals("--task")) {
				edu.cmu.cs.sasylf.util.Util.SHOW_TASK_COMMENTS = true;
				DSLToolkitParser.addListener(TaskReport.commentListener);
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
			if (args[i].equals("--waitForCR")) {
				System.out.println("Press <ENTER> to continue");
				while (System.in.read() != '\n') {
					// do nothing
				}
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
						System.err.println("Not a directory: " + dir);
						System.exit(-1);
					} else {
						System.err.println("No such file or directory: " + dir);
						System.exit(-1);
					}
				}
				mf = new RootModuleFinder(root);
				continue;
			}
			if (args[i].startsWith("--path=")) {
				mf = new PathModuleFinder(args[i].substring(7));
				continue;
			}
			String filename = args[i];
			File file = null;
			ModuleId id = null;
			Proof pf = null;
			if (args[i].startsWith("--stdin")) filename = "stdin";
			if (mf != null) {
				try {
					id = new ModuleId(filename);
				} catch (RuntimeException ex) {
					System.err.println(ex.getMessage());
					exitCode = -1;
					continue;
				}
				try {
					pf = mf.findProof(id, new Location("<commandline>", 0, 0));
				} catch (SASyLFError e) {
					// already handled
				}
			} else {
				if (!args[i].startsWith("--stdin")) {
					file = new File(filename);
					if (!file.canRead()) {
						System.err.println("Could not open file " + filename);
						exitCode = -1;
					}
				}
				try {
					/**
					 * We take the input from stdin and turn
					 * that into a VSDocument that is needed
					 * for the quickfixes.
					 */
					InputStreamReader r;
					if (args[i].startsWith("--stdin"))
						r = new InputStreamReader(System.in);
					else r = new InputStreamReader(new FileInputStream(file), "UTF-8");

					pf = Proof.parseAndCheck(defaultMF, filename, null, r);
				} catch (FileNotFoundException ex) {
					System.err.println("Could not open file " + filename);
					exitCode = -1;
					continue;
				} catch (IOException | RuntimeException e) {
					// System.err.println("Internal SASyLF
					// error analyzing " + filename
					// + " !");
					e.printStackTrace(); // unexpected
															 // exception
					ErrorHandler.recoverableError(Errors.INTERNAL_ERROR, e.toString(),
																				null); // "recoverable" = "don't
																							 // throw"
				}
			}

			if (pf == null) continue;

			int newErrorCount = pf.getErrorCount();
			int newWarnings = pf.getWarningCount();
			@SuppressWarnings("resource")
			PrintStream ps = (newErrorCount == 0) ? System.out : System.err;
			if (newErrorCount == 0) ps.print(filename + ": No errors");
			else if (newErrorCount == 1) ps.print(filename + ": 1 error");
			else ps.print(filename + ": " + newErrorCount + " errors");
			if (newWarnings > 0) {
				if (newWarnings > 1) {
					ps.print(" and " + newWarnings + " warnings");
				} else {
					ps.print(" and 1 warning");
				}
			}
			ps.println(" reported.");
			if (newErrorCount > 0) exitCode = -1;

			System.setOut(out);
			System.setErr(err);
			if (Proof.getLsp()) {
				System.out.println(Proof.getJSON());
			}
		}
		System.exit(exitCode);
	}

	/**
	 * Analyze the SASyLF code in the reader and return a compilation unit
	 * if no (unrecoverable) errors are found.
	 * @param mf may be null
	 * @param filename must not be null
	 * @param id may be null if not interested in checking
	 *     package/module-name errors
	 * @param r contexts: must not be null
	 * @throws SASyLFError is an error is found.
	 * @deprecated this method gives no way to access the errors that
	 *     resulted.
	 * Use <code>Proof p = new Proof(filename,id);
	 * p.parseAndCheck(mf,r);</code> and then use p to access the AST and
	 * the errors separately.
	 */
	@Deprecated
	public static CompUnit parseAndCheck(ModuleFinder mf, String filename,
																			 ModuleId id, Reader r) {
		Proof p = new Proof(filename, id);
		p.parseAndCheck(mf, r);
		return p.getCompilationUnit();
	}
}
