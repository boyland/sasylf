package edu.cmu.cs.sasylf;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import edu.cmu.cs.sasylf.module.ModuleFinder;
import edu.cmu.cs.sasylf.module.PathModuleFinder;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.ErrorReport;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Report;
import edu.cmu.cs.sasylf.util.SASyLFError;
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
		
		Set<Errors> encountered = new HashSet<>();
		for (String s : args) {
			try (Reader r = new FileReader(s)) {
				Main.parseAndCheck(mf, s, null, r);
			} catch (FileNotFoundException e) {
				System.err.println("Unable to open '" + s + "': " + e.getLocalizedMessage());
			} catch (IOException e) {
				System.err.println("Error reading '" + s + "': " + e.getLocalizedMessage());
			} catch (SASyLFError e) {
				// already handled
			} catch (RuntimeException e) {
				System.out.println("While checking " + s);
				e.printStackTrace();
			}
			Collection<Report> reports = ErrorHandler.getReports();
			for (Report r : reports) {
				if (r instanceof ErrorReport) {
					ErrorReport er = (ErrorReport)r;
					final Errors type = er.getErrorType();
					if (type == Errors.INTERNAL_ERROR) {
						System.err.println(r.formatMessage());
						if (er.getExtraInformation() != null) {
							System.err.println("  " + er.getExtraInformation());
						}
					}
					encountered.add(type);
				}
				r.getMessage();
			}
		}
		System.out.println("Error types generated: " + encountered.size());
		System.out.println("The following errors were never generated:");
		for (Errors error : Errors.values()) {
			if (!encountered.contains(error)) {
				System.out.println(error.name());
			}
		}
	}
}
