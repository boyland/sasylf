package edu.cmu.cs.sasylf.term;


public class UnificationFailed extends RuntimeException {
    /**
     * Keep Eclipse Happy
     */
    private static final long serialVersionUID = 1L;

    public UnificationFailed() {}
    public UnificationFailed(String text) { super(text); }
    public UnificationFailed(String text, Term t1, Term t2) { super(text); term1 = t1; term2 = t2; }

    public Term term1, term2;
}
