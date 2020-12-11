package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.ast.grammar.GrmNonTerminal;
import edu.cmu.cs.sasylf.ast.grammar.GrmRule;
import edu.cmu.cs.sasylf.ast.grammar.GrmTerminal;
import edu.cmu.cs.sasylf.ast.grammar.GrmUtil;
import edu.cmu.cs.sasylf.grammar.Symbol;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;

/**
 * Syntactic sugar productions: syntax that 
 * is "short" for other syntax.  These declarations affect parsing,
 * but are replaced with their definitions when they are used.
 * For now: only single terminal sugar is supported 
 */
public class Sugar extends Syntax {
	private Clause sugar;
	private NonTerminal typeName;
	private SyntaxDeclaration type;
	private Clause replacement;
	
	public Sugar(Element lhs, Clause rhs) {
		super(lhs.getLocation());
		if (lhs instanceof Clause) {
			sugar = (Clause)lhs;
		} else {
			sugar = new Clause(lhs);
			sugar.add(lhs);
		}
		replacement = rhs;
		if (rhs != null) this.setEndLocation(rhs.getEndLocation());
	}
	
	public Sugar(Element lhs, SyntaxDeclaration syn) {
		this(lhs,syn.getNonTerminal());
		type = syn;
	}
	
	public Sugar(Element lhs, NonTerminal nt) {
		this(lhs,(Clause)null);
		typeName = nt;
	}
	
	@Override
	public void prettyPrint(PrintWriter out) {
		sugar.prettyPrint(out);
		out.print(" := ");
		replacement.prettyPrint(out);
		out.println();
	}

	@Override
	public Set<Terminal> getTerminals() {
		return sugar.getTerminals();
	}

	@Override
	public void typecheck(Context ctx) {
		sugar.typecheck(ctx);
		for (Element e: sugar.getElements()) {
			if (e instanceof Clause) {
				ErrorHandler.error(Errors.CLAUSE_DEF_PAREN, e);
			}
		}
		Set<String> lhsVars = new HashSet<String>();
		for (Element e: sugar.getElements()) {
			if (e instanceof NonTerminal) {
				if (!lhsVars.add(((NonTerminal)e).getSymbol())) {
					ErrorHandler.error(Errors.SUGAR_MULTIPLE_USES, ": " + e, sugar);
				}
			}
		}
		if (replacement != null) {
			replacement.typecheck(ctx);
			for (NonTerminal nt : replacement.getFree(true)) {
				if (!lhsVars.remove(nt.getSymbol())) {
					ErrorHandler.error(Errors.SUGAR_NO_USES, " :" + nt, this);
				}
			}
			if (!lhsVars.isEmpty()) {
				ErrorHandler.error(Errors.SUGAR_UNUSED, ": " + lhsVars, this);			
			}
		} else {
			if (type == null) {
				type = ctx.getSyntax(typeName.getSymbol());
				if (type == null) {
					ErrorHandler.error(Errors.SUGAR_SYNTAX_UNKNOWN, ": " + typeName, this);
				}
			}
			if (!type.isAbstract()) {
				ErrorHandler.error(Errors.SUGAR_ABSTRACT,this);
			}
		}
	}

	@Override
	public void postcheck(Context ctx) {
		// here we convert the replacement using the updated grammar,
		// and then update the grammar ourself:
		// sugar definitions cannot be used before they are defined.
		if (replacement != null) {
			Element newClause = replacement.computeClause(ctx, false);
			if (!(newClause instanceof Clause))
				ErrorHandler.error(Errors.SUGAR_UNKNOWN, this);
			else if (!(newClause.getType() instanceof SyntaxDeclaration))
				ErrorHandler.error(Errors.SUGAR_JUDGMENT,this);
			replacement = (Clause) newClause;
			Map<String,List<ElemType>> bindingTypes = new HashMap<String, List<ElemType>>();
			replacement.checkBindings(bindingTypes, this);
			type = (SyntaxDeclaration)replacement.getElemType();
			typeName = type.getNonTerminal();
		}
		
		ClauseDef cd;
		if (sugar instanceof ClauseDef) cd = (ClauseDef) sugar;
		else if (replacement != null) cd = new SugarClauseDef(sugar, type, replacement);
		else cd = new ClauseDef(sugar, type);
		sugar = cd;
		
		// SyntaxDeclaration adds things to parseMap (unused?) and prodMap (needed only for the constant)
		
		// The following code is copy&pasted from SyntaxDeclaration.  Ideally it should be done in one place:
		GrmRule r = new GrmRule(getSymbol(), cd.getSymbols(), cd);
		ctx.ruleSet.add(r);

		if (r.getRightSide().size() > 1 || r.getRightSide().get(0) instanceof GrmTerminal) {
			GrmRule rParens = new GrmRule(getSymbol(), new ArrayList<Symbol>(r.getRightSide()), cd);
			rParens.getRightSide().add(0, GrmUtil.getLeftParen());
			rParens.getRightSide().add(GrmUtil.getRightParen());					
			ctx.ruleSet.add(rParens);
		}

	}
	
	// the following is also copied from SyntaxDeclaration:
	
	private GrmNonTerminal gnt;
	public edu.cmu.cs.sasylf.grammar.NonTerminal getSymbol() {
		if (gnt == null)
			gnt = new GrmNonTerminal(typeName.getSymbol());
		return gnt;
	}

}
