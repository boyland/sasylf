package org.sasylf.util;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;

import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Span;

public class DocumentUtil {

	private DocumentUtil() {   }

	/** The size of tabs.
	 * Must match {@link edu.cmu.cs.sasylf.parser.JavaCharStream#tabSize}
	 */
	private static int TABSIZE = 8; 

	/**
	 * Return the absolute offset within the given document for this location
	 * @param loc location to compute offset for, must not be null
	 * @param doc document to show offset within, must not be null
	 * @return absolute offset within document
	 * @throws BadLocationException
	 */
	public static int getOffset(Location loc, IDocument doc) throws BadLocationException {
		IRegion info = doc.getLineInformation(loc.getLine()-1);
		// the column reported by edu.cmu.cs.sasylf.parser.JavaCharStream
		// includes handling of tab characters, of currently 8.
		int column = loc.getColumn();
		int off = info.getOffset(), col = 1;
		while (col < column) {
			if (doc.getChar(off) == '\t') {
				col -= 1;
				col += TABSIZE - (col % TABSIZE);
			}
			++col;
			++off;
		}
		return off;
	}

	/**
	 * Return a new unregistered position within a document for the extent
	 * of this node.  If no end location has been set, the length will be zero.
	 * @param node AST to compute position for, must not be null.
	 * @param doc document for the position, must not be null
	 * @return new position for the extent of this AST.
	 * @throws BadLocationException if the locations are wrong for the document.
	 */
	public static Position getPosition(Span node, IDocument doc) throws BadLocationException {
		int off1 = getOffset(node.getLocation(),doc);
		int off2 = getOffset(node.getEndLocation(),doc);
		if (off1 > off2) {
			System.out.println("Bad locations for " + node);
			return new Position(off1,0);
		}
		return new Position(off1,off2-off1);
	}

	public static Position getPositionToNextLine(Span node, IDocument doc) throws BadLocationException {
		int off1 = getOffset(node.getLocation(),doc);
		Location l = node.getEndLocation();
		int off2 = getOffset(new Location(l.getFile(),l.getLine()+1,1),doc);
		return new Position(off1,off2-off1);
	}
}
