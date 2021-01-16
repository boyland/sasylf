package edu.cmu.cs.sasylf.module;

import edu.cmu.cs.sasylf.Proof;
import edu.cmu.cs.sasylf.util.Span;

/**
 * A partial implementation of module finder.
 */
public abstract class AbstractModuleFinder implements ModuleFinder {

	protected String[] currentPackage = EMPTY_PACKAGE;
	
	@Override
	public Module findModule(String name, Span location) {
		return findModule(new ModuleId(currentPackage,name),location);
	}

	@Override
	public Module findModule(ModuleId id, Span location) {
		Proof p = findProof(id,location);
		if (p == null) return null;
		return p.getCompilationUnit();
	}

	@Override
	public void setCurrentPackage(String[] pName) {
		currentPackage = pName.clone();
	}

}
