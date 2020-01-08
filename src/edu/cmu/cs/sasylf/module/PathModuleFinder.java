package edu.cmu.cs.sasylf.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Span;

/**
 * Module finder that uses a path of module providers.
 */
public abstract class PathModuleFinder implements ModuleFinder {

	private String[] currentPackage = EMPTY_PACKAGE;
	private final List<ModuleId> inProcess = new ArrayList<ModuleId>();
	private final Map<ModuleId,ModuleProvider> presentCache = new HashMap<ModuleId,ModuleProvider>();
	private final Map<ModuleId,CompUnit> cache = new HashMap<ModuleId,CompUnit>();
	private final List<ModuleProvider> providers = new ArrayList<ModuleProvider>();
	
	/**
	 * Create a path module finder with initially a single provider.
	 * @param p a single provider
	 */
	protected PathModuleFinder(ModuleProvider p) {
		providers.add(p);
	}

	@Override
	public void setCurrentPackage(String[] pName) {
		currentPackage = pName;
	}

	@Override
	public boolean hasCandidate(ModuleId id) {
		if (cache.containsKey(id)) return true;
		if (presentCache.containsKey(id)) {
			return presentCache.get(id) != null;
		}
		for (ModuleProvider p : providers) {
			if (p.has(id)) {
				presentCache.put(id, p);
				return true;
			}
		}
		presentCache.put(id, null);
		return false;
	}

	@Override
	public Module findModule(String name, Span location) {
		return findModule(new ModuleId(currentPackage,name),location);
	}

	@Override
	public Module findModule(ModuleId id, Span location) {
		if (!hasCandidate(id)) {
			ErrorHandler.report("Cannot find module " + id, location);
		}
		if (cache.containsKey(id)) {
			Module previous = cache.get(id);
			if (previous == null) {
				ErrorHandler.report("Module has errors: " + id, location);
			}
			return previous;
		}
		if (inProcess.contains(id)) {
			StringBuilder path = new StringBuilder();
			for (int i=inProcess.indexOf(id); i < inProcess.size(); ++i) {
				path.append(inProcess.get(i).toString());
				path.append(" -> ");
			}
			path.append(id);
			cache.put(id, null);
			ErrorHandler.report("Cyclic module reference: "+path.toString(), location);
		}
		ModuleProvider provider = presentCache.get(id);
		String[] savedPackage = currentPackage;
		inProcess.add(id);
		CompUnit result = null;
		try {
			CompUnit result1;
			result1 = provider.get(this, id, location);
			result = result1;
			return result;
		} finally {
			currentPackage = savedPackage;
			inProcess.remove(inProcess.size()-1);
			cache.put(id, result);
		}
	}

	protected ModuleId lastModuleId() {
		if (inProcess.isEmpty()) return null;
		return inProcess.get(inProcess.size()-1);
	}

	protected void clearCache() {
		presentCache.clear();
		cache.clear();
	}

	protected boolean removeCacheEntry(ModuleId id) {
		presentCache.remove(id);
		return cache.remove(id) != null;
	}
}