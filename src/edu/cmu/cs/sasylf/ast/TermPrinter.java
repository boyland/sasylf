package edu.cmu.cs.sasylf.ast;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.cmu.cs.sasylf.parser.DSLToolkitParser;
import edu.cmu.cs.sasylf.parser.ParseException;
import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Atom;
import edu.cmu.cs.sasylf.term.BoundVar;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Util;

/**
 * A class to convert terms back into elements.
 * The conversion is non-trivial for a number of reasons:
 * <ul>
 * <li> Fresh (internal) variables need to be converted into user-visible unused meta-variables.
 * This is currently accomplished by keeping a dictionary mapping fresh variables to nonterminals,
 * and adding to it on demand.
 * <li> We need to convert the deBruijn indices into variables that are unique for their context, 
 * but don't need to be unique for the whole generated element (scoping is relevant).
 * This mapping is handled by a "vars" list that is passed around.  New variables are added at the end.
 * <li> Named contexts are omitted in the internal form, and must be generated or else 
 * the empty context terminal must be used.  The "assumed" context is provided as a constructor
 * parameter to the term printer.
 * <li> Variable contexts (e.g., "x:T") are expressed as LF abstractions.
 * higher-order functions are <em>also</em> represented by abstractions.  
 * So, we treat converting a term to a "clause" (judgments) different that converting
 * a term into an "element" (syntax).  Currently all variable contexts are represented
 * by <em>two</em> abstractions: the first for the variables and the second for the
 * judgment that has the assumption rule for this variable.  Recall that each variable
 * may be bound in exactly one context and each binding form must be used in 
 * exactly one assumption rule.  Once we get the binding form, we need to copy
 * it into the correct place in the judgment instance. (ClauseUse).  To complicate matters, 
 * "and" and "or" judgments may have multiple places where a context is used.
 * <li> Context judgments do <em>not</em> represent the context in this way, but rather
 * accept a higher-order function as a parameter.  It seems to work now.
 * <li> If a nonterminal is imported twice with different nonterminals,
 * internally there is no difference, and sometimes the incorrect non-terminal is chosen.
 * Fixing this would require looking at the context to see which local nonterminal should be used.
 * </ul>
 */
public class TermPrinter {
	/**
	 * Printing can fail through no fault of the rest of the code.
	 * Catching this exception can avoid an internal error report.
	 * XXX: We need to move most of the runtime exceptions
	 * thrown in this class to this exception instead.
	 * Then change the {@link #toString()} method to catch this exception alone. 
	 */
	public static class PrintException extends RuntimeException {
		/**
		 * KEH
		 */
		private static final long serialVersionUID = 1L;

		public PrintException(String s) {
			super(s);
		}
	}
	
	private final Context ctx;
	private final Element context;
	private final Location location;
	private final Map<FreeVar,NonTerminal> varMap = new HashMap<FreeVar,NonTerminal>();
	private final Set<FreeVar> used = new HashSet<FreeVar>();
	
	/** whether this TermPrinter creates names for free and bound variables;
	 *  otherwise grabs the name from the FreeVar or the binding Abstraction **/
	private final boolean rename;

	public static String toString(Context ctx, Element gamma, Location loc, Term term, boolean asClause) {
		return new TermPrinter(ctx,gamma,loc).toString(term,asClause);
	}

	public TermPrinter(Context ctx, Element gamma, Location loc) {
		this(ctx, gamma, loc, true);
	}
	
	public TermPrinter(Context ctx, Element gamma, Location loc, boolean rename) {
		this.ctx = ctx;
		context = rootElement(gamma);
		location = loc;
		this.rename = rename;
		//System.out.println("TermPrinter(context = " + context + ")");
	}

	private Element rootElement(Element gamma) {
		if (gamma == null || gamma instanceof NonTerminal) return gamma;
		ClauseUse cu = (ClauseUse)gamma;
		int n = cu.getElements().size();
		boolean onlyTerminals = true;
		for (int i=0; i < n; ++i) {
			Element e = cu.getElements().get(i);
			if (e.getType().equals(cu.getType())) {
				return rootElement(e);
			}
			if (!(e instanceof Terminal)) onlyTerminals = false;
		}
		if (onlyTerminals) return gamma;
		throw new RuntimeException("cannot analyze context: " + gamma);
	}

