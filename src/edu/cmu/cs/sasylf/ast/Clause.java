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

import edu.cmu.cs.sasylf.CloneData;
import edu.cmu.cs.sasylf.SubstitutionData;
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

public class Clause extends Element implements CanBeCase {
	
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

	public Clause clone() {
		try {
			return (Clause) super.clone();
		}
		catch (CloneNotSupportedException e) {
			System.out.println("Clone not supported in Clause");
			System.exit(1);
			return null;
		}
	}

	public Clause copy(CloneData cd) {
		if (cd.containsCloneFor(this)) return (Clause) cd.getCloneFor(this);
		Clause clone = (Clause) super.copy(cd);

		cd.addCloneFor(this, clone);

		List<Element> newElements = new ArrayList<Element>();

		for (Element e : elements) {
			newElements.add(e.copy(cd));
		}
		clone.elements = newElements;

		return clone;
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
		if (t instanceof RenameSyntaxDeclaration) {
			RenameSyntaxDeclaration rsd = (RenameSyntaxDeclaration)t;
			return asLFType(rsd.original);
		}
		if (t instanceof SyntaxDeclaration) {
			return ((SyntaxDeclaration)t).typeTerm();
		}
		else if (t instanceof Judgment)
			return ((Judgment)t).typeTerm();
		else throw new RuntimeException("Cannot convert " + t + " to an LF type");
	}
	
	private static Element findOriginal(Element e) {
		// if it's a NonTerminal, we want to find the originally defined version of it

		if (e instanceof NonTerminal) {
			NonTerminal nt = (NonTerminal) e;
			// get the type
			SyntaxDeclaration declaration = nt.getType();
			// check if it's a renamed syntax
			// if it is, cast it, then go to the original and fetch the nonterminal
			// call the function recursively on the nonterminal
			if (declaration instanceof RenameSyntaxDeclaration) {
				RenameSyntaxDeclaration renameDeclaration = (RenameSyntaxDeclaration) declaration;
				SyntaxDeclaration originalDeclaration = renameDeclaration.original;

				// get the nonterminal
				
				NonTerminal originalNonTerminal = originalDeclaration.getNonTerminal();

				// call the function recursively on the original nonterminal

				return findOriginal(originalNonTerminal);
			}
		}

		return e;
	}
	
