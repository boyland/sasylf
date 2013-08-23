package org.sasylf.editors;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.sasylf.Options;

/**
 * A utility class sharing code between auto-indent and indent correction classes.
 */
public class SASyLFIndentStrategy extends DefaultIndentLineAutoEditStrategy {

  private static StringBuilder spaces = new StringBuilder("    ");

  public int getIndentUnit() {
    int result = Options.getIntOption(Options.FORMATTER_INDENT_SIZE);
    if (result < 0) return 0;
    return result;
  }

  public String indentToSpaces(int i) {
    while (spaces.length() < i) {
      spaces.append("        ");
    }
    return spaces.substring(0, i);
  }

  /**
   * Get the start offset of the line that this offset is in.
   * @param d document offset is in, must not be null
   * @param offset offset within document
   * @throws BadLocationException if a problem happens
   */
  protected int getLineStart(IDocument d, int offset)
      throws BadLocationException {
        int p = (offset == d.getLength() ? offset - 1 : offset);
        return d.getLineInformationOfOffset(p).getOffset();
      }

  protected int getLineLength(IDocument d, int offset)
      throws BadLocationException {
        int p = (offset == d.getLength() ? offset - 1 : offset);
        IRegion r = d.getLineInformationOfOffset(p);
        return r.getOffset() + r.getLength() - offset;
        
      }

  protected SASyLFIndentStrategy() {
    super();
  }

  /**
   * Return the line up to the point passed, trimming left indentation.
   * @param d
   * @param offset
   * @return string from start of line to the point given.
   * @throws BadLocationException
   */
  protected String getLineUpTo(IDocument d, int offset)
      throws BadLocationException {
        int start = getLineStart(d, offset);
        while (start < offset && Character.isWhitespace(d.getChar(start))) {
          ++start;
        }
        String line = d.get(start, offset-start);
        return line;
      }

  protected enum CommentStatus { NONE, LINE, LONG }

  protected CommentStatus inComment(String line) {
    if (line.startsWith("*")) { // require /* comments to have * leaders on lines for indenting to work
      line = "/" + line;
    }
    int i = 0;
    do {
      int p1 = line.indexOf("/*",i);
      int p2 = line.indexOf("//",i);
      if (p1 < 0 && p2 < 0) return CommentStatus.NONE;
      if (p1 < 0 || (p2 >= 0 && p2 < p1)) return CommentStatus.LINE;
      i = p1+2;
      int p3 = line.indexOf("*/",i);
      if (p3 < 0) return CommentStatus.LONG;
      i = p3+2;
    } while (true);
  }

}