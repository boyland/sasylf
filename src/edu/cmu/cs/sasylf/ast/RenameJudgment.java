package edu.cmu.cs.sasylf.ast;

import java.util.List;
import java.util.function.Consumer;

import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
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
			ErrorHandler.recoverableError(Errors.RENAME_NO_RULES, this);
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
		Object resolution = source.resolveNotPackage(ctx);
		if (resolution != null) {
			if (resolution instanceof Judgment) {
				original = (Judgment)resolution;
			} else {
				ErrorHandler.recoverableError(Errors.RENAME_JUDGMENT, QualName.classify(resolution) + " " + source, this);
				return;
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
					ErrorHandler.recoverableError(Errors.RENAME_ASSUME_MISMATCH, this);
				} else {
					super.getAssume().typecheck(ctx);
					SyntaxDeclaration r = super.getAssume().getType();
					if (!r.equals(o)) {
						ErrorHandler.recoverableError(Errors.RENAME_ASSUME_MISMATCH, this);
					}
				}
			} else if (super.getAssume() != null) {
				ErrorHandler.recoverableError(Errors.RENAME_ASSUME_MISMATCH, this);
			}
			getForm().checkClauseMatch(original.getForm());
		}
		// super.typecheck(ctx);
	}

	
	@Override
	public CanBeCase findRule(Constant c) {
		if (original != null) return original.findRule(c);
		return super.findRule(c);
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

	@Override
	public void collectQualNames(Consumer<QualName> consumer) {
		source.visit(consumer);
	}
}
