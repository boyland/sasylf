/**
 * 
 */
package org.sasylf.editors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.statushandlers.StatusManager;
import org.sasylf.Activator;
import org.sasylf.IDEProof;

import edu.cmu.cs.sasylf.ast.Element;
import edu.cmu.cs.sasylf.ast.Rule;
import edu.cmu.cs.sasylf.ast.RuleLike;
import edu.cmu.cs.sasylf.ast.Theorem;

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
		IStatus status = new Status(IStatus.WARNING,Activator.PLUGIN_ID, reason);
		StatusManager.getManager().handle(status);
		return NO_COMPLETIONS;
	}

	private IContextInformation[] noInformation(String reason) {
		//System.out.println("No information: " + reason);
		failureReason = reason;
		return NO_CONTEXTS;
	}

	private IDEProof getProof() {
		IResource res = ResourceUtil.getResource(editor.getEditorInput());
		if (res == null) return null;
		return IDEProof.getProof(res);
	}

	private List<Map.Entry<String,RuleLike>> findMatches(String type, String pattern) {
		IDEProof p = getProof();
		if (p == null || p.getCompilation() == null) {
			return Collections.emptyList();
		}

		List<Map.Entry<String,RuleLike>> result = new ArrayList<Map.Entry<String,RuleLike>>();
		for (Map.Entry<String,RuleLike> e : p.findRuleLikeByPrefix(pattern).entrySet()) {
			RuleLike rl = e.getValue();
			if (type.equals("rule")) {
				if (rl instanceof Rule) result.add(e);
			} else if (type.equals("lemma") || type.equals("theorem")) {
				if (rl instanceof Theorem) result.add(e);
			}
		}
		return result;
	}

	private String findPremiseHelp(String type, String name, int numArg) {
		// System.out.println("content assist called for "+type + " " + name);
		IDEProof p = getProof();
		if (p == null || p.getCompilation() == null) {
			failureReason = "Proof is not checked or has syntax errors";
			return null;
		}
		RuleLike result = p.findRuleLikeByName(name);
		if (result == null) {
			failureReason = "can find no " + type + " named " + name;
			return null;
		}
		if (result.getPremises().size() <= numArg) {
			failureReason = type + " " + name + " has only " + 
					result.getPremises().size() + ", can't find " + (numArg+1);
			return null;
		}
		String argString = result.getPremises().get(numArg).toString();
		if (numArg + 1 < result.getPremises().size()) argString += " ...";
		return argString;
	}

	private ICompletionProposal makeCompletionProposal(String name, RuleLike match, int cursor, String prefix) {
		int len = prefix.length();
		int newCursor = name.length();
		List<? extends Element> inputs = match.getPremises();
		// System.out.println("split = " + Arrays.toString(inputs));
		StringBuilder repl = new StringBuilder();
		IContextInformation info = null;
		repl.append(name);
		if (!inputs.isEmpty()) {
			repl.append(" on ");
			newCursor += 4;
			String extra = " ...";
			Element in = inputs.get(0);
			info = new ContextInformation("input 1", in + extra);
		}
		/*if (info != null) {
      System.out.println("info = (" + info.getContextDisplayString() + "," + info.getInformationDisplayString() + ")");
    } else {
      System.out.println("info = " + info);
    }*/
		return new CompletionProposal(repl.toString(), cursor-len, len, newCursor, null, name, info, match.toString());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
	 */
	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
			int offset) {
		IDocument doc = viewer.getDocument();
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
				return noProposals("no 'by rule/lemma/theorem' on line");
			}
			/* // hack for whitespace
      if (Character.isWhitespace(doc.getChar(offset-1))) {
        pieces[pieces.length-2] = pieces[pieces.length-1];
        pieces[pieces.length-1] = "";
      }*/
			//System.out.println("looking for " + pieces[pieces.length-2] + " for '" + pieces[pieces.length-1] + "'");
			List<Map.Entry<String, RuleLike>> matches;
			String key = pieces[pieces.length-2];
			String prefix = pieces[pieces.length-1];
			matches = findMatches(key, prefix);
			if (matches == null || matches.size() == 0) {
				return noProposals("no " + key + " starting with " + prefix + " found");
			}
			ICompletionProposal[] result = new ICompletionProposal[matches.size()];
			for (int i=0; i < result.length; ++i) {
				Entry<String, RuleLike> entry = matches.get(i);
				result[i] = makeCompletionProposal(entry.getKey(),matches.get(i).getValue(),offset,prefix);
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
				return noInformation("no 'by rule/lemma/theorem on' on line");
			}
			// System.out.println("looking for " + pieces[0] + " for '" + pieces[1] + "'");
			int commaCount = 0;
			for (int i=3; i < pieces.length; ++i) {
				int n = pieces[i].length();
				for (int j=0; j < n; ++j) {
					if (pieces[i].charAt(j) == ',') ++commaCount;
				}
			}
			String argString = findPremiseHelp(pieces[0],pieces[1],commaCount);
			if (argString == null) {
				return NO_CONTEXTS;
			}
			ContextInformation info = new ContextInformation("input " + (commaCount+1),argString);
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
				@Override
				public void run() {
					assistant.showContextInformation();
				}
			});
			return false;
		}

	}

}
