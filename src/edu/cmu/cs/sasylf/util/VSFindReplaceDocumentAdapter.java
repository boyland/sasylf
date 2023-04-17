package edu.cmu.cs.sasylf.util;

import edu.cmu.cs.sasylf.util.VSDocument;
import edu.cmu.cs.sasylf.util.VSRegion;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Allows for easier search and replace of the text document.
 */

public class VSFindReplaceDocumentAdapter {
  private final VSDocument doc;

  public VSFindReplaceDocumentAdapter(VSDocument doc) { this.doc = doc; }

  public VSRegion find(int offset, String search, boolean searchForward,
                       boolean caseSensitive, boolean wholeWord,
                       boolean regExSearch) {
    Pattern pattern =
        regExSearch
            ? Pattern.compile(findString,
                              caseSensitive ? 0 : Pattern.CASE_INSENSITIVE)
            : null;

    int searchDirection = forwardSearch ? 1 : -1;

    while (offset >= 0 && offset < document.getLength()) {
      String text = doc.get(offset, doc.getLength() - offset);
      int matchIndex;

      if (regExSearch) {
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
          matchIndex = offset + matcher.start();
        } else {
          matchIndex = -1;
        }
      } else {
        matchIndex = caseSensitive
                         ? text.indexOf(findString)
                         : text.toLowerCase().indexOf(findString.toLowerCase());

        if (matchIndex != -1) {
          matchIndex += offset;
        }
      }

      if (matchIndex != -1) {
        return new VSRegion(matchIndex, findString.length());
      }

      offset += searchDirection;
    }

    return null;
  }
}
