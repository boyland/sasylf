package org.sasylf.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.widgets.Display;

import edu.cmu.cs.sasylf.util.Util;

/**
 * A collection of regions where edit actions have happened.
 * The regions are with regard to the latest version of the document;
 * they are adjusted for every new document event.
 * This class helps implement a replacement for poorly designed
 * incremental reconcilers.  It should be made a document listener.
 * If it is moved from one document to another, the regions must be cleared.
 * @author boyland
 */
public class TrackDirtyRegions implements IDocumentListener {

	private final List<MutableDirtyRegion> regions = new LinkedList<MutableDirtyRegion>();

	static boolean report(String s) {
		System.err.println("Invariant error: " + s);
		return false;
	}

	private boolean invariant() {
		MutableDirtyRegion last = null;
		for (MutableDirtyRegion mtr : regions) {
			if (!mtr.invariant()) return false;
			if (last != null) {
				if (last.getEnd() >= mtr.getOffset()) {
					return report("out of order: " + last + " and " + mtr);
				}
			}
			last = mtr;
		}
		return true;
	}

	public TrackDirtyRegions() { }

	/**
	 * Return the list of dirty regions since the last such call.
	 * The list is cleared as a side-effect. If this method
	 * is called outside the UI thread, it first synchronizes with the UI
	 * before returning, which means this thread will be suspended.
	 * @return fresh list of dirty regions.
	 */
	public List<IDirtyRegion> getDirtyRegions() {
		final List<IDirtyRegion> result = new ArrayList<IDirtyRegion>();
		if (Display.getCurrent() == null) {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					Util.verify(invariant(), "invariant bad");
					result.addAll(regions);
					regions.clear();
				}
			});
		} else {
			Util.verify(invariant(), "invariant bad");
			result.addAll(regions);
			regions.clear();
		}
		return result;
	}

	/**
	 * Remove all dirty regions, for instance when switching from one document to another.
	 * This method is a shorthand for {@link #getDirtyRegions()}.
	 */
	public void clear() {
		getDirtyRegions();
	}

	private String oldText;

	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {
		try {
			oldText = event.fDocument.get(event.fOffset, event.fLength);
		} catch (BadLocationException e) {
			e.printStackTrace();
			StringBuilder sb = new StringBuilder();
			for (int i=0; i < event.fLength; ++i) {
				sb.append('?');
			}
			oldText = sb.toString();
		}
	}

	@Override
	public void documentChanged(DocumentEvent event) {
		Util.verify(invariant(), "invariant bad");
		ListIterator<MutableDirtyRegion> it = regions.listIterator();
		int eventEnd = event.fOffset + event.fLength;
		int eLength = event.fText == null ? 0 : event.fText.length();
		int diff = eLength - event.fLength; 
		MutableDirtyRegion reg = null;

		// find first effected region
		while (it.hasNext() && (reg = it.next()).getEnd() < event.fOffset) {
			reg = null;
		}

		if (reg == null) {
			regions.add(new MutableDirtyRegion(event.fOffset,oldText,eLength));
			Util.verify(invariant(), "invariant bad");
			return;
		}

		handleOverlap: {
			// EASIEST: no overlap at all: reg starts after event
			if (reg.getOffset() > eventEnd) {
				// backup
				it.previous();
				it.add(new MutableDirtyRegion(event.fOffset,oldText,eLength));
				break handleOverlap;
			}

			// if current starts after event, we move it back 
			// to contain the start of the new dirty region,
			// and then continue;
			if (reg.getOffset() > event.fOffset) {
				int shift = reg.getOffset() - event.fOffset;
				reg.fOffset -= shift;
				reg.fNewLength += shift;
				reg.fOldText.insert(0, oldText.substring(0,shift));
			}

			// now if the (possibly modified) region includes the entire event region,
			// we can just internally modify the region, and then we're done handling overlap
			while (reg.getEnd() < eventEnd && it.hasNext()) {
				MutableDirtyRegion later = it.next();
				if (later.getOffset() > eventEnd) {
					it.previous();
					break;
				}
				it.remove();
				reg.fOldText.append(oldText.substring(reg.getEnd()-event.fOffset,later.getOffset()-event.getOffset()));
				reg.fOldText.append(later.fOldText);
				reg.fNewLength = later.getEnd() - reg.getOffset();
			}

			if (reg.getEnd() < eventEnd) {
				// expand to cover the whole event
				reg.fOldText.append(oldText.substring(reg.getEnd()-event.fOffset));
				reg.fNewLength = eventEnd - reg.getOffset();
			}

			// now modify current event to handle change:
			reg.fNewLength += diff;
		}

		// all remaining regions just have their offset moved
		while (it.hasNext()) {
			it.next().fOffset += diff;
		}
		Util.verify(invariant(), "invariant bad");
	}

	public static interface IDirtyRegion extends IRegion {
		/**
		 * Return the text in document that was changed.
		 * @return text from old version of document
		 */
		public String getOldText();
	}

	private class MutableDirtyRegion implements IDirtyRegion {
		StringBuilder fOldText;
		int fOffset, fNewLength;

		boolean invariant() {
			if (fOldText == null) return report("fOldText is null");
			if (fOffset < 0) return report("fOffset < 0");
			if (fNewLength < 0) return report("fNewLength < 0");
			return true;
		}

		/**
		 * Create a new region.
		 *
		 * @param offset the offset of the region
		 * @param length the length of the region
		 */
		public MutableDirtyRegion(int offset, String oldText, int newLength) {
			fOffset= offset;
			fOldText = new StringBuilder(oldText);
			fNewLength= newLength;
		}

		/*
		 * @see org.eclipse.jface.text.IRegion#getLength()
		 */
		@Override
		public int getLength() {
			return fNewLength;
		}

		@Override
		public String getOldText() {
			return fOldText.toString();
		}

		/*
		 * @see org.eclipse.jface.text.IRegion#getOffset()
		 */
		@Override
		public int getOffset() {
			return fOffset;
		}

		public int getEnd() {
			return fOffset + fNewLength;
		}

		/*
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {
			if (o instanceof IRegion) {
				IRegion r= (IRegion) o;
				return r.getOffset() == fOffset && r.getLength() == fNewLength;
			}
			return false;
		}

		/*
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return (fOffset << 24) | (fNewLength << 16);
		}

		/*
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "offset: " + fOffset + ", length: " + fNewLength +", was " + fOldText;
		}

	}
}
