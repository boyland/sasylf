package edu.cmu.cs.sasylf.module;

import java.io.File;

import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.util.Span;

/**
 * Module finder that uses a root directory and caches results.
 */
public class RootModuleFinder extends AbstractModuleFinder {
	final ModuleProvider provider;
	public RootModuleFinder(File dir) {
		provider = new RootModuleProvider(dir);
	}
	
	protected RootModuleFinder(ModuleProvider p) {
		provider = p;
	}

	@Override
	protected boolean lookupCandidate(ModuleId id) {
		return provider.has(id);
	}

	@Override
	protected CompUnit loadModule(ModuleId id, Span location) {
		CompUnit result;
		result = provider.get(this, id, location);
		return result;
	}
}
