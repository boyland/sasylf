package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.util.CopyData;

/**
 * A syntax assumption with a context, and maybe bindings
 * e.g.  t[x] assumes Gamma, x:T
 * or    t[x]     (as part of a syntax case pattern such as "fn x => t[x]")
 */
public class BindingAssumption extends NonTerminalAssumption {
	private Binding binding;
	
	public BindingAssumption(Binding b, Element assumes) {
		super(b.getNonTerminal(), b.getLocation(), assumes);
		binding = b;
	}

	public BindingAssumption(Binding b) {
		this(b,null);
	}

	@Override
	public Element getElementBase() {
		return binding;
	}

	@Override
	public void typecheck(Context ctx) {
		binding.typecheck(ctx);
		super.typecheck(ctx);
		/*
		if (ctx == null) { // XXX: Not sure if this should always happen.  Originally only if being added to map.
			for (Element e : binding.getElements()) {
				if (!(e instanceof Variable)) {
					if (e != null) throw new IllegalStateException("I don't know what is happening");
					ErrorHandler.error("cannot match a substituted term", this);
				}
			}
		}
		super.typecheck(ctx);*/
	}

	public void substitute(SubstitutionData sd) {
		if (sd.didSubstituteFor(this)) return;
		super.substitute(sd);
		sd.setSubstitutedFor(this);
		binding.substitute(sd);
	}

	public BindingAssumption copy(CopyData cd) {
		if (cd.containsCopyFor(this)) return (BindingAssumption) cd.getCopyFor(this);

		BindingAssumption clone = (BindingAssumption) super.copy(cd);

		cd.addCopyFor(this, clone);

		clone.binding = clone.binding.copy(cd);
		
		return clone;
	}
}
