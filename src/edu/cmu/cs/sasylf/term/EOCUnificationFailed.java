package edu.cmu.cs.sasylf.term;


public class EOCUnificationFailed extends UnificationFailed {
  /**
   * Keep Eclipse Happy
   */
  private static final long serialVersionUID = 1L;

    public EOCUnificationFailed(String text, Term eocTerm) { super(text); this.eocTerm = eocTerm; }

    public Term eocTerm;
}
