# Introduction #

The body of a theorem is a block of steps (derivations), the last of which must be the result to be proved.
This page describes how each step in a proof is structured, and how steps can be proved in SASyLF.

The meta-syntax ( _stuff_ )? is used to express optional parts and ellipsis (...) to indicate appropriate repetion.

# Details #

A derivation has the form:
  * _name_ : _judgment_  (`and` _name_ : _judgment_ ...)? `by` _justification_
Alternately, the last derivation of a block may use the keyword `proof` to substitute for all that comes before the keyword `by`; it is implied that one is justifying the result that must be proved.

In place of a derivation, one may use the syntax
  * `use inversion of` _rule\_name_ `on` _name_if the case analysis of _name_ has only one case (_rule\_name_) and that rule has no premises.  This derivation doesn't prove anything, but it performs any unification of variables presumed by the case.  This special form (new to SASyLF 1.2.3) is useful for inverting equality judgments which require absolute identity.  As of SASyLF 1.2.5, the rule could have premises; they are simply ignored.

In place of a derivation, one may also use the syntax (new to SASyLF 1.2.6)
  * `use induction on` _name_
which marks that induction will be on the given derivation/nonterminal without forcing an immediate case analysis.  It has been a long-standing annoyance that these were coupled.  Starting in SASyLF 1.3.1, instead of a name, one may use a comma-separated series of names for lexicographic induction.  Furthermore, one may use, in place of a name, a set of names (`{` _name_ `,` ... `}`) for unordered induction which means that some permutation of the names has every corresponding argument to the induction hypothesis staying the same or getting smaller, with at least one getting smaller.

## Justification ##

This section described each of the ways in which derivations can be justified:
  * `by unproved`
> > This justification is accepted with a warning.  This justification is useful when
> > writing a proof to outline steps before the proof is completed.

  * `by` _name_
> > This proves the current derivation by referencing a previous derivation.  1.3.1 and later: If the derivation being proved is a disjunction (judgments `or`ed together), then the derivation named may be any one of the constituents.

  * `by` _name_ `,` _name_ `,` ...
> > This proves a conjunction derivation (an `and` judgment)
> > by showing how each part was proved     earlier.

  * `by case analysis on` _name_ `:` _case1_ _case2_ ... `end case analysis`
> > This justification is delegated to a case analysis on a _target_, an existing derivation or syntax term.
> > Each case has a pattern that must match the target.
> > Pattern matching is at a single level only.
> > Each case has a block of derivations, the last of which must be the derivation
> > being proved here (and which therefore may use the `proof` keyword).

  * `by induction on` _name_ `:` _case1_ _case2_ ... `end induction`
> > This justification is a case analysis that, at the same time, indicates the inductive
> > term, judgment which must be an explicit input of the theorem.
> > This form of justification can only be done at the top-level of a theorem,
> > and there can only be one induction justification per theorem.

  * `by inversion of` _rule-name_ `on` _name_
> > If a case analysis would need only one case and there is at least one premise,
> > then it can be written as a justification of the premises (conjoined by `and` if there are
> > more than one) using this form.<br>
<blockquote>An older form of inversion may be used if one is interested in only a single premise<br>
(even if multiple premises exist for the rule),<br>
and this premise does not require the binding of new variables.  Then one<br>
can justify just this single premise by inversion.</blockquote></li></ul>

<ul><li><code>by contradiction on</code> <i>name</i>
<blockquote>(This syntax was added only in SASyLF 1.2.3)<br>
If case analysis on <i>name</i> needs <b>no</b> cases, then<br>
you can use this form of justification.<br>
</blockquote></li><li><code>by rule</code> <i>rule-name</i> (<code>on</code> <i>name</i> <code>,</code> <i>name</i> ...)?<br>
<blockquote>The derivation is justified by applying the given named rule to the previously proved derivations.<br>
The derivations must satisfy the rule's premises, in order.</blockquote></li></ul>

<ul><li><code>by theorem</code> theorem-name (<code>on</code> <i>name</i> <code>,</code> <code>(</code><i>syntax</i><code>)</code> <code>,</code> ...)?<br>
<blockquote>The derivation is proved by calling a theorem with the given inputs; judgment<br>
inputs are provided using previously proved derivations, syntax inputs by giving<br>
the syntax, which if more than a single term, must be parenthesized.<br>
<br>
If the theorem being called is the one being proved, or a mutually inductive theorem,<br>
then the provided input for the induction variable must be a subderivation / subsyntax<br>
of the input for this theorem.  If the mutually inductive theorem occurs earlier in the<br>
file, the inductive argument may be the same as the one for this theorem.</blockquote></li></ul>

  * `by induction hypothesis` (`on` ...)?
> > This is an inductive (recursive) call to the theorem being proved.  Otherwise,
> > it works the same as a theorem call (see previous).

  * `by solve`
> > This justification asks SASyLF to try to find a proof.  If it can, it prints the steps.
> > Automatic solving is very limited.

The following three justifications concern contexts and bindings:
  * `by weakening on` _name_
> > This justification applies if the derivation can be proved from an existing derivation by adding additional variables/assumptions.  The names of variables must not change.

  * `by exchange on` _name_
> > This justification applies if the derivation can be proved from an existing derivation by reordering variable bindings and/or assumptions. The names of variables must not change. The new derivation must not move uses of a variable out of scope.

  * `by substitution on`  _name_`,` _name_
