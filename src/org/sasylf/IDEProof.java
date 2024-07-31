package org.sasylf;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.sasylf.util.DocumentUtil;
import org.sasylf.util.IProjectStorage;
import org.sasylf.util.TrackDirtyRegions;

import edu.cmu.cs.sasylf.Proof;
import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.ast.ModulePart;
import edu.cmu.cs.sasylf.ast.Named;
import edu.cmu.cs.sasylf.ast.Node;
import edu.cmu.cs.sasylf.ast.RuleLike;
import edu.cmu.cs.sasylf.module.Module;
import edu.cmu.cs.sasylf.module.ModuleFinder;
import edu.cmu.cs.sasylf.module.ModuleId;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.Span;

/**
 * Information about a SASyLF Proof:
 * the compilation unit and edits since the last check.
 */
public class IDEProof extends Proof {

	private IProjectStorage resource;
	private IDocument document;
	private TrackDirtyRegions tracker;
	private List<Node> declarations;
	private SortedMap<String,RuleLike> ruleLikeCache = new TreeMap<String,RuleLike>();

	/**
	 * Create a proof object with compilation not set yet.
	 * If the document is provided, we will track changes until
	 * otherwise specified.  This information is not registered.
	 * @param name name to associated with this proof (must not be null)
	 * @param id module id of the resource being read, may be null
	 * @param res resource/project storage for this proof, must not be null
	 * @param doc document for the proof source, may be null (no tracking desired)
	 * @see #changeProof(IDEProof, IDEProof)
	 */
	public IDEProof(String name, ModuleId id, IAdaptable res, IDocument doc) {
		super(name,id);
		resource = IProjectStorage.Adapter.adapt(res);
		if (resource == null) {
			throw new NullPointerException("No project storage for " + res);
		}
		document = doc;
		if (doc != null) {
			tracker = new TrackDirtyRegions();
			doc.addDocumentListener(tracker);
		}
	}

	@Override
	public String toString() {
		return "Proof(" + resource + "," + (getCompilationUnit() == null ? "<no contents>" : "<contents>") + ")";
	}
	
	@Override
	public void parseAndCheck(ModuleFinder mf, Reader r) {
		super.parseAndCheck(mf, r);
		declarations = new ArrayList<Node>();
		if (super.getCompilationUnit() != null) {
			updateCache();
		}
	}

	private void updateCache() {
		CompUnit compilation = getCompilationUnit();
		compilation.collectTopLevel(declarations);
		compilation.collectRuleLike(ruleLikeCache);
		for (Node n : declarations) {
			if (n instanceof ModulePart) {
				ModulePart mpart = (ModulePart)n;
				Object res = mpart.getModule().resolve(null);
				if (res instanceof Module) {
					Map<String,RuleLike> tmp = new HashMap<String,RuleLike>();
					((Module)res).collectRuleLike(tmp);
					String prefix = mpart.getName() + ".";
					for (Map.Entry<String, RuleLike> e : tmp.entrySet()) {
						ruleLikeCache.put(prefix+e.getKey(), e.getValue());
					}
				}
			}
		}
	}
	
	/**
	 * Get the storage associated with this proof.
	 * @return storage that was parsed for this proof.
	 */
	public IProjectStorage getStorage() {
		return resource;
	}
	
	public List<TrackDirtyRegions.IDirtyRegion> getChanges(IDocument doc) {
		if (doc != document || tracker == null) return null;
		return tracker.getDirtyRegions();
	}

	public CompUnit getCompilation() {
		return super.getCompilationUnit();
	}

	/**
	 * Convert a location in a proof file to an offset in the proof's document
	 * @param l location to convert, must not be null
	 * @return offset in document
	 * @throws BadLocationException in the location wasn't in the proof
	 */
	public int convert(Location l) throws BadLocationException {
		if (l == null) throw new NullPointerException("null location!");
		if (document == null) {
			System.err.println("null document for " + this);
			throw new BadLocationException("document is null");
		}
		return DocumentUtil.getOffset(l, document);
	}
	
	/**
	 * Convert a span inside this proof file into a region within the document
	 * @param sp span to convert, must not be null
	 * @return region within the proof document
	 * @throws BadLocationException  if the span didn't correspond to this proof
	 */
	public IRegion convert(Span sp) throws BadLocationException {
		int l1 = convert(sp.getLocation());
		int l2 = convert(sp.getEndLocation());
		return new Region(l1,l2-l1);
	}
	
