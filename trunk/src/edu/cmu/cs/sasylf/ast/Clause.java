package edu.cmu.cs.sasylf.ast;

import java.util.*;
import java.io.*;

import edu.cmu.cs.sasylf.ast.grammar.GrmRule;
import edu.cmu.cs.sasylf.ast.grammar.GrmTerminal;
import edu.cmu.cs.sasylf.ast.grammar.GrmUtil;
import edu.cmu.cs.sasylf.grammar.*;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Pair;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;

import static edu.cmu.cs.sasylf.util.Util.*;

public class Clause extends Element implements CanBeCase {
	public Clause(Location l) { super(l); verify(getLocation() != null, "location provided is null!"); }
	public Clause(Element e) { super(e.getLocation()); verify(getLocation() != null, "location null for " + e + " of type " + e.getClass()); }

	public List<Element> getElements() { return elements; }

	protected List<Element> elements = new ArrayList<Element>();

	public List<ElemType> getElemTypes() {
		List<ElemType> list = new ArrayList<ElemType>();
		for (Element e : elements) {
			list.add(e.getElemType());
		}
		return list;
	}

	private List<GrmTerminal> getTerminalSymbols() {
		List<GrmTerminal> list = new ArrayList<GrmTerminal>();
		for (Element e : elements) {
			if (e instanceof Clause) {
				List<GrmTerminal> sublist = ((Clause)e).getTerminalSymbols();
				list.add(GrmUtil.terminalFor("("));
				list.addAll(sublist);
				list.add(GrmUtil.terminalFor(")"));
			} else
				list.add(e.getTerminalSymbol());
		}
		return list;
	}

	public List<Symbol> getSymbols() {
		List<Symbol> list = new ArrayList<Symbol>();
		for (Element e : elements) {
			list.add(e.getGrmSymbol());
		}
		return list;
	}

	public ElemType getElemType() {
		throw new RuntimeException("should only call getElemTypes on syntax def clauses which don't have sub-clauses; can't call getElemType() on a Clause");
	}

	public Symbol getGrmSymbol() {
		throw new RuntimeException("should only call getSymbols on syntax def clauses which don't have sub-clauses; can't call getSymbol() on a Clause");
	}

	@Override
	protected String getTerminalSymbolString() {
		throw new RuntimeException("should only call getTerminalSymbols on clauses which don't have sub-clauses; can't call getTerminalSymbol() on a Clause");
	}

	public boolean isVarOnlyClause() {
		return elements.size() == 1 && elements.get(0) instanceof Variable;
	}

	@Override
	public void prettyPrint(PrintWriter out, PrintContext ctx) {
		boolean prev = false;
		for (Element e : elements) {
			if (prev)
				out.print(' ');
			if (e instanceof Clause)
				out.print('(');
			e.prettyPrint(out, null);
			if (e instanceof Clause)
				out.print(')');
			prev = true;
		}
	}

	public Set<Terminal> getTerminals() {
		Set<Terminal> s = new HashSet<Terminal>();
		for (Element e : elements)
			if (e instanceof Terminal)
				s.add((Terminal)e);
		return s;
	}

	public void getVariables(Map<String,Variable> map) {
		for (Element e : elements) {
			if (e instanceof Binding) {
				for (Element e2 : ((Binding)e).getElements()) {
					if (!(e2 instanceof NonTerminal))
						ErrorHandler.report("Only variables are permitted inside a binding on the right hand side of a syntax definition", e2);
					NonTerminal nt = (NonTerminal)e2;
					String key = nt.getSymbol();
					if (!map.containsKey(key)) {
						map.put(key, new Variable(key, nt.getLocation()));
					}
				}
			}
		}
	}

	// computes Syntax type for each variable
	public void computeVarTypes(Syntax parent, Map<String,Variable> varMap) {

		// singleton elements matching variables define variable type
		if (elements.size() == 1 && elements.get(0) instanceof NonTerminal) {
			NonTerminal nt = (NonTerminal)elements.get(0);
			Variable var = varMap.get(nt.getSymbol());
			if (var != null) {
				var.setType(parent);
			}
		}
	}

	// computes Syntax for each NonTerminal
	// converts NonTerminal into Variable where appropriate
	// sets type of NonTerminals and Bindings
	// error if NonTerminal does not match a Syntax or Variable (likely should have been a Terminal)
	public Element typecheck(Context ctx) {
		for (int i = 0; i < elements.size(); ++i) {
			Element e = elements.get(i);
			elements.set(i, elements.get(i).typecheck(ctx));
			/*if (e instanceof NonTerminal) {
		NonTerminal nt = (NonTerminal) e;
		elements.set(i, nt.typecheck(synMap, varMap));
	    } else if (e instanceof Binding) {
		((Binding)e).typecheck(synMap, varMap);
	    }*/
		}

		return this;
	}
	
