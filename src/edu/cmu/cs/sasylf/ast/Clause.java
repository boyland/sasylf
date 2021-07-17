package edu.cmu.cs.sasylf.ast;

import static edu.cmu.cs.sasylf.util.Util.debug_parse;
import static edu.cmu.cs.sasylf.util.Util.verify;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import edu.cmu.cs.sasylf.ast.grammar.GrmRule;
import edu.cmu.cs.sasylf.ast.grammar.GrmTerminal;
import edu.cmu.cs.sasylf.ast.grammar.GrmUtil;
import edu.cmu.cs.sasylf.grammar.AmbiguousSentenceException;
import edu.cmu.cs.sasylf.grammar.NotParseableException;
import edu.cmu.cs.sasylf.grammar.ParseNode;
import edu.cmu.cs.sasylf.grammar.Rule;
import edu.cmu.cs.sasylf.grammar.RuleNode;
import edu.cmu.cs.sasylf.grammar.Symbol;
import edu.cmu.cs.sasylf.grammar.TerminalNode;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.Util;

public class Clause extends Element implements CanBeCase, Cloneable {
	
	public Clause(Location l) { super(l); verify(getLocation() != null, "location provided is null!"); }
	public Clause(Element e) { 
		super(e.getLocation()); 
		verify(getLocation() != null, "location null for " + e + " of type " + e.getClass()); 
		super.setEndLocation(e.getEndLocation());
	}

	/**
	 * Add an element at the end of the clause, updating endLocation, if it would increase.
	 * @param e
	 */
	public void add(Element e) {
		elements.add(e);
		Location l = e.getEndLocation();
		if (l != null) setEndLocation(l);
	}

	public List<Element> getElements() { return elements; }

	protected List<Element> elements = new ArrayList<Element>();

	@Override
	public Clause clone() {
		Clause result;

		try {
			result = (Clause) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}

		result.elements = new ArrayList<Element>(elements);

		return result;
	}

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
				list.add(GrmUtil.getLeftParen());
				list.addAll(sublist);
				list.add(GrmUtil.getRightParen());
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

	@Override
	public ClauseType getType() {
		throw new RuntimeException("Cannot determine type of unparsed Clause");
	}

	@Override
	public ElemType getElemType() {
		throw new RuntimeException(getLocation().getLine() + ": should only call getElemTypes on syntax def clauses which don't have sub-clauses; can't call getElemType() on a Clause");
	}

