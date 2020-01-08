package edu.cmu.cs.sasylf.ast;

import java.util.List;

import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Location;

/**
 * A judgment defined in terms of an existing judgment.
 * This is used to handle importing judgments.
 * Currently rules cannot be imported/renamed, but we permit
 * them to be used with qualification, nat.plus-s, at application
 * and in pattern matching.
 */
public class RenameJudgment extends Judgment {
	private QualName source;
	private Judgment original;
	
	public RenameJudgment(Location loc, String n, QualName qn, List<Rule> l, Clause c,
			NonTerminal a) {
		super(loc, n, l, c, a);
		if (!l.isEmpty()) {
			ErrorHandler.recoverableError("Renaming a judgment should not include renaming of rules", this);
		}
		source = qn;
	}

	@Override
	public boolean isAbstract() {
		if (original != null) return original.isAbstract();
		return super.isAbstract();
	}
	
	@Override
	public void defineConstructor(Context ctx) {
		Object resolution = source.resolve(ctx);
		if (resolution != null) {
			if (resolution instanceof Judgment) {
				original = (Judgment)resolution;
			} else {
				ErrorHandler.report("Renaming of non-judgment!",this);
			}
			getForm().typecheck(ctx);
			ClauseDef cd;
			if (getForm() instanceof ClauseDef) cd = (ClauseDef)getForm();
			else cd = new ClauseDef(getForm(),this,original.getName());
			super.setForm(cd);
		}
		super.defineConstructor(ctx);
	}

	@Override
	public void typecheck(Context ctx) {
		if (original != null) {
			NonTerminal oa = original.getAssume();
			if (oa != null) {
				SyntaxDeclaration o = oa.getType();
				// TODO: get local version of this syntax declaration
				// to use in following statements
				if (super.getAssume() == null) {
					ErrorHandler.recoverableError("Renaming should not have an assumption", this);
				} else {
					super.getAssume().typecheck(ctx);
					SyntaxDeclaration r = super.getAssume().getType();
					if (r != o) {
						ErrorHandler.recoverableError("Renaming uses wrong assumpion name, should use " + oa, this);
					}
				}
			} else if (super.getAssume() != null) {
				ErrorHandler.recoverableError("Renaming should not have an assumption", this);
			}
			getForm().checkClauseMatch(original.getForm());
		}
		// super.typecheck(ctx);
	}

	
	@Override
	public List<Rule> getRules() {
		if (original != null) return original.getRules();
		return super.getRules();
	}

	@Override
	protected Constant computeTypeTerm() {
		if (original != null) return original.typeTerm();
		return super.computeTypeTerm();
	}

	
}
