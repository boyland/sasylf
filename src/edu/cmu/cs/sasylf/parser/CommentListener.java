package edu.cmu.cs.sasylf.parser;

/**
 * This object is interested in the content of
 * any comments parsed while reading a SASyLF file.
 */
public interface CommentListener {

	/**
	 * This method is called when a comment is read.
	 * It is called in the same thread that the parser is running.
	 * This method should avoid much computation.
	 * @param content the token (never null)
	 * @param filename the name of the current file being read.
	 * This string may not be a legal file name of an actual file.
	 */
	public void commentRead(Token content, String filename);
}
