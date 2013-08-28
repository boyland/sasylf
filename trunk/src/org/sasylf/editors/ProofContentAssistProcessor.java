/**
 * 
 */
package org.sasylf.editors;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.widgets.Display;
import org.sasylf.editors.propertyOutline.ProofElement;
import org.sasylf.editors.propertyOutline.ProofOutline;

/**
 * @author boyland
 *
 */
public class ProofContentAssistProcessor implements IContentAssistProcessor {

  private final ProofEditor editor;
  private final ContentAssistant assistant;
  private final IContextInformationValidator validator = new MyValidator();
  private String failureReason;
  private static final IContextInformation[] NO_CONTEXTS = { };
  private static final char[] NO_CHARS = { };
  private static final ICompletionProposal[] NO_COMPLETIONS = { };
  
  public ProofContentAssistProcessor(ProofEditor pe, ContentAssistant assist) {
    editor = pe;
    assistant = assist;
  }

  private ICompletionProposal[] noProposals(String reason) {
    //System.out.println("No completions: " + reason);
    failureReason = reason;
    return NO_COMPLETIONS;
  }
  
  private IContextInformation[] noInformation(String reason) {
    //System.out.println("No information: " + reason);
    failureReason = reason;
    return NO_CONTEXTS;
  }
  
  private ICompletionProposal makeCompletionProposal(String match, int cursor, String prefix) {
    int colon = match.indexOf(':');
    String name = (colon < 0) ? match : match.substring(0, colon);
    String type = (colon < 0) ? "" : match.substring(colon+2);
    int len = prefix.length();
    int newCursor = name.length();
    String[] inputs = type.split("forall");
    // System.out.println("split = " + Arrays.toString(inputs));
    StringBuilder repl = new StringBuilder();
    IContextInformation info = null;
    repl.append(name);
    if (inputs.length > 1) {
      repl.append(" on ");
      newCursor += 4;
      String extra = " ...";
      if (inputs.length == 2) {
        inputs[1] = inputs[1].split("exists")[0];
        extra = "";
      }
      inputs[1] = inputs[1].trim();
      info = new ContextInformation("input 1", inputs[1] + extra);
    }
    /*if (info != null) {
      System.out.println("info = (" + info.getContextDisplayString() + "," + info.getInformationDisplayString() + ")");
    } else {
      System.out.println("info = " + info);
    }*/
    return new CompletionProposal(repl.toString(), cursor-len, len, newCursor, null, name, info, type);
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
   */
  @Override
  public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
      int offset) {
    IDocument doc = viewer.getDocument();
    ProofOutline outline = editor.getProofOutline();
    try {
      /* Attempt to work around, bug that auto context information doesn't work.
      if (doc.getChar(offset-1) == ',') {
        IContextInformation[] info = this.computeContextInformation(viewer, offset);
        if (info == null || info.length == 0) return NO_COMPLETIONS;
        return new ICompletionProposal[]{
            new CompletionProposal(" ",offset,0,1,null,null,info[0],null)};
      }
      */
      IRegion lineInfo = doc.getLineInformationOfOffset(offset);
      String line = doc.get(lineInfo.getOffset(), lineInfo.getLength()).substring(0,offset-lineInfo.getOffset());
      String[] pieces = line.split(" ",-1);
      if (pieces.length < 3) {
        return noProposals("no 'by rule' or 'by theorem' on line");
      }
      /* // hack for whitespace
      if (Character.isWhitespace(doc.getChar(offset-1))) {
        pieces[pieces.length-2] = pieces[pieces.length-1];
        pieces[pieces.length-1] = "";
      }*/
      //System.out.println("looking for " + pieces[pieces.length-2] + " for '" + pieces[pieces.length-1] + "'");
      List<String> matches;
      String key = pieces[pieces.length-2];
      String prefix = pieces[pieces.length-1];
      matches = outline.findContentAssist(key, prefix);
      if (matches == null || matches.size() == 0) {
        return noProposals("no " + key + " starting with " + prefix + " found");
      }
      ICompletionProposal[] result = new ICompletionProposal[matches.size()];
      for (int i=0; i < result.length; ++i) {
        result[i] = makeCompletionProposal(matches.get(i),offset,prefix);
      }
      failureReason = null;
      return result;
    } catch (BadLocationException e) {
      return noProposals("internal error: " + e.toString());
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer, int)
   */
  @Override
  public IContextInformation[] computeContextInformation(ITextViewer viewer,
      int offset) {
    // content assist on parameter values (premises to rules, inputs to theorems)
    IDocument doc = viewer.getDocument();
    ProofOutline outline = editor.getProofOutline();
    try {
      IRegion lineInfo = doc.getLineInformationOfOffset(offset);
      String line = doc.get(lineInfo.getOffset(), lineInfo.getLength()).substring(0,offset-lineInfo.getOffset());
      int byIndex = line.lastIndexOf(" by ");
      if (byIndex < 0) {
        return noInformation("cannot find 'by' in line");
      }
      String[] pieces = line.substring(byIndex+4).split(" ");
      if (pieces.length < 3 || !pieces[2].equals("on")) {
        // System.out.println("pieces = " + Arrays.toString(pieces));
        return noInformation("no 'by rule on' or 'by theorem on' on line");
      }
      // System.out.println("looking for " + pieces[0] + " for '" + pieces[1] + "'");
      ProofElement pe = outline.findProofElementByName(pieces[1]);
      if (pe == null) {
        return noInformation("no information for " + pieces[1]);
      }
      String[] template = pe.getContent().split("forall");
      template[template.length-1] = template[template.length-1].split("exists")[0];
      int commaCount = 0;
      for (int i=3; i < pieces.length; ++i) {
        int n = pieces[i].length();
        for (int j=0; j < n; ++j) {
          if (pieces[i].charAt(j) == ',') ++commaCount;
        }
      }
      if (commaCount+1 >= template.length) {
        return noInformation("Only " + template.length + " parameters expected, not " + (1+commaCount));
      }
      String extra = (commaCount+2 == template.length) ? "" : " ...";
      ContextInformation info = new ContextInformation("input " + (commaCount+1),template[commaCount+1].trim() + extra);
      // System.out.println("info = (" + info.getContextDisplayString() + "," + info.getInformationDisplayString() + ")");
      failureReason = null;
      return new IContextInformation[]{ info};
    } catch (BadLocationException e) {
      return noInformation("internal error: " + e.toString());
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
   */
  @Override
  public char[] getCompletionProposalAutoActivationCharacters() {
    return NO_CHARS;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
   */
  @Override
  public char[] getContextInformationAutoActivationCharacters() {
    return new char[]{','};
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
   */
  @Override
  public String getErrorMessage() {
    return failureReason;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
   */
  @Override
  public IContextInformationValidator getContextInformationValidator() {
    return validator;
  }
  
  private class MyValidator implements IContextInformationValidator {

    private IContextInformation information;
    private ITextViewer textViewer;
    //private int origOffset;
    
    @Override
    public void install(IContextInformation info, ITextViewer viewer, int offset) {
      information = info;
      textViewer = viewer;
      //origOffset = offset;
    }

    @Override
    public boolean isContextInformationValid(int offset) {
      IContextInformation[] newInfo = computeContextInformation(textViewer, offset);
      if (newInfo == null || newInfo.length == 0) return false;
      for (IContextInformation info : newInfo) {
        if (info.equals(information)) return true;
      }
      
      Display.getDefault().asyncExec(new Runnable() {
        public void run() {
          assistant.showContextInformation();
        }
      });
      return false;
    }
    
  }

}
