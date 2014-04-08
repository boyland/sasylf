package org.sasylf.editors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;
import org.sasylf.Marker;
import org.sasylf.Preferences;
import org.sasylf.ProofChecker;
import org.sasylf.util.CompletionProposal;
import org.sasylf.util.CompletionProposalMarkerResolution;
import org.sasylf.util.EclipseUtil;
import org.sasylf.views.ProofElement;

import edu.cmu.cs.sasylf.util.Errors;

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
      // System.out.println("replacementString = " + replacementString + ", replacementOffset = " + replacementOffset);
      resource = res;
    }

    @Override
    public void apply(IDocument document) {
      super.apply(document);
      ProofChecker.analyzeSlf(resource,document);
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
    case WRONG_END:
    case INDUCTION_REPEAT: return true;
    case WRONG_MODULE_NAME:
    case WRONG_PACKAGE: return true;
    case EXTRA_CASE: return true;
    case PARTIAL_CASE_ANALYSIS: return true;
    case OTHER_JUSTIFIED: return true;
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
    
    ProofEditor proofEditor = null;
    {
      IResource res = marker.getResource();
      if (res instanceof IFile) {
        IWorkbench wb = PlatformUI.getWorkbench();
        if (wb != null) {
          IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
          if (win != null) {
            IWorkbenchPage page = win.getActivePage();
            if (page != null) {
              IEditorPart ep = ResourceUtil.findEditor(page, (IFile)res);
              if (ep instanceof ProofEditor) {
                // System.out.println("Found Proof Editor!");
                proofEditor = (ProofEditor)ep;
              }
            }
          }
        }
      }
    }

    //System.out.println("getProposals(" + marker + ") with type=" + markerType + ", info = " + fixInfo);
    //System.out.println("lineInfo = (" + lineInfo.getOffset() + ":" + lineInfo.getLength() + ")");
    //System.out.println("  line = " + line + ", region = " + lineText);
    List<CompletionProposal> proposals = new ArrayList<CompletionProposal>();

    String[] split = fixInfo.split("\n",-1);

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
      IRegion old = new FindReplaceDocumentAdapter(doc).find(lineInfo.getOffset(), split[0], true, true, false, false);
      /* Too conservative:
      if (old != null && lineInfo != null && old.getOffset() - lineInfo.getOffset() > lineInfo.getLength()) {
        old = null;
      }*/
      if (old == null) {
        if (split[0].equals(lineText)) {
          old = new Region(lineInfo.getOffset(),lineText.length());
        }
      }

      switch (markerType) {
      default: break;
      case MISSING_CASE:
        int newCursor;
        StringBuilder sb = new StringBuilder();
        if (fixInfo.indexOf("\n\n") == -1) { // syntax case
          int n = split.length-1;
          for (int i=0; i < n; ++i) {
            sb.append(lineIndent); sb.append(indent);
            sb.append("case ");
            sb.append(split[i]);
            sb.append(" is"); sb.append(nl);
            sb.append(lineIndent); sb.append(indent); sb.append(indent);
            sb.append("proof by unproved"); sb.append(nl);
            sb.append(lineIndent); sb.append(indent);
            sb.append("end case"); sb.append(nl); sb.append(nl);
          }
          newCursor = lineIndent.length() + indentAmount + 5;
        } else {
          newCursor = -1;
          boolean startCase = true;
          int n=split.length-1; // extra line at end
          for (int i=0; i < n; ++i) {
            if (startCase) {
              sb.append(lineIndent);
              sb.append(indent);
              sb.append("case rule");
              sb.append(nl);
              startCase = false;
            }
            if (split[i].length() == 0) {
              sb.append(lineIndent); sb.append(indent);
              sb.append("is"); sb.append(nl);
              sb.append(lineIndent); sb.append(indent);sb.append(indent);
              sb.append("proof by unproved"); sb.append(nl);
              sb.append(lineIndent); sb.append(indent);
              sb.append("end case"); sb.append(nl); sb.append(nl);
              startCase = true;
              continue;
            }
            if (newCursor == -1) newCursor = sb.length();
            sb.append(lineIndent); sb.append(indent); sb.append(indent);
            if (split[i].startsWith("---")) {
              String ruleName = split[i].split(" ")[1];
              if (proofEditor != null) {
                ProofElement pe = proofEditor.getProofOutline().findProofElementByName(ruleName);
                if (pe != null && pe.getCategory().equals("Rule")) {
                  String bar = pe.getLexicalInfo();
                  if (bar.length() >= 3 ) {
                    String prefix = bar.substring(0,3);
                    split[i] = prefix + prefix + bar + " " + ruleName;
                  }
                }
              }
            } else {
              sb.append("_: ");
            }
            sb.append(split[i]);
            sb.append(nl);
          }
        }
        String newText = sb.toString();
        proposals.add(new MyCompletionProposal(res, newText, doc.getLineOffset(line), 0, newCursor, 
            null, "insert missing case(s)", null, fixInfo));
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
      case INDUCTION_REPEAT:
      case WRONG_END:
      case WRONG_MODULE_NAME:
      case PARTIAL_CASE_ANALYSIS:
        if (old != null) {
          if (split.length > 1 && split[1].length() > 0) {
            proposals.add(new MyCompletionProposal(res, split[1], old.getOffset(), old.getLength(),0,
                null, "replace '" + split[0] +"' with '" + split[1] + "'", null, null));
          }
        }
        break;
      case WRONG_PACKAGE:
        if (split[0].length() == 0) {
          newText = split[1];
          proposals.add(new MyCompletionProposal(res,newText+doc.getLineDelimiter(line), doc.getLineOffset(line), 0, newText.length(),
              null, "insert '" + newText + "'", null, null));
        }
        // System.out.println("fixInfo = " + fixInfo + ", old = " + old + ", res = " + res + ", split = " + Arrays.toString(split));
        if (old != null && split.length > 1) {
          if (split[1].length() == 0) {
            proposals.add(new MyCompletionProposal(res,"",old.getOffset(), old.getLength(), 0, 
                null, "remove '" + split[0] +"'", null, null));
          } else {
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
      case EXTRA_CASE:
        if (proofEditor != null && lineInfo != null) {
          ProjectionAnnotationModel annotationModel = proofEditor.getProjectionAnnotationModel();
          Iterator<?> annos = annotationModel.getAnnotationIterator();
          while (annos.hasNext()) {
            Object anno = annos.next();
            if (anno instanceof ProjectionAnnotation) {
              Position enclosing = annotationModel.getPosition((ProjectionAnnotation)anno);
              if (enclosing == null) {
                // System.out.println("couldn't find position");
                continue;
              }
              if (lineInfo.getOffset() > enclosing.getOffset() ||
                  lineInfo.getOffset()+lineInfo.getLength() <= enclosing.getOffset()) {
                // System.out.println("Wrong position was at " + doc.get(enclosing.offset, enclosing.length));
                continue;
              }
              // System.out.println("Found " + doc.get(enclosing.offset, enclosing.length));
              IRegion endInfo = doc.getLineInformationOfOffset(enclosing.getOffset()+enclosing.getLength());
              proposals.add(new MyCompletionProposal(res, "", lineInfo.getOffset(), endInfo.getOffset()+endInfo.getLength()-lineInfo.getOffset(),0,
                  null, "remove case", null, null));
              break;
            }
          }
        }
        break;
      case OTHER_JUSTIFIED:
        if (lineInfo != null) {
          newText = " " + split[1];
          int holeStart = split[0].indexOf("...");
          String startPat = split[0].substring(0, holeStart);
          String endPat = split[0].substring(holeStart+3);
          int findStart = lineText.indexOf(startPat);
          // System.out.println("indexOf(" + startPat + ") = " + findStart + " in " + lineText);
          if (findStart < 0) break;
          int findEnd = lineText.indexOf(endPat, findStart);
          // System.out.println("indexOf(" + endPat + ") = " + findEnd + " in " + lineText);
          if (findEnd < 0) break;
          int oldStart = findStart + startPat.length();
          String oldText = lineText.substring(oldStart, findEnd);
          proposals.add(new MyCompletionProposal(res, newText, lineInfo.getOffset() + oldStart, oldText.length(), 0, null,
              "replace '" + oldText + "' with '" + newText + "'", null, null));
        }
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
