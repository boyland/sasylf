package edu.cmu.cs.sasylf.util;

import edu.cmu.cs.sasylf.util.VSRegion;

/**
 * This simulates the IDocument class in Eclipse.
 */

public class VSDocument {
  private final String body;
  private final String[] lines;
  private final int indentSize;
  private final String nl;

  public VSDocument(String body, int indentSize, String nl) {
    this.body = body;
    this.lines = body.split("\r\n|\r|\n");

    this.indentSize = indentSize;
    this.nl = nl;
  }

  public String get(int start, int length) {
    return body.substring(start, start + length);
  }

  public int getLength() { return body.length(); }

  public VSRegion getLineInformation(int line) {
    return new VSRegion(this.getLineOffset(line), lines[line].length());
  }

  public int getLineOffset(int line) {
    int offset = 0;

    for (int i = 0; i < line; ++i) {
      offset += lines[i].length() + ((i != line - 1) ? 1 : 0);
    }

    return offset;
  }

  public String[] getLines() { return lines; }

  public String getBody() { return body; }

  public int getIndentSize() { return indentSize; }

  public String getLineDelimiter() { return nl; }
}