	/**
	 * Return all declarations in this proof.
	 * @return
	 */
	public Collection<Node> getDeclarations() {
		return Collections.unmodifiableCollection(declarations);
	}
	
	/**
	 * Find a rule-like by name 
	 * @param name key to look for, must not be null
	 * @return rule-like in this proof that has the given name, or null if none such exists.
	 */
	public RuleLike findRuleLikeByName(String name) {
		if (ruleLikeCache == null) return null;
		return ruleLikeCache.get(name);
	}
	
	/** Find all rule-likes in the given compilation unit that start with the given prefix.
	 * @param prefix key to start with, must not be null
	 * @return submap (never null) of rule-likes that start with the given prefix.
	 */
	public Map<String,RuleLike> findRuleLikeByPrefix(String prefix) {
		return ruleLikeCache.subMap(prefix, prefix+Character.MAX_VALUE);
	}
	
	public Named findDeclarationByName(String name) {
		if (declarations == null) return null;
		for (Node d : declarations) {
			if (d instanceof Named) {
				Named decl = (Named)d;
				if (decl.getName().equals(name)) return decl;
			}
		}
		return null;
	}
	/**
	 * If this proof object is collecting incremental changes, stop doing that,
	 * and release resources.  If the proof thus changed is the current object
	 * for a resource, this will force the next compilation to be non-incremental.
	 * For example, if we are about to save a resource, we do this.
	 * We also stop incrementality when we revert.
	 */
	public void stopTracking() {
		IDEProof oldProof = this;
		if (oldProof.tracker != null) {
			oldProof.document.removeDocumentListener(oldProof.tracker);
			oldProof.tracker = null;
			oldProof.document = null;
		}    
	}

	/**
	 * Release any resources for this proof object.
	 */
	public void dispose() {
		stopTracking();
		proofs.remove(resource, this); // just in case
		resource = null;
	}

	private static ConcurrentMap<IProjectStorage,IDEProof> proofs = new ConcurrentHashMap<IProjectStorage,IDEProof>();

	/**
	 * Return the proof object for this resource, if there is one.
	 * @param res
	 * @return proof object or null
	 */
	public static IDEProof getProof(IAdaptable res) {
		return proofs.get(IProjectStorage.Adapter.adapt(res));
	}

	public static CompUnit getCompUnit(IAdaptable res) {
		IDEProof p = getProof(res);
		if (p != null) return p.getCompilation();
		return null;
	}

	/**
	 * Remove the proof object for this resource.
	 * This turns off incrementality, but does not dispose the current
	 * proof for this resource (in case someone already has access to it).
	 * @param res resource to remove proof for, must not be null
	 * @return whether there was a proof to remove
	 */
	public static boolean removeProof(IAdaptable res) {
		IProjectStorage ps = IProjectStorage.Adapter.adapt(res);
		if (ps == null) {
			System.err.println("Warning: Cannot find proof file for " + res);
			return false;
		}
		IDEProof oldProof = proofs.remove(ps);
		if (oldProof != null) {
			oldProof.stopTracking();
		}
		return oldProof != null;
	}

	/**
	 * Replace the proof from its old value to the new value,
	 * but only if the old value matches.  If the old proof
	 * had already been replaced, then the new proof is disposed
	 * and this method returns false.
	 * @param oldProof proof to replace, may be null 
	 * (should be result of previous {@link #getProof(IResource)} call).
	 * @param newProof proof to replace with, must not be null
	 * @return whether proof was replaced.
	 */
	public static boolean changeProof(IDEProof oldProof, IDEProof newProof) {
		if (oldProof == null) {
			if (proofs.putIfAbsent(newProof.resource, newProof) == null) {
				return true;
			}
		} else if (proofs.replace(newProof.resource, oldProof, newProof)) {
			oldProof.stopTracking(); // not dispose: people may still have access
			return true;
		}
		newProof.dispose();
		return false;
	}
	
	public static void listProofs() {
		System.out.println("Proofs maintained:");
		for (IDEProof p : proofs.values()) {
			System.out.println(p);
		}
	}
}