	public String toString(Term x, boolean asClause) {
		Element e;
		try {
			e = asClause ? asClause(x) : asElement(x);
		} catch (RuntimeException ex) {
			ErrorHandler.recoverableError(Errors.INTERNAL_ERROR,": Failed to convert "+x, location);
			ex.printStackTrace();
			return x.toString();
		}
		try {
			return toString(e);
		} catch (Exception e1) {
			ErrorHandler.recoverableError(Errors.INTERNAL_ERROR,": Failed to print "+e, location);
			e1.printStackTrace();
			return e.toString();
		}
	}

	public Element asElement(Term x) {
		return asElement(x, new ArrayList<Variable>());
	}

	public Element asCaseElement(Term x) {
		return asCaseElement(x, new ArrayList<Variable>());
	}

	public ClauseUse asClause(Term x) {
		return asClause(x, new ArrayList<Variable>());
	}

	public Element asElement(Term x, List<Variable> vars) {
		// System.out.println("as element : " + x);
		if (x instanceof FreeVar) {
			if (varMap.containsKey(x)) return varMap.get(x);
			Term ty = ((FreeVar) x).getBaseType();
			if (!(ty instanceof Constant)) {
				throw new RuntimeException("base type of " + x + " = " + ty);
			}
			SyntaxDeclaration syn = ctx.getSyntax(ty);
			String base = syn.getName();
			if (((FreeVar)x).getStamp() == 0 && ctx.isKnownVar((FreeVar) x)) 
				return new NonTerminal(x.toString(),location, syn);
			if (rename) { // MDA: only rename free variables if the option is enabled
				for (int i=0; true; ++i) {
					FreeVar v = new FreeVar(base + i, ((FreeVar) x).getType());
					if (ctx.isKnown(base + i)) continue;
					if (used.contains(v)) continue;
					used.add(v);
					NonTerminal result = new NonTerminal(v.toString(),location, syn);
					varMap.put((FreeVar)x, result);
					return result;
				}
			}
			else {
				NonTerminal result = new NonTerminal(((FreeVar)x).toString(),location, syn);
				varMap.put((FreeVar)x, result);
				return result;
			}
		} else if (x instanceof BoundVar) {
			BoundVar v = (BoundVar)x;
			return vars.get(vars.size()-v.getIndex()); // index is "one" based.
		} else if (x instanceof Constant) {
			return appAsClause((Constant)x, Collections.<Element>emptyList(), vars);
		} else if (x instanceof Application) {
			Application app = (Application)x;
			List<Element> args = new LinkedList<Element>();
			for (Term arg : app.getArguments()) {
				args.add(asElement(arg,vars));
			}
			Atom func = app.getFunction();
			if (func instanceof Constant) {
				return appAsClause((Constant)func,args, vars);
			} else {
				return new Binding(location,(NonTerminal)asElement(func),args,location);
			}
		} else if (x instanceof Abstraction) {
			Abstraction abs = (Abstraction)x;
			//FreeVar etaFV = abs.getEtaEquivFreeVar();
			//if (etaFV != null) return asElement(etaFV,vars);
			Term ty = abs.varType;
			SyntaxDeclaration syn = ctx.getSyntax(ty);
			boolean createName = rename;
			if (vars.contains(new Variable(abs.varName,location))) {
				createName = true;
			}
			if (syn == null) {
				System.out.println("null syntax for " + ty + " in " + x);
				createName = false;
			}
			Variable v = createName ? new Variable(createVarName(syn,vars),location)
									: new Variable(abs.varName, location);
			if (syn != null) v.setType(syn);
			vars.add(v);
			Element bodyElem = asElement(abs.getBody(),vars);
			vars.remove(vars.size()-1);
			// System.out.println("Insert variable " + v + " in " + bodyElem);
			ClauseUse bindingClause = variableAsBindingClause(v);
			// System.out.println("  using binding clause: " + bindingClause);
			if (bodyElem instanceof AssumptionElement) {
				AssumptionElement ae = (AssumptionElement)bodyElem;
				replaceAssume(bindingClause,ae.getAssumes());
				return bodyElem;
			} else {
				return new AssumptionElement(location,bodyElem,bindingClause);
			}
		} else {
			throw new RuntimeException("unknown element: " + x);
		}
	}

