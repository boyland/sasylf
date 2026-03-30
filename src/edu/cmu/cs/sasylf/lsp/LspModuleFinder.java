package edu.cmu.cs.sasylf.lsp;

import java.io.File;

import edu.cmu.cs.sasylf.Proof;
import edu.cmu.cs.sasylf.module.Module;
import edu.cmu.cs.sasylf.module.ModuleFinder;
import edu.cmu.cs.sasylf.module.ModuleId;
import edu.cmu.cs.sasylf.module.PathModuleFinder;
import edu.cmu.cs.sasylf.util.Span;

public class LspModuleFinder implements ModuleFinder {

	private final PathModuleFinder delegate;

	public LspModuleFinder(DocumentManager documents, String currentFile) {
		File f = new File(currentFile);
		File dir = f.getParentFile();
		// walk up to find the package root by counting package segments
		String path = (dir != null) ? dir.getAbsolutePath() : ".";
		this.delegate = new PathModuleFinder(path);
	}

	@Override
	public boolean hasCandidate(ModuleId id) {
		return delegate.hasCandidate(id);
	}

	@Override
	public Module findModule(String name, Span location) {
		return delegate.findModule(name, location);
	}

	@Override
	public Module findModule(ModuleId id, Span location) {
		return delegate.findModule(id, location);
	}

	@Override
	public Proof findProof(ModuleId id, Span location) {
		return delegate.findProof(id, location);
	}

	@Override
	public void setCurrentPackage(String[] pName) {
		delegate.setCurrentPackage(pName);
	}
}
