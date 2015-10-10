# Introduction #

SASyLF is a natural-language based proof system.
We are using this Wiki format to create a manual of use.
This documentation covers SASyLF 1.1.0 and later releases.


# Structure of SASyLF Files #

A SASyLF file consists of the following parts:
  * An optional package declaration
  * A list of terminals
  * Description of (abstract) syntax
  * Declaration of judgments and the rules for each.
  * Theorems
Currently, there is no way to refer to elements declared in other files.  Starting in SASyLF 1.2.4, you can interleave judgments and theorems.

## Example ##


```
package edu.uwm.cs.plus;

terminals S

syntax

n ::= 0
  | S n


judgment plus: n + n = n

--------- plus-zero
0 + n = n

n1 + n2 = n3
---------------- plus-succ
S n1 + n2 = S n3


lemma plus-right-zero:
    forall n
    exists n + 0 = n.
    proof by induction on n:
        case 0 is
            _: 0 + 0 = 0 by rule plus-zero
        end case

        case S n' is
            p': n' + 0 = n' by induction hypothesis on n'
            _: S n' + 0 = S n' by rule plus-succ on p'
        end case
    end induction
end lemma
```

## Comments ##

SASyLF files support both kinds of C++ comments: line comments starting with `//` and comments starting `/*` and ending with `*/`.  Sometimes a linebreak is semantically meaningful, in which case it is important to know that both kinds of comments swallow linebreaks.

## Packages ##

