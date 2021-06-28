package edu.cmu.cs.sasylf.module;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.sasylf.Proof;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Span;

/**
 * Module finder that uses a path of module providers.
 */
public class PathModuleFinder extends AbstractModuleFinder implements ModuleFinder, ModuleEventListener {

	private final List<ModuleId> inProcess = new ArrayList<ModuleId>();
	private final Map<ModuleId,ModuleProvider> presentCache = new HashMap<ModuleId,ModuleProvider>();
	private final Map<ModuleId,Proof> cache = new HashMap<>();
	private final List<ModuleProvider> providers = new ArrayList<ModuleProvider>();
	
	/**
	 * Create a path module finder with initially a single provider.
	 * @param p a single provider, must not be null
	 */
	protected PathModuleFinder(ModuleProvider p) {
		addProvider(p);
	}
	
	/**
	 * Get the {@link ModuleProvider} associated with the provided {@link ModuleId} in the cache.
	 * @param id the module id to check for, must not be null
	 * @return the module provider associated with this id in the cache
	 */
	public ModuleProvider lookupModule(ModuleId id) {
		hasCandidate(id);
		
		return presentCache.get(id);
	}

	/**
	 * Add a provider to the module finder
	 * @param p extra provider to add
	 */
	protected void addProvider(ModuleProvider p) {
		providers.add(p);
		p.addModuleEventListener(this);
	}
	
	/**
	 * Create a path module finder with given list of providers
	 * @param l list of providers, must not be null
	 */
	protected PathModuleFinder(List<ModuleProvider> l) {
		for (ModuleProvider p : l) {
			addProvider(p);
		}
	}
	
	/**
	 * Create a path module finder with the given path,
	 * a separated list of places to find modules.
	 * @param path non-null string of places separated with {@link File#pathSeparator}.
	 */
	public PathModuleFinder(String path) {
		addProvider(new ResourceModuleProvider());
		String[] pieces = path.split(File.pathSeparator);
		for (String piece : pieces) {
			ModuleProvider p;
			if (piece.equals("")) p = new RootModuleProvider();
			else p = new RootModuleProvider(new File(piece));
			addProvider(p);
		}
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
	public Proof findProof(ModuleId id, Span location) {
		if (!hasCandidate(id)) {
			ErrorHandler.error(Errors.MODULE_NOT_FOUND, id.toString(), location);
		}
		if (cache.containsKey(id)) {
			Proof previous = cache.get(id);
			// if null, the error should have been reported already
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
			ErrorHandler.error(Errors.MODULE_CYCLE, path.toString(), location);
		}
		ModuleProvider provider = presentCache.get(id);
		String[] savedPackage = currentPackage;
		inProcess.add(id);
		Proof result = null;
		try {
			result = provider.get(this, id, location);
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

	@Override
	public void moduleChanged(ModuleChangedEvent e) {
		//System.out.println("Got changed event " + e);
		removeCacheEntry(e.getModuleId());
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