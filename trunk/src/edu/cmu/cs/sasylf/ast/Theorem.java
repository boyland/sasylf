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
		out.println("theorem " + getName() + ":");
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
			interfaceChecked = true;
			List<String> inputNames = new ArrayList<String>();
			for (Fact f : foralls) {
				f.typecheck(ctx, false);
				inputNames.add(f.getName());
				// TODO: rationalize the following special cases:
				if (f instanceof DerivationByAssumption) {
	        ClauseUse cu = (ClauseUse)f.getElement();
	        cu.asTerm();
	        if (cu.getRoot() != null) {
	          setAssumes(cu.getRoot());
	        }				  
				} else if (f instanceof SyntaxAssumption) {
				  SyntaxAssumption sa = (SyntaxAssumption)f;
          Clause c = sa.getContext();
				  if (c == null) continue;
				  Element x = c.computeClause(ctx, false);
				  NonTerminal root = null;
				  if (x instanceof NonTerminal) root = (NonTerminal)x;
				  else {
				    ClauseUse cu = (ClauseUse)x;
				    root = cu.getRoot();
				  }
				  if (root != null) {
				    if (!root.getType().canAppearIn(sa.getSyntax().typeTerm())) {
				      ErrorHandler.report("assumes irrelevant for " + sa, this);
				    }
				    setAssumes(root);
				  }
				}
			}
	
			exists.typecheck(ctx);
			exists = (Clause) exists.computeClause(ctx, false);
			
			for (Derivation d : derivations) {
			  if (d instanceof DerivationByInduction) {
			    DerivationByInduction dbi = (DerivationByInduction)d;
			    String dn = dbi.getTargetDerivationName();
			    int i = inputNames.indexOf(dn);
			    if (i == -1) {
			      ErrorHandler.report("Induction target "+ dn +" must be an explicit forall argument of this theorem", this);
			    } else inductionIndex = i;
			  }
			}
		}
	}

	public void typecheck(Context ctx) {
		int oldErrorCount = ErrorHandler.getErrorCount();
		try {
		debug("checking theorem "+this.getName());
		
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
		
		/*
		if (andTheorem != null) {
			andTheorem.addToMap(ctx);
		}*/
		firstInGroup.addToMap(ctx);

		Derivation.typecheck(this, ctx, derivations);
		
		ctx.recursiveTheorems = new HashMap<String, Theorem>();
		} catch (SASyLFError e) {
			// ignore the error; it has already been reported
			//e.printStackTrace();
		} finally {
			int newErrorCount = ErrorHandler.getErrorCount() - oldErrorCount;
			if (edu.cmu.cs.sasylf.util.Util.VERBOSE) {
				if (newErrorCount == 0) {
					System.out.println("Theorem " + getName() + " OK");
				} else {
					System.out.println("Error(s) in theorem " + getName());					
				}
			}
		}
    ctx.ruleMap.put(getName(), this);
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
			Term derivTerm = DerivationByAnalysis.adapt(last.getElement().asTerm(), ((ClauseUse)last.getElement()).getRoot(), ctx);
			debug("orig theoremTerm: "+theoremTerm);
			debug("orig derivTerm: "+derivTerm);
			theoremTerm = DerivationByAnalysis.adapt(theoremTerm, ((ClauseUse)theoremElem).getRoot(), ctx);
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

	public void setAssumes(NonTerminal c) { 
	  if (assumes != null && !assumes.equals(c))
	    ErrorHandler.report(Errors.INCONSISTENT_CONTEXTS,"Theorem has inconsistent contexts " + assumes + " and " + c, this);
	  assumes = c; 
	}
	@Override
	public NonTerminal getAssumes() { return assumes; }
	
	public void setExists(Clause c) { exists = c; }

	private NonTerminal assumes = null;
	private List<Fact> foralls = new ArrayList<Fact>();
	private Clause exists;
	private List<Derivation> derivations = new ArrayList<Derivation>();
	private Theorem andTheorem;
	private Theorem firstInGroup = this;
	private int indexInGroup = 0;
	private int inductionIndex = 0; // default to first argument
	private boolean interfaceChecked=false;

}

