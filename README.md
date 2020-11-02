# SASyLF: An Educational Proof Assistant for Language Theory

Teaching and learning formal programming language theory is hard, in part because it's easy to make mistakes and hard to find them.  Proof assistants can help check proofs, but their learning curve is too steep to use in most classes, and is a barrier to researchers too.

SASyLF (pronounced "Sassy Elf") is an LF-based proof assistant specialized to checking theorems about programming languages and logics.  SASyLF has a simple design philosophy: language and logic syntax, semantics, and meta-theory should be written as closely as possible to the way it is done on paper.  SASyLF can express proofs typical of an introductory graduate type theory course.  SASyLF proofs are generally very explicit, but its built-in support for variable binding provides substitution properties for free and avoids awkward variable encodings.

See the Wiki for documentation.

See README.TXT for release notes

## Contributing

Third-party contributors are welcome to submit pull requests.  Development requires an Eclipse "for Eclipse committers" IDE using Java 7 or 8.  JavaCC is required as well.
