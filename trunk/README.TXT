SASyLF version 1.3.3a7
Copyright (c) 2009 by Jonathan Aldrich
Additional contributions by John Boyland

This software is distributed under the Eclipse Public License Version 1.0
("EPL"). A copy of the EPL is provided with this distribution and is also
available at http://www.eclipse.org/legal/epl-v10.html.

PermutationIterator from Apache Commons licensed under 
http://www.apache.org/licenses/LICENSE-2.0




INSTALLATION

To run SASyLF, you must have Java version SE 6 or later installed.  You
can get Java at:

http://java.sun.com/javase/downloads/index.jsp




RUNNING SASyLF

You can run SASyLF within Eclipse by downloading the plugin into
the "dropins" folder of your Eclipse distribution.
 
 
You can run SASyLF on the command line
by explicitly invoking java on the SASyLF.jar file,
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

This directory is an Eclipse project and can be compiled with Eclipse 4.2
(Juno) or later.  You will need to compile the edu/cmu/cs/sasylf/parser/parser.jj 
file with the JavaCC Eclipse plugin, available from update site
http://eclipse-javacc.sourceforge.net/ (compile the .jj file to .java
by right-clicking on parser.jj and choosing Compile with JavaCC).

Alternatively, if you fetch the source from SVN, you can build SASyLF
(under Unix) assuming you have java and javacc in your path using
	make build



CONTACT

If you have any trouble installing or running SASyLF, or understanding
how to use the tool or interpret its output, contact John Boyland
<boyland@uwm.edu> or else submit an issue report on the Google code site,
or discuss the problem on the Google group http://groups.google.com/group/sasylf-users



KNOWN LIMITATIONS (incomplete list)

Only one context nonterminal permitted per judgment/theorem.

Error messages point to the line of code and the kind of error, but
could use some improvement.

The automated prover ("by solve") works poorly.  It works only
for straightforward derivations without the use of induction or case
analysis.  Its use is deprecated.



RELEASE HISTORY

1.3.3   Enhancements and Bug fixes
    Fixed input var bugs (bad36.slf, bad37.slf)
    Fixed partial case analysis bugs (bad39.slf, good22.slf)
    Added bug test for issue #51 (bad38.slf) 
    Fixed theorem interface bugs (bad40.slf, good23.slf, good24.slf, good26.slf, good27.slf)
    Fixed nested clause bugs (bad47.slf bad48.slf)
    An 'or' goal can be proven by proving a subset, or an individual element.
    warning when identifier for derivation already declared.
    Pattern matching of judgments with contexts > 1 is now supported.
    Higher-order unification doesn't make greedy (unsound) assumptions (bad45.slf, bad46.slf)
    nontermination issue in GLR(0) parser fixed (see good25.slf)
    Resolved issues #59, #61
1.3.2	Cleanup
    Add backward compatibility to give 1.2.X experience (1.2.X no longer maintained)
    Fix missing case insertion to quote special terminals
    Code reorganization / speedup
    Warn about overly general cases (see bad35.slf)
    Resolved issues #53, #56, #57, #58
1.3.1	Extensions (& 1.2.6)
	Abstract modules
	"or" judgments
	"contradiction" is sugar for or[], and or[] can be used as a last derivation 
	Lexicographic and unordered induction
	Proof of cut elimination
	Bug fixes: bad23
	Resolved issues #33, #50
1.3.0	Packages and Projects (& 1.2.5)
    The package declaration has a semantics now and we require folders = packages.
    Eclipse plugin updated to add project and package support and builders
    Editor has fold support
    Resolved issues #45
1.2.6   Bug fixes to stable 1.2
    Unproved warning now gives derivation to prove
    Substitution semantics changed for complex contexts, poplmark-2a proof updated
    'use induction on' syntax added
    "by inversion" cleaned-up
    Resolved issues #47, #48, #49, #52, #54
1.2.5   Bug fixes to stable 1.2
    Resolved issues #43, #44, #46
1.2.4	Minor bug fixes and plugin enhancements
    Quick Fixes & Preferences page added (only one property for now)
    Fixed bug from 1.2.3 that some versions of lambda-loc.slf gave an error
    poplmark2a.slf no longer requires "unproved"
    starting to supported mixed judgments and theorems
    more checking of assumes and losing of context
    Stricter separation of rules/lemmas/theorems
    Resolved issues #8, #32, #34, #37, #38, #39, #40, #41, #42
1.2.3	Improvements to plugin and bug fixes
    Command-line tool reports a count of warnings too.
    Proof tool doesn't give up on first error in a list any more
    plugin now has a "New Proof" wizard
    plugin has auto indent and indentation correction in editor
    plugin has "Open Declaration" Popup menu.
    plugin improved information in outline window (added rules)
    plugin has basic content-assist enabled.
    Resolved issues #24, #26, #27, #29, #30, #31
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