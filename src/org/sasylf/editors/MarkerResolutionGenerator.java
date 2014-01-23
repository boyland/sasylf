package org.sasylf.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.sasylf.Marker;
import org.sasylf.Preferences;
import org.sasylf.actions.CheckProofsAction;
import org.sasylf.util.CompletionProposal;
import org.sasylf.util.CompletionProposalMarkerResolution;
import org.sasylf.util.EclipseUtil;

import edu.cmu.cs.sasylf.ast.Errors;

/**
 * Resolution of SASyLF problems.
 * This class handles both resolution or markers and quick fix for annotations.
 */
public class MarkerResolutionGenerator implements IMarkerResolutionGenerator2 {

  private static final IMarkerResolution[] NO_MARKER_RESOLUTIONS = new IMarkerResolution[0];

  private static class MyCompletionProposal extends CompletionProposal {

    private final IResource resource;
    
    public MyCompletionProposal(IResource res, String replacementString,
        int replacementOffset, int replacementLength, int cursorPosition,
        Image image, String displayString,
        IContextInformation contextInformation, String additionalProposalInfo) {
      super(replacementString, replacementOffset, replacementLength, cursorPosition,
          image, displayString, contextInformation, additionalProposalInfo);
      resource = res;
    }

    @Override
    public void apply(IDocument document) {
      super.apply(document);
      CheckProofsAction.analyzeSlf(resource,document);
    }
    
  }
  public MarkerResolutionGenerator() {
  }

  @Override
  public IMarkerResolution[] getResolutions(IMarker marker) {
    IMarkerResolution[] result = NO_MARKER_RESOLUTIONS;
    if (hasResolutions(marker)) {
      IResource res = marker.getResource();
      if (res == null) return result;
      IDocument document = EclipseUtil.getDocumentFromResource(res);
      if (document == null) {
        System.err.println("Cannot find document for " + res);
        return result;
      }
      ICompletionProposal[] proposals = getProposals(document, marker);
      if (proposals != null) {
        int n = proposals.length;
        result = new IMarkerResolution[n];
        for (int i=0; i < n; ++i) {
          result[i] = new CompletionProposalMarkerResolution(document,proposals[i]);
        }
      }
    }
    return result;
  }

  @Override
  public boolean hasResolutions(IMarker marker) {
    return hasProposals(marker);
  }

  public static boolean hasProposals(IMarker marker) {
    if (marker == null || !marker.exists()) return false;
    Errors markerType = null;
    String fixInfo;
    try {
      String type = (String) marker.getAttribute(Marker.SASYLF_ERROR_TYPE);
      if (type != null) markerType = Errors.valueOf(type);
      fixInfo = (String) marker.getAttribute(Marker.SASYLF_ERROR_INFO);
    } catch (CoreException e) {
      e.printStackTrace();
      return false;
    }
    if (markerType == null || fixInfo == null) return false;
    
    switch (markerType) {
    default: break;
    case MISSING_CASE: return true;
    case EXTRANEOUS_ASSUMES:
    case MISSING_ASSUMES: 
    case ILLEGAL_ASSUMES:
    case ASSUMED_ASSUMES: return true;
    case RULE_NOT_THEOREM: 
    case THEOREM_NOT_RULE:
    case THEOREM_KIND_WRONG:
    case THEOREM_KIND_MISSING: return true;
    }
    // NO_DERIVATION
    return false;
  }
  
