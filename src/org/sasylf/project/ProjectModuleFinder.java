package org.sasylf.project;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.sasylf.IDEProof;
import org.sasylf.util.ResourceDocumentProvider;
import org.sasylf.util.ResourceStorage;

import edu.cmu.cs.sasylf.Proof;
import edu.cmu.cs.sasylf.module.ModuleId;
import edu.cmu.cs.sasylf.module.PathModuleFinder;
import edu.cmu.cs.sasylf.module.ResourceModuleProvider;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.Span;

public class ProjectModuleFinder extends PathModuleFinder {
	private final IProject project;
	
	private ConcurrentHashMap<ModuleId,Set<ModuleId>> dependencies = new ConcurrentHashMap<ModuleId,Set<ModuleId>>();

	/**
	 * Create an instance of {@link ProjectModuleFinder} from an {@link IProject}.
	 */
	public ProjectModuleFinder(IProject p) {
		super(new ProjectModuleProvider(p));
		addProvider(new ResourceProvider());
		project = p;
	}

	public void dispose() {
	}
	
	/**
	 * Get the set of dependences associated with the given module id.  
	 * If none exist, an empty set is returned instead.
	 * @param id must not be null
	 * @return the set of dependencies
	 */
	public Set<ModuleId> getDependencies(ModuleId id) {
		if (id == null) throw new IllegalArgumentException("module id cannot be null!");
		
		if (dependencies.containsKey(id)) {
			Set<ModuleId> copy = new HashSet<ModuleId>();
			copy.addAll(dependencies.get(id));
			return copy;
		} else {
			System.out.println("no dependencies to get!");
		}
		
		return Collections.emptySet();
	}

	@Override
	public Proof findProof(ModuleId id, Span location) {
		ModuleId last = super.lastModuleId();
		if (last != null) {
			Set<ModuleId> deps = dependencies.get(id);
			if (deps == null) {
				dependencies.putIfAbsent(id, new HashSet<ModuleId>());
				deps = dependencies.get(id);
			}
			deps.add(last);
			// dependencies.put(id, deps); // redundant?!			
			// System.out.println("adding dependency from " + last + " on " + id);
		} else {
			// System.out.println("Unknown dependency on " + id);
		}
		
		return super.findProof(id, location);
	}

	private final Set<ModuleId> toRecheck = new HashSet<ModuleId>();

	/**
	 * Mark this module ID as needing rechecking and anything that depends on it.
	 * @param id must not be null.
	 */
	public void recheckNeeded(ModuleId id) {
		super.removeCacheEntry(id);
		if (!toRecheck.add(id)) return;
		Set<ModuleId> dep = dependencies.get(id);
		if (dep == null) return;
		for (ModuleId id2 : dep) {
			recheckNeeded(id2);
		}
	}

	/**
	 * Recheck all modules previously marked as needing rechecking.
	 */
	public void recheckAll(IProgressMonitor monitor) {
		if (monitor == null) monitor = new NullProgressMonitor();
		Set<ModuleId> todo = new HashSet<ModuleId>(toRecheck);
		toRecheck.clear();
		SubMonitor sub = SubMonitor.convert(monitor, "rechecking proofs", todo.size());

		for (ModuleId id : todo) {
			IProgressMonitor subMonitor = sub.split(1);
			subMonitor.beginTask("rechecking " + id, 1);
			try {
				super.findModule(id, null);
				subMonitor.worked(1);
			} catch (SASyLFError e) {
				// muffle
				subMonitor.worked(1);
			}
			if (monitor.isCanceled()) {
				throw new OperationCanceledException("Build stopped");
			}
		}
	}

	public void remove(ModuleId id) {
		super.removeCacheEntry(id);
		dependencies.remove(id);
	}

	public void clear() {
		super.clearCache();
	}
	
	protected class ResourceProvider extends ResourceModuleProvider {

		@Override
		public Proof get(PathModuleFinder mf, ModuleId id, Span loc) {
			final String resourceName = super.asResourceString(id);
			ResourceStorage st = new ResourceStorage(resourceName,project);
			IDEProof oldProof = IDEProof.getProof(st);
			
			final IDEProof newProof = (IDEProof)super.get(mf, id, loc);
			if (newProof == null) return null;
			
			if (IDEProof.changeProof(oldProof, newProof)) {
				return newProof;
			}
			return IDEProof.getProof(st);
		}

		@Override
		protected Proof makeResults(String resourceName, ModuleId id) {
			ResourceStorage st = new ResourceStorage(resourceName,project);
			final ResourceDocumentProvider provider = ResourceDocumentProvider.getInstance(st.asEditorInput());
			try {
				provider.connect(st.asEditorInput());
			} catch (CoreException e) {
				System.out.println("Cannot load resource");
			}
			// TODO Auto-generated method stub
			return new IDEProof(resourceName, id, st, provider.getDocument(st.asEditorInput()));
		}
		
	}
}
