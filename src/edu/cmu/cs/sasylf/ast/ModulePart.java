package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import edu.cmu.cs.sasylf.CopyData;
import edu.cmu.cs.sasylf.ModuleComponent;
import edu.cmu.cs.sasylf.SubstitutionData;
import edu.cmu.cs.sasylf.module.Module;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.ErrorReport;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.SASyLFError;

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
	private void throwModuleArgumentMismatch(Object parameter, Object argument) {

		String parameterClass = "";
		String argumentClass = "";

		if (parameter instanceof Syntax) {
			parameterClass = "syntax";
		}
		else if (parameter instanceof Judgment) {
			parameterClass = "judgment";
		}
		else if (parameter instanceof Theorem) {
			parameterClass = "theorem";
		}
		else {
			parameterClass = parameter.getClass().toString();
		}

		if (argument instanceof Syntax) {
			argumentClass = "syntax";
		}
		else if (argument instanceof Judgment) {
			argumentClass = "judgment";
		}
		else if (argument instanceof Theorem) {
			argumentClass = "theorem";
		}
		else if (argument instanceof CompUnit) {
			argumentClass = "module";
		}
		else {
			argumentClass = "a nonexistant declaration";
		}

		ErrorHandler.modArgTypeMismatch(argumentClass, parameterClass, this);
		return;
	}
	
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

			/*
				If
					1. argument.source == null
					2. argument.name == sd.from
					3. argument.isSubstitutable()

				that means that argument is the argument that we want to substitute for
				Therefore, replace set argument.name to sd.to
				Also, set argument.resolution to null because we changed the name, so it points to something else now
			*/

			if (argument.getSource() == null && argument.getName().equals(sd.getFrom()) && argument.isSubstitutable()) {
				argument.setName(sd.getTo());
				argument.nullifyResolution();
				argument.setUnsubstitutable();
			}

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