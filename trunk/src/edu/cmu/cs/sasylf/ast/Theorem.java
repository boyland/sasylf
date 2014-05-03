package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Util.debug;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.sasylf.reduction.InductionSchema;
import edu.cmu.cs.sasylf.reduction.StructuralInduction;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Pair;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.Util;


public class Theorem extends RuleLike {
	public Theorem(String n, Location l) { 
	  this(n,l,false);
	}
	public Theorem(String n, Location l, boolean abs) { 
	  super(n, l); 
	  isAbstract = abs; 
	  if (isAbstract) {
	    derivations = Collections.emptyList();
	  } else {
	    derivations = new ArrayList<Derivation>();
	  }
	}

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
		  ctx.bindingTypes = new HashMap<String, List<ElemType>>();
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
			  ErrorHandler.recoverableError(Errors.EXISTS_SYNTAX,  computed);
			}
			
			inductionScheme = null;
			for (Derivation d : derivations) {
			  if (d instanceof DerivationByInduction) {
			    DerivationByInduction dbi = (DerivationByInduction)d;
			    InductionSchema is = InductionSchema.create(this, dbi.getArgStrings(), this);
			    if (is != null) {
			      inductionScheme = is;
			      // Inconsistency found later
			      break;
			    }
			  }
			}
			if (inductionScheme == null) {
			  if (foralls.size() == 0) {
			    inductionScheme = InductionSchema.nullInduction;
			  }
			  else {
			    inductionScheme = StructuralInduction.create(this, foralls.get(0).getName(), this);
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
    } else oldCtx.ruleMap.put(getName(), this);
    
		int oldErrorCount = ErrorHandler.getErrorCount();
		Context ctx = oldCtx.clone();
		try {
		debug("checking ", kind, " ", this.getName());
		
		ctx.derivationMap = new HashMap<String, Fact>();
		ctx.inputVars = new HashSet<FreeVar>();
		ctx.outputVars = new HashSet<FreeVar>();
		ctx.currentSub = new Substitution();
		ctx.currentTheorem = this;
		ctx.adaptationMap = new HashMap<NonTerminal,AdaptationInfo>();
		ctx.innermostGamma = null;
		
		checkInterface(ctx);
		
		if (isAbstract) {
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
		  ctx.innermostGamma = assumes;
		  ctx.adaptationRoot = assumes;
		}
		ctx.varfreeNTs.clear();
		
		for (Fact f : foralls) {
		  f.typecheck(ctx);
			f.addToDerivationMap(ctx);
			ctx.subderivations.put(f, new Pair<Fact,Integer>(f,0));
			ctx.inputVars.addAll(f.getElement().asTerm().getFreeVariables());
			if (this.assumes != null) {
			  // determine var free nonterminals if we have a context around
			  if (f instanceof NonTerminalAssumption) {
			    NonTerminalAssumption nta = (NonTerminalAssumption)f;
			    if (nta.getContext() == null) {
			      Util.debug("var free: ",nta);
			      ctx.varfreeNTs.add(nta.getRoot());
			    }
			  } else if (f instanceof DerivationByAssumption) {
			    ClauseUse cu = (ClauseUse)f.getElement();
			    int assumeIndex = cu.getConstructor().getAssumeIndex();
			    if (cu.isRootedInVar()) continue; // definitely has variables
			    hasAssume: if (assumeIndex >= 0) {
			      Element e = cu.getAssumes();
			      if (e instanceof ClauseUse) {
			        ClauseUse au = (ClauseUse)e;
			        if (!au.hasVariables()) break hasAssume;
			      }
			      continue; // not varFree
			    }
			    Util.debug("var free: ", cu);
			    for (Element e : cu.getElements()) {
			      ctx.addVarFree(e);
			    }
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
	private final List<Derivation> derivations;
	private Theorem andTheorem;
	private Theorem firstInGroup = this;
	private int indexInGroup = 0;
	private InductionSchema inductionScheme = InductionSchema.nullInduction;
	private boolean interfaceChecked=false;
	private boolean interfaceOK = false;
	private final boolean isAbstract;

}

