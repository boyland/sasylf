/**
 * 
 */
package org.sasylf.editors;

import java.util.LinkedList;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.jface.text.formatter.IFormattingStrategy;
import org.eclipse.jface.text.formatter.IFormattingStrategyExtension;
import org.sasylf.Options;

/**
 * Formatting SASyLF proofs according to simple rules.
 * Examples of formatting strategies out there are confusing:
 * some assume that one can ignore previous {@link #formatterStarts(IFormattingContext)}
 * calls; others assume that {@link #format()} will be called as many times
 * as the former method. Some assume that every call gets its own partition,
 * others that every call gets its own region to apply to.
 * TODO: Implement this class.  It's currently a NOP.
 */
public class ProofFormattingStrategy implements IFormattingStrategy,
    IFormattingStrategyExtension {

  // these lists are queues in synch with each other.
  private LinkedList<IDocument> documents;
  private LinkedList<IRegion> regions;
  private LinkedList<Map<?,?>> preferences;
  
  @Override
  public void formatterStarts(IFormattingContext context) {
    IDocument document = (IDocument) context.getProperty(FormattingContextProperties.CONTEXT_DOCUMENT);
    IRegion region = (IRegion) context.getProperty(FormattingContextProperties.CONTEXT_REGION);
    Map<?,?> prefs = (Map<?,?>) context.getProperty(FormattingContextProperties.CONTEXT_PREFERENCES);
    if (document == null || prefs == null) {
      System.out.println("no document ot prefes: nothing to do");
      return;
    }
    documents.addLast(document);
    regions.addLast(region);
    preferences.addLast(prefs);
  }

  @Override
  public void formatterStops() {
    documents.clear();
    regions.clear();
    preferences.clear();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.formatter.IFormattingStrategyExtension#format()
   */
  @Override
  public void format() {
    while (!documents.isEmpty()) {
      IDocument document = documents.removeFirst();
      IRegion region = regions.removeFirst();
      Map<?, ?> prefs = preferences.removeFirst();
      
      if (document.getLength() == 0) continue;
      
      if (region == null) region = new Region(0,document.getLength());
      else {
        int p = region.getOffset();
        int q = p + region.getLength();
        if (p >= document.getLength()) p = q = document.getLength()-1;
        else if (q >= document.getLength()) q = document.getLength()-1;
        IRegion firstLine, lastLine;
        try {
          firstLine = document.getLineInformationOfOffset(p);
          lastLine = document.getLineInformationOfOffset(q);
        } catch (BadLocationException e) {
          throw new AssertionError("not possible");
        }
        region = new Region(firstLine.getOffset(),lastLine.getOffset()-firstLine.getOffset()+lastLine.getLength());
      }
      format(document, region, prefs);
    }
  }


  /// the following two methods are vestigial and do not need to be implemented

  @Override
  public void formatterStarts(String initialIndentation) {
    throw new AssertionError("use the extension interface");
  }


  @Override
  public String format(String content, boolean isLineStart, String indentation,
      int[] positions) {
    throw new AssertionError("use the extension interface");
  }


  /// The formatting code
  protected void format(IDocument document, IRegion region, Map<?,?> preferences) {
    System.out.println("format called for region: " + region.getOffset() + " for " + region.getLength());
    System.out.println("  indent = " + Options.getOption(preferences, Options.FORMATTER_INDENT_SIZE));
  }
}