  public static ICompletionProposal[] getProposals(IDocument doc, IMarker marker) {
    Errors markerType = null;
    String fixInfo;
    int line;
    IRegion lineInfo = null;
    String lineText = "";
    try {
      String type = (String) marker.getAttribute(Marker.SASYLF_ERROR_TYPE);
      if (type != null) markerType = Errors.valueOf(type);
      fixInfo = (String) marker.getAttribute(Marker.SASYLF_ERROR_INFO);
      line = marker.getAttribute(IMarker.LINE_NUMBER, 0);
      if (line > 0) {
        lineInfo = doc.getLineInformation(line-1);
        lineText = doc.get(lineInfo.getOffset(), lineInfo.getLength());
      }
    } catch (CoreException e) {
      e.printStackTrace();
      return null;
    } catch (BadLocationException e) {
      System.err.println("unexpected bad location exception caught:");
      e.printStackTrace();
      return null;
    }
    if (markerType == null || fixInfo == null || line == 0) return null;
    /*System.out.println("getProposals(" + marker + ") with type=" + markerType + ", info = " + fixInfo);
    System.out.println("  line = " + line + ", region = " + lineText);*/
    List<CompletionProposal> proposals = new ArrayList<CompletionProposal>();

    String[] split = fixInfo.split("\n");

    String lineIndent;
    {
      int i;
      for (i=0; i < lineText.length(); ++i) {
        int ch = lineText.charAt(i);
        if (ch == ' ' || ch == '\t') continue;
        break;
      }
      lineIndent = lineText.substring(0,i);
    }
    int indentAmount = Preferences.getFormatterIndentSize();
    String indent = "    ";
    if (indentAmount >= 0 && indentAmount <= 8) {
      indent = "        ".substring(0,indentAmount);
    }
    
    IResource res = marker.getResource();
    String extraIndent = "";
    
    try {
      String nl = doc.getLineDelimiter(line);
      IRegion old = new FindReplaceDocumentAdapter(doc).find(lineInfo.getOffset(), split[0], true, false, true, false);

      switch (markerType) {
      default: break;
      case MISSING_CASE:
        int newCursor;
        String newText;
        String descr;
        if (split.length == 1) { // syntax case
          descr = fixInfo;
          newText = lineIndent + indent + "case " + fixInfo + " is" + nl +
              lineIndent + indent + indent + "proof by unproved" + nl +
              lineIndent + indent + "end case" + nl;
          newCursor = lineIndent.length() + indentAmount + 5;
        } else {
          StringBuilder sb = new StringBuilder();
          sb.append(lineIndent);
          sb.append(indent);
          sb.append("case rule");
          sb.append(nl);
          newCursor = sb.length();
          for (int i=0; i < split.length; ++i) {
            sb.append(lineIndent);
            sb.append(indent);
            sb.append(indent);
            if (i != split.length-2)  sb.append("_: ");
            sb.append(split[i]);
            sb.append(nl);
          }
          sb.append(lineIndent);
          sb.append(indent);
          sb.append("is");
          sb.append(nl);
          sb.append(lineIndent);
          sb.append(indent);
          sb.append(indent);
          sb.append("proof by unproved");
          sb.append(nl);
          sb.append(lineIndent);
          sb.append(indent);
          sb.append("end case");
          sb.append(nl);
          newText = sb.toString();
          String ruleLine = split[split.length-2];
          descr = ruleLine.substring(ruleLine.indexOf(' ')+1);
        }
        proposals.add(new MyCompletionProposal(res, newText, doc.getLineOffset(line), 0, newCursor, 
            null, "insert case for " + descr, null, fixInfo));
        break;
      case ILLEGAL_ASSUMES:
      case EXTRANEOUS_ASSUMES:
        if (old != null) {
          proposals.add(new MyCompletionProposal(res,"",old.getOffset(), old.getLength(), 0, 
              null, "remove '" + split[0] +"'", null, null));
        }
        // fall through
      case RULE_NOT_THEOREM: 
      case THEOREM_NOT_RULE:
      case THEOREM_KIND_WRONG:
      case THEOREM_KIND_MISSING:
        if (old != null) {
          if (split.length > 1 && split[1].length() > 0) {
            proposals.add(new MyCompletionProposal(res, split[1], old.getOffset(), old.getLength(),0,
                null, "replace '" + split[0] +"' with '" + split[1] + "'", null, null));
          }
        }
        break;
      case ASSUMED_ASSUMES:
        extraIndent = indent;
        // fall through
      case MISSING_ASSUMES: 
        newText = lineIndent + extraIndent + fixInfo;
        proposals.add(new MyCompletionProposal(res,newText+doc.getLineDelimiter(line), doc.getLineOffset(line), 0, newText.length(), 
            null, "insert '" + fixInfo + "'", null, null));
        break;
      }
    } catch (BadLocationException e) {
      System.err.println("unexpected bad location exception caught:");
      e.printStackTrace();
      return null;
    }
 
    if (proposals.size() == 0) return null;
    
    return proposals.toArray(new CompletionProposal[proposals.size()]);
  }
}