	public Element computeClause(Context ctx, boolean inBinding) {
		return computeClause(ctx, inBinding, ctx.getGrammar());
	}
	public Element computeClause(Context ctx, boolean inBinding, edu.cmu.cs.sasylf.grammar.Grammar g) {
		// compute a ClauseUse based on parsing the input
		List<GrmTerminal> symList = getTerminalSymbols();
		RuleNode parseTree = null;
		try {
			parseTree = g.parse(symList);
			return computeClause(parseTree);
		} catch (NotParseableException e) {
			/*for (edu.cmu.cs.sasylf.grammar.Rule r : g.getRules()) {
				debug_parse(r.toString());
			}
			debug_parse(g.getStart().toString());*/
			ErrorHandler.report("Cannot parse any syntactic case or judgment for expression "+ this /*+ " with  " + symList/*+" with elements " + elemTypes*/, this);
			throw new RuntimeException("should be unreachable");
		} catch (AmbiguousSentenceException e) {
			if (e.getParseTrees().size() == 1)
				debug_parse("ambiguous parse trees are actually equal!");
			// if inBinding and only one parse tree has a potentially variable type, try to escape
			if (inBinding) {
				Set<String> varTypes = new HashSet<String>();
				for (Variable v : ctx.varMap.values()) {
					varTypes.add(v.getType().toString());
				}
				//System.err.println(varTypes.toString());
				Set<RuleNode> keptTrees = new HashSet<RuleNode>();
				for (RuleNode tree : e.getParseTrees()) {
					String key = ((RuleNode)tree.getChildren().get(0)).getRule().getLeftSide().toString();
					if (varTypes.contains(key))
						keptTrees.add(tree);
				}
				if (keptTrees.size() == 1) {
					// saved!
					//System.err.println("Saved!");
					parseTree = keptTrees.iterator().next();
					return computeClause(parseTree);
				}
			}
			ErrorHandler.report("Ambiguous expression "+ this + " has differing parse trees " + e.getParseTrees() /*+" with elements " + elemTypes*/, this);
			throw new RuntimeException("should be unreachable");
		}
	}
	
	private Element computeClause(RuleNode parseTree) {
		List<Element> newElements = new ArrayList<Element>();
		for (ParseNode p : parseTree.getChildren()) {
			if (p instanceof RuleNode) {
				newElements.add(computeClause((RuleNode) p));
			} else {
				GrmTerminal n = (GrmTerminal) ((TerminalNode)p).getTerminal();
				if (n.getElement() != null)
					newElements.add(n.getElement());
			}
		}
		ClauseDef cd = ((GrmRule)parseTree.getRule()).getClauseDef();
		if (cd == null) {
			// this rule goes from start to a nonterminal
			if (newElements.size() == 1 && newElements.get(0) instanceof ClauseUse
					&& parseTree.getRule().getLeftSide().equals(GrmUtil.getStartSymbol())
					&& ((ClauseUse)newElements.get(0)).getConstructor().getType() instanceof Syntax) {
				return (ClauseUse)newElements.get(0);
			}
			// this is a binding, nonterminal or a variable
			if (newElements.size() == 1) {
				Element e = newElements.get(0);
				if (e instanceof Binding || e instanceof NonTerminal || e instanceof Variable) {
					return e;
				}
			}
			ErrorHandler.report("internal error: not sure what to do with null ClauseUse on " + newElements, this);
		}
		return new ClauseUse(getLocation(), newElements, cd);
	}
	
	public Term computeTerm(List<Pair<String, Term>>  varBindings) {
		throw new RuntimeException("internal error: can't compute the term before typechecking");
	}

	@Override
	void checkBindings(Map<String, List<ElemType>> bindingTypes, Node nodeToBlame) {
		for (Element e : elements) {
			e.checkBindings(bindingTypes, nodeToBlame);
		}
	}
	@Override
	public String getErrorDescription(Term t, Context ctx) {		
		if (t != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			this.prettyPrint(pw, new PrintContext(t, ctx.inputVars, ctx.innermostGamma));
			return sw.toString();
		} else {
			return toString();
		}
		//return toString();
		/*StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		prettyPrint(pw);
		return sw.toString();*/
	}
	
}
