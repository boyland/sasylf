package edu.cmu.cs.sasylf.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.sasylf.term.Abstraction;
import edu.cmu.cs.sasylf.term.Application;
import edu.cmu.cs.sasylf.term.Atom;
import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.Substitution;
import edu.cmu.cs.sasylf.term.Term;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Pair;
import edu.cmu.cs.sasylf.util.SingletonList;
import edu.cmu.cs.sasylf.util.Util;

/**
 * A generated judgment that builds in a layer of context around an existing judgment.
 * For example, support the judgment is a typing judgment Gamma |- t : T
 * and the context clause is Gamma, x : T, then we are building a judgment
 * with form Gamma, x : T |- t[x] : T.  It has one rule which uses the existing judgment
 * in the premise with an expanded context.  We don't add this to the parser since
 * it would cause parsing ambiguity.  Judgments of this form are created when we want to conjoin
 * two clauses with different contexts.
 * <p>
 * If the context clause is the empty context, then the generated judgment doesn't have
 * any context at all, which is convenient for combining with other clauses with and/or.
 */
public class ContextJudgment extends Judgment {

	private final Judgment base;
	private final ClauseDef context;
	
	private ContextJudgment(Location loc, Judgment b, ClauseUse use, ClauseDef assume, ClauseUse assumeUse) {
		super(loc,makeName(b,assume),new ArrayList<>(),makeForm(loc,b,use,assume,assumeUse),assume.getAssumeIndex() >= 0 ? b.getAssume() : null);
		base = b;
		context = assume;
		NonTerminal root = b.getAssume();
		ElementGenerator gen = new ElementGenerator();
		List<Element> premElems = new ArrayList<>();
		List<Element> concElems = new ArrayList<>();
		List<Element> contextElems = new ArrayList<>();
		Iterator<Element> useIt = assumeUse.elements.iterator();
		for (Element e : assume.getElements()) {
			Element u = useIt.next();
			if (e.equals(root)) {
				contextElems.add(gen.makeCopy(e, u, false));
			} else {
				contextElems.add(gen.makeCopy(e, u, true)); 
			}
		}
		gen.useVariableDependencies();
		useIt = use.elements.iterator();
		for (Element e : b.getForm().getElements()) {
			Element u = useIt.next();
			if (e.equals(root)) {
				concElems.addAll(contextElems);
				premElems.add(new ClauseUse(u.getLocation(),new ArrayList<>(contextElems),context));
			} else {
				Element copy = gen.makeCopy(e, u, true);
				concElems.add(copy);
				premElems.add(copy);
			}
		}
		ClauseDef cd = new ClauseDef(super.getForm(), this, typeTerm().getName());
		super.setForm(cd);
		ClauseUse premise = new ClauseUse(loc,premElems,(ClauseDef)b.getForm());
		ClauseUse conclusion = new ClauseUse(loc,concElems,cd);
		super.getRules().add(new Rule(loc,super.getName(),new SingletonList<>(premise),conclusion));
	}
	
	private static String makeName(Judgment base, ClauseDef context) {
		return base.getName() + "+" + context.getConstructorName();
	}
	
	private static Clause makeForm(Location l, Judgment base, ClauseUse use, ClauseDef assume, ClauseUse assumeUse) {
		Clause result = new Clause(l);
		List<Element> elems = result.getElements();
		ElementGenerator gen = new ElementGenerator();
		List<Element> assumeElems = new ArrayList<>();
		Iterator<Element> useIt = assumeUse.getElements().iterator();
		for (Element e : assume.getElements()) {
			Element u = useIt.next();
			assumeElems.add(gen.makeCopy(e, u, false));
		}
		gen.useVariableDependencies();
		useIt = use.getElements().iterator();
		for (Element e : base.getForm().getElements()) {
			Element u = useIt.next();
			if (e.equals(base.getAssume())) {
				elems.addAll(assumeElems);
			} else {
				elems.add(gen.makeCopy(e, u, false));
			}
		}
		return result;
	}
	
	@Override
	public void defineConstructor(Context ctx) {
		this.getForm().typecheck(ctx);
		ctx.setProduction(typeTerm().getName(), (ClauseDef)getForm());
		ctx.setJudgment(getName(), this);
	}

	public Judgment getBase() { return base; }
	
	public ClauseDef getContext() { return context; }
	
	protected static class NoCommonPrefixException extends Exception {
		/**
		 * KEH
		 */
		private static final long serialVersionUID = 1L;

		NoCommonPrefixException(Element e1, Element e2) {
			super("No common prefix for " + e1 + " and " + e2);
		}
	}

	/**
	 * Compute the common prefix of two clauses that represent context.
	 * If they do not, an exception is thrown
	 * @param e1 context element, must not be null
	 * @param e2 context element, must not be null
	 * @return common prefix
	 * @throws AndOrClauseUse.NoCommonPrefixException  if the elements have no common prefix
	 */
	protected static Element getCommonPrefix(Element e1, Element e2) throws NoCommonPrefixException {
		List<Element> l1 = new ArrayList<>();
		List<Element> l2 = new ArrayList<>();
		addPrefixes(e1,l1);
		addPrefixes(e2,l2);
		int n = l1.size();
		if (l2.size() < n) n = l2.size();
		int last = -1;
		for (int i=0; i < n; ++i) {
			if (!l1.get(i).equals(l2.get(i))) break;
			last = i;
		}
		if (last >= 0) return l1.get(last);
		throw new NoCommonPrefixException(e1,e2);
	}
	
	protected static void addPrefixes(Element e, List<Element> list) {
		if (e instanceof ClauseUse) {
			ClauseUse cu = (ClauseUse)e;
			final int ai = cu.getConstructor().getAssumeIndex();
			if (ai >= 0) addPrefixes(cu.getElements().get(ai), list);
		}
		list.add(e);
	}