	/**
	 * Generate an error if the two elements don't match.
	 * Neither will be a terminal or a clause.
	 * @param orig original element
	 * @param repl new element
	 */
	protected static void checkMatch(Element orig, Element repl) {
		Element origOriginal = findOriginal(orig);
		Element replOriginal = findOriginal(repl);

		Term type1 = asLFType(origOriginal.getElemType());
		Term type2 = asLFType(replOriginal.getElemType());

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

	public void substitute (SubstitutionData sd) {
		super.substitute(sd);
		for (int j = 0; j < getElements().size(); j++) {
			// if the element is a NonTerminal, check if it has the same name as the one we are substituting for
			Element e = getElements().get(j);
			// check if it's a NonTerminal
			if (e instanceof NonTerminal) {
				NonTerminal nt = (NonTerminal) e;
				// check if it has the same name as the one we are substituting for
				if (sd.containsSyntaxReplacementFor(nt)) {
					// get the filler characters
					String fillerCharacters;

					if (nt.getSymbol().length() == sd.from.length()) {
						fillerCharacters = "";
					}
					else {
						fillerCharacters = nt.getSymbol().substring(sd.from.length());
					}

					// create a shallow copy of the new NonTerminal

					NonTerminal newNonTerminal = sd.getSyntaxReplacementNonTerminal().clone();

					// TODO: maybe the new nonterminal should actually just be the original (without cloning)
					
					//NonTerminal newNonTerminal = sd.getSyntaxReplacementNonTerminal();

					// add the filler characters to the new nonterminal symbol

					newNonTerminal.symbol += fillerCharacters;

					// replace it in the elements list

					getElements().set(j, newNonTerminal);

				}
			}
			else if (e instanceof Clause) {
				Clause c = (Clause) e;
				c.substitute(sd);
			}
		}
	}

	public static void checkClauseSameStructure (
		Clause paramClause, 
		Clause argClause,
		Map<Syntax, Syntax> paramToArgSyntax,
		Map<Judgment, Judgment> paramToArgJudgment,
		Map<String, String> nonTerminalMapping
		)
	{
		
		if (paramClause instanceof AndClauseUse && !(argClause instanceof AndClauseUse)) {
			System.out.println("paramClause is AndClauseUse but argClause is not");
			System.exit(0);
		}

		if (paramClause instanceof OrClauseUse && !(argClause instanceof OrClauseUse)) {
			System.out.println("paramClause is OrClauseUse but argClause is not");
			System.exit(0);
		}
		
		// make sure that the types of the clauses match

		checkClausesCorrespondingTypes(paramClause, argClause, paramToArgSyntax, paramToArgJudgment);

		// ignore everything except for nonterminals
		List<Element> c1Elements = paramClause.withoutTerminals();
		List<Element> c2Elements = argClause.withoutTerminals();

		if (c1Elements.size() != c2Elements.size()) {
			// failure
			System.out.println("c1Elements.size() != c1Elements.size()");
			System.exit(0);
			return;
		}

		// go through each of the elements

		for (int i = 0; i < c1Elements.size(); i++) {
			Element e1 = c1Elements.get(i);
			Element e2 = c2Elements.get(i);

			if (e1 instanceof NonTerminal && e2 instanceof NonTerminal) {
				NonTerminal nt1 = (NonTerminal) e1;
				NonTerminal nt2 = (NonTerminal) e2;

				// nt1 is the parameter, and nt2 is the argument

				// check if the syntax declaration of nt1 is already mapped to something
				if (paramToArgSyntax.containsKey(nt1.getType())) {
					if (paramToArgSyntax.get(nt1.getType()) != nt2.getType().getOriginalDeclaration()) {
						// the syntax declaration of nt2 is not the syntax declaration that the syntax declaration of nt1 is mapped to
						// failure
						System.exit(0);
					}
				}
				// otherwise, add the mapping from the syntax declaration of nt1 to the syntax declaration of nt2
				else {
					paramToArgSyntax.put(nt1.getType(), nt2.getType());
				}

				// make sure that the nonterminal mapping is consistent
				
				String paramSymbol = nt1.getSymbol();
				String argSymbol = nt2.getSymbol();

				if (nonTerminalMapping.containsKey(paramSymbol)) {
					if (!nonTerminalMapping.get(paramSymbol).equals(argSymbol)) {
						System.out.println("Nonterminals don't match when typechecking module. Expected " + nonTerminalMapping.get(paramSymbol) + " but got " + argSymbol);
					}
				}
				else {
					nonTerminalMapping.put(paramSymbol, argSymbol);
				}

			}

			// if they are both clauses, recursively call this function

			else if (e1 instanceof Clause && e2 instanceof Clause) {

				Clause e1c = (Clause) e1;
				Clause e2c = (Clause) e2;

				checkClauseSameStructure(e1c, e2c, paramToArgSyntax, paramToArgJudgment, nonTerminalMapping);
			}

			// TODO: implement the other cases for the other types of elements

			else {
				System.out.println("Clause same structure check failure 2");
				System.exit(0);
			}

		}

	}
	
	private static void checkClausesCorrespondingTypes(
		Clause c1,
		Clause c2,
		Map<Syntax, Syntax> paramToArgSyntax,
		Map<Judgment, Judgment> paramToArgJudgment
	) {
		ClauseType ct1 = c1.getType();
		ClauseType ct2 = c2.getType();

		if (ct1 instanceof Syntax && ct2 instanceof Syntax) {
			Syntax s1 = (Syntax) ct1;
			Syntax s2 = (Syntax) ct2;
			// check is s1 is already bound to something
			if (paramToArgSyntax.containsKey(s1)) {
				if (paramToArgSyntax.get(s1) != s2) {
					System.out.println("Clause same structure check failure 3");
					System.exit(0);
				}
			}
			else {
				// add the mapping
				paramToArgSyntax.put(s1, s2);
			}
		}

		else if (ct1 instanceof Judgment && ct2 instanceof Judgment) {
			Judgment j1 = (Judgment) ct1;
			Judgment j2 = (Judgment) ct2;
			// check if j1 is already bound to something
			if (paramToArgJudgment.containsKey(j1)) {
				if (paramToArgJudgment.get(j1) != j2) {
					System.out.println("Clause same structure check failure 4");
					System.exit(0);
				}
			}
			else {
				// add the mapping
				paramToArgJudgment.put(j1, j2);
			}
		}

		else {
			System.out.println("Clause same structure check failure 5");
			System.exit(0);
		}

	}


	
}
