SASyLF version 1.2.3a1
Copyright (c) 2009 by Jonathan Aldrich
Additional contributions by John Boyland

This software is distributed under the Eclipse Public License Version 1.0
("EPL"). A copy of the EPL is provided with this distribution and is also
available at http://www.eclipse.org/legal/epl-v10.html.




INSTALLATION

To run SASyLF, you must have Java version SE 6 or later installed.  You
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

The system does not check context roots: Gamma and * are treated
almost the same way.

Error messages point to the line of code and the kind of error, but
could use some improvement.

The automated prover ("by solve") is very primitive.  It works only
for straightforward derivations without the use of induction or case
analysis.



RELEASE HISTORY

1.2.3
1.2.2	Bug fixes
    Issue #21
	Issue #23
	Added "About SASyLF"
	Better creation of jars
1.2.1	Bug fixes
	Fixed issues #18, #19, #20, #22
	Added "proof" and "and" as highlightable keywords
1.2.0	Integrated Eclipse plugin source
1.1.3	Bug fix
1.1.2	Bug fixes
    Address Issue #17
1.1.1	Bug fixes
    Address Issues #15, #16
1.1.0	UWM changes:
    Address Issues #1, #2, #3, #4, #5, #6, #7, #9, #10, #11. #12, #13, #14
    Added implicit "and" judgments
    Induction and case analysis for HOAS
	Implemented all remaining checks:
	   inversion
	   mutual induction
	   exchange, weakening, substitution
	SLF files read in UTF-8: unicode operators permitted
	Various bug fixes
	Regression test files
	Improved error messages
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