	/**
	 * Convert a term used for case analysis back into an element, possibly an assumption element. 
	 * @param x term to convert
	 * @param vars list of bound variables
	 * @return element for this term
	 */
	public Element asCaseElement(Term x, List<Variable> vars) {
		if (x instanceof Abstraction) {
			Abstraction abs = (Abstraction)x;
			Term ty = abs.varType;
			SyntaxDeclaration syn = ctx.getSyntax(ty);
			if (syn == null) {
				System.out.println("No syntax for " + ty);
			}
			Variable v = rename ? new Variable(createVarName(syn,vars),location)
								: new Variable(abs.varName, location);
			v.setType(syn);
			vars.add(v);
			Term body1 = abs.getBody();
			if (body1 instanceof Abstraction) {
				Abstraction abs2 = (Abstraction)body1;
				ClauseUse bindingClause = assumeTypeAsClause(abs2.varType, vars);
				vars.add(new Variable("<internal>",location));
				Element bodyElem = asCaseElement(abs2.getBody(),vars);
				vars.remove(vars.size()-1);
				vars.remove(vars.size()-1);
				if (bodyElem instanceof AssumptionElement) {
					AssumptionElement ae = (AssumptionElement)bodyElem;
					replaceAssume(bindingClause,ae.getAssumes());
					return bodyElem;
				} else {
					return new AssumptionElement(location,bodyElem,bindingClause);
				}
			} else {
				throw new RuntimeException("abstraction with only one arg?: " + x);
			}

		} else return asElement(x,vars);
	}

	public ClauseUse asClause(Term x, List<Variable> vars) {
		if (x instanceof Application) {
			Application app = (Application)x;
			List<Element> args = new LinkedList<Element>();
			for (Term arg : app.getArguments()) {
				args.add(asElement(arg,vars));
			}
			Atom func = app.getFunction();
			if (func instanceof Constant) {
				return appAsClause((Constant)func,args, vars);
			} else {
				throw new RuntimeException("not a clause: " + x);
			}
		} else if (x instanceof Abstraction) {
			Abstraction abs = (Abstraction)x;
			Term ty = abs.varType;
			SyntaxDeclaration syn = ctx.getSyntax(ty);
			boolean createName = rename;
			if (vars.contains(new Variable(abs.varName,location))) {
				createName = true;
			}
			Variable v = createName ? new Variable(createVarName(syn,vars),location)
								    : new Variable(abs.varName, location);
			v.setType(syn);
			vars.add(v);
			Term body1 = abs.getBody();
			if (body1 instanceof Abstraction) {
				Abstraction abs2 = (Abstraction)body1;
				ClauseUse bindingClause = assumeTypeAsClause(abs2.varType, vars);
				vars.add(new Variable("<internal>",location));
				ClauseUse body = asClause(abs2.getBody(),vars);
				Util.debug("Trying to replace ",bindingClause," in body: ",body," with constructor = ",body.getConstructor());
				List<Integer> indices = body.getConstructor().getAssumeIndices();
				for (int ai : indices) {
					body.getElements().set(ai, replaceAssume(bindingClause,body.getElements().get(ai)));
				}
				vars.remove(vars.size()-1);
				vars.remove(vars.size()-1);
				return body;
			} else {
				throw new RuntimeException("abstraction with only one arg?: " + x);
			}
		} else if (x instanceof Constant) {
			return appAsClause((Constant)x,Collections.<Element>emptyList(), vars);
		} else {
			throw new RuntimeException("unknown element: " + x + " of class " + x.getClass());
		}
	}

	private String createVarName(SyntaxDeclaration s, List<Variable> vars) {
		StringBuilder sb = new StringBuilder(s.getVariable().toString());
		for (;;) {
			boolean found = false;
			String temp = sb.toString();
			for (Variable v : vars) {
				if (v.getType() == s && temp.equals(v.getSymbol())) found = true;
			}
			if (found == false) return temp;
			sb.append("'");
		}
		// previously, we would keep track of which variables were used,
		// but this doesn't take into account the fact that variables are scoped.
	}

