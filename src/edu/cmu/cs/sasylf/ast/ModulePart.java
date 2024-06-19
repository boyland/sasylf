package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import edu.cmu.cs.sasylf.CloneData;
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
	
	/*
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
	*/
	
	public void typecheck(Context ctx) {
		Object resolution = module.resolve(ctx);
		if (resolution instanceof Module) {
			ctx.modMap.put(name, (Module)resolution);
		} else {
			ErrorHandler.error(Errors.MODULE_NOT_FOUND, module.toString(), this);
		}
		if (!arguments.isEmpty()) {
			// This is a module appliation
			// We already verified above that resolution is an instance of Module
			// Since Module is the only subclass of CompUnit, we know that resolution is an instance of CompUnit
			// TODO: Add more checking here for casting safety
			CompUnit functor = (CompUnit) resolution;

			// check that the number of parameters and arguments is the same

			int numParams = functor.getParams().size();
			int numArgs = arguments.size();

			if (numParams != numArgs) {
				// Output a detailed error message
				System.out.println("Error: Number of parameters and arguments do not match in module application. Expected " + numParams + " arguments, but found " + numArgs + " arguments.");
				System.exit(0);
			}

			// Next, check that the kind of each argument matches the kind of the corresponding parameter

			for (int i = 0; i < numParams; i++) {
				Part parameter = functor.getParams().get(i);
				QualName argument = arguments.get(i);

				// resolve the argument

				Object argResolution = argument.resolve(ctx);

				// Use instanceof to check that argument and parameter match
			
				if (parameter instanceof SyntaxPart && !(argResolution instanceof Syntax)) {
					System.out.println("Error: Argument does not match parameter in module application. Expected a syntax, but found something else.");
				}
				else if (parameter instanceof JudgmentPart && !(argResolution instanceof Judgment)) {
					System.out.println("Error: Argument does not match parameter in module application. Expected a judgment, but found something else.");
				}
				else if (parameter instanceof TheoremPart && !(argResolution instanceof Theorem)) {
					System.out.println("Error: Argument does not match parameter in module application. Expected a theorem, but found something else.");
				}

				// The argument should not be of type TerminalsPart or ModulePart

				if (argResolution instanceof TerminalsPart) {
					System.out.println("Error: Argument does not match parameter in module application. Expected a syntax, judgment, or theorem, but found a terminals part.");
					System.exit(0);
				}
				else if (argResolution instanceof ModulePart) {
					System.out.println("Error: Argument does not match parameter in module application. Expected a syntax, judgment, or theorem, but found a module part.");
					System.exit(0);
				}

			}

			// The arguments and parameters match, so we can evaluate the module application

			CompUnit newModule = functor.clone();

			// Remove all parameters from the new module

			newModule.getParams().clear();

			// Substitute the parameters with the arguments in the parts of newModule

			for (int i = 0; i < numParams; i++) {
				Part parameter = functor.getParams().get(i);
				QualName argument = arguments.get(i);

				// argument.source should be null. In the future, we will adjust this

				if (argument.getSource() != null) {
					System.out.println("Error: Argument source is not null. This is not yet supported.");
					System.exit(0);
				}

				String argumentName = argument.getName();

				// Get the parameter name

				String parameterName = "";

				if (parameter instanceof SyntaxPart) {
					SyntaxPart sp = (SyntaxPart) parameter;
					// get the first syntax, since that is the argument
					Syntax s = sp.getSyntax().get(0);
					// it should be a SyntaxDeclaration
					if (s instanceof SyntaxDeclaration) {
						SyntaxDeclaration sd = (SyntaxDeclaration) s;
						parameterName = sd.getName();
					}
				}
				
				else {
					// we were not able to get the name of the parameter
					System.out.println("Error: Could not get the name of the parameter.");
					System.exit(0);
				}
				
				// substitute the parameter with the argument

				newModule.substitute(parameterName, argumentName);
			
			}

			// add the new module to the context

			ctx.modMap.put(name, newModule);

			System.out.println("\n              New module: \n");
			System.out.println(newModule);
			System.out.println("\n\n");


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

	public ModulePart copy(CloneData cd) {
		if (cd.containsCloneFor(this)) return (ModulePart) cd.getCloneFor(this);
		ModulePart clone;

		try {
			clone = (ModulePart) super.clone();
		}
		catch (CloneNotSupportedException e) {
			System.out.println("Clone not supported in ModulePart");
			System.exit(1);
			return null;
		}

		cd.addCloneFor(this, clone);
		
		clone.module = clone.module.copy(cd);
		
		List<QualName> newArguments = new ArrayList<>();

		for (QualName argument : arguments) {
			newArguments.add(argument.copy(cd));
		}
		clone.arguments = newArguments;

		return clone;
	}


}
