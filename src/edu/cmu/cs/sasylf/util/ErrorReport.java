package edu.cmu.cs.sasylf.util;


public class ErrorReport extends Report {
	public ErrorReport(Errors errorType, String msg, Span loc, String debugInfo, boolean isError) {
		super(loc,msg);
		this.errorType = errorType;
		this.debugInfo = debugInfo;
		this.isError = isError;
	}

	/**
	 * @return The detailed error message including file, line, and actual error text message
	 */
	@Override
	public String formatMessage() {
		String msg;
		if (getSpan() == null) msg = "unknown file: ";
		else if (getSpan().getLocation() == null) msg = "unknown file (in bad span " + (getSpan().getClass()) + "): ";
		else msg = getSpan().getLocation().toString() + ": ";
		if (!isError())
			msg = msg + "warning: ";
		if (isError())
			msg = msg + "error: ";
		msg = msg + getMessage();
		return msg;
	}

	/**
	 * @return The actual error text message
	 */
	@Override
	public String getMessage() {
		String msg = "";
		if (errorType != null)
			msg = errorType.getText();
		msg = msg + super.getMessage();
		return msg;
	}

	@Override
	public boolean isError() {
		return isError;
	}

	public Errors getErrorType() {
		return errorType;
	}
	
	/**
	 * Return the portion of the message without the error type.
	 * @return specific message from this report, ignoring the error type.
	 */
	public String getErrorMessage() {
		return super.getMessage();
	}
	
	@Override
	public String getExtraInformation() {
		return debugInfo;
	}

	@Override
	public boolean shouldPrint() {
		return Util.PRINT_ERRORS;
	}

	public final Errors errorType;
	public final String debugInfo;
	private final boolean isError;
}