	/**
	 * Convert a term used as the type of the hypothetical
	 * assumed along with the variable.  Convert it to an element.
	 * @param assumeType the type of the hypothetical assumption
	 * @param vars bindings for variables in scope.
	 * @return element that gives the context as an environment.
	 */
	ClauseUse assumeTypeAsClause(Term assumeType, List<Variable> vars) {
		ClauseUse bindingClause = (ClauseUse)asElement(assumeType,vars);
		if (assumeType instanceof Application) {
			Application vtApp = (Application)assumeType;
			Map<String,Element> vtMap = new HashMap<String,Element>();
			Judgment vtj = ctx.getJudgment(vtApp.getFunction());
			if (vtj == null) {
				throw new RuntimeException("Cannot find judgment for " + vtApp);
			}
			for (Rule r : vtj.getRules()) {
				if (r.isAssumption()) {
					ClauseUse vtcu = (ClauseUse)r.getConclusion();
					int ai = vtcu.getConstructor().getAssumeIndex();
					List<Element> vtes = vtcu.getElements();
					int n = vtes.size();
					Iterator<? extends Term> vtai = vtApp.getArguments().iterator();
					for (int i=0; i < n; ++i) {
						if (i == ai) continue;
						Element elem = vtes.get(i);
						if (elem instanceof NonTerminal || elem instanceof Variable) {
							if (!vtai.hasNext()) {
								throw new RuntimeException("cannot find non-terminal value for " + vtes.get(i));
							} else {
								vtMap.put(elem.toString(), asElement(vtai.next(),vars));
							}
						}
					}
					// System.out.println("map is " + vtMap);
					ClauseUse bu = (ClauseUse)vtes.get(ai);
					// System.out.println("  hand-substitution of " + bu + " using " + vtMap + ", ai = " + bu.getConstructor().getAssumeIndex());
					List<Element> bes = new ArrayList<Element>(bu.getElements());
					n = bes.size();
					for (int i=0; i < n; ++i) {
						Element elem = bes.get(i);
						// this next thing doesn't work: no assume index (I think) for var rules.
						// if (i == bu.getConstructor().getAssumeIndex()) bes.set(i,context); else 
						if (elem instanceof NonTerminal || elem instanceof Variable) {
							Element element = vtMap.get(elem.toString());
							if (element != null)
								bes.set(i,element); 
							else if (elem instanceof Variable)
								bes.set(i, vars.get(vars.size()-1));
							else {
								if (i != bu.getConstructor().getAssumeIndex()) {
									// System.out.println("didn't find " + elem + " in " + vtMap);
									continue;
								}
								bes.set(i,getContext((NonTerminal)elem));
							}
						}
					}
					bindingClause = new ClauseUse(location,bes,bu.getConstructor());
					// System.out.println("binding clause is " + bindingClause);
				}
			}
			//if (!(vtCD.get))
		} else {
			throw new RuntimeException("Can't figure out binding clause for " + assumeType);
		}
		return bindingClause;
	}

	public ClauseUse variableAsBindingClause(Variable v) {
		SyntaxDeclaration s = v.getType();
		ClauseDef cd = s.getContextClause();
		List<Element> elems = cd.getElements();
		List<Element> newElems = new ArrayList<Element>(elems);
		int n = elems.size();
		for (int i=n-1; i>=0; --i ) {
			Element e = newElems.get(i);
			if (e instanceof NonTerminal) {
				NonTerminal nt = (NonTerminal)e;
				NonTerminal copy;
				for (int j=0; true; ++j) {
					String newName = nt.getSymbol() + j;
					if (ctx.isKnown(newName)) continue;
					FreeVar fake = new FreeVar(newName,nt.getTypeTerm());
					if (used.contains(fake)) continue;
					used.add(fake);
					copy = new NonTerminal(newName, location, nt.getType());
					break;
				}
				newElems.set(i, copy);
			} else if (e instanceof Variable) {
				newElems.set(i, v);
				v = null; // replace earlier variables with null
			}
		}
		ClauseUse bindingClause = new ClauseUse(location,newElems,cd);
		// System.out.println("var binding clause = " + bindingClause);
		return bindingClause;
	}

