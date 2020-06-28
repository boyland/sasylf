package org.sasylf.project;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.sasylf.ProofChecker;

import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.module.Module;
import edu.cmu.cs.sasylf.module.ModuleId;
import edu.cmu.cs.sasylf.module.PathModuleFinder;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.Span;

public class ProjectModuleFinder extends PathModuleFinder {

	private ConcurrentHashMap<ModuleId,Set<ModuleId>> dependencies = new ConcurrentHashMap<ModuleId,Set<ModuleId>>();

	public ProjectModuleFinder(IProject p) {
		super(new ProjectModuleProvider(p));
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
		
		if (dependencies.contains(id)) {
			Set<ModuleId> copy = new HashSet<ModuleId>();
			copy.addAll(dependencies.get(id));
			return copy;
		}
		
		return Collections.emptySet();
	}

	@Override
	public Module findModule(ModuleId id, Span location) {
		ModuleId last = super.lastModuleId();
		if (last != null) {
			Set<ModuleId> deps = dependencies.get(id);
			if (deps == null) {
				dependencies.putIfAbsent(id, new HashSet<ModuleId>());
				deps = dependencies.get(id);
			}
			deps.add(last);
			System.out.println("adding dependency from " + last + " on " + id);
		} else {
			System.out.println("Unknown dependency on " + id);
		}
		
		return super.findModule(id, location);
	}

	protected CompUnit parseAndCheck(File f, ModuleId id, Span loc) {
		
		// The problem here is that we want to read the current state of the
		// editor buffer instead of the resource on disk, if it is available.
		// This requires us to convert back to a resource and then call the
		// analyzeSlf method that will try to find an editor.
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IPath path = Path.fromOSString(f.getAbsolutePath());
		IResource res = workspace.getRoot().getFileForLocation(path);
		CompUnit result;
		try {
			result = ProofChecker.analyzeSlf(this, id, res);
		} finally {
			// TODO: handle finally clause
		}
		if (result == null) {
			// We should not generate any errors NOW since 
			// this error will not be handled properly (using markers etc).
			// And this will commonly happen if there is a syntactic error.
			// A print to console might be useful for debugging:
			System.out.println("proof checker failed to return a valid AST.");
		}
		return result;
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
		monitor.beginTask("rechecking proofs", todo.size());
		try {
			for (ModuleId id : todo) {
				IProgressMonitor subMonitor = new SubProgressMonitor(monitor,1);
				subMonitor.beginTask("rechecking " + id, 1);
				try {
					super.findModule(id, null);
					subMonitor.worked(1);
				} catch (SASyLFError e) {
					// muffle
					subMonitor.worked(1);
				} finally {
					subMonitor.done();
				}
				if (monitor.isCanceled()) {
					throw new OperationCanceledException("Build stopped");
				}
			}
		} finally {
			monitor.done();
		}
	}

	public void remove(ModuleId id) {
		super.removeCacheEntry(id);
		dependencies.remove(id);
	}

	public void clear() {
		super.clearCache();
	}
}



/*
Notes:

parseAndCheck() does indeed go through first if statement
   - both "id" and "last" are the same
   - what does adding things to "deps" actually do for us?
need to figure out how to add dependencies from other files
*/
