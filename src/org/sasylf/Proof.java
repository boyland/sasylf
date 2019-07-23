package org.sasylf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.sasylf.util.TrackDirtyRegions;

import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.ast.Node;
import edu.cmu.cs.sasylf.ast.RuleLike;

/**
 * Information about a SASyLF Proof:
 * the compilation unit and edits since the last check.
 */
public class Proof {

	private IResource resource;
	private IDocument document;
	private TrackDirtyRegions tracker;
	private CompUnit compilation;
	private List<Node> declarations;
	private SortedMap<String,RuleLike> ruleLikeCache = new TreeMap<String,RuleLike>();

	/**
	 * Create a proof object with compilation not set yet.
	 * If the document is provided, we will track changes until
	 * otherwise specified.  This information is not registered.
	 * @see #setCompilation(CompUnit)
	 * @see #changeProof(Proof, Proof)
	 * @param res resource for this proof, must not be null
	 * @param doc document for the proof source, may be null (no tracking desired)
	 */
	public Proof(IResource res, IDocument doc) {
		resource = res;
		document = doc;
		if (doc != null) {
			tracker = new TrackDirtyRegions();
			doc.addDocumentListener(tracker);
		}
		compilation = null;
	}

	/**
	 * Set the compilation unit for this proof.  This may be done only
	 * once (setting to a non-null compilation).
	 * @param cu compilation unit null for this proof.
	 * If null, it only checks that the compilation has not yet been set.  
	 * @throws IllegalStateException if compilation already set.
	 */
	public void setCompilation(CompUnit cu) {
		if (cu == null) return; // or throw?
		if (compilation != null) {
			throw new IllegalStateException("can only set compilation once");
		}
		compilation = cu;
		declarations = new ArrayList<Node>();
		updateCache();
	}

	private void updateCache() {
		compilation.collectTopLevel(declarations);
		compilation.collectRuleLike(ruleLikeCache);
	}
	
	public IResource getResource() {
		return resource;
	}

	public List<TrackDirtyRegions.IDirtyRegion> getChanges(IDocument doc) {
		if (doc != document || tracker == null) return null;
		return tracker.getDirtyRegions();
	}

	public CompUnit getCompilation() {
		return compilation;
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
		return ruleLikeCache.get(name);
	}
	
	/** Find all rule-likes in teh given compilation unit that start with the given prefix.
	 * @param prefix key to start with, must not be null
	 * @return iterator (never null) of rule-likes that start with the given prefix.
	 */
	public Collection<RuleLike> findRuleLikeByPrefix(String prefix) {
		return ruleLikeCache.subMap(prefix, prefix+Character.MAX_VALUE).values();
	}
	
	/**
	 * If this proof object is collecting incremental changes, stop doing that,
	 * and release resources.  If the proof thus changed is the current object
	 * for a resource, this will force the next compilation to be non-incremental.
	 * For example, if we are about to save a resource, we do this.
	 * We also stop incrementality when we revert.
	 */
	public void stopTracking() {
		Proof oldProof = this;
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
		compilation = null;
	}

	private static ConcurrentMap<IResource,Proof> proofs = new ConcurrentHashMap<IResource,Proof>();

	/**
	 * Return the proof object for this resource, if there is one.
	 * @param res
	 * @return proof object or null
	 */
	public static Proof getProof(IResource res) {
		return proofs.get(res);
	}

	public static CompUnit getCompUnit(IResource res) {
		Proof p = getProof(res);
		if (p != null) return p.compilation;
		return null;
	}

	/**
	 * Remove the proof object for this resource.
	 * This turns off incrementality, but does not dispose the current
	 * proof for this resource (in case someone already has access to it).
	 * @param res resource to remove proof for, must not be null
	 * @return whether there was a proof to remove
	 */
	public static boolean removeProof(IResource res) {
		Proof oldProof = proofs.remove(res);
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
	public static boolean changeProof(Proof oldProof, Proof newProof) {
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
}
