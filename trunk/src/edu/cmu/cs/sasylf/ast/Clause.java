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
import edu.cmu.cs.sasylf.grammar.RuleNode;
import edu.cmu.cs.sasylf.grammar.Symbol;
import edu.cmu.cs.sasylf.grammar.TerminalNode;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;

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

	public ClauseType getType() {
	  throw new RuntimeException("Cannot determine type of unparsed Clause");
	}
	
	public ElemType getElemType() {
		throw new RuntimeException(getLocation().getLine() + ": should only call getElemTypes on syntax def clauses which don't have sub-clauses; can't call getElemType() on a Clause");
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
				    key = nt.getSymbol();
				    v = new Variable(key, nt.getLocation());
				  } else {
						ErrorHandler.report(Errors.BAD_SYNTAX_BINDING, e2);
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
			elements.set(i, e.typecheck(ctx));
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
    if (symList.isEmpty()) return OrClauseUse.makeEmptyOrClause(getLocation());
		return parseClause(ctx, inBinding, g, symList);
	}
	
  /**
   * @param ctx
   * @param inBinding
   * @param g
   * @param symList
   * @return
   */
  private Element parseClause(Context ctx, boolean inBinding,
      edu.cmu.cs.sasylf.grammar.Grammar g, List<GrmTerminal> symList) {
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
      if (elem instanceof AndJudgment.AndTerminal) {
        hasAnd = true;
        if (hasNot) {
          ErrorHandler.report("ambiguous use of 'not'",this);
        }
      } else if (elem instanceof OrJudgment.OrTerminal) {
        hasOr = true;
        if (hasNot) {
          ErrorHandler.report("ambiguous use of 'not'",this);
        }
      } else if (elem instanceof NotJudgment.NotTerminal) {
        hasNot = true;
      }
    }
    if (hasOr && hasAnd) {
      ErrorHandler.report("Ambiguous use of 'and' and 'or'.  Use parentheses.", this);
    }
    if (hasNot) {
      ErrorHandler.report("'not' judgments not supported",this);
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
            (t.getElement() instanceof AndJudgment.AndTerminal ||
             t.getElement() instanceof OrJudgment.OrTerminal)) {
          symLists.add(aList);
          aList = new ArrayList<GrmTerminal>();
          sepList.add(t);
        } else {
          aList.add(t);
        }
      }
      symLists.add(aList);
      List<ClauseUse> clauses = new ArrayList<ClauseUse>();
      List<Judgment> types = new ArrayList<Judgment>();
      Element sharedContext = null;
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
          subClause.elements.add(element);
        }
        // using a subClause forces the error to print correctly.
        Element e = subClause.parseClause(ctx,inBinding,g,sublist);
        if (e instanceof ClauseUse) {
          ClauseUse cu = (ClauseUse)e;
          if (!clauses.isEmpty() && cu.getElements().size() > 0) {
            // set location, which otherwise refers to the whole thing
            cu.setLocation(cu.getElements().get(0).getLocation());
          }
          ClauseType ty = cu.getConstructor().getType();
          if (ty instanceof Judgment) types.add((Judgment)ty);
          else ErrorHandler.report("cannot '"+sepList.get(0)+"' syntax only judgments", this);
          clauses.add(cu);
          
          if (((Judgment)ty).getAssume() != null) {
            Element context = cu.getElements().get(cu.getConstructor().getAssumeIndex());
            if (sharedContext != null) {
              if (!sharedContext.equals(context)) {
                ErrorHandler.report("all '"+sepList.get(0)+"'ed judgments must use the same context", this);
              }
            } else {
              sharedContext = context;
            }
          }
        } else {
          ErrorHandler.report("can only '"+sepList.get(0)+"' clauses together, not nonterminals", this);
        }
      }
      List<Element> newElements = new ArrayList<Element>();
      Iterator<GrmTerminal> seps = sepList.iterator();
      for (ClauseUse cl : clauses) {
        for (Element e : cl.getElements()) {
          newElements.add(e);
        }
        if (seps.hasNext()) newElements.add(seps.next().getElement());
      }
      if (hasAnd) {
        ClauseDef cd = (ClauseDef)AndJudgment.makeAndJudgment(getLocation(), ctx, types).getForm();
        return new AndClauseUse(getLocation(),newElements,cd,clauses);
      } else {
        ClauseDef cd = (ClauseDef)OrJudgment.makeOrJudgment(getLocation(), ctx, types).getForm();
        return new OrClauseUse(getLocation(),newElements,cd,clauses);
      }
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
			ErrorHandler.report("internal error: not sure what to do with null ClauseUse on " + newElements, this);
		}
		return new ClauseUse(getLocation(), newElements, cd);
	}
	
	
	@Override
  public Fact asFact(Context ctx, Element assumes) {
    throw new RuntimeException("internal error: can't get fact before typechecking");
  }
	
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

}
