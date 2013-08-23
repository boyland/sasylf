package org.sasylf.editors;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

/**
 * Correct the indentation of a region of a document.
 */
public class SASyLFCorrectIndentStrategy extends SASyLFIndentStrategy {

  public SASyLFCorrectIndentStrategy() {
  }

  /**
   * Return delta in number of indents after this line, +1, 0, -1
   * @param line contents of this line.
   * @return
   * @throws  
   */
  protected int getNextLineIndentDelta(String line) {
    switch (inComment(line)) {
    case LINE:
    case LONG:
      return 0;
    default:
      break;
    }
    if (line.equals("is")) {
      return +1;
    } else if (line.endsWith(":") || line.startsWith("case ") || line.equals("case") || line.startsWith("terminals ") || line.startsWith("is" )) {
      return +1;
    } else {
      return 0;
    }
  }
  
  protected int getThisLineIndentDelta(String line) {
    if (line.equals("is") || line.startsWith("end ")) return -1;
    return 0;
  }
  
  protected int getPreviousSpaces(IDocument document, int lineNum) throws BadLocationException {
    int n;
    IRegion bounds;
    do {
      if (lineNum == 0) return 0;
      --lineNum;
      bounds = document.getLineInformation(lineNum);
      n = 0;
      for (; n < bounds.getLength(); ++n) {
        if (!Character.isWhitespace(document.getChar(n+bounds.getOffset()))) break;
      }
      if (n < bounds.getLength()) break;
    } while (true);
    
    String line = document.get(bounds.getOffset(), bounds.getLength()).trim();
    return n + getNextLineIndentDelta(line)*getIndentUnit();
  }
  
  /**
   * Correct indentation over lines of a document.
   * @param document document to correct, must not be null
   * @param first first line (not offset) to check
   * @param count number of lines to check.
   * @return text edit to correct all indentation on given lines.
   */
  public TextEdit correctIndentation(IDocument document, int first, int count) {
    int indentUnit = getIndentUnit();
    MultiTextEdit result = new MultiTextEdit();
    int i = first;
    try {
      int currentSpaces = getPreviousSpaces(document,first);
      do {
        IRegion linfo = document.getLineInformation(i);
        int off = linfo.getOffset();
        int len = linfo.getLength();
        int n = 0;
        while (n < len && Character.isWhitespace(document.getChar(off+n))) {
          ++n;
        }
        if (n < len) {
          String line = document.get(off+n,len-n).trim();
          currentSpaces += getThisLineIndentDelta(line)*indentUnit;
          // special case: "terminals" doesn't have a good terminal
          if (line.equals("syntax")) currentSpaces = 0;
          if (currentSpaces < 0) currentSpaces = 0;
          if (currentSpaces != n) {
            // System.out.println("Line #" + i + " has indent " + n + ", should be " + currentSpaces);
            if (n < currentSpaces) {
              result.addChild(new InsertEdit(off+n,indentToSpaces(currentSpaces-n)));
            } else {
              result.addChild(new DeleteEdit(off+currentSpaces,n-currentSpaces));
            }
          }
          currentSpaces += getNextLineIndentDelta(line)*indentUnit;
        }
        ++i; --count;
      } while (count > 0);
    } catch (BadLocationException e) {
      e.printStackTrace();
      return null;
    }
    return result;
  }
}
