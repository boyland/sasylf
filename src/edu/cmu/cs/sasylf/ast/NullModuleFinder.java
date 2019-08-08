package edu.cmu.cs.sasylf.ast;

import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Span;

public class NullModuleFinder implements ModuleFinder {
	private static NullModuleFinder prototype = new NullModuleFinder();

	public static NullModuleFinder get() {
		return prototype;
	}

	protected NullModuleFinder() { }

	@Override
	public boolean hasCandidate(ModuleId id) {
		return false;
	}

	@Override
	public Module findModule(String name, Span location) {
		return findModule(new ModuleId(EMPTY_PACKAGE,name),location);
	}

	@Override
	public Module findModule(ModuleId id, Span location) {
		ErrorHandler.report("no root directory or SASyLF project,  cannot find module " + id,location);
		return null;
	}

	@Override
	public void setCurrentPackage(String[] pName) {
		// ignore
	}

}
