/** Written by Matthew Rodriguez, 2007.
 * An LR parse automaton. Tries to parse the given sentence using the given grammar.
 */

package edu.cmu.cs.sasylf.grammar;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

class Automaton {

	private List<Terminal> sentence;
	private LRParseTable lrz;
	private Stack<Integer> state;
	private int index;
	private LinkedList<ParseNode> buffer;
	private Grammar grammar;
	private int id;
	
	private static int idcounter = 0;

	/** Creates a new Automaton to try to parse a sentence.
	 * @param list The sentence to be parsed.
	 * @param g The Grammar to be used.
	 * @throws NotParseableException 
	 */
	public Automaton(List<? extends Terminal> list, Grammar g) throws NotParseableException {
		buffer = new LinkedList<ParseNode>();
		state = new Stack<Integer>();
		sentence = new LinkedList<Terminal>();

		for(Terminal t: list) {
			sentence.add(t);
		}		
		sentence.add(AcceptSymbol.getAcceptSymbol());

		state.push(0);
		
		lrz = g.getTable();
		index = 0;
		grammar = g;
		id = idcounter++;
	}
	
	/** Clones another automaton. Used extensively in GLR parsing.
	 * @param a the Automaton to be cloned.
	 */
	public Automaton(Automaton a) {
		sentence = a.sentence;
		lrz = a.lrz;
		state = duplicateStack(a.state);
		index = a.index;
		buffer = new LinkedList<ParseNode>(a.buffer);
		grammar = a.grammar;
		id = idcounter++;
	}

	/** Helper method that creates a shallow clone of a Stack.
	 * @param <T> The type of Stack.
	 * @param stack the stack to be cloned.
	 * @return a new stack.
	 */
	private <T> Stack<T> duplicateStack(Stack<T> stack) {
		Stack<T> temp = new Stack<T>();
		Stack<T> clone = new Stack<T>();
		temp.addAll(stack);
		clone.addAll(temp);
		return clone;
	}

	/** Try to parse the entire sentence in linear time. Only works if there are no
	 * conflicts in the parse table. If GLR parse functionality is necessary, use
	 * Grammar.parse(). Only slightly faster than Grammar.parse() in that case anyway.
	 * @throws NotParseableException if sentence cannot be parsed, even with GLR
	 * @throws AmbiguousSentenceException if not parseable in linear time; use Grammar.parse()
	 */
	public void parse() throws NotParseableException, AmbiguousSentenceException {
		while(!step());
	}
	
	/** Try to take a single step in parsing the sentence.
	 * @return true if we're finished; false otherwise
	 * @throws NotParseableException If encountered a null on the lookup table
	 * @throws AmbiguousSentenceException If encountered a Conflict on the lookup table
	 */
	public boolean step() throws NotParseableException, AmbiguousSentenceException {
		//Consult the parse table for the next action
		Action a = lrz.nextAction(state.peek(), sentence.get(index));
		
		//If there's nothing there, the sentence isn't parseable with this grammar
		if(a == null) {
			throw new NotParseableException();
		}
		
		switch(a.getType()) {
		//If it's a shift action, go ahead and shift.
		case SHIFT:
			shift(a);
			break;
		//If it's a reduce action, go ahead and reduce.
		case REDUCE:
			reduce(a);
			break;
		//If it's a conflict, throw an exception.
		case CONFLICT:
			throw new AmbiguousSentenceException((Conflict)a);
		//If it's an accept state, go ahead and accept.
		case ACCEPT:
			return true;
		default:
			throw new AssertionError("grammar is malformed");  
		}
		return false;
	}
	
	/** Manually force the Automaton to take an Action.
	 * @param a The Action to be taken.
	 * @throws NotParseableException 
	 */
	public void act(Action a) throws NotParseableException {
		if(a.getType() == ActionType.SHIFT) {
			shift(a);
		} else if(a.getType() == ActionType.REDUCE) {
			reduce(a);
		}
	}
	
	/** Shift according to the provided action.
	 * @param a The action used to shift.
	 */
	private void shift(Action a) {
		buffer.add(new TerminalNode(sentence.get(index)));
		index++;
		state.push(a.getNext());
	}
	
	/** Reduce according to the provided action.
	 * @param a The action used to reduce.
	 * @throws NotParseableException if the slot on the goto table is blank
	 */
	private void reduce(Action a) throws NotParseableException {
		List<ParseNode> reduced = new LinkedList<ParseNode>();
		Rule r = grammar.getRule(a.getNext());

		//Pops symbols and states to be reduced
		for(int i = 0; i < r.getRightSide().size(); i++) {
			reduced.add(0, buffer.removeLast());
			state.pop();
		}
		
		//Place the new symbol in the buffer
		buffer.add(new RuleNode(r, reduced));
		
		//Consult the goto table to see what state to goto next
		Integer pk = state.peek();
		NonTerminal ls = r.getLeftSide();
		Action act = lrz.nextGoto(pk, ls);
		if(act == null) {
			throw new NotParseableException();
		}
		int st = act.getNext(); 
		state.push(st);
	}

	/** Call only when done parsing!
	 * @return The parsed tree.
	 */
	public RuleNode results() {
		return (RuleNode)buffer.get(0);
	}

	/** Prints out the current buffer. Not recursive; won't print out the entire tree.
	 * Use RuleNode.toString() for that.
	 */
	public void printBuffer() {
		//Prints this automatons ID number and state
		System.out.print(id + " (" + state.peek() + "): ");
		
		//Prints out each parsenode in the buffer.
		for(ParseNode p: buffer) {
			if(p instanceof RuleNode) {
				RuleNode r = (RuleNode)p;
				System.out.print(r.toStringAsNonTerminal() + " ");
			} else {
				System.out.print(p + " ");
			}
		}
		
		//Prints out symbols we've not yet looked at.
		System.out.print("\t\t");
		for(int i = index; i < sentence.size(); i++) {
			System.out.print(sentence.get(i) + " ");
		}
		
		System.out.println();
	}
	
	/** Returns a unique ID generated for each automaton. Useful in debugging GLR parsing.
	 */
	public int getID() {
		return id;
	}
}
