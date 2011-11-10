package edu.cmu.cs.sasylf.ast;

public enum Errors {
	// tested errors
	DERIVATION_UNPROVED		("derivation unproved"),
	RULE_NOT_FOUND			("cannot find a rule named "),
	RULE_PREMISE_NUMBER		("wrong number of premises for rule "),
	BAD_RULE_APPLICATION,
	// untested errors
	NO_DERIVATION			("must provide a derivation"),
	NOT_SUBDERIVATION		("argument to induction hypothesis must be a subderivation of theorem input"),
	WRONG_RESULT			("the last derivation in a sequence does not match the statement to be proven"),
	MISSING_CASE			("must provide a case for "),
	EXTRA_CASE				("case is redundant or unnecessary"),
	MISSING_ASSUMES			("found a rule for using an assumption but no assumes clause"),
	CANNOT_USE_ASSUMPTION	("did you give a rule for using the assumption?\n\t(didn't check to see if assumption use rule was in another judgment this judgment depends on)"),
	SYNTAX_CASE_FOR_DERIVATION ("When case-analyzing a derivation, must use rule cases, not syntax cases"),
	UNBOUND_VAR_CASE,
	DERIVATION_NOT_FOUND,
	NONTERMINAL_CASE,
	INVALID_CASE,
	EXPECTED_VARIABLE,
	BINDING_INCONSISTENT,
	MISSING_ASSUMPTION_RULE,
	UNBOUND_VAR_USE,
	DUPLICATE_CASE			("Already provided a case of this syntactic form"),
	NOT_EQUIVALENT,
	JUDGMENT_EXPECTED,
	WRONG_JUDGMENT,
	FORWARD_REFERENCE		("mutual induction is unchecked"),
	UNDECLARED_NONTERMINAL,
	VAR_STRUCTURE_KNOWN,
	UNKNOWN_CONTEXT,
	REUSED_CONTEXT,
	INCONSISTENT_CONTEXTS,
	WRONG_SUBSTITUTION_ARGUMENTS ("expected 2 arguments to a substitution justification: the judgment being substituted, and the judgment being substituted into"),
	SUBSTITUTION_UNCHECKED ("substitution is unchecked"),
	SUBSTITUTION_NO_CONTEXT ("no context to substitute"),
	SUBSTITUTION_FAILED ("context cannot be filled with given clause"),
	WRONG_WEAKENING_ARGUMENTS ("expected 1 argument as a weakening justification: the stronger judgment being weaked"),
	BAD_WEAKENING("result cannot be produced by adding unused variable bindings"),
	WRONG_EXCHANGE_ARGUMENTS("expected 1 arguments an an exchange justification on which to permute bindings"),
	BAD_EXCHANGE("result cannot be produced by permuting variable bindings"),
	INVERSION_UNCHECKED("inversion is unchecked"),
	INVERSION_REQUIRES_CLAUSE("inversion cannot be applied to syntax, only judgments"),
	INVERSION_NOT_FOUND("did not find claimed judgment in inversion of rule"),
	WEIRD_ADAPT_ERROR("internal error in adapt"),
	MUTUAL_INDUCTION_NO_INDUCTION("mutual induction cannot be used without an inductive argument"),
  MUTUAL_NOT_SUBDERIVATION   ("argument to mutual induction must be a subderivation of theorem induction"),
  MUTUAL_NOT_EARLIER ("if inductive argument is unchanged, the mutual induction must be to an earlier theoremc"),
	;

	Errors() {
		text = "";
	}

	Errors(String text) {
		this.text = text;
	}
	
	private String text;
	
	public String getText() {
		return text;
	}
}
