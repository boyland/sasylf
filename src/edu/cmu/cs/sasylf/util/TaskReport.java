package edu.cmu.cs.sasylf.util;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.sasylf.parser.CommentListener;
import edu.cmu.cs.sasylf.parser.Token;

/**
 * A report of a task notes in the proof.
 */
public class TaskReport extends Report {
	
	private final int priority;
	
	public TaskReport(Span loc, String message, int priority) {
		super(loc, message);
		this.priority = priority;
	}

	public int getPriority() {
		return priority;
	}


	/// Methods for generating and printing reports.
	
	private static final String[] TASK_TAGS = {
			"XXX",
			"TODO",
			"FIXME",
	};
	
	private static final int[] TASK_PRIORITIES = { 0, 1, 2 };
			
	private static Map<String,Integer> taskTagMap = new HashMap<String,Integer>();
	
	static {
		for (int i=0; i < TASK_TAGS.length; ++i) {
			taskTagMap.put(TASK_TAGS[i], TASK_PRIORITIES[i]);
		}
	}
	
	public static CommentListener commentListener = (t,f) -> checkForTaskTag(t);
	
	public static void checkForTaskTag(Token s) {
		// System.out.println(DSLToolkitParser.currentFile + ":" + s.beginLine + ":" + s.beginColumn + ": Looking at " + s.image + ", while map size = " + taskTagMap.size());
		int lineNumber = s.beginLine;
		int startLine = 0;
		int startCol = s.beginColumn;
		int startWord = -1;
		int startTag = -1;
		int priority = 0;
		for (int i=0; i < s.image.length(); ++i) {
			int ch = s.image.codePointAt(i);
			if (Character.isAlphabetic(ch)) {
				/* TODO: does the tag extend to
				 * the next line?
				 */
				if (startWord == -1) startWord = i; // TODO: TODO: testing
			} else {
				if (startWord != -1) {
					String word = s.image.substring(startWord, i);
					Integer pri = taskTagMap.get(word);
					// System.out.println("Found word: '" + word + "' with pri = " + pri);
					if (pri != null) {
						// System.out.println("Found task word start: " + word);
						if (startTag != -1) {
							// System.out.println("Ending task tag");
							int mycol = startCol + startTag - startLine;
							foundTaskTag(s,priority,lineNumber,mycol,startTag,startWord-startTag);
						}
						startTag = startWord;
						priority = pri;
					}
					startWord = -1;
				}
				if (ch == '\n' || ch == '\r') {
					if (startTag != -1) {
						// System.out.println("Ending task tag at index " + i);
						int mycol = startCol + startTag - startLine;
						foundTaskTag(s,priority,lineNumber,mycol,startTag,i-startTag);
						startTag = -1;
					}
					// CRLF kludge:
					if (ch == '\r' && i+1 < s.image.length() && s.image.codePointAt(i+1) == '\n') {
						++i;
					}
					startCol = 1;
					startLine = i+1;
					++lineNumber;
				}
			}
		}
		if (startTag != -1) {
			int mycol = startCol + startTag - startLine;
			foundTaskTag(s,priority,lineNumber,mycol,startTag,s.image.length()-startTag);			
		}
	}
	
	private static void foundTaskTag(Token within, int pri, int lineNum, int startCol, int begin, int length) {
		// System.out.println("unknown:" + lineNum + ":" + startCol + ": Found task tag: " + within.image.substring(begin,begin+length));
		String file = new Location(within).getFile();
		Location start = new Location(file, lineNum, startCol);
		Span span = new DefaultSpan(start, start.add(length));
		TaskReport r = new TaskReport(span,within.image.substring(begin, begin+length),pri);
		ErrorHandler.report(r);
	}
	
	/**
	 * Return true if this string names a task type.
	 * @param s string to examine
	 * @return true if a string known as a task tag
	 */
	public static boolean isTaskTag(String s) {
		return taskTagPriority(s) >= 0;
	}
	
	/**
	 * Return the priority of this task tag, or -1 if not a task tag
	 * @param s name to examine
	 * @return -1 or priority of this task tag
	 */
	public static int taskTagPriority(String s) {
		int i = 0;
		for (String tt : TASK_TAGS) {
			if (tt.equals(s)) return TASK_PRIORITIES[i];
			++i;
		}
		return -1;
	}

	@Override
	public boolean shouldPrint() {
		return Util.SHOW_TASK_COMMENTS;
	}
}
