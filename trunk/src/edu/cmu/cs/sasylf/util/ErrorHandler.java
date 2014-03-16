package edu.cmu.cs.sasylf.util;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.sasylf.ast.Errors;
import edu.cmu.cs.sasylf.ast.Location;
import edu.cmu.cs.sasylf.ast.Node;
import edu.cmu.cs.sasylf.term.FreeVar;


public class ErrorHandler {
	public static void report(Errors errorType, String msg, Location loc, String debugInfo, boolean isError, boolean print) {
		if (msg == null)
			msg = "";
		ErrorReport rep = new ErrorReport(errorType, msg, loc, debugInfo, isError);
		reports.add(rep);
		if (print)
			System.err.println(rep.getMessage());
		if (debugInfo != null && edu.cmu.cs.sasylf.util.Util.EXTRA_ERROR_INFO)
			System.err.println(debugInfo);
		if (isError) {
			errorCount++;
			throw new SASyLFError(rep);
		} else {
			warningCount++;
		}
	}
	
  public static void recoverableError(Errors x, Node obj) {
    try {
      report(x,obj);
    } catch (SASyLFError ex) {
      // stop throw
    }
  }

  public static void recoverableError(String msg, Node obj) {
    try {
      report(msg,obj);
    } catch (SASyLFError x) {
      // stop throw
    }
  }
  
  public static void recoverableError(Errors x, String msg, Node obj) {
    try {
      report(x, msg,obj);
    } catch (SASyLFError ex) {
      // stop throw
    }
  }
  
  public static void recoverableError(Errors error, Location location) {
    try {
      report(error,"",location,null,true,true);
    } catch (SASyLFError x) {
      // stop throw
    }
  }

  public static void recoverableError(String msg, Location obj) {
    try {
      report(msg,obj);
    } catch (SASyLFError x) {
      // stop throw
    }
  }
  
	public static void recoverableError(String msg, Node obj, String debugInfo) {
	  try {
	    report(msg,obj,debugInfo);
	  } catch (SASyLFError x) {
	    // stop throw
	  }
	}

  public static void recoverableError(Errors errorType, String msg, Node obj, String debugInfo) {
    try {
      report(errorType, msg, obj.getLocation(), debugInfo, true, true);
    } catch (SASyLFError x) {
      // stop throw
    }
  }

  public static void recoverableError(Errors errorType, String msg, Location loc, String debugInfo) {
    try {
      report(errorType, msg, loc, debugInfo, true, true);
    } catch (SASyLFError x) {
      // stop throw
    }
  }

	public static void warning(Errors errorType, Node obj) {
		report(errorType, null, obj.getLocation(), null, false, true);
	}

	public static void warning(String msg, Node obj) {
		report(null, msg, obj.getLocation(), null, false, true);
	}
	
	public static void warning(Errors errorType, Node obj, String fixInfo) {
	  report(errorType, null, obj.getLocation(), fixInfo, false, true);
	}

  public static void warning(Errors errorType, String msg, Node obj, String debugInfo) {
    report(errorType, msg, obj.getLocation(), debugInfo, false, true);
  }

  public static void report(Errors errorType, Node obj) {
		report(errorType, null, obj.getLocation(), null, true, true);
	}

	public static void report(Errors errorType, Node obj, String debugInfo) {
		report(errorType, null, obj.getLocation(), debugInfo, true, true);
	}

	// TODO: out of date
	public static void report(String msg, Node obj) {
		report(null, msg,obj.getLocation(),null, true, true);
	}

	// TODO: out of date
	public static void report(String msg, Location loc) {
		report(null, msg,loc,null, true, true);
	}

	/*
	 * @deprecated use report(errorType, msg,obj.getLocation(),null, true, true)
	 */
	public static void report(Errors errorType, String msg, Node obj) {
		report(errorType, msg,obj.getLocation(),null, true, true);
	}

	// TODO: deprecated
	public static void report(String msg, Node obj, String debugInfo) {
		report(null, msg, obj.getLocation(), debugInfo, true, true);
	}

	// TODO: rename error
	public static void report(Errors errorType, String msg, Node obj, String debugInfo) {
		report(errorType, msg, obj.getLocation(), debugInfo, true, true);
	}
	
	public static List<ErrorReport> getReports() { return reports; }
	public static void clearAll() {
		reports = new ArrayList<ErrorReport>();
		errorCount = 0;
		warningCount = 0;
		FreeVar.reinit();
	}
	public static int getErrorCount() { return errorCount; }
	public static int getWarningCount() { return warningCount; }

	private static List<ErrorReport> reports = new ArrayList<ErrorReport>();
	
	private static int errorCount=0;
	private static int warningCount=0;
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
