package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import edu.cmu.cs.sasylf.CopyData;
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
	
	public void typecheck(Context ctx) {

		System.out.println("typechecking module part");

		Object resolution = module.resolve(ctx);
		if (resolution instanceof Module) {
			ctx.modMap.put(name, (Module)resolution);
		} else {
			ErrorHandler.error(Errors.MODULE_NOT_FOUND, module.toString(), this);
		}
		if (!arguments.isEmpty()) {

			// Since there are arguments, this is a module appliation
			// We already verified above that resolution is an instance of Module
			// Since Module is the only subclass of CompUnit, we know that resolution is an instance of CompUnit
			CompUnit functor = (CompUnit) resolution;

			// check that the number of parameters and arguments is the same

			List<Object> params = new ArrayList<Object>(); // list of module parameters

			for (Part part : functor.getParams()) {
				if (part instanceof SyntaxPart) {
					SyntaxPart sp = (SyntaxPart) part;
					// go through each syntax in the SyntaxPart
					for (Syntax s : sp.getSyntax()) {
						/*
						 * Since s is a module parameter, it must be an instance of SyntaxDeclaration
						 */
						if (s instanceof SyntaxDeclaration) {
							SyntaxDeclaration sd = (SyntaxDeclaration) s;
							params.add(sd);
						}
						else {
							ErrorHandler.error(Errors.INTERNAL_ERROR, " s is not an instance of SyntaxDeclaration", this);
							return;
						}
					}
				}
				else if (part instanceof JudgmentPart) {
					JudgmentPart jp = (JudgmentPart) part;
					// Add each of the judgments to the list of parameters
					for (Judgment j : jp.getJudgments()) {
						params.add(j);
					}
				}
				else if (part instanceof TheoremPart) {
					TheoremPart tp = (TheoremPart) part;
					// Add each of the theorems to the list of parameters
					for (Theorem t : tp.getTheorems()) {
						params.add(t);
					}
				}
				else {
					// The part is not a SyntaxPart, JudgmentPart, or TheoremPart, which is not allowed
					String argumentClass = "";

					if (part instanceof TerminalsPart) {
						argumentClass = "terminals";
					}
					else {
						argumentClass = "module";
					}
					ErrorHandler.modArgInvalid(argumentClass, this);
					return;
				}
			}

			int numParams = params.size();
			
			int numArgs = arguments.size();

			if (numParams != numArgs) {
				ErrorHandler.wrongNumModArgs(numArgs, numParams, this);
				return;
			}

			/* Next, check that the kind of each argument matches the kind of the corresponding parameter
			 * 
			 * Kind refers to: syntax, judgment, or theorem 
			 */

			for (int i = 0; i < numParams; i++) {
				Object parameter = params.get(i);
				QualName argument = arguments.get(i);

				Object argResolution = argument.resolve(ctx);

				// Use instanceof to check that argument and parameter have the same kind

				// make sure that the argument isn't a TerminalPart or CompUnit

				if (argResolution instanceof TerminalsPart
				|| argResolution instanceof CompUnit
				|| argResolution instanceof ModulePart
				) {
					ErrorHandler.modArgInvalid(argResolution, this);
					return;
				}
			
				if (parameter instanceof SyntaxDeclaration && !(argResolution instanceof Syntax)) {
					throwModuleArgumentMismatch(parameter, argResolution);
					return;
				}
				else if (parameter instanceof Judgment && !(argResolution instanceof Judgment)) {
					throwModuleArgumentMismatch(parameter, argResolution);
					return;
				}
				else if (parameter instanceof Theorem && !(argResolution instanceof Theorem)) {
					throwModuleArgumentMismatch(parameter, argResolution);
					return;
				}

				// The argument should not be of type TerminalsPart or ModulePart
				

				if (argResolution instanceof TerminalsPart) {
					throwModuleArgumentMismatch(parameter, argResolution);
					return;
				}
				else if (argResolution instanceof ModulePart
				|| argResolution instanceof CompUnit
				) {
					throwModuleArgumentMismatch(parameter, argResolution);
					return;
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
					/*
					 * This should never happen because we have already checked that the parameter is an instance of SyntaxDeclaration, Judgment, or Theorem
					 * 
					 * If this does happen, raise an internal error
					 */
					ErrorHandler.error(Errors.INTERNAL_ERROR, ". Could not get the name of the parameter in ModulePart.typecheck.", this);
					return;
				}

				SubstitutionData sd;
				Object argResolution = argument.resolve(ctx);

				/*
				 * Match against the kind of the argument
				 */

				if (argResolution instanceof Syntax) {					
					SyntaxDeclaration argumentSyntax = ((Syntax) argResolution).getOriginalDeclaration();
					SyntaxDeclaration parameterSyntax = (SyntaxDeclaration) parameterObject;

					if (argumentSyntax.matchesParam(parameterSyntax, this, paramToArgSyntax, paramToArgJudgment)) {
						sd = new SubstitutionData(parameterName, argumentName, argumentSyntax, parameterSyntax);
					}
					else {
						return;
					}
				}

				else if (argResolution instanceof Judgment) {
					Judgment param = (Judgment) parameterObject;
					Judgment arg = (Judgment) argResolution;

					if (arg.matchesParam(param, this, paramToArgSyntax, paramToArgJudgment)) {
						sd = new SubstitutionData(parameterName, argumentName, arg);
					}
					else {
						return;
					}
					
				}

				else if (argResolution instanceof Theorem) {
					Theorem param = (Theorem) parameterObject;
					Theorem arg = (Theorem) argResolution;

					if (arg.matchesParam(param, this, paramToArgSyntax, paramToArgJudgment)) {
						sd = new SubstitutionData(parameterName, argumentName, arg);
					}
					else {
						return;
					}

				}

				else {
					ErrorHandler.modArgInvalid(argResolution, this);
					return;
				}

				newModule.substitute(sd);

			}
			newModule.moduleName = name;
			ctx.modMap.put(name, newModule);
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

			if (argument.getSource() == null && argument.getName().equals(sd.from) && argument.isSubstitutable()) {
				argument.setName(sd.to);
				argument.nullifyResolution();
				argument.setUnsubstitutable();
			}

		}

	}

	public ModulePart copy(CopyData cd) {
		if (cd.containsCopyFor(this)) return (ModulePart) cd.getCopyFor(this);
		ModulePart clone;

		try {
			clone = (ModulePart) super.clone();
		}
		catch (CloneNotSupportedException e) {
			ErrorReport report = new ErrorReport(Errors.INTERNAL_ERROR, "Clone not supported in class: " + getClass(), this, "", true);
			throw new SASyLFError(report);
		}

		cd.addCopyFor(this, clone);
		
		clone.module = clone.module.copy(cd);
		
		List<QualName> newArguments = new ArrayList<>();

		for (QualName argument : arguments) {
			newArguments.add(argument.copy(cd));
		}
		clone.arguments = newArguments;

		return clone;
	}


}
