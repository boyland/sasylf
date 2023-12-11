# SASyLF: An Educational Proof Assistant for Language Theory

Teaching and learning formal programming language theory is hard, in part because it's easy to make mistakes and hard to find them.  Proof assistants can help check proofs, but their learning curve is too steep to use in most classes, and is a barrier to researchers too.

SASyLF (pronounced "Sassy Elf") is an LF-based proof assistant specialized to checking theorems about programming languages and logics.  SASyLF has a simple design philosophy: language and logic syntax, semantics, and meta-theory should be written as closely as possible to the way it is done on paper.  SASyLF can express proofs typical of an introductory graduate type theory course.  SASyLF proofs are generally very explicit, but its built-in support for variable binding provides substitution properties for free and avoids awkward variable encodings.

## Installing and Running

The [Wiki](https://github.com/boyland/sasylf/wiki) has pages for [installing](https://github.com/boyland/sasylf/wiki/Install) and [running](https://github.com/boyland/sasylf/wiki/Running-SASyLF) SASyLF.

Summary: To use VSCode, type SASyLF in the Extensions:Marketplace search box and install the [extension](https://marketplace.visualstudio.com/items?itemName=sasylf.SASyLF).  To use the Eclipse IDE or the command line, get the JAR file from the release page and either put in the `dropins/` folder of the Eclipse distribution or use it with `java -jar`.

## Documentation

See the [Wiki](https://github.com/boyland/sasylf/wiki) for documentation.

## SASyLF Examples

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
 * featherweight-java.slf - Definition of Featherweight Java (soundness proof omitted)
 * lambda-unicode.slf - version of lambda.slf using unicode identifiers and operators
 * cut-elimination - demonstrate some of the more advanced features of SASyLF 1.3
 * LF.slf - mechanization of main results of Harper & Licata's "Mechanizing Metatheory in a Logical Framework" (2007).  This example uses features first in SASyLF 1.5


## Known Limitations (incomplete list)

Only one context nonterminal permitted per judgment/theorem.

Derivations with different contexts cannot be conjoined or disjoined.

Error messages point to the line of code and the kind of error, but could use some improvement.

The automated prover ("by solve") works poorly and is unmaintained.  It works only for straightforward derivations without the use of induction or case analysis.  Its use is deprecated.


## Detailed Change Log

See ChangeLog.txt for more details on feature history.


## Contact

If you have any trouble installing or running SASyLF, or understanding
how to use the tool or interpret its output, contact John Boyland
<boyland@uwm.edu> or else submit an issue report on the github site.


## Contributing

Third-party contributors are welcome to submit pull requests.  Development requires an Eclipse "for Eclipse committers" IDE using Java 8.  JavaCC is required as well.  If you are changing any code in the core (edu.cmu.cs.sasylf packages), please make sure your change passes all the tests ("make test") before submitting a pull request.

This directory is an Eclipse project and can be compiled with Eclipse 4.8
(Photon) or later.  You will need to compile the parser file

> edu/cmu/cs/sasylf/parser/parser.jj 

with the JavaCC Eclipse plugin, available from update site
http://eclipse-javacc.sourceforge.net/ (compile the .jj file to .java
by right-clicking on parser.jj and choosing Compile with JavaCC).

Alternatively, if you fetch the source from github, you can build SASyLF
(under Unix) assuming you have java and javacc in your path using
>	make build

There's no easy way to build the Eclipse plugin from the command-line.


