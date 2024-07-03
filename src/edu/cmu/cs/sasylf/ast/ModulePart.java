package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import edu.cmu.cs.sasylf.CloneData;
import edu.cmu.cs.sasylf.SubstitutionData;
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
	public void  (Context ctx) {
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

			// get the parameters of the functor
			// for this, we have go through each of the parts

			List<Object> params = new ArrayList<Object>();

			for (Part part : functor.getParams()) {
				if (part instanceof SyntaxPart) {
					SyntaxPart sp = (SyntaxPart) part;
					// go through each syntax
					for (Syntax s : sp.getSyntax()) {
						if (s instanceof SyntaxDeclaration) {
							SyntaxDeclaration sd = (SyntaxDeclaration) s;
							params.add(sd);
						}
						else {
							// The syntax is not a SyntaxDeclaration, which is not allowed
							System.out.println("Error: Syntax is not a SyntaxDeclaration in ModulePart.");
							System.exit(1);
						}
					}
				}
				else if (part instanceof JudgmentPart) {
					JudgmentPart jp = (JudgmentPart) part;
					// go through each judgment
					for (Judgment j : jp.getJudgments()) {
						params.add(j);
					}
				}
				else if (part instanceof TheoremPart) {
					TheoremPart tp = (TheoremPart) part;
					// go through each theorem
					for (Theorem t : tp.getTheorems()) {
						params.add(t);
					}
				}
				else {
					// The part is not a SyntaxPart, JudgmentPart, or TheoremPart, which is not allowed
					System.out.println("Error: Part is not a SyntaxPart, JudgmentPart, or TheoremPart in ModulePart.");
					System.exit(1);
				}
			}

			int numParams = params.size();
			
			int numArgs = arguments.size();

			if (numParams != numArgs) {
				// Output a detailed error message
				System.out.println("Error: Number of parameters and arguments do not match in module application. Expected " + numParams + " arguments, but found " + numArgs + " arguments.");
				System.exit(0);
			}

			// Next, check that the kind of each argument matches the kind of the corresponding parameter

			for (int i = 0; i < numParams; i++) {
				Object parameter = params.get(i);
				QualName argument = arguments.get(i);

				// resolve the argument

				Object argResolution = argument.resolve(ctx);

				// Use instanceof to check that argument and parameter match
			
				if (parameter instanceof SyntaxDeclaration && !(argResolution instanceof Syntax)) {
					System.out.println("Error: Argument does not match parameter in module application. Expected a syntax, but found something else.");
				}
				else if (parameter instanceof Judgment && !(argResolution instanceof Judgment)) {
					System.out.println("Error: Argument does not match parameter in module application. Expected a judgment, but found something else.");
				}
				else if (parameter instanceof Theorem && !(argResolution instanceof Theorem)) {
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


			Map<Syntax, Syntax> paramToArgSyntax = new IdentityHashMap<Syntax, Syntax>();
			Map<Judgment, Judgment> paramToArgJudgment = new IdentityHashMap<Judgment, Judgment>();

			// paramToArgSyntax maps a parameter syntax to the argument syntax that is provided in the functor application
			// paramToArgJudgment maps a parameter judgment to the argument judgment that is provided in the functor application

			// At this point, we know that each argument has the same kind as the corresponding parameter

			for (int i = 0; i < numParams; i++) {
				Object parameterObject = params.get(i);
				QualName argument = arguments.get(i);

				// argument.source should be null. In the future, we will adjust this

				if (argument.getSource() != null) {
					System.out.println("Error: Argument source is not null. This is not yet supported.");
					System.exit(0);
				}

				String argumentName = argument.getName();

				String parameterName = "";

				if (parameterObject instanceof SyntaxDeclaration) {
					SyntaxDeclaration sd = (SyntaxDeclaration) parameterObject;
					parameterName = sd.getName();
				}
				
				else if (parameterObject instanceof Judgment) {
					Judgment j = (Judgment) parameterObject;
					parameterName = j.getName();
				}

				else if (parameterObject instanceof Theorem) {
					Theorem t = (Theorem) parameterObject;
					parameterName = t.getName();
				}
				
				else {
					// we were not able to get the name of the parameter
					System.out.println("Error: Could not get the name of the parameter.");
					System.exit(0);
				}
				SubstitutionData sd;
				Object argResolution = argument.resolve(ctx);

				if (argResolution instanceof Syntax) {
					Syntax argumentSyntax = (Syntax) argResolution;
					Syntax parameterSyntax = (Syntax) parameterObject; // it will always be an instance of Syntax

					// check if parameterSyntax is already bound to argumentSyntax in the map

					if (paramToArgSyntax.containsKey(argumentSyntax)) {
						// check if the parameterSyntax is bound to the same argumentSyntax
						if (paramToArgSyntax.get(argumentSyntax) != parameterSyntax) {
							System.out.println("Error: The same argument syntax is bound to two different parameter syntaxes.");
							System.exit(0);
						}
					}

					// otherwise, bind the parameterSyntax to the argumentSyntax in the map

					paramToArgSyntax.put(parameterSyntax, argumentSyntax);

					// TODO: if parameterSyntax is not abstract, need to check the productions of the syntaxes

					sd = new SubstitutionData(parameterName, argumentName, (Syntax) argResolution);
				}

				else if (argResolution instanceof Judgment) {

					
					Judgment argumentJudgment = (Judgment) argResolution;
					Judgment parameterJudgment = (Judgment) parameterObject; // it will always be an instance of Judgment

					// check if parameterJudgment is already bound to argumentJudgment in the map

					if (paramToArgJudgment.containsKey(parameterJudgment)) {
						// check if the parameterJudgment is bound to the same argumentJudgment
						if (paramToArgJudgment.get(parameterJudgment) != argumentJudgment) {
							System.out.println("Error: The same argument judgment is bound to two different parameter judgments.");
							System.exit(0);
						}
					}

					// otherwise, bind the parameterJudgment to the argumentJudgment in the map

					// now, we need to check the forms of the judgments and make sure that they match

					Clause argumentJudgmentForm = argumentJudgment.getForm();
					Clause parameterJudgmentForm = parameterJudgment.getForm();
					
					// check if the forms of the judgments have the same structure
					
					Clause.checkClauseSameStructure(parameterJudgmentForm, argumentJudgmentForm, paramToArgSyntax, paramToArgJudgment);

					// TODO: if parameterJudgment is not abstract, need to check the rules of the judgments
					
					sd = new SubstitutionData(parameterName, argumentName, (Judgment) argResolution);
				}

				else if (argResolution instanceof Theorem) {
					// Theorem is not yet implemented
					System.out.println("Error: Theorem is not yet implemented in module application.");
					System.exit(0);
					sd = new SubstitutionData(parameterName, argumentName, (Theorem) argResolution);
				}

				else {
					System.out.println("Error: Argument is not an instance of Syntax, Judgment, or Theorem.");
					System.exit(1);
					return;
				}

				newModule.substitute(sd);
			}
			newModule.moduleName = name;
			ctx.modMap.put(name, newModule); 
			Context.updateVersion();

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

	public void substitute(SubstitutionData sd) {
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
