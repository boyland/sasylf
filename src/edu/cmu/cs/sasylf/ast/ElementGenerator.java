package edu.cmu.cs.sasylf.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.sasylf.term.Constant;
import edu.cmu.cs.sasylf.term.FreeVar;
import edu.cmu.cs.sasylf.util.Location;

/**
 * A class to create elements according to a schema.
 * For each element we generate, we consult a reference and a user.
 * The reference indicates the form to take, the use is used to get
 * the location so that the generated elements have a user-identifiable location.
 * This system also handles generating elements that are dependent on variables.
 */
public class ElementGenerator {
	private int unique;
	private Map<Variable,Variable> varRecord = new LinkedHashMap<>();

	private List<Variable> dependencies = Collections.emptyList();
	private List<Constant> families = Collections.emptyList();
	
	/**
	 * Forget any variables generated so far, so they aren't reused
	 * if the same reference variable shows up.
	 */
	public void forgetVariables() {
		varRecord.clear();
	}
	
	/**
	 * Take the current variables and set them up so that later
	 * elements generated are checked to see if they have dependencies,
	 * and if so to add them (turning nonterminals into bidings as necessary.)
	 * The current variables are forgotten.
	 */
	public void useVariableDependencies() {
		dependencies = new ArrayList<>(varRecord.values());
		varRecord.clear();
		families = new ArrayList<>(dependencies.size());
		for (Variable v : dependencies) {
			families.add(v.getTypeTerm().baseTypeFamily());
		}
	}
	
	/**
	 * Make an element that is a copy of the given element,
	 * possibly a binding dependent on the variable passed in.
	 * Nonterminals (and bindings) are generated to be unique.
	 * @param e element to copy, must not be null or a clause
	 * @param u element to use for location of new element
	 * @param newName if true, create a unique name (append a number)
	 * @return fresh element that copies the given element.
	 */
	public Element makeCopy(Element e, Element u, boolean newName) {
		// XXX: Consider pushing this code into Element and subclasses
		Element result;
		Location loc = u.getLocation();
		if (e instanceof Terminal) {
			Terminal t = (Terminal)e;
			Terminal rt = new Terminal(t.getSymbol(), loc);
			if (t.mustQuote()) {
				rt.setMustQuote();
			}
			result = rt;
		} else if (e instanceof Variable) {
			Variable ev = (Variable)e;
			Variable rv = varRecord.get(ev);
			if (rv == null) {
				String name = ev.getSymbol();
				if (newName) name = name + ++unique;
				rv = new Variable(name, loc);
				rv.setType(ev.getType());
				varRecord.put(ev, rv);
			}
			result = rv;
		} else if (e instanceof NonTerminal) {
			NonTerminal nt = (NonTerminal)e;
			Constant nFam = nt.getTypeTerm().baseTypeFamily();
			List<Element> depends = new ArrayList<>();
			for (int i=0; i < families.size(); ++i) {
				Constant vFam = families.get(i);
				if (FreeVar.canAppearIn(vFam, nFam)) {
					depends.add(dependencies.get(i));
				}
			}
			if (!depends.isEmpty()) newName = true; // need to avoid name clash
			String name = nt.getSymbol();
			if (newName) name = nt.getType().toString()+ ++unique;
			NonTerminal rnt = new NonTerminal(name,loc);
			rnt.setType(nt.getType());
			if (depends.isEmpty()) {
				result = rnt;
			} else {
				result = new Binding(loc,rnt,depends,loc);
			}
		} else if (e instanceof Binding) {
			Binding b = (Binding)e;
			Element newBase = makeCopy(b.getNonTerminal(),u, newName);
			NonTerminal newNT;
			List<Element> newVars;
			if (newBase instanceof Binding) {
				newNT = ((Binding)newBase).getNonTerminal();
				newVars = ((Binding)newBase).getElements(); //XXX: mutating
			} else {
				newNT = (NonTerminal)newBase;
				newVars = new ArrayList<>();
			}
			for (Element be : b.getElements()) {
				newVars.add(makeCopy(be,u, newName));
			}
			result = new Binding(loc, newNT,newVars,loc);
		} else {
			throw new IllegalArgumentException("cannot copy: " + e);
		}
		result.setEndLocation(u.getEndLocation());
		return result;
	}
}