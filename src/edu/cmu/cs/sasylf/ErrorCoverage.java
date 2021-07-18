package edu.cmu.cs.sasylf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.module.ModuleFinder;
import edu.cmu.cs.sasylf.module.ModuleId;
import edu.cmu.cs.sasylf.module.PathModuleFinder;
import edu.cmu.cs.sasylf.parser.DSLToolkitParser;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.ErrorReport;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Report;
import edu.cmu.cs.sasylf.util.Util;

public class ErrorCoverage {

	public static void main(String[] args) {
		ModuleFinder mf = new PathModuleFinder("");
		Util.SHOW_TASK_COMMENTS = false;
		Util.COMP_WHERE = true;
		Util.EXTRA_ERROR_INFO = false;
		Util.VERBOSE = false;
		Util.PRINT_ERRORS = false;
		Util.PRINT_SOLVE = false;
		
		boolean showSource = false; // option
		boolean verbose = false; // option
		boolean showCoverage = true; // option
		Set<Errors> interestingErrors = new HashSet<>();
		// These should not be happening
		interestingErrors.add(Errors.INTERNAL_ERROR);
		interestingErrors.add(Errors.UNSPECIFIED);
		// command line can set other interesting errors
	
		DSLToolkitParser.addListener((t,f) -> {
			if (t.image.startsWith("//!") || t.image.startsWith("/*!") || t.image.startsWith("//?")) {
				ErrorHandler.report(new ExpectedError(t.beginLine,t.image.substring(3))); 
			}
		} ); 
		
		Map<Errors,List<String>> index = new HashMap<>();
		Set<Errors> encountered = index.keySet();
		for (String s : args) {
			if (s.startsWith("--")) {
				if (s.equals("--showSource")) showSource = true;
				else if (s.equals("--verbose")) verbose = true;
				else if (s.equals("--noCoverage")) showCoverage = false;
				else usage();
				continue;
			} else if (!s.endsWith(".slf")) {
				try {
					Errors e = Errors.valueOf(s);
					interestingErrors.add(e);
				} catch (IllegalArgumentException ex) {
					usage();
				}
				continue;
			}
			File f = new File(s);
			Proof results  = null;
			results = mf.findProof(new ModuleId(f), new Location("<command line",0,0));
			Collection<Report> reports = results.getReports();
			int parseReports = results.getParseReports().size();
			BitSet markedLines = new BitSet();
			BitSet errorLines = new BitSet();
			Map<Integer,String> expectedMessages = new HashMap<>();
			String shortName = f.getName();
			boolean printedName = false;
			int reportIndex = 0;
			if (verbose) {
				System.out.println(results.getCompilationUnit());
				System.out.println("Reports for " + shortName + " : " + reports.size() + " of which " + parseReports + " are parse errors.");
			}
			for (Report r : reports) {
				if (r instanceof ExpectedError) {
					final int line = r.getSpan().getLocation().getLine();
					markedLines.set(line);
					String m = r.getMessage();
					if (m.startsWith("=")) expectedMessages.put(line, m.substring(1));
				}
			}
			for (Report r : reports) {
				++reportIndex;
				int line = r.getSpan().getLocation().getLine();
				if (r instanceof ErrorReport) {
					errorLines.set(line);
					ErrorReport er = (ErrorReport)r;
					final Errors type = er.getErrorType();
					if ((type.ordinal() <= Errors.PARSE_ERROR.ordinal()) != 
							(reportIndex <= parseReports)) {
						if (reportIndex <= parseReports) {
							System.err.println("Non parse-type error generated during parsing: " + r);
						} else {
							System.err.println("Parse-type error generated after parsing: " + r);
						}
					}
					if (verbose || interestingErrors.contains(type) || 
							(type != Errors.WHERE_MISSING && type != Errors.DERIVATION_UNPROVED &&
								!markedLines.get(line))) {
						if (showSource) {
							if (!printedName) {
								System.out.println("In " + s);
								printedName = true;
							}
							Location loc = r.getSpan().getLocation();
							System.out.format("%6d: %s\n", loc.getLine(),getLine(f,loc.getLine()));
							int col = loc.getColumn()+8-1; // 6 digits plus colon plus space, minus caret
							for (int i=0; i < col; ++i) {
								System.out.print("-");
							}
							System.out.println("^- " + er.getMessage());
						} else {
							System.err.println(r.formatMessage());
							if (er.getExtraInformation() != null) {
								System.err.println("  " + er.getExtraInformation());
							}
						}
					}
					List<String> located = index.get(type);
					if (located == null) {
						located = new ArrayList<>();
						index.put(type, located);
					}
					located.add(shortName);
					String expected = expectedMessages.get(line);
					if (expected != null) {
						expected = expected.trim();
						String actual = er.getErrorMessage();
						if (!actual.equals(expected)) {
							System.err.println(line + ": Expected '" + expected + "',\n" + 
						                       line + ":  but got '" + actual + "'");
						}
					}
				}
				r.getMessage(); // XXX: Why call this for side-effect only?
			}
			markedLines.andNot(errorLines);
			// marked and not error -> missing errors
			for (int i = markedLines.nextSetBit(0); i >= 0; i = markedLines.nextSetBit(i+1)) {
			     System.out.println("Missing expected report for " + s + ":" + i);
			}
			markedLines.clear();
		}
		if (!showCoverage) return;
		for (Errors error : Errors.values()) {
			System.out.print(error);
			List<String> located = index.get(error);
			if (located == null || located.isEmpty()) {
				System.out.println(" [no examples]");
			} else {
				int i = 0;
				for (String s : located) {
					if (++i > 5) {
						System.out.print(" ...");
						break;
					}
					System.out.print(" " + s);
				}
				System.out.println();
			}
		}
		System.out.println("Error types generated: " + encountered.size());
		System.out.println("Error types never generated: " + (Errors.values().length - encountered.size()));
	}
	
	private static void usage() {
		System.out.println("usage: java " + ErrorCoverage.class.getName() + " [--showSource|--verbose] ERRORNAME... filename.slf...");
		System.exit(1);
	}
	
	private static String getLine(File f, int line) {
		// inefficient, but maybe not an issue
		try (BufferedReader r = new BufferedReader(new FileReader(f))) {
			while (line > 1) {
				r.readLine();
				--line;
			}
			return r.readLine();
		} catch (IOException e) {
			return "[" + f + ":" + line + "]";
		}
	}
	
	private static class ExpectedError extends Report {
		ExpectedError(int line) {
			super (new Location("",line,0),null);
		}
		
		ExpectedError(int line, String message) {
			super (new Location("", line, 0), message.startsWith("=") ? message : null);
		}

		@Override
		public boolean shouldPrint() {
			return false;
		}
	}
}