	@Override
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
		Element prev = null;
		for (Element e : elements) {
			if (prev != null && addSpace(prev,e))
				out.print(' ');
			if (e instanceof Clause)
				out.print('(');
			e.prettyPrint(out, null);
			if (e instanceof Clause)
				out.print(')');
			prev = e;
		}
	}

	@Override
	public String getName() {
		return this.toString();
	}
	public static boolean addSpace(Element e1, Element e2) {
		if (!(e2 instanceof Terminal)) return true;
		String thisTerminal = e2.toString();
		if (thisTerminal.equals(",") || thisTerminal.equals(";")) return false; // special case
		if (!(e1 instanceof Terminal)) return true;
		if (thisTerminal.isEmpty()) return false;
		if (Character.isUnicodeIdentifierPart(thisTerminal.charAt(0))) return true;
		return false;
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
					String key;
					Variable v;
					if (e2 instanceof Variable) { // idempotence of type checking
						v = (Variable)e2;
						key = ((Variable)e2).getSymbol();
					} else if (e2 instanceof NonTerminal) {
						NonTerminal nt = (NonTerminal)e2;
						key = Util.stripId(nt.getSymbol());
						v = new Variable(key, nt.getLocation());
					} else {
						ErrorHandler.error(Errors.BAD_SYNTAX_BINDING, e2);
						throw new RuntimeException("should not get here");
					}
					if (!map.containsKey(key)) {
						map.put(key, v);
					}
				}
			}
		}
	}

	@Override
	void getFree(Set<NonTerminal> freeSet, boolean rigidOnly) {
		for (Element e: elements) {
			e.getFree(freeSet, rigidOnly);
		}
	}

	// computes Syntax type for each variable
	public void computeVarTypes(SyntaxDeclaration parent, Map<String,Variable> varMap) {

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
	@Override
	public Clause typecheck(Context ctx) {
		for (int i = 0; i < elements.size(); ++i) {
			Element e = elements.get(i);
			elements.set(i, e.typecheck(ctx));
		}
		if (elements.size() == 1 && elements.get(0) instanceof Clause) {
			return (Clause)elements.get(0);
		}
		return this;
	}

	/**
	 * Return a copy of the contents without terminals.
	 * @param es element list, must not be null
	 * @return a new list including only the elements that are not terminals (noise words).
	 */
	protected List<Element> withoutTerminals() {
		List<Element> result = new ArrayList<Element>();
		for (Element e : elements) {
			if (e instanceof Terminal) continue;
			result.add(e);
		}
		return result;
	}
	
	private static Term asLFType(ElemType t) {
		if (t instanceof SyntaxDeclaration)
			return ((SyntaxDeclaration)t).typeTerm();
		else if (t instanceof Judgment)
			return ((Judgment)t).typeTerm();
		else throw new RuntimeException("Cannot convert " + t + " to an LF type");
	}
	
	/**
	 * Generate an error if the two elements don't match.
	 * Neither will be a terminal or a clause.
	 * @param orig original element
	 * @param repl new element
	 */
	protected static void checkMatch(Element orig, Element repl) {
		Term type1 = asLFType(orig.getElemType());
		Term type2 = asLFType(repl.getElemType());
		if (type1 != type2) {
			ErrorHandler.error(Errors.RENAME_TYPE_MISMATCH, repl.getElemType().getName() + " != " + orig.getElemType().getName(), repl,
					"SASyLF computed the LF types as " + type1 + " and " + type2);
		}
		if (orig instanceof NonTerminal) {
			if (!(repl instanceof NonTerminal)) {
				ErrorHandler.error(Errors.RENAME_MISMATCH, "" + orig, repl);
			}
		} else if (orig instanceof Variable) {
			if (!(repl instanceof Variable)) {
				ErrorHandler.error(Errors.RENAME_MISMATCH, "" + orig,repl);
			}
		} else if (orig instanceof Binding) {
			if (!(repl instanceof Binding)) {
				ErrorHandler.error(Errors.RENAME_MISMATCH, "" + orig, repl);
			}
			Binding ob = (Binding)orig;
			Binding rb = (Binding)repl;
			List<Element> oes = ob.getElements();
			List<Element> res = rb.getElements();
			if (oes.size() != res.size()) {
				ErrorHandler.error(Errors.RENAME_MISMATCH, "" + orig, rb);
			}
			int n = oes.size();
			for (int i=0; i < n; ++i) {
				checkMatch(oes.get(i),res.get(i));
			}
		}
	}
	
	/**
	 * Check that a clause re-interpreted with this syntax has the
	 * right number and type of content parts.
	 * @param o original clause
	 */
	public void checkClauseMatch(Clause o) {
		List<Element> cf = withoutTerminals();
		List<Element> of = o.withoutTerminals();
		if (cf.size() != of.size()) {
			ErrorHandler.error(Errors.RENAME_MISMATCH,"" + o, this);
		}
		int m = cf.size();
		for (int j = 0; j < m; ++j) {
			Clause.checkMatch(of.get(j),cf.get(j));
		}
	}
	
	/**
	 * Parse the contents of this clause and return the parsed version.
	 * @param ctx context to use, must not be null
	 * @return the parsed version of this clause
	 * @throws SASyLFError if clause cannot be parsed at all, or parse is ambiguous.
	 * @see #computeClause(Context, boolean) to parse only for something that can be a variable
	 * @see #computeClause(Context, NonTerminal) to parse only for a nonterminal
	 * @see #computeClause(Context, edu.cmu.cs.sasylf.grammar.Grammar) to parse using a custom grammar
	 */
	public Element computeClause(Context ctx) throws SASyLFError {
		return computeClause(ctx,false);
	}
	
	/**
	 * Parse the contents of this clause for the nonterminal and return the parse version
	 * @param ctx context to use, must not be null
	 * @param nt nonterminal (optional) to parse for
	 * @return the parsed version of this clause
	 * @throws SASyLFError if the clause cannot be parsed at all, or parse is ambiguous.
	 */
	public Element computeClause(Context ctx, NonTerminal nt) throws SASyLFError {
		return computeClause(ctx,false,ctx.getGrammar(),nt);
	}
	
	/**
	 * Parse the contents of this clause where we can select whether
	 * the result must be something that can be passed as a parameter.
	 * @param ctx context to use, must not be null
	 * @param inBinding true if the result must be something that can be passed as a parameter
	 * @return the parsed version of this clause
	 * @throws SASyLFError if the clause cannot be parsed at all, or parse is ambiguous.
	 */
	public Element computeClause(Context ctx, boolean inBinding) {
		return computeClause(ctx, inBinding, ctx.getGrammar(), null);
	}	
	
	/**
	 * Parse the contents of this clause using a custom grammar.
	 * @param ctx context (must not be null)
	 * @param g grammar to use 
	 * @return parsed version of this clause
	 * @throws SASyLFError if the clause cannot be parsed at all, or if parse is ambiguous
	 */
	public Element computeClause(Context ctx, edu.cmu.cs.sasylf.grammar.Grammar g) throws SASyLFError {
		return computeClause(ctx, false, g, null);
	}
	
	protected Element computeClause(Context ctx, boolean inBinding,
			edu.cmu.cs.sasylf.grammar.Grammar g, NonTerminal nt) {
		// compute a ClauseUse based on parsing the input
		List<GrmTerminal> symList = getTerminalSymbols();
		if (symList.isEmpty()) return OrClauseUse.makeEmptyOrClause(getLocation());
		return parseClause(ctx, inBinding, g, symList, nt);
	}

	/**
	 * @param ctx
	 * @param inBinding
	 * @param g
	 * @param symList
	 * @return
	 */
	private Element parseClause(Context ctx, boolean inBinding,
			edu.cmu.cs.sasylf.grammar.Grammar g, List<GrmTerminal> symList,
			NonTerminal nt) {
		/*
		 * JTB: The following section is to implement parsing of "and" and "or" judgments
		 * without requiring us to change the grammar.
		 */
		boolean hasAnd = false;
		boolean hasOr = false;
		boolean hasNot = false;
		int parens = 0;
		for (GrmTerminal t : symList) {
			if (t == GrmUtil.getLeftParen()) ++parens;
			else if (t == GrmUtil.getRightParen()) --parens;
			if (parens > 0) continue;
			Element elem = t.getElement();
			if (elem instanceof AndOrJudgment.OpTerminal) {
				switch (((AndOrJudgment.OpTerminal)elem).getOpName()) {
				case "and": hasAnd = true; break;
				case "or": hasOr = true; break;
				default:
					ErrorHandler.error(Errors.INTERNAL_ERROR, "Unknown operator: " + elem, elem);
				}
			} else if (elem instanceof NotJudgment.NotTerminal) {
				hasNot = true;
			}
		}
		if (hasOr && hasAnd) {
			ErrorHandler.error(Errors.ANDOR_AMBIGUOUS, this);
		}
		if (hasNot) {
			ErrorHandler.error(Errors.NOT_UNSUPPORTED,this);
		}
		if (hasAnd || hasOr) {
			// System.out.println("Found 'and'/'or'" + symList);
			List<List<GrmTerminal>> symLists = new ArrayList<List<GrmTerminal>>();
			List<GrmTerminal> sepList = new ArrayList<GrmTerminal>();
			List<GrmTerminal> aList = new ArrayList<GrmTerminal>();
			parens = 0;
			for (GrmTerminal t : symList) {
				if (t == GrmUtil.getLeftParen()) ++parens;
				else if (t == GrmUtil.getRightParen()) --parens;
				if (parens == 0 &&
						(t.getElement() instanceof AndOrJudgment.OpTerminal ||
								t.getElement() instanceof OrJudgment.OpTerminal)) {
					symLists.add(aList);
					aList = new ArrayList<GrmTerminal>();
					sepList.add(t);
				} else {
					aList.add(t);
				}
			}
			symLists.add(aList);
			List<ClauseUse> clauses = new ArrayList<ClauseUse>();
			for (List<GrmTerminal> sublist : symLists) {
				stripParens(sublist);
				// The following crashes if it starts with a paren:
					// Clause subClause = new Clause(sublist.isEmpty() ? this.getLocation() : sublist.get(0).getElement().getLocation()); 
				Clause subClause = new Clause(this.getLocation()); 
				Stack<Clause> stack = new Stack<Clause>();
				for (GrmTerminal t : sublist) {
					Element element = t.getElement();
					if (element == null) {
						if (t == GrmUtil.getLeftParen()) {
							stack.push(subClause);
							subClause = new Clause(this.getLocation());
							continue;
						} else if (t == GrmUtil.getRightParen()) {
							element = subClause;
							subClause = stack.pop();
						}
					}
					if (subClause.elements.isEmpty()) { // first to add!
						subClause.setLocation(element.getLocation());
					}
					subClause.add(element);
				}
				// using a subClause forces the error to print correctly.
				Element e = subClause.parseClause(ctx,inBinding,g,sublist,null);
				if (e instanceof ClauseUse) {
					clauses.add((ClauseUse)e);
				} else {
					ErrorHandler.error(Errors.ANDOR_NOSYNTAX, "" + e, this);
				}
			}
			return AndOrClauseUse.create(hasAnd, getLocation(), ctx, clauses);
		}
		/*
		 * JTB: End of section to implement parsing of "and"/"or" judgments
		 */

		RuleNode parseTree = null;
		try {
			parseTree = g.parse(symList);
			return computeClause(parseTree);
		} catch (NotParseableException e) {
			// kludge while we figure out what to do with "contradiction"
			if (symList.size() == 1 && symList.get(0).toString().equals("contradiction")) {
				return OrClauseUse.makeEmptyOrClause(getLocation());
			}
			/*for (edu.cmu.cs.sasylf.grammar.Rule r : g.getRules()) {
				debug_parse(r.toString());
			}
			debug_parse(g.getStart().toString());*/
			ErrorHandler.error(Errors.CLAUSE_PARSE," "+ this, this);
			throw new RuntimeException("should be unreachable");
		} catch (AmbiguousSentenceException e) {
			final Set<RuleNode> trees = e.getParseTrees();
			Util.debug_parse("ambiguous trees number: "+trees.size());
			if (Util.DEBUG_PARSE) {
				for (ParseNode pn : trees) {
					printTreeVerbose(pn,1);
				}
			}
			if (trees.size() == 1)
				debug_parse("ambiguous parse trees are actually equal!");
			// if inBinding and only one parse tree has a potentially variable type, try to escape
			if (inBinding) {
				Set<String> varTypes = new HashSet<String>();
				for (Variable v : ctx.varMap.values()) {
					varTypes.add(v.getType().toString());
				}
				//System.err.println(varTypes.toString());
				Set<RuleNode> keptTrees = new HashSet<RuleNode>();
				for (RuleNode tree : trees) {
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
			
			// look for unique tree that follows the given rule
			if (nt != null) {
				Symbol grmSymbol = nt.getGrmSymbol();
				RuleNode follows = null;
				int found = 0;
				for (RuleNode tree : trees) {
					Rule treeRule = tree.getRule();
					List<Symbol> symbols = treeRule.getRightSide();
					if (symbols.size() == 1 && symbols.get(0) == grmSymbol) {
						follows = tree;
						found++;
					}
				}
				if (found == 1)
					return computeClause(follows);
			}
						
			List<String> possibilities = getAmbiguousParses(trees);
			ErrorHandler.error(Errors.CLAUSE_AMBIGUOUS," " +possibilities, this, "The underlying parse trees are " + trees);
			throw new RuntimeException("should be unreachable");
		}
	}
	
	private static final int SHOW_AMBIGUOUS_COUNT = 2;
	private List<String> getAmbiguousParses(Set<RuleNode> trees) {
		List<String> possibilities = new ArrayList<>();
		int n = 0;
		for (Iterator<RuleNode> it = trees.iterator(); it.hasNext() && n < SHOW_AMBIGUOUS_COUNT; ++n) {
			RuleNode poss = it.next();
			possibilities.add(computeClause(poss).toString());
		}
		return possibilities;
	}
	
	public static void printTreeVerbose(ParseNode pn, int indent) {
		for (int i=0; i < indent; ++i) {
			System.out.print("  ");
		}
		if (pn instanceof RuleNode) {
			RuleNode rn = (RuleNode)pn;
			System.out.println(rn.getRule());
			for (ParseNode ch : rn.getChildren()) {
				printTreeVerbose(ch,indent+1);
			}
		} else {
			System.out.println(pn);
		}
	}

	private void stripParens(List<GrmTerminal> syms) {
		// simple not super-efficient implementation
		int l = syms.size()-1;
		GrmTerminal leftParen = GrmUtil.getLeftParen();
		GrmTerminal rightParen = GrmUtil.getRightParen();
		while (syms.size() >= 2 && 
				syms.get(0) == leftParen && 
				syms.get(l) == rightParen) {
			int parens = 0;
			for (int i=1; i < l; ++i) {
				GrmTerminal t = syms.get(i);
				if (t == leftParen) ++parens;
				else if (t == rightParen) {
					if (--parens < 0) return; // no change
				}
			}
			syms.remove(l);
			syms.remove(0);
			l -= 2;
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
			if (newElements.size() == 1) {
				return newElements.get(0);
			}
			/*
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
			}*/
			ErrorHandler.error(Errors.INTERNAL_ERROR, ": not sure what to do with null ClauseUse on " + newElements, this);
		}
		Location loc = getLocation();
		if (!newElements.isEmpty()) loc = newElements.get(0).getLocation();
		return new ClauseUse(loc, newElements, cd);
	}


	@Override
	public Fact asFact(Context ctx, Element assumes) {
		throw new RuntimeException("internal error: can't get fact before typechecking");
	}

	@Override
	public Term computeTerm(List<Pair<String, Term>>  varBindings) {
		throw new RuntimeException("internal error: can't compute the term before typechecking, at line " + getLocation().getLine());
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
			this.prettyPrint(pw, new PrintContext(t, ctx.inputVars, ctx.assumedContext));
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
	@Override
	public Set<Pair<Term,Substitution>> caseAnalyze(Context ctx, Term term, Element target,
			Node source) {
		throw new RuntimeException("should only call caseAnalyze on a clause def, not " + this);
	}

}
