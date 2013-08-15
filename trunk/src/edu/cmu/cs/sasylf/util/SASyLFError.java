package edu.cmu.cs.sasylf.util;

public class SASyLFError extends RuntimeException {
	/**
   * Keep Eclipse Happy
   */
  private static final long serialVersionUID = 1L;
  
  private ErrorReport report;
	
	SASyLFError(ErrorReport report) {  // package-private constructor - can only be called from ErrorHandler
		super(report.getMessage());
		this.report = report;
	}
	
	public ErrorReport getReport() { return report; }
}
