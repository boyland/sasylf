package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Util.debug;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;  
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import edu.cmu.cs.sasylf.CopyData;
import edu.cmu.cs.sasylf.ModuleArgument;
import edu.cmu.cs.sasylf.SubstitutionData;
import edu.cmu.cs.sasylf.reduction.InductionSchema;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.UpdatableErrorReport;


public class Theorem extends RuleLike implements ModuleArgument {
	private String kind = "theorem";
	private String kindTitle = "Theorem";
	private NonTerminal assumes = null;
	private List<Fact> foralls = new ArrayList<Fact>(); // substitution here
	private Clause exists; // substitution here
	private List<Derivation> derivations;
	private Theorem andTheorem;
	private Theorem firstInGroup = this;
	private int indexInGroup = 0;
	private InductionSchema inductionScheme = InductionSchema.nullInduction;
	private boolean interfaceChecked=false;
	private boolean interfaceOK = false;
	private final boolean isAbstract;

	public Theorem(String n, Location l) { 
		this(n,l,false);
	}
	public Theorem(String n, Location l, boolean abs) { 
		super(n, l); 
		isAbstract = abs; 
		derivations = new ArrayList<Derivation>();
	}

	public List<Fact> getForalls() { return foralls; }
	@Override
	public List<Element> getPremises() {
		List<Element> l = new ArrayList<Element>();
		for (Fact f : foralls) {
			l.add(f.getElement());
		}
		return l;
	}
	@Override
	public Clause getConclusion() { return exists; }
	public Clause getExists() { return exists; }
	public List<Derivation> getDerivations() { return derivations; }

	public void setAnd(Theorem next) {
		debug("setting and of ", this.getName(), " to ", next.getName());
		andTheorem = next;
		andTheorem.firstInGroup = firstInGroup;
		andTheorem.indexInGroup = indexInGroup+1;
	}
	public Theorem getGroupLeader() {
		return firstInGroup;
	}
	public int getGroupIndex() {
		return indexInGroup;
	}
	public InductionSchema getInductionSchema() {
		return inductionScheme;
	}

	/** A theorem's existential variables are those that appear in its conclusion
	 * but not in its premises.
	 */
	@Override
	public Set<FreeVar> getExistentialVars() {
		Set<FreeVar> vars = exists.asTerm().getFreeVariables();
		for (Element e : getPremises()) {
			vars.removeAll(e.asTerm().getFreeVariables());
		}
		return vars;
	}

	@Override
	public void prettyPrint(PrintWriter out) {
		out.print(getKind());
		out.print(' ');
		out.print(getName());
		out.println(':');
		for (Fact forall : getForalls()) {
			out.print("  forall ");
			forall.prettyPrint(out);
			out.println();
		}
		out.print("  exists ");
		getExists().prettyPrint(out);
		out.println(".");
		/*for (Derivation d : derivations) {
			d.prettyPrint(out);
		}*/
		out.print("end ");
		out.print(getKind());
		out.println();
	}

