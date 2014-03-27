package edu.cmu.cs.sasylf.ast;

import java.util.*;
import java.io.*;

import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.term.UnificationFailed;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.SASyLFError;

import static edu.cmu.cs.sasylf.util.Util.*;


public class Theorem extends RuleLike {
	public Theorem(String n, Location l) { super(n, l); }

	public List<Fact> getForalls() { return foralls; }
	public List<Element> getPremises() {
		List<Element> l = new ArrayList<Element>();
		for (Fact f : foralls) {
			l.add(f.getElement());
		}
		return l;
	}
	public Clause getConclusion() { return exists; }
	public Clause getExists() { return exists; }
	public List<Derivation> getDerivations() { return derivations; }
	public void setAnd(Theorem next) {
		debug("setting and of "+this.getName() + " to " + next.getName());
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
	public int getInductionIndex() {
	  return inductionIndex;
	}

	/** A theorem's existential variables are those that appear in its conclusion
	 * but not in its premises.
	 */
    public Set<FreeVar> getExistentialVars() {
    	Set<FreeVar> vars = exists.asTerm().getFreeVariables();
    	for (Element e : getPremises()) {
    		vars.removeAll(e.asTerm().getFreeVariables());
    	}
    	return vars;
    }

	public void prettyPrint(PrintWriter out) {
		out.println(getKind() + " " + getName() + ":");
		for (Fact forall : getForalls()) {
			out.print("forall ");
			forall.prettyPrint(out);
			out.print(' ');
		}
		out.print("exists ");
		getExists().prettyPrint(out);
		out.println(".");
		for (Derivation d : derivations) {
			d.prettyPrint(out);
		}
		out.println("end theorem\n");
	}
	
	public void checkInterface(Context ctx) {
		if (!interfaceChecked) {
		  int oldErrors = ErrorHandler.getErrorCount();
			interfaceChecked = true;
			if (assumes != null) {
			  Syntax syntax = assumes.getType();
			  if (syntax == null || !syntax.isInContextForm()) {
			    ErrorHandler.recoverableError(Errors.ILLEGAL_ASSUMES, this);
			  }
			}
			List<String> inputNames = new ArrayList<String>();
			for (Fact f : foralls) {
				f.typecheck(ctx);
				inputNames.add(f.getName());
				// TODO: rationalize the following special cases:
				if (f instanceof DerivationByAssumption) {
	        ClauseUse cu = (ClauseUse)f.getElement();
	        cu.asTerm();
	        if (cu.getRoot() != null) {
	          if (assumes == null) {
	            ErrorHandler.warning(Errors.ASSUMED_ASSUMES, this, "assumes " +cu.getRoot().toString());
	          }
	          setAssumes(cu.getRoot());
	        }				  
				} else if (f instanceof NonTerminalAssumption) {
				  NonTerminalAssumption sa = (NonTerminalAssumption)f;
				  NonTerminal root = sa.getRoot();
				  if (root != null) {
            if (assumes == null) {
              ErrorHandler.warning(Errors.ASSUMED_ASSUMES, this, "assumes " + root.toString());
            }
				    if (!root.getType().canAppearIn(sa.getSyntax().typeTerm())) {
				      ErrorHandler.report(Errors.EXTRANEOUS_ASSUMES, f, "assumes " + root.toString());
				    }
				    setAssumes(root);
				  }
				}
			}
	
			exists.typecheck(ctx);
			Element computed = exists.computeClause(ctx, false);
			if (computed instanceof ClauseUse && computed.getType() instanceof Judgment) {
			  exists = (Clause) computed;
			} else {
			  ErrorHandler.recoverableError("'exists' of theorem must be a judgment, not syntax",  computed);
			}
			
			for (Derivation d : derivations) {
			  if (d instanceof DerivationByInduction) {
			    DerivationByInduction dbi = (DerivationByInduction)d;
			    String dn = dbi.getTargetDerivationName();
			    int i = inputNames.indexOf(dn);
			    if (i == -1) {
			      ErrorHandler.report("Induction target "+ dn +" must be an explicit forall argument of this theorem/lemma", this);
			    } else inductionIndex = i;
			  }
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
    } else oldCtx.ruleMap.put(getName(), this);
    
		int oldErrorCount = ErrorHandler.getErrorCount();
		Context ctx = oldCtx.clone();
		try {
		debug("checking "+kind+" "+this.getName());
		
		ctx.derivationMap = new HashMap<String, Fact>();
		ctx.inputVars = new HashSet<FreeVar>();
		ctx.outputVars = new HashSet<FreeVar>();
		ctx.currentSub = new Substitution();
		ctx.currentTheorem = this;
		ctx.inductionVariable = null;
		ctx.bindingTypes = new HashMap<String, List<ElemType>>();
		ctx.adaptationMap = new HashMap<NonTerminal,AdaptationInfo>();
		ctx.innermostGamma = null;
		
		checkInterface(ctx);
		
		if (ErrorHandler.getErrorCount() > oldErrorCount) {
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
		  ctx.innermostGamma = assumes;
		  ctx.adaptationRoot = assumes;
		}
		ctx.varfreeNTs.clear();
		
		for (Fact f : foralls) {
			f.addToDerivationMap(ctx);
			ctx.inputVars.addAll(f.getElement().asTerm().getFreeVariables());
			// determine var free nonterminals
			if (f instanceof DerivationByAssumption) {
        ClauseUse cu = (ClauseUse)f.getElement();
        int assumeIndex = cu.getConstructor().getAssumeIndex();
        if (assumeIndex >= 0) continue; // not varFree
        // System.out.println("var free: " + cu);
        for (Element e : cu.getElements()) {
          if (e instanceof NonTerminal) ctx.varfreeNTs.add((NonTerminal)e);
        }
			}
		}

		Term theoremTerm = exists.asTerm();
		ctx.currentGoal = theoremTerm;
		ctx.currentGoalClause = exists;
		ctx.outputVars.addAll(theoremTerm.getFreeVariables());
		ctx.outputVars.removeAll(ctx.inputVars);
		
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

	/**
	 * Verifies that the last derivation is what this theorem requires
	 * 
	 * @param ctx
	 * @param theoremTerm
	 */
	public static void verifyLastDerivation(Context ctx, Term theoremTerm, Element theoremElem, List<Derivation> derivs, Node errorNode) {
		// verify: that last derivation is what theorem requires
		if (derivs.size() == 0)
			ErrorHandler.report(Errors.NO_DERIVATION, errorNode);
		else {
			Derivation last = derivs.get(derivs.size()-1);
			Term derivTerm = DerivationByAnalysis.adapt(last.getElement().asTerm(), ((ClauseUse)last.getElement()).getRoot(), ctx, last);
			debug("orig theoremTerm: "+theoremTerm);
			debug("orig derivTerm: "+derivTerm);
			theoremTerm = DerivationByAnalysis.adapt(theoremTerm, ((ClauseUse)theoremElem).getRoot(), ctx, errorNode);
			debug("adapted theoremTerm: " + theoremTerm);

			try {
				debug("end of theorem ("+last.getLocation().getLine()+"): unifying " + derivTerm + " to match " + theoremTerm);
				debug("current sub = " + ctx.currentSub);
				debug("wrapping sub = " + ctx.adaptationSub);
				Substitution instanceSub = derivTerm.instanceOf(theoremTerm);
				// must not require instantiating free variables
				if (!instanceSub.avoid(ctx.inputVars)) {
					Set<FreeVar> unavoidable = instanceSub.selectUnavoidable(ctx.inputVars);
					ErrorHandler.report(Errors.WRONG_RESULT,"\n    could not avoid vars " + unavoidable, last);
				}
				// TODO: probably should add to currentSub for DerivationByAnalysis, but it's not clear this is necessary for soundness
				// important - would need to do it BEFORE analyzing the cases, then take it out of currentSub.  So it doesn't work to put it here, as below.
				/*if (!instanceSub.getMap().isEmpty() && last instanceof DerivationByAnalysis) {
					// could support this by adding to currentSub and replacing in outputVars, but for now just say it's an error
					//ctx.currentSub.compose(instanceSub);
					tdebug("instanceSub = " + instanceSub);
					ErrorHandler.report(Errors.WRONG_RESULT,"\n    SASyLF does not currently support substituting one variable with another in the expected result during case analysis", last);
				}*/
			} catch (UnificationFailed e) {
				ErrorHandler.report(Errors.WRONG_RESULT, last, "\twas checking " + derivTerm + " instance of " + theoremTerm);
			}

		}
	}

	public void setKind(String k) {
	  if (kind != null && kind.equals(k)) return;
	  if (k == null) k = "theorem";
	  if (k.length() == 0) k = "theorem";
	  kind = k;
	  kindTitle = Character.toTitleCase(k.charAt(0)) + kind.substring(1);
	}
	
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
	
	public void setAssumes(NonTerminal c) { 
	  if (assumes != null && !assumes.equals(c))
	    ErrorHandler.report(Errors.INCONSISTENT_CONTEXTS,"Theorem has inconsistent contexts " + assumes + " and " + c, this);
	  assumes = c; 
	}
	@Override
	public NonTerminal getAssumes() { return assumes; }
	
	public void setExists(Clause c) { exists = c; }

	private String kind = "theorem";
	private String kindTitle = "Theorem";
	private NonTerminal assumes = null;
	private List<Fact> foralls = new ArrayList<Fact>();
	private Clause exists;
	private List<Derivation> derivations = new ArrayList<Derivation>();
	private Theorem andTheorem;
	private Theorem firstInGroup = this;
	private int indexInGroup = 0;
	private int inductionIndex = 0; // default to first argument
	private boolean interfaceChecked=false;
	private boolean interfaceOK = false;

}