> > This justification applies if the derivation can be proved from an existing derivation (first argument) with an assumption supplied (second argument).  The assumption must be an instance of the special rule related to the binding being replaced.  It is not a syntax term to substitute for the bound variable.


> Substitution was given a cleaner semantics in 1.2.6/1.3.1. Assuming we have
> > `d1:` _bindings_`, x : T,` _other bindings_ `|- t[x] : T'`<br>
<blockquote><code>d2:</code> <i>bindings</i> <code>|- t2 : T</code>
</blockquote><blockquote>then <code>substitution on d1, d2</code> will give us<br>
<blockquote><i>bindings</i><code>,</code> <i>other bindings</i> <code>|- t[t2] : T'</code></blockquote></blockquote>

<blockquote>Substitution requires that the second argument be an instance of the judgment which has the special rule for the binding.  The <i>bindings</i> of the second derivation must appear in the same order at the start of the second derivation. Different than <code>weakening</code> or <code>exchange</code> above, the <i>bindings</i> in common <i>may</i> have different variable names.  This ability is important when performing contraction like operations:<br>
<blockquote><code>d1 : Gamma, x1 : T, x : T |- t[x,x1] : T'</code><br>
<code>d2 : Gamma, x : T |- x : T</code>
</blockquote>yields<br>
<blockquote><code>Gamma, x : T |- t[x,x] : T'</code></blockquote></blockquote>

<h2>Cases</h2>

There are three forms for cases: those for syntax, those for judgments, and those for disjunctions.<br>
<br>
<h3>Syntax Case</h3>

If the target of the case analysis is a syntax term (instance of a syntactic nonterminal), then (aside from variables) one must provide one case for each production for the nonterminal.  Each case takes the form<br>
<br>
<blockquote><code>case</code> <i>production</i> <code>is</code> <i>proof</i> <code>end case</code></blockquote>

All the variables in the production must be new, for example by adding a suffix such as <code>1</code> or <code>'</code>.<br>
If the syntax term is bound in a non-empty context and a variable is one of the possibilities for the nonterminal, then one must provide a variable case that shows a possible binding for the variable in the context (using a new context):<br>
<br>
<blockquote><code>case</code> x <code>assumes</code> (Gamma', x:T) <code>is</code> <i>proof</i> <code>end case</code></blockquote>

SASyLF currently does not support case analysis of terms using a variable, such as <code>t1[x]</code>.<br>
<br>
<h3>Rule Case</h3>

If target of a case analysis is an instance of a judgment, then each case must be a <i>rule case</i>
of the form:<br>
<br>
<blockquote><code>case rule</code>
<blockquote><i>name</i> <code>:</code> <i>premise</i><br>
...<br>
<code>----------------</code> <i>rule-name</i><br>
<i>name</i> <code>:</code> <i>result</i>
</blockquote><code>is</code>
<blockquote><i>proof</i>
</blockquote><code>end case</code></blockquote>

Each premise and the result must be named, although the result is usually named <code>_</code> because there's no need to get a new name for the target derivation we are pattern matching.<br>
<br>
The case must have the most general instance of the rule whose result matches the target derivation.<br>
If the rule result cannot be made to match the target, it is an error to have a case for the rule at all.<br>
In the body of the proof, one can use the named premises to justify other derivations.<br>
One may also assume the unification implied by the pattern match.  All uses inside the local proof of any derivation from outside the scope of the pattern match are localized through application of the required unification.<br>
<br>
If, because of pattern matching, only one case is possible, <i>inversion</i> (q.v.) may be a good option to use.<br>
If <i>no</i> case applies at all, then the case analysis can be empty.  This allows us to justify any (type valid) judgment at all.<br>
<br>
<h4>Context Cases</h4>

<i>TODO</i>

<h3>Disjunction Cases</h3>

If the derivation being matched is a disjunction (using the <code>or</code> syntax of SASyLF 1.3.1 and later), then the case has the form<br>
<br>
<blockquote><code>case or</code> <i>name</i> <code>:</code> <i>premise</i> <code>is</code>
<blockquote><i>proof</i>
</blockquote><code>end case</code></blockquote>

A disjunction derivation is true if there is a derivation for one of the parts; the case handles this one.  To cover all cases, one must have a case for each part.<br>
<br>
<h2>Partial Case Analysis</h2>

When writing a proof, one may wish to handle certain situations up-front in a partial case analysis.  Starting in SASyLF 1.3.1, one can write<br>
<br>
<blockquote><code>do case analysis on</code> <i>name</i>
<blockquote><i>case1</i>
<i>case2</i>
...<br>
</blockquote><code>end case analysis</code></blockquote>

Each case should prove the statement required by the context. The case analysis need not be complete, and this form cannot be used as the last statement in a series precisely because it doesn't finish the proof.  But the cases that are handled here will not need to be handled later.  This can reduce a Cartesian product of cases to a more manageable number, as seen in the proof of cut elimination.<br>
<br>
<h2>Last Derivation</h2>

The last derivation of a block should be for the judgment being proved.  As a special case (since 1.3.1), if this is an empty disjunction (<code>contradiction</code>) it is accepted as a proof for anything.<br>
This last judgment must be the empty disjunction, not a judgment that case analysis can show is uninhabited; for the latter case, you must continue to use a case analysis, for example <code>by contradiction on</code>.