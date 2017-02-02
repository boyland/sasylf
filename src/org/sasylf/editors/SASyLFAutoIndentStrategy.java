/**
 * 
 */
package org.sasylf.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.swt.widgets.Display;

/**
 * Auto indent for proof files.
 */
public class SASyLFAutoIndentStrategy extends SASyLFIndentStrategy {

	public SASyLFAutoIndentStrategy() { }

	private class DelayedEvent extends DocumentEvent {
		DelayedEvent(IDocument d, int off, int length, String text, IDocumentListener add) {
			super(d, off,length,text);
			fAdditional = add;
		}
		IDocumentListener fAdditional;
		public void execute() throws BadLocationException {
			if (fAdditional != null) fAdditional.documentAboutToBeChanged(this);
			getDocument().replace(fOffset,fLength,fText);
			if (fAdditional != null) fAdditional.documentChanged(this);
		}
	}

	private Map<IDocument,List<DelayedEvent>> todo = new HashMap<IDocument,List<DelayedEvent>>();
	private DelayedEvent[] empty = new DelayedEvent[0];

	private IDocumentListener listener = new IDocumentListener() {

		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
			// do nothing
		}

		@Override
		public void documentChanged(DocumentEvent event) {
			System.out.println("running delayed events");
			List<DelayedEvent> events = todo.get(event.getDocument());
			if (events == null || events.isEmpty()) return;
			event.getDocument().removeDocumentListener(this);
			DelayedEvent[] copy = events.toArray(empty);
			events.clear();
			// int caret = event.getDocument().
			try {
				for (DelayedEvent e : copy) {
					e.execute();
				}
			} catch (BadLocationException e) {
				System.out.println("bad event replayed: " + e);
				// muffle
			}
		}  
	};

	private void invokeReplaceLater(IDocument doc, int offset, int length, String text, IDocumentListener l) {
		final DelayedEvent event = new DelayedEvent(doc,offset,length,text,l);
		if (l == null) {
			// System.err.println("No owner of document command, so delay may cause problems.");
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					try {
						event.execute();
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}
			});
			return;
		} else {
			// System.out.println("Owner is " + l);
		}
		List<DelayedEvent> events = todo.get(doc);
		if (events == null) {
			events = new ArrayList<DelayedEvent>();
			todo.put(doc, events);
		}
		events.add(event);
		if (events.size() == 1) doc.addDocumentListener(listener);
	}

	private void doDefaultNewlineAutoIndent(IDocument d, DocumentCommand c) {
		super.customizeDocumentCommand(d, c);
	}

	private void doModifiedNewlineAutoIndent(IDocument d, DocumentCommand c, int delta) {
		doDefaultNewlineAutoIndent(d,c);
		if (delta == 0) return;
		if (delta > 0) {
			c.text += indentToSpaces(delta*getIndentUnit());
		} else {
			// NB: code untested and unused currently
			c.text = c.text.substring(c.text.length() + delta*getIndentUnit());
		}
	}

	private void addUndent(IDocument d, DocumentCommand c) throws BadLocationException {
		int st = getLineStart(d,c.offset);
		int in = getIndentUnit();
		if (c.offset - st < in) return;
		// System.out.println("comparing '" + d.get(st,in) + "' with '" + indentToSpaces(in) + "'");
		if (d.get(st, in).equals(indentToSpaces(in))) {
			this.invokeReplaceLater(d, st, in, "",c.owner);
			// System.out.println("delayed undent");
		}
	}

	/**
	 * @param d
	 * @param offset
	 * @param thisIndent
	 * @throws BadLocationException
	 */
	protected boolean notAlreadyUndented(IDocument d, int offset, int thisIndent)
			throws BadLocationException {
		IRegion thisLineInfo = d.getLineInformationOfOffset(offset);
		if (thisLineInfo.getOffset() == 0) {
			// System.out.println("on first line");
			return false;
		}
		IRegion prevLineInfo = d.getLineInformationOfOffset(thisLineInfo.getOffset()-1);
		if (prevLineInfo.getLength() < thisIndent) {
			// System.out.println("previous line '" + d.get(prevLineInfo.getOffset(), prevLineInfo.getLength())+ "'is shorter than us: " + thisIndent);
			return false;
		}
		for (int k=0; k < thisIndent; ++k) {
			char c1 = d.getChar(prevLineInfo.getOffset()+k);
			char c2 = d.getChar(thisLineInfo.getOffset()+k);
			if (c1 != c2) {
				// System.out.println("doesn't match previous line at char #" + k + " '" + c1 + "' != '" + c2 + "'");
				return false;
			}
		}
		// System.out.println("all previous characters match: [0," + thisIndent + ")");
		if (prevLineInfo.getLength() > thisIndent && Character.isWhitespace(d.getChar(prevLineInfo.getOffset()+thisIndent))) {
			// System.out.println("already undented");
			return false;
		}
		return true;
	}

	private void doNewlineAutoIndent(IDocument d, DocumentCommand c) throws BadLocationException {
		String line = (getLineUpTo(d, c.offset) + c.text).trim();
		switch (inComment(line)) {
		case LINE:
			doDefaultNewlineAutoIndent(d, c);
			return;
		case LONG:
			doDefaultNewlineAutoIndent(d, c);
			if (line.startsWith("/*")) c.text += " * "; 
			else c.text += "* ";
			return;
		default:
			break;
		}
		if (line.equals("is")) {
			// we want to avoid the "is" undenting over and over
			int thisIndent = 0;
			IRegion thisLineInfo = d.getLineInformationOfOffset(c.offset);
			// System.out.println("thisLineInfo = " + thisLineInfo.getOffset() + ":" + thisLineInfo.getLength());
			while (thisIndent < thisLineInfo.getLength() && Character.isWhitespace(d.getChar(thisLineInfo.getOffset()+thisIndent))) {
				++thisIndent;
			}
			// System.out.println("  thisIndent in '" + d.get(thisLineInfo.getOffset(), thisLineInfo.getLength()) +"' = " + thisIndent);
			if (thisIndent == thisLineInfo.getLength()) {
				int k=0;
				while (k < c.text.length() && Character.isWhitespace(c.text.charAt(k))) {
					++k;
				}
				thisIndent += k;
			}
			if (notAlreadyUndented(d,c.offset,thisIndent)) {
				addUndent(d,c);
				doDefaultNewlineAutoIndent(d,c);  
			} else {
				doModifiedNewlineAutoIndent(d,c,+1);
			}
		} else if (line.endsWith(":") || line.startsWith("case ") || line.equals("case") || line.startsWith("terminals ") || line.startsWith("is" )) {
			doModifiedNewlineAutoIndent(d,c,+1);
		} else {
			doDefaultNewlineAutoIndent(d, c);
		}
	}

	private static final String[] electricKeywords = {"end","is"};

	@Override
	public void customizeDocumentCommand(IDocument d, DocumentCommand c) {
		try {
			if (d.getLength() > 0 && c.offset > 0 && c.length == 0 && c.text != null) {
				c.text = c.text.replaceAll("\t", indentToSpaces(getIndentUnit()));
				if (TextUtilities.endsWith(d.getLegalLineDelimiters(), c.text) != -1) {
					doNewlineAutoIndent(d,c);
				} else {
					if (getLineLength(d,c.offset) != 0) {
						// System.out.println("line length = " + getLineLength(d,c.offset));
						// System.out.println("not at end of line");
						return;
					}
					String upTo = getLineUpTo(d,c.offset);
					int i = TextUtilities.startsWith(electricKeywords,upTo + c.text);
					if (i < 0) {
						// System.out.println("no electric keyword found");
						return;
					}
					int j = electricKeywords[i].length() - upTo.length();
					if (j < 0) {
						// System.out.println("word already in line");
						return;
					}
					if (c.text.length() <= j) {
						// System.out.println("word not delimited yet");
						return;
					}
					if (!Character.isWhitespace(c.text.charAt(j))) {
						// System.out.println("word not delimited with whitespace: " + j);
						return;
					} 
					// int thisIndent = c.offset - upTo.length() - d.getLineInformationOfOffset(c.offset).getOffset();
					/*if (notAlreadyUndented(d, c.offset, thisIndent)): does the wrong thing for "end". */ 
					{
						// System.out.println("attempting to undent");
						addUndent(d,c);
					}
				}
			}
		} catch (BadLocationException e) {
			e.printStackTrace(System.err);
			return;
		}
	}

}
