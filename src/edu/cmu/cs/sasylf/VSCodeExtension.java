package edu.cmu.cs.sasylf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.cmu.cs.sasylf.ast.Clause;
import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.ast.Context;
import edu.cmu.cs.sasylf.ast.Element;
import edu.cmu.cs.sasylf.ast.Fact;
import edu.cmu.cs.sasylf.ast.Judgment;
import edu.cmu.cs.sasylf.ast.Node;
import edu.cmu.cs.sasylf.ast.Rule;
import edu.cmu.cs.sasylf.ast.RuleLike;
import edu.cmu.cs.sasylf.ast.TermPrinter;
import edu.cmu.cs.sasylf.ast.Theorem;
import edu.cmu.cs.sasylf.module.PathModuleFinder;
import edu.cmu.cs.sasylf.module.ResourceModuleFinder;
import edu.cmu.cs.sasylf.parser.DSLToolkitParser;
import edu.cmu.cs.sasylf.parser.ParseException;
import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Facade;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.Location;

public class VSCodeExtension {
	public static void main(String[] args) throws ParseException, IOException {
        PrintStream out = new PrintStream(System.out, true, "UTF-8");
        PrintStream err = new PrintStream(System.err, true, "UTF-8");

		if (!edu.cmu.cs.sasylf.util.Util.DEBUG) {
			System.setOut(new PrintStream(OutputStream.nullOutputStream()));
			System.setErr(new PrintStream(OutputStream.nullOutputStream()));
		}

		Clause clause = null, sclause = null, newVar = null;
		String oldVar = null, rule = null, filename = null;
		List<Clause> premise = null;
		final ObjectMapper objectMapper = new ObjectMapper();
		final ObjectNode newClause = objectMapper.createObjectNode();
		final ObjectNode premises = objectMapper.createObjectNode();
		final ObjectNode conclusions = objectMapper.createObjectNode();
		final PathModuleFinder defaultMF = new PathModuleFinder("");
		int exitCode = 0;

		for (int i = 0; i < args.length; ++i) {
			if (args[i].startsWith("--parse=")) {
				String str = args[i].substring(8);
				StringReader reader = new StringReader(str);
				DSLToolkitParser parser = new DSLToolkitParser(reader);
				clause = parser.ExprToNL();
				continue;
			}
			if (args[i].startsWith("--substitute=")) {
				String str = args[i].substring(13);
				StringReader reader = new StringReader(str);
				DSLToolkitParser parser = new DSLToolkitParser(reader);
				sclause = parser.ExprToNL();
				continue;
			}
			if (args[i].startsWith("--old=")) {
				oldVar = args[i].substring(6);
				continue;
			}
			if (args[i].startsWith("--new=")) {
				String str = args[i].substring(6);
				StringReader reader = new StringReader(str);
				DSLToolkitParser parser = new DSLToolkitParser(reader);
				newVar = parser.ExprToNL();
				continue;
			}
			if (args[i].startsWith("--premises=")) {
				try {
					Map<String, List<String>> map =
							objectMapper.readValue(args[i].substring(11), Map.class);
					premise = new ArrayList<>();
					for (String s : map.get("premises")) {
						StringReader reader = new StringReader(s);
						DSLToolkitParser parser = new DSLToolkitParser(reader);
						premise.add(parser.ExprToNL());
					}
				} catch (Exception e) {
				}
				continue;
			}
			if (args[i].startsWith("--rule=")) {
				rule = args[i].substring(7);
				continue;
			}
			filename = args[i];
		}

		File file = new File(filename);
		Proof pf = null;

		if (!file.canRead()) {
			System.err.println("Could not open file " + filename);
			exitCode = -1;
		}

		try {
			InputStreamReader r = new InputStreamReader(new FileInputStream(file));
			pf = Proof.parseAndCheck(defaultMF, filename, null, r);
		} catch (FileNotFoundException ex) {
			System.err.println("Could not open file " + filename);
			exitCode = -1;
            System.exit(exitCode);
		}

		if (pf.getErrorCount() != 0) {
            System.setOut(out);
			System.out.println(pf.getErrorReports());
			exitCode = -1;
		} else {
			CompUnit syntaxTree = pf.getCompilationUnit();
			Context ctx = new Context(new ResourceModuleFinder(), syntaxTree);
			// the substitution to get fresh variables
			Substitution freshSub = new Substitution();

			// the substitution to handle possible dependencies on the context
			// (this cannot be used until we have removed bindings of variable free
			// NTs)
			Substitution adaptSub = new Substitution();

			// the variable-free NTs that should not be adapted:
			Set<FreeVar> varFree = new HashSet<FreeVar>();

			// after adaptation, the free variables in the premises
			Set<FreeVar> freeVars = new HashSet<FreeVar>();

			List<Term> addedTypes = new ArrayList<Term>();

			if (clause != null && rule != null) {
				try {
					RuleLike ruleLike = VSCodeExtension.findRule(rule, syntaxTree);
					syntaxTree.typecheck(ctx, null);
					clause.typecheck(ctx);
					Element e = clause.computeClause(ctx);
					List<Fact> inputs = new ArrayList<Fact>();
					List<Abstraction> addedContext = new ArrayList<Abstraction>();

					for (Element prem : ruleLike.getPremises())
						inputs.add(prem.asFact(ctx, ctx.assumedContext));

					Term subject = ruleLike.checkApplication(
							ctx, inputs, e.asFact(ctx, ctx.assumedContext), addedContext,
							null, false, addedTypes, freshSub, adaptSub, varFree, true,
							false);

					Set<FreeVar> conclusionFreeVars = new HashSet<FreeVar>();
					Term pattern = ruleLike.getFreshAdaptedRuleTerm(addedContext, conclusionFreeVars);
					Substitution callSub = pattern.unify(subject);
					Set<FreeVar> vars = e.asTerm().getFreeVariables();
					callSub.avoid(vars);
					Term actual = subject.substitute(callSub);

					TermPrinter tp =
							new TermPrinter(ctx, null, ruleLike.getLocation(), false);
					if (ctx.inputVars == null) ctx.inputVars = new HashSet<FreeVar>();
					if (ctx.outputVars == null) ctx.outputVars = new HashSet<FreeVar>();
					if (ctx.recursiveTheorems == null)
						ctx.recursiveTheorems = new HashMap<String, Theorem>();

					ArrayNode premiseNode = objectMapper.createArrayNode();
					premises.put("arguments", premiseNode);
					for (Term arg : ((Application)actual).getArguments())
						premiseNode.add(tp.toString(tp.asClause(arg)));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (premise != null && rule != null) {
				try {
					RuleLike ruleLike = VSCodeExtension.findRule(rule, syntaxTree);
					syntaxTree.typecheck(ctx, null);
					List<Fact> inputs = new ArrayList<Fact>();
					List<Abstraction> addedContext = new ArrayList<Abstraction>();

					Element conclusion = ruleLike.getConclusion();

					Set<FreeVar> vars = new HashSet<FreeVar>();

					for (Clause p : premise)
						p.typecheck(ctx);

					for (Clause p : premise) {
						Element e = p.computeClause(ctx);
						vars.addAll(e.asTerm().getFreeVariables());
						inputs.add(e.asFact(ctx, ctx.assumedContext));
					}

					Term subject = ruleLike.checkApplication(
							ctx, inputs, conclusion.asFact(ctx, ctx.assumedContext),
							addedContext, null, true, addedTypes, freshSub, adaptSub, varFree,
							false, true);

					Set<FreeVar> conclusionFreeVars = new HashSet<FreeVar>();
					Term pattern = ruleLike.getFreshAdaptedRuleTerm(addedContext, conclusionFreeVars);
					Substitution callSub = subject.unify(pattern);
					callSub.avoid(vars);
					Term actual = subject.substitute(callSub);

					TermPrinter tp =
							new TermPrinter(ctx, null, ruleLike.getLocation(), false);
					if (ctx.inputVars == null) ctx.inputVars = new HashSet<FreeVar>();
					if (ctx.outputVars == null) ctx.outputVars = new HashSet<FreeVar>();
					if (ctx.recursiveTheorems == null)
						ctx.recursiveTheorems = new HashMap<String, Theorem>();

					ArrayNode conclusionNode = objectMapper.createArrayNode();
					conclusions.put("conclusion", conclusionNode);
					conclusionNode.add(tp.toString(tp.asClause(
							((Application)actual)
									.getArguments()
									.get(((Application)actual).getArguments().size() - 1))));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (sclause != null) {
				try {
					syntaxTree.typecheck(ctx, null);
					sclause.typecheck(ctx);

					Element e = sclause.computeClause(ctx);
					FreeVar oldTerm = null;

					for (FreeVar var : e.asTerm().getFreeVariables())
						if (var.getName().equals(oldVar)) {
							oldTerm = var;
							break;
						}

					if (oldTerm != null) {
						Clause c = newVar.typecheck(ctx);
						Element ce = c.computeClause(ctx);

						Substitution sub = new Substitution(
								Facade.FVar(newVar.toString(), ce.asTerm().getType()), oldTerm);
						Term t = e.asTerm().substitute(sub);
						TermPrinter tp =
								new TermPrinter(ctx, null, new Location(filename, 0, 0), false);

						if (ctx.inputVars == null) ctx.inputVars = new HashSet<FreeVar>();
						if (ctx.outputVars == null) ctx.outputVars = new HashSet<FreeVar>();
						if (ctx.recursiveTheorems == null)
							ctx.recursiveTheorems = new HashMap<String, Theorem>();

						newClause.put("result", tp.toString(t, true));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

            System.setOut(out);
            System.setErr(err);

			if (clause != null && rule != null)
                System.out.println(premises.toString());
			if (premise != null && rule != null)
                System.out.println(conclusions.toString());
            if (sclause != null)
                System.out.println(newClause.toString());
		}

		System.exit(exitCode);
	}

	private static RuleLike findRule(String n, CompUnit syntaxTree) {
		List<Node> pieces = new ArrayList<>();
		syntaxTree.collectTopLevel(pieces);

		for (Node piece : pieces) {
			if (piece instanceof Theorem && ((Theorem)piece).getName().equals(n))
				return (RuleLike)piece;
			else if (piece instanceof Judgment) {
				Judgment judgment = (Judgment)piece;

				for (Rule rule : judgment.getRules())
					if (rule.getName().equals(n))
                        return (RuleLike)rule;
			}
		}

		return null;
	}
}
