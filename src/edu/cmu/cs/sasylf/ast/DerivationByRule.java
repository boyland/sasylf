package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.term.Facade.App;

import java.util.*;
import java.io.*;

import edu.cmu.cs.sasylf.term.*;
import edu.cmu.cs.sasylf.util.ErrorHandler;

import static edu.cmu.cs.sasylf.util.Util.*;

public class DerivationByRule extends DerivationByIHRule {
	public DerivationByRule(String n, Location l, Clause c, String rn) {
		super(n,l,c); ruleName = rn;
	}

	@Override
	public String getRuleName() { return ruleName; }
	public RuleLike getRule(Context ctx) {
		if (rule == null) {
			// make sure we can find the rule
			rule = ctx.ruleMap.get(ruleName);
			if (rule == null) {
				rule = ctx.recursiveTheorems.get(ruleName);
				if (rule == null)
					ErrorHandler.report(Errors.RULE_NOT_FOUND, ruleName, this);
				else
					ErrorHandler.warning(Errors.FORWARD_REFERENCE, this);
			}
		}
		return rule;
	}

	public String prettyPrintByClause() {
		return " by rule " + ruleName;
	}

	private static int constId = 0;

	private String ruleName;
	private RuleLike rule;
}
