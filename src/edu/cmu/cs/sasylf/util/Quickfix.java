package edu.cmu.cs.sasylf.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import edu.cmu.cs.sasylf.util.Errors;

// import org.sasylf.Preferences;

public class Quickfix {
  public String makeQuickfix(String jsonData) {
    Errors markerType = null;
    String fixInfo;
    int line;
    String lineText = "";

    ObjectMapper objectMapper;
    String doc;
    JsonNode jsonNode;
    try {
      objectMapper = new ObjectMapper();
      jsonNode = objectMapper.readTree(jsonData);
      doc = jsonNode.get("textDocument").asText();
    }
    catch (JsonProcessingException e) {
      e.printStackTrace();
      return "Error";
    }
    // try {
    //   String type = jsonNode.get("errorType").asText();
    //   if (type != null)
    //     markerType = Errors.valueOf(type);
    //   fixInfo = jsonNode.get("errorInfo").asText();
    //   line = jsonNode.get("lineNumber").asInt();
    //   lineText = doc.split("\\r?\\n")[line];
    // } catch (CoreException e) {
    //   e.printStackTrace();
    //   return null;
    // } catch (BadLocationException e) {
    //   System.err.println("unexpected bad location exception caught:");
    //   e.printStackTrace();
    //   return null;
    // }
    String type = jsonNode.get("errorType").asText();
    if (type != null)
      markerType = Errors.valueOf(type);
    fixInfo = jsonNode.get("errorInfo").asText();
    line = jsonNode.get("lineNumber").asInt();
    lineText = doc.split("\\r?\\n")[line];
    if (markerType == null || line == 0 || lineText == "")
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
    int indentAmount = jsonNode.get("indentAmount").asInt();
    String indent = "    ";
    if (indentAmount >= 0 && indentAmount <= 8) {
      indent = "        ".substring(0, indentAmount);
    }

    String nl = jsonNode.get("lineDelimeter").asText();
    String newText;
    int colon = split[0].indexOf(':');
    int useStart = jsonNode.get("charStart").asInt();
    int useEnd = jsonNode.get("charStart").asInt();
    String useName = doc.substring(useStart, useEnd);
    String defName;

    JsonNodeFactory factory = JsonNodeFactory.instance;
    if (colon >= 0) {
      defName = split[0].substring(0, colon);
    } else {
      defName = split[0];

      ObjectNode res = factory.objectNode();
      res.put("newText", defName);
      res.put("charStart", Integer.toString(useStart));
      res.put("charEnd", Integer.toString(useStart + useName.length()));
      res.put("title", "replace '" + useName + " with '" + defName + "'");

      try {
        String resString = objectMapper.writeValueAsString(res);
        System.out.println(resString);
        return resString;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    int diff = 0;
    try {
      if (split.length > 1)
        diff = Integer.parseInt(split[1]);
    } catch (RuntimeException ex) {
      // muffle array or number format
    }

    String prevLine = doc.split("\\r?\\n")[line - diff];
    int prevStart;
    for (prevStart = 0; prevStart < prevLine.length(); ++prevStart) {
      int ch = lineText.charAt(prevStart);
      if (ch == ' ' || ch == '\t')
        continue;
      break;
    }
    String prevIndent = lineText.substring(0, prevStart);
    newText = prevIndent + split[0] + " by unproved" + nl;

    String extra = "";
    if (!defName.equals(useName)) {
      if (useName.equals("_")) {
        extra = ", and replace '_' with '" + defName + "'";
      }
    }

    int offset = 0;
    for (int i = 0; i < line - diff; ++i) {
      offset += doc.split("\\r?\\n")[i].length();
    }

    ObjectNode res = factory.objectNode();
    res.put("newText", newText);
    res.put("charStart", offset);
    res.put("charEnd", offset);
    res.put("title",
            "insert '" + split[0] + " by unproved' before this line" + extra);

    try {
      String resString = objectMapper.writeValueAsString(res);
      System.out.println(resString);
      return resString;
    } catch (Exception e) {
      e.printStackTrace();
      return "Error";
    }
  }
}
