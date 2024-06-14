package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import edu.cmu.cs.sasylf.module.Module;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;

/**
 * Using a module as a part of a compilation unit.
 */
public class ModulePart extends Node implements Part, Named {
	private String name;
	QualName module;
	List<QualName> arguments;
	
	public ModulePart(Location l, String name, QualName module, List<QualName> arguments, Location endl) {
		super(l,endl);
		this.name = name;
		this.module = module;
		this.arguments = new ArrayList<>(arguments);
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	/**
	 * Return the module being renamed here.
	 * @return the module.
	 */
	public QualName getModule() {
		return module;
	}
	
	@Override
	public void typecheck(Context ctx) {
		Object resolution = module.resolve(ctx);
		if (resolution instanceof Module) {
			ctx.modMap.put(name, (Module)resolution);
		} else {
			ErrorHandler.error(Errors.MODULE_NOT_FOUND, module.toString(), this);
		}
		if (!arguments.isEmpty()) {
			ErrorHandler.error(Errors.MODULE_PARAMETERS, this);
		} else if (((Module)resolution).isAbstract()) {
			ErrorHandler.error(Errors.MODULE_ABSTRACT, this);
		}
	}

	@Override
	public void prettyPrint(PrintWriter out) {
		out.print("module " + name + " = ");
		module.prettyPrint(out);
		if (!arguments.isEmpty()) {
			out.print("[");
			boolean first = true;
			for (QualName arg : arguments) {
				if (first) first = false;
				else out.print(", ");
				arg.prettyPrint(out);
			}
			out.print("]");
		}
		out.println();
	}

	@Override
	public void collectTopLevel(Collection<? super Node> things) {
		things.add(this);
	}

	@Override
	public void collectRuleLike(Map<String, ? super RuleLike> map) {
		// Nothing
	}
	
	@Override
	public void collectQualNames(Consumer<QualName> consumer) {
		module.visit(consumer);
		
		for (QualName name : arguments) {
			name.visit(consumer);
		}
	}

	public void substitute(String from, String to) {
		// Do nothing
		// TODO: I'm pretty sure that nothing should be done here
	}

	public ModulePart clone() {
		/*
			private String name;
			private QualName module;
			private List<QualName> arguments;
		*/

		ModulePart clone = (ModulePart) super.clone();
		
		clone.module = clone.module.clone();
		
		List<QualName> newArguments = new ArrayList<>();

		for (QualName argument : arguments) {
			newArguments.add(argument.clone());
		}
		clone.arguments = newArguments;

		return clone;
	}


}
