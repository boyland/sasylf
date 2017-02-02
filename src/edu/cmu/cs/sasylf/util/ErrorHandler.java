package edu.cmu.cs.sasylf.util;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.sasylf.term.FreeVar;


public class ErrorHandler {
	public static void report(Errors errorType, String msg, Span loc, String debugInfo, boolean isError, boolean print) {
		if (msg == null)
			msg = "";
		if (loc == null)
			loc = lastSpan.get();
		ErrorReport rep = new ErrorReport(errorType, msg, loc, debugInfo, isError);
		reports.get().add(rep);
		if (print)
			System.err.println(rep.getMessage());
		if (debugInfo != null && edu.cmu.cs.sasylf.util.Util.EXTRA_ERROR_INFO)
			System.err.println(debugInfo);
		if (isError) {
			throw new SASyLFError(rep);
		}
	}

	public static void recoverableError(Errors x, Span obj) {
		try {
			report(x,obj);
		} catch (SASyLFError ex) {
			// stop throw
		}
	}

	public static void recoverableError(String msg, Span obj) {
		try {
			report(msg,obj);
		} catch (SASyLFError x) {
			// stop throw
		}
	}

	public static void recoverableError(Errors x, String msg, Span obj) {
		try {
			report(x, msg,obj);
		} catch (SASyLFError ex) {
			// stop throw
		}
	}

	public static void recoverableError(String msg, Span obj, String debugInfo) {
		try {
			report(msg,obj,debugInfo);
		} catch (SASyLFError x) {
			// stop throw
		}
	}

	public static void recoverableError(Errors errorType, String msg, Span obj, String debugInfo) {
		try {
			report(errorType, msg, obj, debugInfo, true, true);
		} catch (SASyLFError x) {
			// stop throw
		}
	}


	public static void warning(Errors errorType, Span obj) {
		report(errorType, null, obj, null, false, true);
	}

	public static void warning(String msg, Span obj) {
		report(null, msg, obj, null, false, true);
	}

	public static void warning(Errors errorType, Span obj, String fixInfo) {
		report(errorType, null, obj, fixInfo, false, true);
	}

	public static void warning(Errors errorType, String msg, Span obj, String debugInfo) {
		report(errorType, msg, obj, debugInfo, false, true);
	}

	public static void warning(String msg, Span span, String debugInfo) {
		report(null, msg, span, debugInfo, false, true);
	}

	public static void report(Errors errorType, Span obj) {
		report(errorType, null, obj, null, true, true);
	}

	public static void report(Errors errorType, Span obj, String debugInfo) {
		report(errorType, null, obj, debugInfo, true, true);
	}

	// TODO: out of date
	public static void report(String msg, Span obj) {
		report(null, msg,obj,null, true, true);
	}


	/*
	 * @deprecated use report(errorType, msg,obj,null, true, true)
	 */
	public static void report(Errors errorType, String msg, Span obj) {
		report(errorType, msg,obj,null, true, true);
	}

	// TODO: deprecated
	public static void report(String msg, Span obj, String debugInfo) {
		report(null, msg, obj, debugInfo, true, true);
	}

	// TODO: rename error
	public static void report(Errors errorType, String msg, Span obj, String debugInfo) {
		report(errorType, msg, obj, debugInfo, true, true);
	}

	public static List<ErrorReport> getReports() { return reports.get(); }
	public static void clearAll() {
		reports.remove();
		FreeVar.reinit();
	}
	public static int getErrorCount() {
		int errorCount = 0;
		for (ErrorReport r : reports.get()) {
			if (r.isError) ++errorCount;
		}
		return errorCount;
	}
	public static int getWarningCount() { 
		return reports.get().size() - getErrorCount();
	}

	public static void recordLastSpan(Span s) {
		if (s != null) {
			lastSpan.set(s);
		}
	}

	private static ThreadLocal<Span> lastSpan = new ThreadLocal<Span>();

	private static ThreadLocal<List<ErrorReport>> reports = new ThreadLocal<List<ErrorReport>>(){
		@Override
		protected List<ErrorReport> initialValue() {
			return new ArrayList<ErrorReport>();
		}
	};

	public static Location lexicalErrorAsLocation(String file, String error) {
		try {
			int lind = error.indexOf("line ");
			int cind = error.indexOf(", column ");
			int eind = error.indexOf(".", cind+1);
			int line = Integer.parseInt(error.substring(lind+5, cind));
			int column = Integer.parseInt(error.substring(cind+9, eind));
			return new Location(file,line,column);
		} catch (RuntimeException e) {
			return new Location(file,0,0);
		}
	}
}
