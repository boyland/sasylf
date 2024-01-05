package edu.cmu.cs.sasylf.prover;

import java.io.PrintWriter;
import java.io.StringWriter;

import edu.cmu.cs.sasylf.util.Report;
import edu.cmu.cs.sasylf.util.Span;
import edu.cmu.cs.sasylf.util.Util;

public class SolveReport extends Report {

	private final Proof proof;
	public SolveReport(Span loc, Proof solution) {
		super(loc, "info: solution found");
		proof = solution;
	}

	public Proof getProof() {
		return proof;
	}
	
	@Override
	public String getExtraInformation() {
		StringWriter sw = new StringWriter();
		PrintWriter out = new PrintWriter(sw);
		proof.prettyPrint(out);
		out.flush();
		return sw.toString();
	}

	@Override
	public boolean shouldPrint() {
		return Util.PRINT_SOLVE;
	}
}
