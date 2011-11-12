SASyLF, version 1.0.2

Copyright (c) 2009 by Jonathan Aldrich

This software is distributed under the Eclipse Public License Version 1.0
("EPL"). A copy of the EPL is provided with this distribution and is also
available at http://www.eclipse.org/legal/epl-v10.html.




INSTALLATION

To run SASyLF, you must have Java version 5.0 or later installed.  You
can get Java at:

http://java.sun.com/javase/downloads/index.jsp




RUNNING SASyLF

You can run SASyLF by explicitly invoking java on the SASyLF.jar file,
as in:

java -jar SASyLF.jar <filename>.slf


Or, you can use the sasylf/sasylf.bat files provided for Unix/Windows,
as follows:

sasylf <filename>.slf



SASyLF EXAMPLES

Exercises to learn SASyLF are in the exercises/ directory
 * exercise1.slf - A simple inductive proof without variable binding
			(solution is examples/sum.slf)
 * exercise2.slf - Adding let to the simply-typed lambda calculus
			(solution is exercises/solution2.slf


Tutorial examples (with comments that explain the SASyLF syntax) in the
examples/ directory include:

 * sum.slf - Commutativity of addition
 * lambda.slf - Type Soundness for the Simply-Typed Lambda Calculus
 * while1.slf - A derivation of program execution in Hoare's While language
		(assumes an oracle for arithmetic)
 * while2.slf - A proof that factorial in While computes factorial
		(assumes an oracle for arithmetic)
 * poplmark-2a.slf - POPLmark challenge 2A: Type Soundness for System Fsub

Other examples include:
 * lambda-loc.slf - shows preservation of a well-formed store in the untyped lambda calculus
 * object-calculus.slf - Definition of Abadi and Cardelli's Simply Typed Object Calculus
 * featherweight-java.slf - Definition of Featherweight Java (soundness proof to come)
 * lambda-unicode.slf - version of lambda.slf using unicode identifiers and operators




COMPILING SASyLF

This directory is an Eclipse project and can be compiled with Eclipse 3.3.
You will need to compile the edu/cmu/cs/sasylf/parser/parser.jj file
with the JavaCC Eclipse plugin, available from update site
http://eclipse-javacc.sourceforge.net/ (compile the .jj file to .java
by right-clicking on parser.jj and choosing Compile with JavaCC).

Alternatively, if you fetch the source from SVN, you can build SASyLF
(under Unix) assuming you have java and javacc in your path using
	make build



CONTACT

If you have any trouble installing or running SASyLF, or understanding
how to use the tool or interpret its output, contact Jonathan Aldrich
at jonathan.aldrich@cs.cmu.edu.



KNOWN LIMITATIONS (incomplete list)

Almost all checks are implemented, including rule application,
correct and completeness case analysis, and uses of lemmas and
the induction hypothesis.  "inversion" is still unchecked;
it is unclear precisely what it means.

Error messages point to the line of code and the kind of error, but
could use some improvement.

The automated prover ("by solve") is very primitive.  It works only
for straightforward derivations without the use of induction or case
analysis.

The tool could use some documentation.


RELEASE HISTORY

1.0.2 (uwm 10)
   Fixed Defect #3: unsoundness.  May break some existing examples
1.0.2 (uwm 9)
    Permit syntax checking of binders, e.g. lambdas (not yet of variables)
1.0.2 (uwm 8)
    Inversion checks implemented; does not handle a new variable above the line
1.0.2 (uwm 7)
    Fixed defect #4.
1.0.2 (uwm 6)
    Mutual induction: by default induction on first argument.
    Revert change in "uwm 3": dot no longer will be permitted in syntax (reserved for modules)
    Correctly identifies variables in contexts e.g. Gamma, X2 <: Top, X1 <: X2
    Substitution now handles cases where the antecedent also has a context.
1.0.2 (uwm 5)
    Mutual induction checked,
    recursion permitted under same restriction as "induction hypothesis"
1.0.2 (uwm 4)
    Substitution is checked
1.0.2 (uwm 3)	Syntax
	Lexical errors cleaned up (JavaCC generated messages a confused mess of double quote)
	Files read in unicode, permit unicode operators (identifiers already handled), see lambda-unicode.slf
	Dot permitted in syntax except at end of line (reserved for "exists")
	"Inversion" partially checked
1.0.2 (uwm 2)	Syntax
	Permit "|", "case" and "end" in syntax.
1.0.2 (uwm 1)	Checking
	Check weakening, exchange
	Print warnings for unchecked derivations
1.0.2	Various bug fixes
	Better error messages
0.23	Fixed bug where an exception was thrown if substitution was used with too few arguments
	Fixed bug due to not correctly expanding free variables in part of a rule application check with an expanded Gamma
	Fixed bug with bindings nested within bindings
	Implemented a more consistent handling of contexts (some more error checking, too)
	Improved the feedback in the --LF option
0.22	Fixed bug where the program crashed when case analyzing a term whose structure was already known
	Fixed a bug with handling multiple assumptions in the context correctly
	Added a --verbose feature that lists all the theorems as their proofs are checked (possibly useful for grading)
0.21	Fixed bug (nested lambdas with the same variable name) by making internal typechecking more robust
	Fixed bug where a derivation currently being proven by analysis can be used as if it's true within a subcase
	Fixed bug due to incorrect variable scoping in mutually recursive theorems
0.20	Fixed bug in input freeness checking, and redid the way case analysis works
	Checked for using a non-variable where a variable is expected in a judgment
	Implemented "and theorem" syntax to allow mutual induction (but we don't yet check that mutual induction is well-founded)
0.19	Added check for derivations that are really just nonterminals (eliminating an internal error)
0.18	Added check for duplicate syntax cases (eliminating an internal error in the process)
0.17	Added check to ensure a case given by the user has the right number of premises
	Added check that in the definition of a judgment form or syntax form,
		only variables that are binders should be permitted, except for the syntax of contexts
	Fixed a bug where a statement could be proved by case analysis on itself
	Implemented tracking of the current context to facilitate case analysis of two judgments
		that have the same context variable
0.16	Bug fixes
0.15	Bug fixes
0.14	Bug fixes
	Added a --help option
	Added a --LF option that sometimes prints out LF terms when errors occur (for LF experts only)
	Rule lines are now at least 3 -'s, so you can use --> as a terminal now
0.13	Fixed several small bugs
0.12	Added a --version option
	Report files that are un-openable properly
	Added a check that bindings are consistent (fixing a thrown exception in the process)
0.11	Fixed another class cast exception bug
0.10	Numerous minor bug fixes: class cast exceptions, missing one case of input freeness,
		and missing checks when citing a previous derivation (e.g. "by d2")
0.9	Fixed bug where the result of case analysis had to be the result of the theorem;
		now case analysis can be used to produce any result.
	Fixed internal exception when parsing certain input
	Enhanced checker to check that the hypothetical parts of judgments match
0.8	Fixed null pointer exception when mixing syntax and rule case analysis
0.7	First stable public release