package edu.cmu.cs.sasylf.module;

import java.io.File;

/**
 * Module finder that uses a root directory and caches results.
 * This module provider also adds the standard library.
 */
public class RootModuleFinder extends PathModuleFinder {
	public RootModuleFinder(File dir) {
		this(new RootModuleProvider(dir));
	}
	
	protected RootModuleFinder(ModuleProvider p) {
		super(p);
		super.addProvider(new ResourceModuleProvider());
	}
}