	private ClauseUse appAsClause(Constant con, List<Element> args, List<Variable> vars) {
		String fname = con.getName();
		if (fname.equals("or[]")) return OrClauseUse.makeEmptyOrClause(location);
		ClauseDef cd = ctx.getProduction(con);
		if (cd == null) {
			throw new PrintException("no cd for " + fname);
		}

		List<Element> contents;

		// undo work of parseClause
		if (cd.getType() instanceof AndOrJudgment) {
			AndOrJudgment j = (AndOrJudgment)cd.getType();
			// System.out.println("Reconsituting for " + j.getName() + " with " + args);
			List<Judgment> judgments = j.getJudgments();
			contents = new ArrayList<Element>(judgments.size());
			Iterator<Element> it = args.iterator();
			boolean first = true;
			for (Judgment j2 : judgments) {
				if (first) first = false; else contents.add(j.makeSeparator(location));
				Constant con2 = (Constant)j2.getForm().asTerm();
				Term tt2 = con2.getType();
				List<Element> args2 = new ArrayList<Element>();
				while (tt2 instanceof Abstraction) {
					args2.add(it.next());
					tt2 = ((Abstraction)tt2).getBody();
				}
				// System.out.println("con2 = " + con2 + ", args2 = " + args2 + ", vars = " + vars);
				contents.addAll(appAsClause(con2,args2, vars).getElements());
			}
		} else {
			contents = new ArrayList<Element>(cd.getElements());
			// System.out.println("In " + contents);
			int n = contents.size();
			int ai = cd.getAssumeIndex();
			// System.out.println("AsClause: " + con + " with " + args);
			Map<Variable,Variable> varMap = new HashMap<>();
			for (Element old : contents) {
				if (old instanceof Variable) {
					Variable newVar = (Variable)old;
					if (vars.contains(old)) {
						newVar = new Variable(createVarName(newVar.getType(),vars),location);
					}
					varMap.put((Variable)old, newVar);
				}
			}
			Iterator<Element> actuals = args.iterator();
			for (int i=0; i < n; ++i) {
				Element old = contents.get(i);
				if (i == ai) {
					Element baseContext = getContext((NonTerminal)old);
					contents.set(i,baseContext);
				} else if (old instanceof NonTerminal) {
					contents.set(i, actuals.next());
				} else if (old instanceof Variable) {
					contents.set(i, varMap.get(old));
					// do nothing
				} else if (old instanceof Binding) {
					Binding b = (Binding)old;
					// System.out.println("old = " + old);
					Element actual = actuals.next();
					// System.out.println("new = " + actual);
					if (actual instanceof NonTerminal) {
						List<Element> newElems = b.getElements().stream().map((e) -> varMap.get(e)).collect(Collectors.toList());
						contents.set(i,new Binding(location,(NonTerminal)actual,newElems,location));
					} else if (actual instanceof AssumptionElement) {
						AssumptionElement ae = (AssumptionElement)actual;
						ClauseUse cu = (ClauseUse)ae.getAssumes();
						// System.out.println("  context clause = " + cu);
						int nvar = b.getElements().size();
						for (int j=nvar-1; j >= 0; --j) {
							Variable oldVar = (Variable)b.getElements().get(j);
							Variable newVar = null;
							for (Element e : cu.getElements()) {
								if (e instanceof Variable) {
									newVar = (Variable)e;
									break;
								}
							}
							if (newVar == null) throw new RuntimeException("Couldn't find newvar in " + cu);
							// System.out.println("Replacing " + oldVar + " (mapped to " + varMap.get(oldVar) + ") with " + newVar + " in " + contents);
							contents.set(contents.indexOf(varMap.get(oldVar)), newVar);
							if (j > 0) {
								cu = (ClauseUse)cu.getElements().get(cu.getConstructor().getAssumeIndex());
							}
						}
						contents.set(i, ae.getBase());
					} else {
						System.out.println("What do I do with " + old + " getting " + actual + " ?");
						contents.set(i, actual);
					}
					// System.out.println("all = " + contents.get(i));
				} else if (old instanceof Variable) {
					System.out.println("What do I do with variable " + old);
				}
			}
		}
		return new ClauseUse(location,contents,cd);
	}

	/**
	 * Return syntax for the current context, giving a nonterminal for the syntax
	 * in case the context is empty.
	 * @param contextNT
	 * @return clause or non-terminal for the current context, never null.
	 */
	public Element getContext(NonTerminal contextNT) {
		Element baseContext = context;
		if (baseContext == null) {
			ClauseDef term = contextNT.getType().getTerminalCase();
			baseContext = new ClauseUse(location,new ArrayList<Element>(term.getElements()),term);
		}
		return baseContext;
	}

