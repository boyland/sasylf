package org.sasylf.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IDocumentPartitioningListener;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.sasylf.Activator;

/**
 * A temporary (immutable) document for a resource.
 * @deprecated use {@link ImmutableDocument} instead
 */
@Deprecated
public class ResourceDocument implements IDocument {

	private static final int DEFAULT_BUFFER_SIZE = 1024;

	private final IResource resource;
	private final String contents;
	private final int[] endOffsets;

	public ResourceDocument(IResource res) throws CoreException {
		resource = res;
		IFile f = resource.getAdapter(IFile.class);
		Reader r;
		try {
			r = new InputStreamReader(f.getContents(),f.getCharset());
		} catch (UnsupportedEncodingException e) {
			Status s = new Status(IStatus.ERROR,Activator.PLUGIN_ID, e.getMessage(), e);
			throw new CoreException(s);
		}
		char[] buf = new char[DEFAULT_BUFFER_SIZE];
		StringBuilder sb = new StringBuilder();
		int n;
		try {
			while( (n = r.read(buf)) >= 0 ) {
				sb.append( buf, 0, n );
			}
		} catch (IOException e) {
			Status s = new Status(IStatus.ERROR,Activator.PLUGIN_ID, e.getMessage(), e);
			throw new CoreException(s);
		}
		String text = sb.toString();
		contents = text;
		List<Integer> endList = computeEndOffsets(text);
		int lines = endList.size();
		endOffsets = new int[lines];
		for (int i=0; i < lines; ++i) {
			endOffsets[i] = endList.get(i);
		}
	}

	/**
	 * @param text
	 * @return
	 */
	 protected List<Integer> computeEndOffsets(String text) {
		 int length = text.length();
		 List<Integer> endList = new ArrayList<Integer>();
		 for (int i=0; i < length; ++i) {
			 char ch = text.charAt(i);
			 while (ch == '\r') {
				 ++i;
				 if (i == length) break;
				 ch = text.charAt(i);
				 if (ch == '\n') {
					 break;
				 }
				 endList.add(i);
			 }
			 if (ch == '\n') {
				 endList.add(i);
			 }
		 }
		 if (endList.size() == 0 || endList.get(endList.size()-1) != length) {
			 endList.add(length);
		 }
		 return endList;
	 }

	 @Override
	 public char getChar(int offset) throws BadLocationException {
		 try {
			 return contents.charAt(offset);
		 } catch (RuntimeException e) {
			 throw new BadLocationException(e.getMessage());
		 }
	 }

	 @Override
	 public int getLength() {
		 return contents.length();
	 }

	 @Override
	 public String get() {
		 return contents;
	 }

	 @Override
	 public String get(int offset, int length) throws BadLocationException {
		 try {
			 return contents.substring(offset, offset+length);
		 } catch (RuntimeException e) {
			 throw new BadLocationException(e.getMessage());
		 }
	 }

	 @Override
	 public void set(String text) {
		 throw new UnsupportedOperationException("Document is read only");
	 }

	 @Override
	 public void replace(int offset, int length, String text)
			 throws BadLocationException {
		 throw new UnsupportedOperationException("Document is read only");
	 }

	 @Override
	 public void addDocumentListener(IDocumentListener listener) {
	 }

	 @Override
	 public void removeDocumentListener(IDocumentListener listener) {
	 }

	 @Override
	 public void addPrenotifiedDocumentListener(IDocumentListener documentAdapter) {
	 }

	 @Override
	 public void removePrenotifiedDocumentListener(
			 IDocumentListener documentAdapter) {
	 }

	 @Override
	 public void addPositionCategory(String category) {
		 throw new UnsupportedOperationException("Document is read only");
	 }

	 @Override
	 public void removePositionCategory(String category)
			 throws BadPositionCategoryException {
		 throw new UnsupportedOperationException("Document is read only");

	 }

	 @Override
	 public String[] getPositionCategories() {
		 return null;
	 }

	 @Override
	 public boolean containsPositionCategory(String category) {
		 return false;
	 }

	 @Override
	 public void addPosition(Position position) throws BadLocationException {
		 throw new UnsupportedOperationException("Document is read only");
	 }

	 @Override
	 public void removePosition(Position position) {
		 throw new UnsupportedOperationException("Document is read only");
	 }

	 @Override
	 public void addPosition(String category, Position position)
			 throws BadLocationException, BadPositionCategoryException {
		 throw new UnsupportedOperationException("Document is read only");
	 }

	 @Override
	 public void removePosition(String category, Position position)
			 throws BadPositionCategoryException {
		 throw new UnsupportedOperationException("Document is read only");
	 }

	 @Override
	 public Position[] getPositions(String category)
			 throws BadPositionCategoryException {
		 return null;
	 }

