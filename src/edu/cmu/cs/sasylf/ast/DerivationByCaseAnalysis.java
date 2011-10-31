package edu.cmu.cs.sasylf.ast;

import java.util.*;
import java.io.*;


public class DerivationByCaseAnalysis extends DerivationByAnalysis {
    public DerivationByCaseAnalysis(String n, Location l, Clause c, String derivName) {
    	super(n,l,c, derivName);
    }

    public String byPhrase() { return "case analysis"; }
}
