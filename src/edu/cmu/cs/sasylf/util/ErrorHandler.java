package edu.cmu.cs.sasylf.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.util.Marker;
import edu.cmu.cs.sasylf.util.VSDocument;
import edu.cmu.cs.sasylf.util.VSMarker;
import edu.cmu.cs.sasylf.util.VSRegion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ErrorHandler {
  /**
   * Report an error or warning.
   * <p>
   * In the description of the parameters, "default" means the value to be used
   * if "null" is passed for the parameter.
   * @param errorType error type, defaults to UNSPECIFIED
   * @param msg extra information associated with error, defaults to "".
   * This text should be language independent to aid (future)
   * internationalization, unless this is an {@link Errors#INTERNAL_ERROR}.
   * Usually it is redundant to include text from within the span covered by
   * this report.
   * @param loc location of the error, defaults to the last span.
   * The default should be avoided, since very few places in the code base
   * update the last span.
   * @param debugInfo extra information used for --LF flag, also used for quick
   *     fixes, optionally null.
   * This information need not be language independent.
   * @param isError whether an error (or just a warning)
   * @param throwable whether this method should throw a SASyLF error after
   *     reporting the error
   */
  public static boolean lsp = false;
  public static VSDocument doc = null;
  public static ArrayNode arrayNode = (JsonNodeFactory.instance).arrayNode();

  public static void report(Errors errorType, String msg, Span loc,
                            String debugInfo, boolean isError,
                            boolean throwable) {
    if (errorType == null)
      errorType = Errors.UNSPECIFIED;
    if (msg == null)
      msg = "";
    if (loc == null)
      loc = lastSpan.get();
    ErrorReport rep = new ErrorReport(errorType, msg, loc, debugInfo, isError);
    report(rep);
    if (throwable) {
      throw new SASyLFError(rep);
    }
  }

  /**
   * Report something to be logged and perhaps printed.
   * @param r report to report, must not be null
   */
  public static void report(Report r) {
    logReport(r);
    if (r.shouldPrint() && !lsp) {
      System.err.println(r.formatMessage());
      String extra = r.getExtraInformation();
      if (extra != null && Util.EXTRA_ERROR_INFO) {
        System.err.println(extra);
      }
    }
  }

  public static void recoverableError(Errors x, Span obj) {
    recoverableError(x, null, obj, null);
  }

  public static void recoverableError(Errors x, String msg, Span obj) {
    recoverableError(x, msg, obj, null);
  }

  public static void recoverableError(Errors x, Span obj, String info) {
    recoverableError(x, null, obj, info);
  }

  public static void recoverableError(Errors errorType, String msg, Span obj,
                                      String debugInfo) {
    report(errorType, msg, obj, debugInfo, true, false);
  }

  public static void warning(Errors errorType, Span obj) {
    warning(errorType, null, obj, null);
  }

  public static void warning(Errors errorType, String added, Span obj) {
    warning(errorType, added, obj, null);
  }

  public static void warning(Errors errorType, Span obj, String fixInfo) {
    warning(errorType, null, obj, fixInfo);
  }

  public static void warning(Errors errorType, String msg, Span obj,
                             String debugInfo) {
    report(errorType, msg, obj, debugInfo, false, false);
  }

  public static void error(Errors errorType, Span obj) {
    error(errorType, null, obj, null);
  }

  public static void error(Errors errorType, Span obj, String debugInfo) {
    error(errorType, null, obj, debugInfo);
  }

  public static void error(Errors errorType, String msg, Span obj) {
    error(errorType, msg, obj, null);
  }

  public static void error(Errors errorType, String msg, Span obj,
                           String debugInfo) {
    report(errorType, msg, obj, debugInfo, true, true);
  }

  private static void logReport(Report r) { reports.get().add(r); }

  public static List<Report> getReports() { return reports.get(); }

  /**
   * Run the given action with a new set of error reports.
   * The current reports are preserved unchanged.
   * @param r action to perform, must not be null
   * @return reports that occurred before a successful end.
   */
  public static List<Report> withFreshReports(Runnable r) {
    List<Report> saved = reports.get();
    reports.remove();
    List<Report> result = reports.get();
    try {
      r.run();
    } finally {
      reports.set(saved);
    }

    /**
     * If the lsp flag is enabled, we have to create a json object that prints
     * out the desired data to stdout.
     */
    ObjectMapper objectMapper = new ObjectMapper();
    // JsonNodeFactory factory = JsonNodeFactory.instance;

    for (Report rep : result) {
      Span s = rep.getSpan();
      String[] lines = doc.getLines();

      int offset = s.getLocation().getColumn();

      // assert s.getLocation().getLine() == s.getEndLocation().getLine();
      // assert s.getLocation().getColumn() == 0;
      int line = s.getLocation().getLine();

      for (int i = 0; i < line - 1; ++i) {
        offset += lines[i].length();
      }

      int length = lines[line].length();

      String severity = "info";

      // assert s.getEndLocation().getColumn() == length;

      ObjectNode tmp = objectMapper.createObjectNode();

      if (rep instanceof ErrorReport) {
        ErrorReport report = (ErrorReport)(rep);
        VSMarker marker = new VSMarker();

        marker.setAttribute(Marker.SASYLF_ERROR_TYPE, report.getErrorType());
        marker.setAttribute(Marker.SASYLF_ERROR_INFO,
                            report.getExtraInformation());
        marker.setAttribute(Marker.LINE_NUMBER, line);
        marker.setAttribute(Marker.CHAR_START, offset);
        marker.setAttribute(Marker.CHAR_END, offset + length);

        Quickfix q = new Quickfix();

        HashMap<String, Object> map = q.makeQuickfix(doc, marker);

        if (map != null) {
          ObjectNode qf = objectMapper.createObjectNode();
          map.forEach((key, value) -> qf.putPOJO(key, value));
          tmp.putObject("Quickfix").setAll(qf);
        } else {
          tmp.putNull("Quickfix");
        }
        severity = (report.isError()) ? "error" : "warning";
      }

      tmp.put("Error Message", rep.getMessage());
      tmp.put("Line", Integer.toString(line));
      tmp.put("Severity", severity);

      arrayNode.add(tmp);
    }

    return result;
  }

  /**
   * Start a new check session.
   * This starts the reports on a fresh list and clears the
   * free variables. Mixing two functions like this is a bad idea.
   * It also doesn't give a way to protect existing problems
   * @deprecated use {@link #withFreshReports(Runnable)} and
   * call {@link FreeVar.reinit} directly.
   */
  @Deprecated
  public static void clearAll() {
    reports.remove();
    FreeVar.reinit();
  }

  public static int getErrorCount() {
    int errorCount = 0;
    for (Report r : reports.get()) {
      if (r.isError())
        ++errorCount;
    }
    return errorCount;
  }
  public static int getWarningCount() {
    int warnCount = 0;
    for (Report r : reports.get()) {
      if (r.isError())
        continue;
      if (!(r instanceof ErrorReport))
        continue;
      ++warnCount;
    }
    return warnCount;
  }

  public static void recordLastSpan(Span s) {
    if (s != null) {
      lastSpan.set(s);
    }
  }

  private static ThreadLocal<Span> lastSpan = new ThreadLocal<Span>();

  private static ThreadLocal<List<Report>> reports =
      new ThreadLocal<List<Report>>() {
        @Override
        protected List<Report> initialValue() {
          return new ArrayList<Report>();
        }
      };

  public static Location lexicalErrorAsLocation(String file, String error) {
    try {
      int lind = error.indexOf("line ");
      int cind = error.indexOf(", column ");
      int eind = error.indexOf(".", cind + 1);
      int line = Integer.parseInt(error.substring(lind + 5, cind));
      int column = Integer.parseInt(error.substring(cind + 9, eind));
      return new Location(file, line, column);
    } catch (RuntimeException e) {
      return new Location(file, 0, 0);
    }
  }
}