	public void checkInterface(Context ctx) {
		if (!interfaceChecked) {
			ctx.bindingTypes = new HashMap<String, List<ElemType>>();
			int oldErrors = ErrorHandler.getErrorCount();
			interfaceChecked = true;
			if (assumes != null) {
				assumes.typecheck(ctx);
				SyntaxDeclaration syntax = assumes.getType();
				if (syntax == null || !syntax.isInContextForm()) {
					ErrorHandler.recoverableError(Errors.ILLEGAL_ASSUMES, assumes);
				}
			}
			List<String> inputNames = new ArrayList<String>();
			for (Fact f : foralls) {
				f.typecheck(ctx);
				inputNames.add(f.getName());
				if (f instanceof NonTerminalAssumption) {
					NonTerminalAssumption sa = (NonTerminalAssumption)f;
					NonTerminal root = sa.getRoot();
					if (root != null) {
						if (!root.getType().canAppearIn(sa.getSyntax().typeTerm())) {
							ErrorHandler.error(Errors.EXTRANEOUS_ASSUMES, f, "assumes " + root.toString());
						}
					}
				}
			}

			exists = exists.typecheck(ctx);
			Element computed = exists.computeClause(ctx, false);
			if (computed instanceof ClauseUse && computed.getType() instanceof Judgment) {
				exists = (Clause) computed;
			} else {
				ErrorHandler.recoverableError(Errors.EXISTS_SYNTAX,  computed);
			}

			inductionScheme = null;
			for (Derivation d : derivations) {
				if (d instanceof DerivationByInduction) {
					DerivationByInduction dbi = (DerivationByInduction)d;
					InductionSchema is = InductionSchema.create(this, dbi.getArgStrings(), true);
					if (is != null) {
						inductionScheme = is;
						// Inconsistency found later
						break;
					} else if (inductionScheme == null) {
						inductionScheme = InductionSchema.nullInduction; // prevent cascade error
					}
				}
			}
			if (inductionScheme == null) {
				if (this != firstInGroup || this.andTheorem != null) {
					ErrorHandler.warning(Errors.INDUCTION_MUTUAL_MISSING, this);
					inductionScheme = InductionSchema.create(this, foralls.get(0).getElement(), true);
				} else { 
					inductionScheme = InductionSchema.nullInduction;
				}
			}
			if (this != firstInGroup) {
				// for side-effect:
				inductionScheme.matches(firstInGroup.getInductionSchema(), this, false);
			}

			if (oldErrors == ErrorHandler.getErrorCount())  interfaceOK = true;
		}
	}

	public void typecheck(Context oldCtx) {
		if (edu.cmu.cs.sasylf.util.Util.VERBOSE) {
			System.out.println(getKindTitle() + " " + getName());
		}
		if (oldCtx.ruleMap.containsKey(getName())) {
			if (oldCtx.ruleMap.get(getName()) != this) {
				ErrorHandler.recoverableError(Errors.RULE_LIKE_REDECLARED, this);
			}
		} else {
			oldCtx.ruleMap.put(getName(), this); 
		}

		int oldErrorCount = ErrorHandler.getErrorCount();
		Context ctx = oldCtx.clone();
		try {
			debug("checking ", kind, " ", this.getName());

			ctx.derivationMap = new HashMap<String, Fact>();
			ctx.inputVars = new HashSet<FreeVar>();
			ctx.outputVars = new HashSet<FreeVar>();
			ctx.currentSub = new Substitution();
			ctx.currentTheorem = this;
			ctx.assumedContext = null;

			checkInterface(ctx);

			if (isAbstract) {
				if (!derivations.isEmpty()) {
					ErrorHandler.recoverableError(Errors.THEOREM_ABSTRACT, this);
				}
				return;
			}

			if (!interfaceOK || ErrorHandler.getErrorCount() > oldErrorCount) {
				return;
			}

			/*
    if (andTheorem != null) {
      andTheorem.addToMap(ctx);
    }*/
			ctx.recursiveTheorems = new HashMap<String, Theorem>();
			firstInGroup.addToMap(ctx);

			ctx.bindingTypes = new HashMap<String, List<ElemType>>();

			if (assumes != null) {
				ctx.assumedContext = assumes;
			}
			ctx.varFreeNTmap.clear();

			for (Fact f : foralls) {
				f.typecheck(ctx);
				f.addToDerivationMap(ctx);
				ctx.subderivations.put(f, new Pair<Fact,Integer>(f,0));
				Set<FreeVar> freeVariables = f.getElement().asTerm().getFreeVariables();
				ctx.inputVars.addAll(freeVariables);
			}

			Term theoremTerm = exists.asTerm();
			ctx.currentGoal = theoremTerm;
			ctx.currentGoalClause = exists;
			ctx.outputVars.addAll(theoremTerm.getFreeVariables());
			ctx.outputVars.removeAll(ctx.inputVars);
			
			for (Fact f : foralls) {
				NonTerminal root = f.getElement().getRoot();
				ctx.addKnownContext(root);
			}
			if (assumes != null) {
				boolean foundAssumption = false;
				for (Fact f : foralls) {
					NonTerminal root = f.getElement().getRoot();
					if (assumes.equals(root)) foundAssumption = true;
				}
				if (assumes.equals(exists.getRoot())) foundAssumption = true;
				if (!foundAssumption) {
					ErrorHandler.warning(Errors.EXTRANEOUS_ASSUMES, assumes);
				}
			}
			if (ctx.knownContexts != null && !ctx.knownContexts.isEmpty()) {
				if (ctx.knownContexts.size() > 1 || ctx.assumedContext != null) {
					ErrorHandler.recoverableError(Errors.THEOREM_MULTIPLE_CONTEXT, this);
				} else if (ctx.knownContexts.size() == 1) {
					NonTerminal root = ctx.knownContexts.iterator().next();
					if (assumes == null) {
						ErrorHandler.warning(Errors.ASSUMED_ASSUMES, this, "assumes " + root);
					}
				}
				if (assumes == null) { // Avoid further errors XXX: EXTENION POINT
					assumes = ctx.knownContexts.iterator().next();
					ctx.assumedContext = assumes;
				}
			}
			Derivation.typecheck(this, ctx, derivations);

		} catch (SASyLFError e) {
			// ignore the error; it has already been reported
			//e.printStackTrace();
		} finally {
			int newErrorCount = ErrorHandler.getErrorCount() - oldErrorCount;
			if (edu.cmu.cs.sasylf.util.Util.VERBOSE) {
				if (newErrorCount > 0) {
					System.out.println("Error(s) in " + getKind() + " " + getName());					
				}
			}
		}
	}

