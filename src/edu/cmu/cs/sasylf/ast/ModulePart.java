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
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.UpdatableErrorReport;

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
					ErrorHandler.error(Errors.INTERNAL_ERROR, ". Could not get the name of the parameter.", this);
					return;
				}

				SubstitutionData sd;
				Object argResolution = argument.resolve(ctx);

				/*
				 * Match against the kind of the argument
				 */

				if (argResolution instanceof Syntax) {
					SyntaxDeclaration argumentSyntax = ((Syntax) argResolution).getOriginalDeclaration();
					SyntaxDeclaration parameterSyntax = (SyntaxDeclaration) parameterObject; // it will always be an instance of SyntaxDeclaration
					
					if (argumentSyntax instanceof RenameSyntaxDeclaration) {
						RenameSyntaxDeclaration d = (RenameSyntaxDeclaration) argumentSyntax;
						argumentSyntax = d.getOriginalDeclaration();
					}

					// check if parameterSyntax is already bound to argumentSyntax in the map

					if (paramToArgSyntax.containsKey(argumentSyntax)) {
						// check if the parameterSyntax is bound to the same argumentSyntax
						if (paramToArgSyntax.get(argumentSyntax) != parameterSyntax) {
							ErrorHandler.modArgMismatchSyntax(parameterSyntax, argumentSyntax, parameterSyntax, this);
							return;
						}
					}

					// otherwise, bind the parameterSyntax to the argumentSyntax in the map

					paramToArgSyntax.put(parameterSyntax, argumentSyntax);

					if (!parameterSyntax.isAbstract()) {
						// This is a concrete syntax declarations, so there are productions to check
						// check that the parameterSyntax and argumentSyntax have the same number of productions
						List<Clause> parameterProductions = parameterSyntax.getClauses();
						
						SyntaxDeclaration argumentSyntaxDeclaration = (SyntaxDeclaration) argumentSyntax;

						List<Clause> argumentProductions = argumentSyntaxDeclaration.getClauses();

						if (parameterProductions.size() != argumentProductions.size()) {
							ErrorHandler.modArgSyntaxWrongNumProductions(argumentSyntax, parameterSyntax, this);
							return;
						}

						// check that each pair of productions has the same structure

						for (int j = 0; j < parameterProductions.size(); j++) {
							Clause paramClause = parameterProductions.get(j);
							Clause argClause = argumentProductions.get(j);
							Clause.checkClauseSameStructure(paramClause, argClause, paramToArgSyntax, paramToArgJudgment, new HashMap<String, String>(), this);
						}
						
					}

					// We can cast argResolution to Syntax because we have already checked that argResolution is an instance of Syntax

					sd = new SubstitutionData(parameterName, argumentName, argumentSyntax, parameterSyntax);
				}

				else if (argResolution instanceof Judgment) {
					Judgment argumentJudgment = ((Judgment) argResolution).getOriginalDeclaration();
					Judgment parameterJudgment = (Judgment) parameterObject; // it will always be an instance of Judgment

					// check if parameterJudgment is already bound to argumentJudgment in the map

					if (paramToArgJudgment.containsKey(parameterJudgment)) {
						// check if the parameterJudgment is bound to the same argumentJudgment
						if (paramToArgJudgment.get(parameterJudgment) != argumentJudgment) {
							ErrorHandler.modArgumentJudgmentWrongNumRules(argumentJudgment, parameterJudgment, null);
							return;
						}
					}

					// otherwise, bind the parameterJudgment to the argumentJudgment in the map
					
					else {
						paramToArgJudgment.put(parameterJudgment, argumentJudgment);
					}

					// verify that the forms of the judgments have the same structure

					Clause argumentJudgmentForm = argumentJudgment.getForm();
					Clause parameterJudgmentForm = parameterJudgment.getForm();
				
					Clause.checkClauseSameStructure(parameterJudgmentForm, argumentJudgmentForm, paramToArgSyntax, paramToArgJudgment, new HashMap<String, String>(), this);

					if (!parameterJudgment.isAbstract()) {
						// This is a concrete judgment, so there are rules to check
						List<Rule> parameterRules = parameterJudgment.getRules();
						List<Rule> argumentRules = argumentJudgment.getRules();

						if (parameterRules.size() != argumentRules.size()) {
							ErrorHandler.modArgumentJudgmentWrongNumRules(argumentJudgment, parameterJudgment, this);
							return;
						}

						// check that each pair of rules has the same structure

						for (int j = 0; j < parameterRules.size(); j++) {
							Rule paramRule = parameterRules.get(j);
							Rule argRule = argumentRules.get(j);

							// check the premises of the rules
							List<Clause> paramPremises = paramRule.getPremises();
							List<Clause> argPremises = argRule.getPremises();
							if (paramPremises.size() != argPremises.size()) {
								ErrorHandler.modArgRuleWrongNumPremises(argRule, paramRule, null);

								return;
							}

							// check that each pair of premises has the same structure

							Map<String, String> nonTerminalMapping = new HashMap<String, String>();

							for (int k = 0; k < paramPremises.size(); k++) {
								Clause paramPremise = paramPremises.get(k);
								Clause argPremise = argPremises.get(k);
								Clause.checkClauseSameStructure(paramPremise, argPremise, paramToArgSyntax, paramToArgJudgment, nonTerminalMapping, this);
							}

							// check the conclusions

							Clause paramConclusion = paramRule.getConclusion();
							Clause argConclusion = argRule.getConclusion();
							
							Clause.checkClauseSameStructure(paramConclusion, argConclusion, paramToArgSyntax, paramToArgJudgment, nonTerminalMapping, this);

						}

					}
					
					sd = new SubstitutionData(parameterName, argumentName, argumentJudgment, parameterJudgment);
				}

				else if (argResolution instanceof Theorem) {
					Theorem argumentTheorem = (Theorem) argResolution;
					Theorem parameterTheorem = (Theorem) parameterObject;

					// make sure that the forall clauses and the exists clauses match

					List<Fact> argumentForalls = argumentTheorem.getForalls();

					List<Fact> parameterForalls = parameterTheorem.getForalls();

					if (argumentForalls.size() != parameterForalls.size()) {
						ErrorHandler.modArgTheoremWrongNumForalls(argumentTheorem, parameterTheorem, this);
						return;
					}

					// check that each pair of foralls has the same structure

					for (int j = 0; j < parameterForalls.size(); j++) {
						Fact paramForall = parameterForalls.get(j);
						Fact argForall = argumentForalls.get(j);
						Element paramElement = paramForall.getElement();
						Element argElement = argForall.getElement();
						// paramElement and argElement should either both be nonterminals or both be clauses

						Map<String, String> nonTerminalMapping = new HashMap<String, String>();

						if (paramElement instanceof Clause && argElement instanceof Clause) {
							Clause paramClause = (Clause) paramElement;
							Clause argClause = (Clause) argElement;
							// check that they have the same structure
							Clause.checkClauseSameStructure(paramClause, argClause, paramToArgSyntax, paramToArgJudgment, nonTerminalMapping, this);
						}

						else if (paramElement instanceof NonTerminal && argElement instanceof NonTerminal) {
							NonTerminal paramNonTerminal = (NonTerminal) paramElement;
							NonTerminal argNonTerminal = (NonTerminal) argElement;
							// Make sure that the types of the nonterminals match
							if (paramToArgSyntax.containsKey(paramNonTerminal.getType())) {
								if (paramToArgSyntax.get(paramNonTerminal.getType()) != argNonTerminal.getType()) {
									ErrorHandler.modArgClauseNonterminalTypeMismatch(argNonTerminal, paramNonTerminal, paramToArgSyntax, this);
									return;
								}
							}
						}

						// check the the exists match

						Clause paramExists = (Clause) parameterTheorem.getExists();
						Clause argExists = (Clause) argumentTheorem.getExists();

						Clause.checkClauseSameStructure(paramExists, argExists, paramToArgSyntax, paramToArgJudgment, nonTerminalMapping, this);

					}

					sd = new SubstitutionData(parameterName, argumentName, (Theorem) argResolution);
				}

				else {
					ErrorHandler.modArgInvalid(argResolution, this);
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
			UpdatableErrorReport report = new UpdatableErrorReport(Errors.INTERNAL_ERROR, "Clone not supported in class: " + getClass(), this);
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
