package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
 */
public class Sugar extends Syntax {
	private Clause sugar;
	private NonTerminal typeName;
	private SyntaxDeclaration type;
	private Element replacement;
	private boolean typeChecked;
	
	/**
	 * Create a sugar production where the lhs is replaced with the right.
	 * @param lhs new form to define, must not be null
	 * @param rhs define it as this, must not be null
	 */
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
		typeChecked = false;
		sugar = sugar.typecheck(ctx);
		for (Element e: sugar.getElements()) {
			if (e instanceof Clause) {
				ErrorHandler.error(Errors.CLAUSE_DEF_PAREN, e);
			}
		}
		Map<String, List<ElemType>> bindings = new LinkedHashMap<>();
		for (Element e: sugar.getElements()) {
			if (e instanceof NonTerminal) {
				NonTerminal nt = (NonTerminal)e;
				if (bindings.containsKey(nt.getSymbol())) {
					ErrorHandler.error(Errors.SUGAR_MULTIPLE_USES, ": " + e, sugar);
				}
			} else if (e instanceof Binding) {
				final Binding b = (Binding)e;
				if (bindings.containsKey(b.getNonTerminal().getSymbol())) {
					ErrorHandler.error(Errors.SUGAR_MULTIPLE_USES, ": " + e, sugar);
				}	
				Set<Variable> args = new HashSet<>();
				for (Element sub : b.getElements()) {
					if (sub instanceof Variable) {
						if (!args.add((Variable)sub)) {
							ErrorHandler.error(Errors.SUGAR_MULTIPLE_VARS, e);
						}
					} else {
						ErrorHandler.error(Errors.SUGAR_BINDING_ARG, sub);
					}
				}
			}
			e.checkBindings(bindings, sugar);
		}
		int numLHS = bindings.size();
		Set<String> boundVars = new HashSet<>();
		sugar.checkVariables(boundVars, false);
		if (replacement != null) {
			replacement = replacement.typecheck(ctx);
			Set<NonTerminal> rhsVars = replacement.getFree(false);
			replacement.checkBindings(bindings, sugar);
			boundVars.clear();
			replacement.checkVariables(boundVars, false);
			if (bindings.size() > numLHS) {
				List<String> allVars = new ArrayList<>(bindings.keySet());
				String newName = allVars.get(numLHS);
				ErrorHandler.error(Errors.SUGAR_NO_USES, ": " + newName, this);
			} else if (bindings.size() > rhsVars.size()) {
				for (NonTerminal nt : rhsVars) {
					bindings.remove(nt.getSymbol());
				}
				ErrorHandler.error(Errors.SUGAR_UNUSED, ": " + bindings.keySet(), this);
			}
		} else {
			// NB: replace == null means sugar is abstract
			// Not clear that this feature is needed.
			// The parser never causes replacement to be null
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
		typeChecked = true;
	}

	@Override
	public void postcheck(Context ctx) {
		if (!typeChecked) return; // don't bother
		// here we convert the replacement using the updated grammar,
		// and then update the grammar ourself:
		// sugar definitions cannot be used before they are defined.
		if (replacement != null) {
			if (replacement instanceof Clause) {
				replacement = ((Clause)replacement).computeClause(ctx, false);
			}
			if (replacement instanceof Variable) {
				ErrorHandler.error(Errors.SUGAR_UNKNOWN, this);
			}
			if (!(replacement.getType() instanceof SyntaxDeclaration))
				ErrorHandler.error(Errors.SUGAR_JUDGMENT,this);
			type = (SyntaxDeclaration)replacement.getType();
			typeName = type.getNonTerminal();
		}
		
		ClauseDef cd;
		if (sugar instanceof ClauseDef) cd = (ClauseDef) sugar;
		else if (replacement != null) cd = new SugarClauseDef(sugar, type, replacement);
		else cd = new ClauseDef(sugar, type);
		sugar = cd;
		
		cd.checkVarUse(false);
		
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
	
	@Override
	public void checkSubordination() {
		if (sugar instanceof ClauseDef) { // else, maybe a problem earlier
			((ClauseDef)sugar).checkSubordination();			
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
