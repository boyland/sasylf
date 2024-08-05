package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import edu.cmu.cs.sasylf.SubstitutionData;
import edu.cmu.cs.sasylf.util.CopyData;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;

/**
 * Using a module as a part of a compilation unit.
 */
public class ModulePart extends Node implements Part, Named {
	private String name;
	private QualName module;
	private List<QualName> arguments;
	
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

	/**
	 * Throws an error, specifying that the module argument type does not match the module parameter type.
	 * @param parameter the module parameter
	 * @param argument the module argument
	 */
	
	@Override
	public void typecheck(Context ctx) {
		
		// resolve the module
		Object resolution = module.resolve(ctx);

		if (!(resolution instanceof CompUnit)) {
			ErrorHandler.error(Errors.MODULE_NOT_FOUND, module.toString(), this);
			return;
		}

		CompUnit functor = (CompUnit) resolution;

		// resolve each of the arguments

		List<ModuleComponent> arguments = new ArrayList<>();

		for (QualName qn : this.arguments) {
			Object argResolution = qn.resolve(ctx);
			if (argResolution instanceof ModuleComponent) {
				arguments.add((ModuleComponent)argResolution);
			}
			else {
				ErrorHandler.modArgInvalid(argResolution, this);
				return;
			}
		}

		functor.accept(arguments, this, ctx, name)
		.ifPresent(newModule -> {
			// If the module application succeeds, add the new module to the context
			ctx.modMap.put(name, newModule);
		});

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

	public void substitute(SubstitutionData sd) {
		if (sd.didSubstituteFor(this)) return;
		sd.setSubstitutedFor(this);

		for (QualName argument : arguments) {			
			argument.substitute(sd);
		}

	}

	public ModulePart copy(CopyData cd) {
		if (cd.containsCopyFor(this)) return (ModulePart) cd.getCopyFor(this);
		ModulePart clone = (ModulePart) super.clone();

		cd.addCopyFor(this, clone);
		
		clone.module = clone.module.copy(cd);
		
		List<QualName> newArguments = new ArrayList<>();

		for (QualName argument : arguments) {
			newArguments.add(argument.copy(cd));
		}
		clone.arguments = newArguments;

		return clone;
	}

	@Override
	public List<ModuleComponent> argsParams() {
		return new ArrayList<>();
	}

	@Override
	public void collectTopLevelAsModuleComponents(Collection<ModuleComponent> things) {
		// do nothing
	}


}