package edu.cmu.cs.sasylf.util;

import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Marker;
import edu.cmu.cs.sasylf.util.VSDocument;
import edu.cmu.cs.sasylf.util.VSMarker;
import edu.cmu.cs.sasylf.util.VSRegion;
import java.util.HashMap;

/**
 * The makeQuickfix method implements the method for quickfixes used in
 * Eclipse, but it replaced the Eclipse classes with custom made classes that
 * mirror the Eclipse ones.
 */
public class Quickfix {
  public HashMap<String, Object> makeQuickfix(VSDocument doc, VSMarker marker) {
    String fixInfo;
    int line;
    VSRegion lineInfo = null;
    String lineText = "";
    Errors markerType = (Errors)marker.getAttribute(Marker.SASYLF_ERROR_TYPE);
    String nl = doc.getLineDelimiter();

    fixInfo = (String)marker.getAttribute(Marker.SASYLF_ERROR_INFO);
    line = (int)marker.getAttribute(Marker.LINE_NUMBER, 0);
    if (line > 0) {
      lineInfo = doc.getLineInformation(line - 1);
      lineText = doc.get(lineInfo.getOffset(), lineInfo.getLength());
    }

    if (markerType == null || fixInfo == null || line == 0)
      return null;

    String[] split = fixInfo.split("\n", -1);

    String lineIndent;
    {
      int i;
      for (i = 0; i < lineText.length(); ++i) {
        int ch = lineText.charAt(i);
        if (ch == ' ' || ch == '\t')
          continue;
        break;
      }
      lineIndent = lineText.substring(0, i);
    }

    int indentAmount =
        doc.getLines()[line - 1].length() -
        doc.getLines()[line - 1].replaceAll("^\\s+", "").length();
    String indent = "    ";

    if (indentAmount >= 0 && indentAmount <= 8) {
      indent = "        ".substring(0, indentAmount);
    }

    String extraIndent = "";
    String newText;

    switch (markerType) {
    default:
      break;
    case DERIVATION_NOT_FOUND:
      int colon = split[0].indexOf(':');
      int useStart = (int)marker.getAttribute(Marker.CHAR_START, -1);
      int useEnd = (int)marker.getAttribute(Marker.CHAR_END, -1);
      String useName = doc.get(useStart, useEnd - useStart);
      String defName;

      if (colon >= 0) {
        defName = split[0].substring(0, colon);
      } else {
        defName = split[0];

        HashMap<String, Object> res = new HashMap<>();
        res.put("newText", defName);
        res.put("charStart", useStart);
        res.put("charEnd", useStart + useName.length());
        res.put("title", "replace '" + useName + " with '" + defName + "'");

        return res;
      }

      VSRegion prevLineInfo = lineInfo;
      // int diff = 0;
      // try {
      //   if (split.length > 1)
      //     diff = Integer.parseInt(split[1]);
      // } catch (RuntimeException ex) {
      //   // muffle array or number format
      // }
      // if (diff > 0)
      //   prevLineInfo = doc.getLineInformation(line - 1 - diff);
      // String prevLine =
      //     doc.get(prevLineInfo.getOffset(), prevLineInfo.getLength());
      // System.out.println(prevLine);
      // int prevStart;
      // for (prevStart = 0; prevStart < prevLine.length(); ++prevStart) {
      //   int ch = lineText.charAt(prevStart);
      //   if (ch == ' ' || ch == '\t')
      //     continue;
      //   break;
      // }
      // String prevIndent = lineText.substring(0, prevStart);
      newText = nl + indent + split[0] + " by unproved";

      String extra = "";

      if (!defName.equals(useName)) {
        if (useName.equals("_")) {
          extra = ", and replace '_' with '" + defName + "'";
        }
      }
      HashMap<String, Object> res = new HashMap<>();
      res.put("newText", newText);
      res.put("charStart", prevLineInfo.getOffset() + prevLineInfo.getLength());
      res.put("charEnd", prevLineInfo.getOffset() + prevLineInfo.getLength());
      res.put("title",
              "insert '" + split[0] + " by unproved' before this line" + extra);

      return res;
    }

    return null;
  }
}