	private ClauseUse replaceAssume(ClauseUse repl, Element old) {
		if (old instanceof NonTerminal) return repl;
		if (!(old instanceof ClauseUse)) {
			throw new RuntimeException("Couldn't locate context in " + old);
		}
		ClauseUse cu = (ClauseUse)old;
		int ai = cu.getConstructor().getAssumeIndex();
		if (ai < 0) return repl; // throw new RuntimeException("Couldn't replace " + repl + " in " + old);
		Element e = cu.getElements().get(ai);
		cu.getElements().set(ai,replaceAssume(repl,e));
		return cu;
		/*
    int n = cu.getElements().size();
    boolean onlyTerminals = true;
    for (int i=0; i < n; ++i) {
      Element e = cu.getElements().get(i);
      if (e.getType().equals(repl.getType())) {
        cu.getElements().set(i,replaceAssume(repl,e));
        // System.out.println("Replaced is now " + cu);
        return cu;
      }
      if (!(e instanceof Terminal)) onlyTerminals = false;
    }
    if (onlyTerminals) return repl;*/
		//throw new RuntimeException("Couldn't locate context in clause " + old);
	}

	/**
	 * Take a term produced by (missing) case analysis (either rule case or syntax case)
	 * and return a string for the case.  The string will end in a newline iff
	 * we have a rule case.
	 * @param caseTerm term for the (missing) case
	 * @return text of the case indicated.
	 */
	public String caseToString(Term caseTerm) {
		StringBuilder sb = new StringBuilder();
		List<Abstraction> abs = new ArrayList<Abstraction>();
		Term bareTerm = Term.getWrappingAbstractions(caseTerm, abs);
		if (bareTerm instanceof Application) {
			Application app = (Application)bareTerm;
			if (app.getFunction() instanceof Constant) {
				Constant baseType = app.getFunction().getType().baseTypeFamily();
				Judgment j = ctx.getJudgment(baseType);
				if (j != null) {
					Rule rule = (Rule)j.findRule((Constant)app.getFunction());
					if (rule != null) {
						if (j instanceof OrJudgment) {
							sb.append("or _: ");              
							Term disj = app.getArguments().get(0);
							ClauseUse clause = asClause(Term.wrapWithLambdas(abs, disj)); 
							prettyPrint(sb,clause,false,0);
						} else {
							int n = app.getArguments().size();
							for (int i=0; i < n; ++i) {
								if (i == n-1) {
									sb.append("--------------- ");
									sb.append(rule.getName());
									sb.append("\n");
								}
								Judgment pj = (Judgment)((ClauseUse)((i == n-1) ? rule.getConclusion() : rule.getPremises().get(i))).getConstructor().getType();
								Term t = app.getArguments().get(i);
								if (pj.getAssume() != null && pj.getAssume().equals(j.getAssume())) t = Term.wrapWithLambdas(abs,t);
								try {
									ClauseUse u = asClause(t);
									prettyPrint(sb,u,false, 0);
								} catch (PrintException ex) {
									sb.append("// " + t);
								}
								sb.append('\n');
							}
						}
						return sb.toString();
					}
				}
			}
		}
		prettyPrint(sb,asCaseElement(caseTerm),false, 0);
		return sb.toString();
	}

	private static int SUSPECT_INFINITE_RECURSION = 100;

	public String toString(Element e) {
		StringBuilder sb = new StringBuilder();
		prettyPrint(sb,e,false,0);
		return sb.toString();
	}

