package edu.cmu.cs.sasylf.ast;

import java.util.*;

public class Grammar {
    public NonTerminal getStart() { return start; }
    public Set<NonTerminal> getNonTerminals() { return nonTerminals; }

    private NonTerminal start;
    private Set<NonTerminal> nonTerminals;
}