	private void addToMap(Context ctx) {
		checkInterface(ctx);
		ctx.recursiveTheorems.put(getName(), this);

		if (andTheorem != null) {
			andTheorem.addToMap(ctx);
		}
	}

	public void setKind(String k) {
		if (kind != null && kind.equals(k)) return;
		if (k == null) k = "theorem";
		if (k.length() == 0) k = "theorem";
		kind = k;
		kindTitle = Character.toTitleCase(k.charAt(0)) + kind.substring(1);
	}

	@Override
	public String getKind() {
		return kind;
	}

	public String getKindTitle() {
		return kindTitle;
	}

	/**
	 * Return true if this theorem has a well-defined interface,
	 * even if it wasn't successfully proved.  Theorems without
	 * OK interfaces should not be used.
	 * @return whether this theorem has a sensible interface
	 */
	@Override
	public boolean isInterfaceOK() {
		return interfaceOK;
	}

	/**
	 * Set the assumption (context) for a theorem.
	 * An internal error is thrown if this method is called
	 * twice with different values.
	 * @param c context to use
	 */
	public void setAssumes(NonTerminal c) { 
		if (assumes != null && !assumes.equals(c))
			ErrorHandler.error(Errors.INTERNAL_ERROR,"setAssumes: " + assumes + " != " + c, this);
		assumes = c; 
	}
	@Override
	public NonTerminal getAssumes() { return assumes; }

	public void setExists(Clause c) { 
		List<Element> elems = c.getElements();
		int n = elems.size();
		// We remove the "dot" at the end, if it is there (old style)
		if (n >= 1) {
			Element last = elems.get(n-1);
			if (last instanceof Terminal && ((Terminal)last).getName().equals(".")) {
				elems.remove(n-1);
				--n;
				if (n > 0) {
					c.setEndLocation((elems.get(n-1).getEndLocation()));
				}
			}
		}
		while (n == 1 && elems.get(0) instanceof Clause) {
			c = (Clause)elems.get(0);
			elems = c.getElements();
			n = elems.size();
		}
		exists = c; 
	}
	
	
	@Override
	public void collectQualNames(Consumer<QualName> consumer) {
		for (Derivation derivation : derivations) {
			derivation.collectQualNames(consumer);
		}
	}

	@Override
	public void substitute(SubstitutionData sd) {
		
		if (sd.didSubstituteFor(this)) return;
		sd.setSubstitutedFor(this);
		
		// substitute in foralls
		for (Fact f : foralls) {
			f.substitute(sd);
		}

		// substitute in exists
		exists.substitute(sd);

		// We are not substituting in derivations, as of now
	}

