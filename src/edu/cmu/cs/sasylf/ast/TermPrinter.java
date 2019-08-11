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

import edu.cmu.cs.sasylf.ast.AndJudgment.AndTerminal;
import edu.cmu.cs.sasylf.ast.OrJudgment.OrTerminal;
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
 * A class to convert terms back into elements
 * @author boyland
 */
public class TermPrinter {

	private final Context ctx;
	private final Element context;
	private final Location location;
	private final Map<FreeVar,NonTerminal> varMap = new HashMap<FreeVar,NonTerminal>();
	private final Set<FreeVar> used = new HashSet<FreeVar>();
	private final Set<String> variableNames = new HashSet<String>();
	
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
		variableNames.clear(); // every clause can reuse variables
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
			String base = ty.toString();
			SyntaxDeclaration syn = ctx.getSyntax(ty);
			if (((FreeVar)x).getStamp() == 0 && ctx.inputVars.contains(x)) 
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
			return appAsClause((Constant)x, Collections.<Element>emptyList());
		} else if (x instanceof Application) {
			Application app = (Application)x;
			List<Element> args = new LinkedList<Element>();
			for (Term arg : app.getArguments()) {
				args.add(asElement(arg,vars));
			}
			Atom func = app.getFunction();
			if (func instanceof Constant) {
				return appAsClause((Constant)func,args);
			} else {
				return new Binding(location,(NonTerminal)asElement(func),args,location);
			}
		} else if (x instanceof Abstraction) {
			Abstraction abs = (Abstraction)x;
			FreeVar etaFV = abs.getEtaEquivFreeVar();
			if (etaFV != null) return asElement(etaFV,vars);
			Term ty = abs.varType;
			SyntaxDeclaration syn = ctx.getSyntax(ty);
			if (syn == null) {
				System.out.println("null syntax for " + ty + " in " + x);
			}
			Variable v = rename ? new Variable(createVarName(syn,vars),location)
								: new Variable(abs.varName, location);
			v.setType(syn);
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
				return appAsClause((Constant)func,args);
			} else {
				throw new RuntimeException("not a clause: " + x);
			}
		} else if (x instanceof Abstraction) {
			Abstraction abs = (Abstraction)x;
			Term ty = abs.varType;
			SyntaxDeclaration syn = ctx.getSyntax(ty);
			Variable v = rename ? new Variable(createVarName(syn,vars),location)
								: new Variable(abs.varName, location);
			v.setType(syn);
			vars.add(v);
			Term body1 = abs.getBody();
			if (body1 instanceof Abstraction) {
				Abstraction abs2 = (Abstraction)body1;
				ClauseUse bindingClause = assumeTypeAsClause(abs2.varType, vars);
				vars.add(new Variable("<internal>",location));
				ClauseUse body = asClause(abs2.getBody(),vars);
				int ai = body.getConstructor().getAssumeIndex();
				body.getElements().set(ai, replaceAssume(bindingClause,body.getElements().get(ai)));
				vars.remove(vars.size()-1);
				vars.remove(vars.size()-1);
				return body;
			} else {
				throw new RuntimeException("abstraction with only one arg?: " + x);
			}
		} else if (x instanceof Constant) {
			return appAsClause((Constant)x,Collections.<Element>emptyList());
		} else {
			throw new RuntimeException("unknown element: " + x + " of class " + x.getClass());
		}
	}

	private String createVarName(SyntaxDeclaration s, List<Variable> vars) {
		int count = 0;
		for (Variable p : vars) {
			if (p != null && p.getType() != null && p.getType().equals(s)) ++count;
		}
		StringBuilder sb = new StringBuilder(s.getVariable().toString());
		int len = sb.length();
		while (count > 0) {
			sb.append("'");
			--count;
		}
		if (variableNames.add(sb.toString())) return sb.toString();
		for (int i=0; true; ++i) {
			sb.setLength(len);
			sb.append(i);
			if (variableNames.add(sb.toString())) return sb.toString();
		}
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
		if (assumeType instanceof Application && 
				ctx.judgMap.containsKey(((Application)assumeType).getFunction().toString())) {
			Application vtApp = (Application)assumeType;
			Map<String,Element> vtMap = new HashMap<String,Element>();
			Judgment vtj = ctx.judgMap.get(vtApp.getFunction().toString());
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
							else {
								if (i != bu.getConstructor().getAssumeIndex()) {
									// throw new RuntimeException("didn't find " + elem + " in " + vtMap);
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

	private ClauseUse appAsClause(Constant con, List<Element> args) {
		String fname = con.getName();
		ClauseDef cd = ctx.prodMap.get(fname);
		if (fname.equals("or[]")) return OrClauseUse.makeEmptyOrClause(location);
		if (cd == null) {
			throw new RuntimeException("no cd for " + fname);
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
				contents.add(appAsClause(con2,args2));
			}
		} else {
			contents = new ArrayList<Element>(cd.getElements());
			// System.out.println("In " + contents);
			int n = contents.size();
			int ai = cd.getAssumeIndex();
			// System.out.println("AsClause: " + c + " with " + args);
			Iterator<Element> actuals = args.iterator();
			for (int i=0; i < n; ++i) {
				Element old = contents.get(i);
				if (i == ai) {
					Element baseContext = getContext((NonTerminal)old);
					contents.set(i,baseContext);
				} else if (old instanceof NonTerminal) {
					contents.set(i,actuals.next());
				} else if (old instanceof Variable) {
					// do nothing
				} else if (old instanceof Binding) {
					Binding b = (Binding)old;
					// System.out.println("old = " + old);
					Element actual = actuals.next();
					// System.out.println("new = " + actual);
					if (actual instanceof NonTerminal) {
						contents.set(i,new Binding(location,(NonTerminal)actual,b.getElements(),location));
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
							// System.out.println("Replacing " + oldVar + " with " + newVar);
							contents.set(contents.indexOf(oldVar), newVar);
							if (j > 0) {
								cu = (ClauseUse)cu.getElements().get(cu.getConstructor().getAssumeIndex());
							}
						}
						contents.set(i, ae.getBase());
					} else {
						System.out.println("What do I do with " + old + " getting " + actual + " ?");
						contents.set(i, actual);
					}
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
				String funcName = app.getFunction().getName();
				if (funcName.endsWith("TERM")) {
					String rName = funcName.substring(0, funcName.length()-4);
					if (ctx.ruleMap.containsKey(rName)) {
						Rule rule = (Rule)ctx.ruleMap.get(rName);
						Judgment j = rule.getJudgment();
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
									sb.append(rName);
									sb.append("\n");
								}
								Judgment pj = (Judgment)((ClauseUse)((i == n-1) ? rule.getConclusion() : rule.getPremises().get(i))).getConstructor().getType();
								Term t = app.getArguments().get(i);
								if (pj.getAssume() != null && pj.getAssume().equals(j.getAssume())) t = Term.wrapWithLambdas(abs,t);
								ClauseUse u = asClause(t);
								prettyPrint(sb,u,false, 0);
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
		} else if (e instanceof AndTerminal) {
			sb.append("and");
		} else if (e instanceof OrTerminal) {
			sb.append("or");
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
				if (e2 instanceof AndTerminal && !parenthesize) {
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
