package org.sasylf.editors;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

/**
 * Correct the indentation of a region of a document.
 */
public class SASyLFAddRemoveCommentStrategy extends SASyLFIndentStrategy {

	private final boolean isAdd;
	
	/**
	 * Create a strategy to add or remove comments from a proof.
	 * @param add whether to add comments.
	 */
	public SASyLFAddRemoveCommentStrategy(boolean add) {
		isAdd = add;
	}

	/**
	 * Compute the indent on the line starting at the offset
	 * @param doc document to examine, must not be null
	 * @param offset starting point to look at characters
	 * @param max maximum indent to look for
	 * @return index, or max (if no non-whitespace characters on line)
	 */
	protected int getIndent(IDocument doc, int offset, int max) {
		try {
			for (int i = 0; i < max; ++i) {
				int ch = doc.getChar(offset+i);
				if (ch == '\r' || ch == '\n') return max;
				if (!Character.isWhitespace(ch)) return i;
			}
		} catch (BadLocationException e) {
			// muffle error
		}
		return max;
	}
	
	/**
	 * Return the offset of a comment start "//" in the line starting
	 * at the given offset, as long as there are only spaces before.
	 * If no comment found on teh start of the line, return -1.
	 * @param doc document to examine, must not be null
	 * @param offset starting offset for line
	 * @return offset of comment start or -1 if no such in line
	 */
	protected int getCommentOffset(IDocument doc, int offset) {
		try {
			while (true) {
				int ch = doc.getChar(offset);
				if (ch == '/') {
					ch = doc.getChar(offset+1);
					if (ch == '/') return offset;
					break;
				}
				if (ch == '\r' || ch == '\n') break;
				if (!Character.isWhitespace(ch)) break;
				++offset;
			}
		} catch (BadLocationException e) {
			// muffle error
		}
		return -1;
	}
	
	/**
	 * Add or remove comment in proof.
	 * @param document document to change, must not be null
	 * @param first first line (not offset) to check
	 * @param count number of lines to check.
	 * @return text edit to add/remove comment on given lines, null if nothing to do
	 */
	public TextEdit changeComment(IDocument document, int first, int count) {
		MultiTextEdit result = new MultiTextEdit();
		int indent = Integer.MAX_VALUE;
		try {
			if (isAdd) {
				for (int i = 0; i < count; ++i) {
					int off = document.getLineOffset(first+i);
					indent = getIndent(document,off,indent);
				}
				System.out.println("Computed indent as " + indent);
				if (indent == Integer.MAX_VALUE) return null;
				for (int i=0; i < count; ++i) {
					int off = document.getLineOffset(first+i);
					int len = document.getLineLength(first+i);
					if (len > indent) {
						result.addChild(new InsertEdit(off+indent,"//"));
					}
				}
			} else {
				for (int i=0; i < count; ++i) {
					int off = document.getLineOffset(first+i);
					int comm = getCommentOffset(document,off);
					if (comm >= 0) {
						result.addChild(new DeleteEdit(comm,2));
					}
				}
			}
		} catch (BadLocationException e) {
			e.printStackTrace();
			return null;
		}
		if (result.getChildrenSize() == 0) return null;
		return result;
	}
}