	private static Map<Pair<Judgment,ClauseDef>,ContextJudgment> cache = new HashMap<>();
	
	public static ContextJudgment create(Location loc, Context ctx, Judgment base, ClauseUse use, ClauseDef context, ClauseUse contextUse) {
		ContextJudgment result = cache.get(Pair.create(base,context));
		if (result != null) return result;
		result = new ContextJudgment(loc, base, use, context, contextUse);
		result.defineConstructor(ctx);
		result.typecheck(ctx);
		cache.put(Pair.create(base, context),result);
		return result;
	}
	
	/**
	 * Wrap the base as ContextJudgments as necessary to handle all
	 * the contexts past the common prefix.  If the context is the same
	 * as the fix, this routine does nothing.  If the context does <em>not</em>
	 * contain the prefix in its assume location, this method will crash.
	 * @param loc location to place everything
	 * @param ctx context for type checking
	 * @param base judgment to wrap
	 * @param prefix stop when reaching this common prefix
	 * @param context assumption to generate context judgments for
	 * @return judgment that takes into account all the context beyond the prefix
	 */
	public static Judgment create(Location loc, Context ctx, Judgment base, ClauseUse use, Element prefix, Element context) {
		Judgment result = base;
		while (context != prefix && !context.equals(prefix)) {
			ClauseUse cu = (ClauseUse)context;
			ClauseDef form = cu.getConstructor();
			result = create(loc, ctx, result, use, form, cu);
			context = cu.getAssumes();
		}
		return result;
	}
	
	/** Convert a clause to use a context judgment for context beyond the shared prefix
	 * @param current current form of the clause
	 * @param prefix context to reduce to
	 * @return clause using the judgment that incorporates anything between this context and the prefix.
	 * It will be the same result if the context clause is the same as the prefix or if
	 * this clause doesn't have a context.
	 */
	public static ClauseUse convert(ClauseUse current, Element prefix) {
		ClauseUse result = current;
		Judgment judg = (Judgment)current.getConstructor().getType();
		Element context = current.getAssumes();
		if (context == null) return current;
		while (context != prefix && !context.equals(prefix)) {
			ClauseUse cu = (ClauseUse)context;
			ClauseDef form = cu.getConstructor();
			ContextJudgment newJudg = cache.get(Pair.create(judg, form));
			Util.verify(newJudg != null, "Didn't create " + judg.getName() + "+" + form.getName());
			List<Element> newParts = new ArrayList<>();
			for (Element e : result.getElements()) { // inline one level
				if (e.equals(cu)) {
					newParts.addAll(cu.getElements());
				} else {
					newParts.add(e);
				}
			}
			result = new ClauseUse(cu.getLocation(),newParts,(ClauseDef)newJudg.getForm());
			context = cu.getAssumes();
			judg = newJudg;
		}
		result.setEndLocation(current.getEndLocation());
		return result;
	}
	
	public static ClauseUse unwrap(ClauseUse current) {
		ClauseUse result = current;
		Judgment judg = (Judgment)current.getConstructor().getType();
		// if (judg.getAssume() == null) return current;
		while (judg instanceof ContextJudgment) {
			Judgment newJudg = ((ContextJudgment)judg).getBase();
			ClauseDef context = ((ContextJudgment)judg).getContext();
			Clause newForm = newJudg.getForm();
			NonTerminal contextNT = newJudg.getAssume();
			List<Element> guides = newForm.getElements();
			List<Element> oldParts = result.getElements();
			List<Element> newParts = new ArrayList<>();
			for (int in=0, out=0; in < oldParts.size(); ++in, ++out) {
				final Element guide = guides.get(out);
				final Element next = oldParts.get(in);
				if (guide.equals(contextNT)) {
					final int contextSize = context.getElements().size();
					List<Element> newContextParts = new ArrayList<>(oldParts.subList(in, in+contextSize));
					ClauseUse newContext = new ClauseUse(next.getLocation(),newContextParts,context);
					newParts.add(newContext);
					in += contextSize - 1;
				} else {
					newParts.add(next);
				}
			}
			result = new ClauseUse(result.getLocation(),newParts,(ClauseDef)newJudg.getForm());
			judg = newJudg;
		}
		return result;
	}
	
	/**
	 * Invert a use of any context judgments used to create this term.
	 * @param input any term (must not be null)
	 * @return inverted term not formed with a context judgment
	 */
	public static Term invertContextJudgments(Context ctx, Term input) {
		List<Abstraction> wrappers = new ArrayList<>();
		Term bare = Term.getWrappingAbstractions(input, wrappers);
		if (!(bare instanceof Application)) return input; 
		Application app = (Application) bare;
		Atom func = app.getFunction();
		if (!(func instanceof Constant)) return input;
		Judgment j = ctx.getJudgment(func);
		if (!(j instanceof ContextJudgment)) return input;
		Util.verify(j.getRules().size() == 1, "context judgment can only have one rule");
		Rule rule = j.getRules().get(0);
		Application freshApp = rule.getFreshAdaptedRuleTerm(wrappers, null);
		Util.verify(freshApp.getArguments().size() == 2, "context rule should have exactly one premise");
		Term wrappedPrem = Term.wrapWithLambdas(wrappers, freshApp.getArguments().get(0));
		Term wrappedConc = Term.wrapWithLambdas(wrappers, freshApp.getArguments().get(1));
		Substitution invertSub = input.unify(wrappedConc);
		Term output = wrappedPrem.substitute(invertSub);
		// System.out.println("Inverted " + input + " as " + output);
		return invertContextJudgments(ctx,output);
	}
}