Starting in 1.3.0, if a proof file is in a project with SASyLF "nature", or if on the command line, a base directory is specified (with `--root=DIR`, then the package declaration must match the directory in which the SASyLF proof file is located, in the same way as in Java.  A project with SASyLF nature may specify what the base directory is; by default it is `slf`.

If the proof file is directly in the base directory, then no package declaration should be used, again as with Java.


## Terminals ##

In the abstract syntax, any terminals that consist of (Unicode) letters must be declared
as terminals.  Terminals consisting entirely of non-letter (Unicode) characters need not and should not be declared.  In the preceding example, `S` was declared, but not `0`.

## Syntax ##

BNF is used for abstract syntax.  Any word (series of letters) is a nonterminal unless it was
declared as a terminal.  Nonterminals may be declared in any order, but all nonterminals must be declared.  SASyLF keywords are usually not permitted as terminals (or nonterminals),
but as an exception `case` and `end` may be used.  Neither may SASyLF special operators:
the period `.`, the pipe `|`, parentheses `()`, brackets `[]`, and a long series of dashes (at least 3 long).

The syntax may use HOAS by using brackets after a nonterminal.  Inside the brackets is a _variable_ name (traditionally `x`) that must occur elsewhere in the same production.  Exactly one production for some nonterminal must consist only of the variable name.  This indicates the nonterminal associated with the variable.
Any other use of a variable can only be used for a 'context' nonterminal.

A context nonterminal must have one production with nothing in it but terminals, and the other productions should be _binding_ productions: with a recursive use of the context nonterminal, a single occurrence of one variable and other terminals and nonterminals, for example:
```
Gamma ::= *
         | Gamma, x : T
         | Gamma, X :: K
```
Currently each variable must be bound by exactly one context.

## Judgments ##

Each judgment is declared with a name, used internally and for identification, but otherwise unimportant and a syntax production.  If a judgment includes a context nonterminal, it must additionally be declared that is `assumes` the nonterminal.  Currently judgments can only assume a single context nonterminal.

After the judgment declaration are the rules that give the (definitive) rules for generating facts of the judgment.  Each rule consists of a series of _premises_, instances of judgments (the same judgment or other judgments) each on its own line.  If a premise must cross a line boundary, the linebreak should be hidden using a `//` comment.  After all the premises is a bar made up of many (at least 3) minus/hyphen symbols (as seen in the example) and the name of the rule, which unlike many
names in SASyLF may include hyphens.
After the bar comes the conclusion, an instance of the judgment being declared.

Judgments may be used in the premises of other judgments, before or after their own declaration without restriction, but all the rules following a judgment declaration must have conclusions
that are instances of that judgment.

Judgments and syntax are parsed using a GLR parser that finds all legal parses; an error is signaled if there are more than one possible parse.  It may be necessary to use parentheses to make the structure unambiguous; sometimes parentheses are used to help the human reader as well.

Every binding of a variable in a context should be associated with exactly one rule which has no premises and in which the conclusion has one instance of the context binding production and one additional instance of the variable.  Every non-terminal that occurs in the context binding must occur elsewhere in the conclusion, and every non-terminal that occurs elsewhere in the conclusion must occur also in the context binding.

SASyLF uses "higher-order unification" to match rule cases against rules, but higher-order unification is undecidable in general, and thus occasionally SASyLF will report a "unification incomplete" error.  This possibility becomes a near certainty if the rule has terms of the form `T1[T2]` (a term `T1` with a hole in it filled by `T2`) where `T2` has no other occurrence in the rule.  SASyLF will issue a warning on the rule declaration in such situations.

### And/Or Judgments ###

Since 1.1.0, SASyLF creates `and` judgments on demand, and since 1.3.1 `or` judgments are created in a similar way.  For example a theorem may have
> `exists (n1 > 0 and n2 > 0) or (n1 == 0 and n2 == 0).`
In both cases, if the judgments assume contexts, all the assumptions must be the same, and the contexts for the joined judgments must be the same.  Thus one **cannot** write
> `exists Gamma |- t1 : T and Gamma, x:T |- t[x] : T`
The `and` construct is not allowed at the outer level of a premise or a `forall` argument to a theorem; it would be simpler anyway to separate such a judgment into separate premises or arguments.

If a derivation being proved is an `and` judgment, the derivation should be written as conjoined named derivations, as in:
> `g1: n1 + n2 = n3 and g2: n3 + n4 = n7 by ...`
Then one can refer to each name individually.

Disjunctions are treated more like normal judgments, except SASyLF provides a special case analysis form for them.

The empty disjunction is written `contradiction` unless a judgment for this form is already defined.  There is currently no way to express a (trivial) empty conjunction.

## Theorems ##

A theorem has inputs (`forall`) and outputs (`exists`).
The inputs are either syntax, for which one uses a name such as `t1'` implicitly giving the type (here `t`) or a judgment which must be named.  The theorem may be declared as potentially using variables from a context by adding the declaration `assumes` _cname_ where _cname_ is a context nonterminal.  The same qualification may also be added to a nonterminal input.  A judgment input will already indicate whether it `assumes` the context.
The result (after `exists`) is not named but must be terminated with a period (`.`).
Any names occurring in the output that do not occur in the input are implicitly outputs.
If one needs to have multiple judgment outputs, one conjoins the outputs using `and`.
The body of the theorem is a proof (See [Proofs](Proofs.md)).
In summary the format is as follows:

`theorem` <i>theorem-name</i> `:`
> ( `assumes` <i>cname</i> )?

> `forall` <i>name</i> ( `assumes` <i>cname</i> )?

> `forall` <i>name</i> `:` <i>judgment</i>
> > ...

> `exists` <i>judgment</i> (`and` <i>judgment</i> ...) `.`

> <i>proof</i>
`end theorem`

Instead of the keyword `theorem`, one may use `lemma`.  SASyLF 1.2.4 and later distinguishes whether a theorem is declared as a theorem rather than a lemma, but they are used in the same way.
Along with rules, theorem names may include hyphens.

One may declare theorems (or lemmas) as mutually inductive by joining them with the `and` keyword.
If a mutually inductive theorem does not explicitly give its induction variable (as explained in the [Proofs](Proofs.md)
page), the first argument is assumed to be the inductive argument.
See [Proofs](Proofs.md) for details in how proofs are written.