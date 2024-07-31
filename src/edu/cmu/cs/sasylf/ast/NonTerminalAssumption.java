package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.CopyData;
import edu.cmu.cs.sasylf.SubstitutionData;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;


public class NonTerminalAssumption extends SyntaxAssumption {
	public NonTerminalAssumption(String n, Location l, boolean isTheoremArg, Element assumes) {
		this(new NonTerminal(n, l), assumes);
		this.isTheoremArg = isTheoremArg;
	}
	public NonTerminalAssumption(String n, Location l) {
		this(new NonTerminal(n, l),null);
	}
	public NonTerminalAssumption(String n, Location l, Element assumes) {
		this(new NonTerminal(n, l),assumes);
	}
	public NonTerminalAssumption(NonTerminal nt, Location l, Element assumes) {
		super(nt.getSymbol(), l, assumes);
		nonTerminal = nt;
	}
	public NonTerminalAssumption(NonTerminal nt, Element assumes) {
		super(nt.getSymbol(), nt.getLocation(),assumes);
		nonTerminal = nt;
	}
	public NonTerminalAssumption(NonTerminal nt) {
		this(nt,null);
	}

	public SyntaxDeclaration getSyntax() { return nonTerminal.getType(); }
	public boolean isTheoremArg() { return isTheoremArg; }

	@Override
	public void typecheck(Context ctx) {
		super.typecheck(ctx);
		Element e = nonTerminal.typecheck(ctx);
	if (e != nonTerminal)
			ErrorHandler.error(Errors.FORALL_NOT_SYNTAX, getName(), this);
		nonTerminal = (NonTerminal)e;
		getElementBase().checkBindings(ctx.bindingTypes, this);
	}

	@Override
	public Element getElementBase() { return nonTerminal; }

	private NonTerminal nonTerminal;
	private boolean isTheoremArg = false;

	public void substitute(SubstitutionData sd) {
		if (sd.didSubstituteFor(this)) return;
		super.substitute(sd);
		sd.setSubstitutedFor(this);

		// check if we should substitute for the nonTerminal

		if (sd.containsSyntaxReplacementFor(nonTerminal)) {
			// make a clone of the replacing NonTerminal and update the name of it with the filler characters
			// get the filler characters
			String fillerCharacters;

			if (nonTerminal.getSymbol().length() == sd.getFrom().length()) {
				fillerCharacters = "";
			}
			else {
				fillerCharacters = nonTerminal.getSymbol().substring(sd.getFrom().length());
			}

			NonTerminal newNonTerminal = sd.getSyntaxReplacementNonTerminal().cloneWithFillerCharacters(fillerCharacters);
			
			nonTerminal = newNonTerminal;
		}

	}

	@Override
	public NonTerminalAssumption copy(CopyData cd) {
		if (cd.containsCopyFor(this)) return (NonTerminalAssumption) cd.getCopyFor(this);
		
		NonTerminalAssumption clone = (NonTerminalAssumption) super.copy(cd);

		cd.addCopyFor(this, clone);
		
		clone.nonTerminal = clone.nonTerminal.copy(cd);
		
		return clone;
	}
}