	@Override
	public Theorem copy(CopyData cd) {
		if (cd.containsCopyFor(this)) return (Theorem) cd.getCopyFor(this);
		
		Theorem clone;
		try {
			clone = (Theorem) super.clone();
		} 
		catch (CloneNotSupportedException e) {
			UpdatableErrorReport report = new UpdatableErrorReport(Errors.INTERNAL_ERROR, "Clone not supported in class: " + getClass(), this);
			throw new SASyLFError(report);
		}

		cd.addCopyFor(this, clone);

		// skip kind and kindTitle

		if (clone.assumes != null) {
			clone.assumes = assumes.copy(cd);
		}

		List<Fact> newForalls = new ArrayList<Fact>();
		for (Fact f : foralls) {
			newForalls.add(f.copy(cd)); // fix this
		}
		clone.foralls = newForalls;

		clone.exists = clone.exists.copy(cd);

		clone.derivations = new ArrayList<>(); // don't clone the derivations

		if (clone.andTheorem != null) {
			clone.andTheorem = clone.andTheorem.copy(cd);
		}

		clone.firstInGroup = clone.firstInGroup.copy(cd);

		return clone;
	}

	@Override
	public Optional<SubstitutionData> matchesParam(
		ModuleArgument paramModArg,
		ModulePart mp,
		Map<Syntax, Syntax> paramToArgSyntax,
		Map<Judgment, Judgment> paramToArgJudgment) {
		
		if (!(paramModArg instanceof Theorem)) {
			// the wrong type of argument has been provided

			String argKind = this.getKind();
			String paramKind = paramModArg.getKind();

			// throw an exception

			ErrorHandler.modArgTypeMismatch(argKind, paramKind, mp);
			return Optional.empty();
		}

		// they are of the same type, so cast the parameter to a Theorem

		Theorem param = (Theorem) paramModArg;
		Theorem arg = this;

		// now, we need to check if the two theorems are compatible with eachother

		// verify that the forall clauses match

		List<Fact> paramForalls = param.getForalls();
		List<Fact> argForalls = this.getForalls();

		if (paramForalls.size() != argForalls.size()) {
			ErrorHandler.modArgTheoremWrongNumForalls(arg, param, mp);
			return Optional.empty();
		}

		// check that each pair of foralls has the same structure

		/*
		 * The code below could probably be abstracted into a different class
		 */

		Map<String, String> nonTerminalMapping = new HashMap<String, String>();
		
		for (int i = 0; i < paramForalls.size(); i++) {
			Fact paramForall = paramForalls.get(i);
			Fact argForall = argForalls.get(i);

			Element paramElement = paramForall.getElement();
			Element argElement = argForall.getElement();

			// paramElement and argElement should either both be nonterminals or both be clauses


			if (paramElement instanceof Clause && argElement instanceof Clause) {
				Clause paramClause = (Clause) paramElement;
				Clause argClause = (Clause) argElement;
				// check that they have the same structure
				boolean match = Clause.checkClauseSameStructure(paramClause, argClause, paramToArgSyntax, paramToArgJudgment, nonTerminalMapping, mp);
				if (!match) return Optional.empty();
			}

			else if (paramElement instanceof NonTerminal && argElement instanceof NonTerminal) {
				NonTerminal paramNonTerminal = (NonTerminal) paramElement;
				NonTerminal argNonTerminal = (NonTerminal) argElement;
				// Make sure that the types of the nonterminals match
				if (paramToArgSyntax.containsKey(paramNonTerminal.getType())) {
					if (paramToArgSyntax.get(paramNonTerminal.getType()) != argNonTerminal.getType()) {
						ErrorHandler.modArgClauseNonterminalTypeMismatch(argNonTerminal, paramNonTerminal, paramToArgSyntax, mp);
						return Optional.empty();
					}
				}
			}
		}

		// verify that the exists clauses match

		Clause paramExists = param.getExists();
		Clause argExists = this.getExists();

		boolean existsMatch = Clause.checkClauseSameStructure(paramExists, argExists, paramToArgSyntax, paramToArgJudgment, nonTerminalMapping, mp);

		if (!existsMatch) return Optional.empty();

		// they match

		SubstitutionData sd = new SubstitutionData(param.getName(), arg.getName(), arg);

		return Optional.of(sd);

	}

}