	private void prettyPrint(StringBuilder sb, Element e, boolean parenthesize, int level) {
		if (level > SUSPECT_INFINITE_RECURSION) {
			sb.append("#");
			return;
		}
		if (e instanceof OrClauseUse && ((OrClauseUse)e).getClauses().isEmpty()) {
			sb.append("contradiction");
			return;
		}
		if (e instanceof NonTerminal || e instanceof Variable) {
			sb.append(e.toString());
		} else if (e instanceof AndOrJudgment.OpTerminal) {
			sb.append(((AndOrJudgment.OpTerminal)e).getOpName());
		} else if (e instanceof Terminal) {
			String str = ((Terminal)e).getTerminalSymbolString();
			if (isTerminal(str)) {
				sb.append(str);
			} else {
				sb.append('"');
				sb.append(str);
				sb.append('"');
			}
		} else if (e instanceof Binding) {
			Binding b = (Binding)e;
			sb.append(b.getNonTerminal());
			int n = b.getElements().size();
			for (int i=0; i < n; ++i) {
				sb.append('[');
				prettyPrint(sb,b.getElements().get(i),false, level+1);
				sb.append(']');
			}
		} else if (e instanceof Clause) {
			List<Element> es = ((Clause)e).getElements();
			if (parenthesize && es.size() > 1) sb.append('(');
			int n = es.size();
			String lastTerminal = null;
			for (int i=0; i < n; ++i) {
				Element e2 = es.get(i);
				String thisTerminal = e2 instanceof Terminal ? ((Terminal)e2).getSymbol() : null;
				if (i > 0 && insertSpace(lastTerminal, thisTerminal)) sb.append(' ');
				prettyPrint(sb,e2,true, level+1);
				lastTerminal = thisTerminal;
				if (e2 instanceof AndOrJudgment.OpTerminal && !parenthesize) {
					sb.append(" _:");
					lastTerminal = null;
				}
			}
			if (parenthesize && es.size() > 1) sb.append(')');
		} else if (e instanceof AssumptionElement) {
			AssumptionElement ae = (AssumptionElement)e;
			if (parenthesize) sb.append('(');
			prettyPrint(sb,ae.getBase(),false, level+1);
			sb.append(" assumes ");
			prettyPrint(sb,ae.getAssumes(),false, level+1);
			if (parenthesize) sb.append(')');
		} else {
			throw new RuntimeException("??" + e);
		}
	}

	/**
	 * Return whether one should insert a space between these two tokens.
	 * By default, we always place spaces between tokens, but if one or both are terminals
	 * we sometimes omit the space.
	 * @param lastTerminal the terminal string before, null if not a terminal
	 * @param thisTerminal the terminal string after, null is not a terminal
	 * @return whether to insert a space between these two
	 */
	protected static boolean insertSpace(String lastTerminal, String thisTerminal) {
		if (thisTerminal == null) return true;
		if (thisTerminal.equals(",") || thisTerminal.equals(";")) return false; // special case
		if (lastTerminal == null) return true;
		if (thisTerminal.isEmpty()) return false;
		if (thisTerminal.startsWith("'")) return true; // 'and' and 'or'
		if (Character.isUnicodeIdentifierPart(thisTerminal.charAt(0))) return true;
		return false;
	}


	/**
	 * Is the string a legal terminal, e.g. an operator or 
	 * @param s
	 * @return
	 */
	public boolean isTerminal(String s) {
		return isTerminal(ctx,s);
	}

	public static boolean isTerminal(Context ctx, String s) {
		if (ctx.isTerminalString(s)) return true;
		return isParseTerminal(s);
	}

	/**
	 * Return a copy of the map used to give names to internal variables.
	 * @return the map.
	 */
	public Map<FreeVar,NonTerminal> getVarMap() {
		return Collections.unmodifiableMap(varMap);
	}
	
	/**
	 * Does the string parse as a terminal?
	 * @param s string to examine, must not be null
	 * @return whether the string is a terminal.
	 */
	public static synchronized boolean isParseTerminal(String s) {
		Boolean result = parseTerminals.get(s);
		if (result != null) return result;
		DSLToolkitParser p = new DSLToolkitParser(new StringReader(s));
		try {
			Element res = p.Term();
			p.matchEOF();
			result = res instanceof Terminal;
		} catch (ParseException er) {
			result = Boolean.FALSE;
		}
		parseTerminals.put(s, result);
		return result;
	}

	private static Map<String,Boolean> parseTerminals = new HashMap<String,Boolean>();

	public static void main(String[] args) {
		Util.verify(isParseTerminal("{"), "braces are terminals");
		Util.verify(!isParseTerminal("("), "parens are not terminals");
		Util.verify(!isParseTerminal("["), "brackets are not terminals");
		Util.verify(isParseTerminal("|-"), "'|-' is a terminal");
		Util.verify(!isParseTerminal("and"),"'and' is not a terminal");
		Util.verify(isParseTerminal("+"), "operators are terminals");
	}
}
