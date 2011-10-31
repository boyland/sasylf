/** Written by Matthew Rodriguez, 2007.
 * A class representing an LR grammar. Can generate a parse table for itself, and parse
 * sentences written in this grammar.
 */

package edu.cmu.cs.sasylf.grammar;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class Grammar {
	private NonTerminal start;
	private LinkedList<Rule> rules;
	private LRParseTable lrz;

	/** Constructs a new Grammar object.
	 * @param startSymbol The Symbol that the grammar starts with.
	 * @param ruleCollection The Collection of Rules the Grammar contains.
	 */
	public Grammar(NonTerminal startSymbol, List<? extends Rule> ruleCollection) {
		rules = new LinkedList<Rule>(ruleCollection);
		start = startSymbol;
	}

	/** Constructs a new Grammar object.
	 * @param startSymbol The Symbol that the grammar starts with.
	 */
	public Grammar(NonTerminal startSymbol) {
		this(startSymbol, new LinkedList<Rule>());
	}

	/** Constructs a new Grammar object.
	 */
	public Grammar() {
		this(null, new LinkedList<Rule>());
	}
	
	/** Set the start symbol for this grammar.
	 * @param s the new start symbol
	 */
	public void setStartSymbol(NonTerminal s) {
		start = s;
	}

	/** Returns the start symbol for this grammar.
	 */
	public NonTerminal getStart() {
		return start;
	}

	/** Add a new rule to this grammar.
	 * @param r The rule to be added to this grammar.
	 */
	public void addRule(Rule r) {
		rules.add(r);
	}

	/** Add a new rule to this grammar at a given index.
	 * @param index The rule to be added to this grammar.
	 * @param r The index to add it at.
	 */
	public void addRule(int index, Rule r) {
		rules.add(index, r);
	}

	/** Find rules in this grammar for which the left side is the symbol s.
	 * @return a list of rules
	 */
	public List<Rule> findRules(Symbol s) {
		LinkedList<Rule> result = new LinkedList<Rule>();
		for(Rule r: rules) {
			if(r.getLeftSide() == s) {
				result.add(r);
			}
		}
		return result;
	}

	/** 
	 * @param i
	 * @return the Rule at index i
	 */
	public Rule getRule(int i) {
		return rules.get(i);
	}

	/**
	 * @return the list of all rules in this grammar
	 */
	public List<Rule> getRules() {
		return rules;
	}

	/** For an augmented grammar (and ONLY an augmented grammar), return the ItemRules
	 *  that S produces.
	 */
	List<ItemRule> findItemRules(Symbol s) {
		LinkedList<ItemRule> result = new LinkedList<ItemRule>();
		for(Rule r: rules) {
			if(r.getLeftSide() == s) {
				result.add((ItemRule)r);
			}
		}
		return result;
	}
	
	/** For an augmented grammar (and ONLY an augmented grammar), return all ItemRules.
	 */
	List<ItemRule> allItemRules() {
		LinkedList<ItemRule> result = new LinkedList<ItemRule>();
		for(Rule r: rules) {
			result.add((ItemRule)r);
		}
		return result;
	}

	/** A method that generates an LR(0) parse table and parses it with a GLR parser.
	 * @param list A sentence to  be parsed.
	 * @return A tree representing the parsing.
	 * @throws NotParseableException when the sentence has no possible parsings.
	 * @throws AmbiguousSentenceException when the sentence has more than one potential parsing.
	 */
	public RuleNode parse(List<? extends Terminal> list) throws NotParseableException, AmbiguousSentenceException {
		Queue<Automaton> queue = new LinkedList<Automaton>();
		LinkedList<Automaton> done = new LinkedList<Automaton>();
		Set<RuleNode> parseTrees = new HashSet<RuleNode>();
		
		//Creates a new automaton to try to parse the sentence, and adds it to the queue.
		queue.add(new Automaton(list, this));
		
		//Attempts to retrieve the next automaton and make it do one step.
		while(!queue.isEmpty()) {
			Automaton a = queue.poll();
			try {
				if(a.step()) {
					//If the automaton finished parsing successfully, add it to the list of 
					//successful parsings and remove it from the queue.
					done.add(a); 
					parseTrees.add(a.results());
					if(parseTrees.size() >= 2)
						break;
					else
						continue;
				}
			} catch (AmbiguousSentenceException e) { //If we encountered a conflict...
				for(Action action: e.getConflict().getActions()) {
					//Create a new automaton for each possible action we can take
					Automaton clone = new Automaton(a);
					clone.act(action);
					queue.offer(clone);
				}
				continue;
			} catch (NotParseableException e) {
				//If we encounter an error, throw this automaton away.
				continue;
			}
			queue.offer(a);
		}
		
		//If no successful parsings were produced, throw an exception.
		if(done.size() == 0) {
			throw new NotParseableException();
		}
		
		//If more than one successful parsing was produced, throw an exception.
		if(parseTrees.size() >= 2) {
			throw new AmbiguousSentenceException(parseTrees);
		}
		
		//Otherwise, return the true parsing.
		return done.getFirst().results();
	}

	/** Create a new Grammar based on this one but with ItemRules instead of Rules and ReadSymbols
	 *  for use in constructing ItemSets.
	 * @return A new augmented grammar.
	 */
	Grammar augment() {
		//To augment a grammar, first add a new start symbol, and a rule that the new
		//start symbol produces the old one. Make this the very first rule.
		Grammar augment = new Grammar(StartSymbol.getStartSymbol());
		LinkedList<Symbol> list = new LinkedList<Symbol>();
		list.add(start);
		augment.addRule(new ItemRule(StartSymbol.getStartSymbol(), list, 0));
		
		//For each rule in the original grammar, make a new one but with a read symbol "$"
		//in front.
		for(Rule r: rules) {
			augment.addRule(new ItemRule(r, rules.indexOf(r)));
		}
		return augment;
	}

	/** Prints a list of all the rules in this grammar.
	 */
	public String toString() {
		String s = "";
		for(Rule r: rules) {
			s += r + "\n";
		}
		return s;
	}

	/** Gets the LRParseTable for this grammar. If it doesn't exist, try to create it.
	 * @return an LRParseTable
	 * @throws NotParseableException if this grammar is empty.
	 */
	LRParseTable getTable() throws NotParseableException {
		if(rules.isEmpty()) {
			throw new NotParseableException();
		}
		if(lrz == null) {
			lrz = new LRZeroParseTable(this);
		}
		return lrz;
	}
}
