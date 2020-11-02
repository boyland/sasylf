package edu.cmu.cs.sasylf.util;

/**
 * Report of something of note in a proof.
 */
public class Report {

	private final String message;
	private final Span loc;

	/**
	 * Create a report (not referenced elsewhere) for the given place in the proof
	 * with the given message.
	 * @param loc
	 * @param message
	 */
	public Report(Span loc, String message) {
		this.loc = loc;
		if (message == null) message = "";
		this.message = message;
	}

	@Override
	public String toString() {
		return formatMessage();
	}

	/**
	 * Format a message with filename and line number as the prefix.
	 * @return string for printing (without newline).
	 */
	public String formatMessage() {
		String prefix = getSpan() == null ? "unknown file: " : getSpan().getLocation().toString() + ": ";
		return prefix + getMessage();
	}
	
	/**
	 * Return the message for this report.
	 * @return the message, never null
	 */
	public String getMessage() {
		return message;
	}


	/**
	 * Return the location of this report in the proof being checked.
	 * @return location (start and end), may be null
	 */
	public Span getSpan() {
		return loc;
	}

	/**
	 * Return true if this report is an error.
	 * @return true if this report is an error.
	 */
	public boolean isError() {
		return false;
	}
}