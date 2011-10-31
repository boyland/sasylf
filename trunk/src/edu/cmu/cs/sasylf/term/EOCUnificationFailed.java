package edu.cmu.cs.sasylf.term;

import java.util.*;
import java.io.*;

public class EOCUnificationFailed extends UnificationFailed {
    public EOCUnificationFailed(String text, Term eocTerm) { super(text); this.eocTerm = eocTerm; }

    public Term eocTerm;
}
