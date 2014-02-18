/** Written by Matthew Rodriguez, 2008.
 * 
 */

package edu.cmu.cs.sasylf.prover;

import static edu.cmu.cs.sasylf.term.Facade.App;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.sasylf.ast.ClauseDef;
import edu.cmu.cs.sasylf.ast.ClauseUse;
import edu.cmu.cs.sasylf.term.*;

public class UnprovedNodeImpl implements UnprovedNode {

	private Judgment judgment;
	private int depth;
	private int choiceDepth;
	
	/** Constructs a new UnprovedNode.
	 * @param j The Judgment to be proven (or not.)
	 * @param d This node's depth in the proof tree.
	 * @param c This node's depth in the choice tree.
	 */
	public UnprovedNodeImpl(Judgment j, int d, int c) {
		judgment = j;
		depth = d;
		choiceDepth = c;
	}
	
	public int getChoiceDepth() {
		return choiceDepth;
	}

	public int getDepth() {
		return depth;
	}

	/**
	 * Written by Dr. Aldrich, 2008
	 */
	public List<Rule> getRulesThatApply(Proof proof) {
		List<Rule> result = new ArrayList<Rule>();
		ProofImpl proofImpl = (ProofImpl) proof;
		
		// get a substituted term
		Term derivTerm = judgment.getTerm().substitute(proofImpl.getSubstitution());
		edu.cmu.cs.sasylf.ast.Judgment judgmentType = judgment.getJudgmentType();
		
		// try all the rules of the appropriate type, in turn
		for (edu.cmu.cs.sasylf.ast.Rule rule: judgmentType.getRules()) {
			Term ruleTerm = rule.getFreshRuleAppTerm(derivTerm, new Substitution(), null);
			
			/*List<Term> termArgs = new ArrayList<Term>();
			for (int i = 0; i < rule.getPremises().size(); ++i) {
				ClauseDef clauseDef = ((ClauseUse) rule.getPremises().get(i)).getConstructor(); 
				Term type = clauseDef.getTypeTerm();
				String name = clauseDef.getConstructorName();
				Term argTerm = Facade.FreshVar(name, type);
				termArgs.add(argTerm);
			}
			termArgs.add(derivTerm);
			Term appliedTerm = App(rule.getRuleAppConstant(), termArgs);*/
			List<Term> termArgs = rule.getFreeVarArgs(derivTerm);
			termArgs.add(derivTerm);
			Term appliedTerm = App(rule.getRuleAppConstant(), termArgs);
			
			Substitution sub = null;
			try {
				sub = appliedTerm.unify(ruleTerm);
				if (! sub.avoid(proofImpl.getInputVars()))
						continue;
			} catch (UnificationFailed e) {
				continue; // try the next possible rule
			}
			
			// construct a RuleInstance and add it to result
			List<Judgment> preconditions = new ArrayList<Judgment>();
			
			for(int i = 0; i < rule.getPremises().size(); ++i) {
				Term termArg = termArgs.get(i);
				ClauseDef clauseDef = ((ClauseUse) rule.getPremises().get(i)).getConstructor();
				edu.cmu.cs.sasylf.ast.Judgment judgeType = (edu.cmu.cs.sasylf.ast.Judgment) clauseDef.getType();
				Term substitutedTerm = termArg.substitute(sub);
				preconditions.add(new Judgment(substitutedTerm, judgeType));
			}
			
			result.add(new RuleInstance(judgment, preconditions, sub, rule));
		}
		
		return result;
	}

	public Judgment getJudgment() {
		return judgment;
	}

	public String toString(int tabs) {
		String s = "";
		for(int i = 0; i < tabs; i++) {
			s += "\t";
		}
		s += judgment.toString();
		return s;
	}
	
	public String toString() {
		return judgment.toString();
	}

	public void prettyPrint(Substitution sub) {
		return;
	}

	public int getId() {
		return 0;
	}
}
