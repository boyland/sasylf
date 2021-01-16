package edu.cmu.cs.sasylf.module;

import edu.cmu.cs.sasylf.Proof;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Span;

public class NullModuleFinder extends AbstractModuleFinder {
	private static NullModuleFinder prototype = new NullModuleFinder();

	public static NullModuleFinder get() {
		return prototype;
	}

	private NullModuleFinder() { }

	@Override
	public boolean hasCandidate(ModuleId id) {
		return false;
	}

	@Override
	public Proof findProof(ModuleId id, Span location) {
		ErrorHandler.error(Errors.MODULE_NOT_FOUND, id.toString(), location);
		return null;
	}

}