	 @Override
	 public boolean containsPosition(String category, int offset, int length) {
		 return false;
	 }

	 @Override
	 public int computeIndexInCategory(String category, int offset)
			 throws BadLocationException, BadPositionCategoryException {
		 return 0;
	 }

	 @Override
	 public void addPositionUpdater(IPositionUpdater updater) {
		 throw new UnsupportedOperationException("Document is read only");
	 }

	 @Override
	 public void removePositionUpdater(IPositionUpdater updater) {
		 throw new UnsupportedOperationException("Document is read only");
	 }

	 @Override
	 public void insertPositionUpdater(IPositionUpdater updater, int index) {
		 throw new UnsupportedOperationException("Document is read only");
	 }

	 @Override
	 public IPositionUpdater[] getPositionUpdaters() {
		 return null;
	 }

	 @Override
	 public String[] getLegalContentTypes() {
		 return null;
	 }

	 @Override
	 public String getContentType(int offset) throws BadLocationException {
		 return null;
	 }

	 @Override
	 public ITypedRegion getPartition(int offset) throws BadLocationException {
		 return null;
	 }

	 @Override
	 public ITypedRegion[] computePartitioning(int offset, int length)
			 throws BadLocationException {
		 return null;
	 }

	 @Override
	 public void addDocumentPartitioningListener(
			 IDocumentPartitioningListener listener) {
	 }

	 @Override
	 public void removeDocumentPartitioningListener(
			 IDocumentPartitioningListener listener) {
	 }

	 @Override
	 public void setDocumentPartitioner(IDocumentPartitioner partitioner) {
		 throw new UnsupportedOperationException("Document is read only");
	 }

	 @Override
	 public IDocumentPartitioner getDocumentPartitioner() {
		 return null;
	 }

	 @Override
	 public int getLineLength(int line) throws BadLocationException {
		 if (line < 0 || line >= endOffsets.length) {
			 throw new BadLocationException("not a legal line number: " + line);
		 }
		 if (line == 0) return endOffsets[0];
		 return endOffsets[line]-endOffsets[line-1];
	 }

	 @Override
	 public int getLineOfOffset(int offset) throws BadLocationException {
		 if (offset < 0 || offset >= contents.length()) {
			 throw new BadLocationException("bad offset " + offset);
		 }
		 int lo = 0, hi = endOffsets.length;
		 while (lo != hi) {
			 int mid = (lo+hi)/2;
			 int test = endOffsets[mid];
			 if (test == offset) return mid+1;
			 if (test > offset) hi = mid;
			 else lo = mid;
		 }
		 return lo;
	 }

	 @Override
	 public int getLineOffset(int line) throws BadLocationException {
		 if (line < 0 || line >= endOffsets.length) {
			 throw new BadLocationException("not a legal line number: " + line);
		 }
		 if (line == 0) return 0;
		 return endOffsets[line-1];
	 }

	 @Override
	 public IRegion getLineInformation(int line) throws BadLocationException {
		 return new Region(getLineOffset(line),getLineLength(line));
	 }

	 @Override
	 public IRegion getLineInformationOfOffset(int offset)
			 throws BadLocationException {
		 return getLineInformation(getLineOfOffset(offset));
	 }

	 @Override
	 public int getNumberOfLines() {
		 return endOffsets.length;
	 }

	 @Override
	 public int getNumberOfLines(int offset, int length)
			 throws BadLocationException {
		 int line1 = getLineOfOffset(offset);
		 if (length < 0) throw new BadLocationException("negative length");
		 if (length == 0) return 1;
		 int line2 = getLineOfOffset(offset+length-1);
		 return line2 - line1 + 1;
	 }

	 @Override
	 public int computeNumberOfLines(String text) {
		 return computeEndOffsets(text).size();
	 }

	 private static String[] DELIMITERS = {"\r", "\r\n", "\n" };

	 @Override
	 public String[] getLegalLineDelimiters() {
		 return DELIMITERS;
	 }

	 @Override
	 public String getLineDelimiter(int line) throws BadLocationException {
		 int offset = getLineOffset(line);
		 int length = getLineLength(line);
		 char ch = contents.charAt(offset+length-1);
		 if (ch == '\r') return "\r";
		 if (ch == '\n') {
			 if (offset+length > 1 && contents.charAt(offset+length-2) == '\r') return "\r\n";
			 return "\n";
		 }
		 return null;
	 }

	 @Override
	 public int search(int startOffset, String findString, boolean forwardSearch,
			 boolean caseSensitive, boolean wholeWord) throws BadLocationException {
		 throw new UnsupportedOperationException("regex search not provided (yet?)");
	 }

}
