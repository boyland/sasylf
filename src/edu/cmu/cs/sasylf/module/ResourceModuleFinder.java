package edu.cmu.cs.sasylf.module;

/**
 * A module finder that can only find library modules.
 */
public class ResourceModuleFinder extends PathModuleFinder {
	
	public ResourceModuleFinder() {
		super(new ResourceModuleProvider());
	}

}
