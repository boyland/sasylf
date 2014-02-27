package edu.cmu.cs.sasylf.ast;

public enum Errors {
  BAD_FILE_NAME_SUFFIX ("Proof file name must end in '.slf'"),
  BAD_FILE_NAME("Proof file for module must be a legal identifier"),
  WRONG_PACKAGE ("wrong package"),
  WRONG_MODULE_NAME ("wrong module name"),
  WRONG_END,
	DERIVATION_UNPROVED		("derivation unproved"),
	RULE_NOT_FOUND			("cannot find a rule named "),
	THEOREM_NOT_FOUND  ("cannot find lemma/theorem named "),
	RULE_NOT_THEOREM  ("expected theorem, not rule "),
	THEOREM_NOT_RULE  ("expected rule, not theorem "),
	THEOREM_KIND_WRONG ("not a "),
	THEOREM_KIND_MISSING ("missing keyword "),
	RULE_LIKE_REDECLARED ("declaration uses same name as previous rule/theorem"),
	RULE_BAD  ("rule/theorem has a bad interface: "),
	RULE_PREMISE_NUMBER		("wrong number of premises for "),
	BAD_RULE_APPLICATION,
	TOO_MANY_VARIABLES ("only one variable per nonterminal is permitted"),
	VARIABLE_HAS_NO_CONTEXT ("the variable for this nonterminal is never bound in a context"),
	VARIABLE_HAS_MULTIPLE_CONTEXTS ("the variable for this nonterminal is illegaly bound in multiple contexts"),
	NO_DERIVATION			("must provide a derivation"),
	INDUCTION_MISSING ("'induction hypothesis' requires explicit proof by induction"),
	INDUCTION_REPEAT ("can't nest induction analysis inside existing induction"),
	INDUCTION_NOT_INPUT,
	NOT_SUBDERIVATION		("argument to induction hypothesis must be a subderivation of theorem input"),
	WRONG_RESULT			("the last derivation in a sequence does not match the statement to be proven"),
	MISSING_CASE			("must provide a case for "),
	EXTRA_CASE				("case is redundant or unnecessary"),
	MISSING_ASSUMES			("found a use of a context nonterminal but no assumes clause"),
	ILLEGAL_ASSUMES    ("assumed entity is not a context nonterminal"),
	ILLEGAL_ASSUMES_CLAUSE ("assumed clause is not a context"),
	EXTRANEOUS_ASSUMES ("found no use of the context nonterminal "),
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
	SYNTAX_EXPECTED ("cannot create judgment by syntax"),
	WRONG_JUDGMENT,
	EMPTY_CONCLUSION_CONTEXT ("conclusion of judgment that assumes a context cannot have an empty context"), 
	VAR_CONCLUSION_CONTEXT ("conclusion of judgment that assumes a context cannot have a context with variables"),
	PREMISE_CONTEXT_MISMATCH ("premise cannot use a context variable not present in conclusion"),
	FORWARD_REFERENCE		("mutual induction is unchecked"),
	UNDECLARED_NONTERMINAL,
	VAR_STRUCTURE_KNOWN,
	UNKNOWN_CONTEXT,
	REUSED_CONTEXT,
	INCONSISTENT_CONTEXTS,
	CONTEXT_DISCARDED ("context discarded"),
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
  MUTUAL_NOT_EARLIER ("if inductive argument is unchanged, the mutual induction must be to an earlier theorem"),
  ASSUMED_ASSUMES("an 'assumes' clause should have been given for this theorem/lemma"),
  OR_NOT_IMPLEMENTED("'or' judgments not yet supported"),
  INTERNAL_ERROR("SASyLF Internal Error")
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
