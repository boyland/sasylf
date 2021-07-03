package edu.cmu.cs.sasylf.util;

public class SASyLFError extends RuntimeException {
	/**
	 * Keep Eclipse Happy
	 */
	private static final long serialVersionUID = 1L;

	private Report report;

	public SASyLFError(Report report) { 
		super(report.formatMessage());
		this.report = report;
	}

	public Report getReport() { return report; }
}
