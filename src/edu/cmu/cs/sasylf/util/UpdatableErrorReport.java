package edu.cmu.cs.sasylf.util;

/**
 * An error report in which debug information can be
 * set later (but just once).  This is needed when the information we will
 * need for a quick fix isn't available yet.  If the information is never set,
 * then no quick fix will be available and this error report works like a 
 * standard error report.
 */
public class UpdatableErrorReport extends ErrorReport {

	private String updatableDebugInfo;
	
	public UpdatableErrorReport(Errors errorType, String msg, Span loc) {
		super(errorType, msg, loc, null, true);
	}

	/**
	 * Set the debugging information (used for quick fixes)
	 * if it has not yet been set.
	 * @param extra extra information, should not be null
	 * @throws IllegalStateException if the extra information would be changed
	 */
	public void setExtraInformation(String extra) {
		if (extra == null) throw new IllegalArgumentException("cannot set to null");
		if (updatableDebugInfo != null && !updatableDebugInfo.equals(extra)) {
			throw new IllegalStateException("cannot change extra information once set");
		}
		updatableDebugInfo = extra;
		if (Util.EXTRA_ERROR_INFO) {
			System.out.println(extra);
		}
	}
	
	@Override
	public String getExtraInformation() {
		return updatableDebugInfo;
	}

}
