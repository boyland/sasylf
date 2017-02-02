package edu.cmu.cs.sasylf.ast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.sasylf.Main;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Span;

/**
 * Module finder that uses a root directory and caches results.
 */
public class RootModuleFinder implements ModuleFinder {
	private final File rootDirectory;
	private final List<ModuleId> inProcess = new ArrayList<ModuleId>();
	private final Map<ModuleId,CompUnit> cache = new HashMap<ModuleId,CompUnit>();
	private String[] currentPackage = EMPTY_PACKAGE;

	public RootModuleFinder(File dir) {
		rootDirectory = dir;
	}

	@Override
	public void setCurrentPackage(String[] pName) {
		currentPackage = pName;
	}

	@Override
	public CompUnit findModule(String name, Span location) {
		return findModule(new ModuleId(currentPackage,name),location);
	}

	@Override
	public CompUnit findModule(ModuleId id, Span location) {
		if (cache.containsKey(id)) {
			CompUnit previous = cache.get(id);
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
		File f = id.asFile(rootDirectory);
		String[] savedPackage = currentPackage;
		inProcess.add(id);
		CompUnit result = null;
		try {
			result = parseAndCheck(f,id,location);
			return result;
		} finally {
			currentPackage = savedPackage;
			inProcess.remove(inProcess.size()-1);
			cache.put(id, result);
		}
	}

	protected CompUnit parseAndCheck(File f, ModuleId id, Span loc) {
		try {
			return Main.parseAndCheck(this, f.toString(), id, new InputStreamReader(new FileInputStream(f),"UTF-8"));
		} catch (UnsupportedEncodingException e) {
			ErrorHandler.report(Errors.INTERNAL_ERROR,  e.getMessage(), loc);
		} catch (FileNotFoundException e) {
			ErrorHandler.report("Module not found: " + id, loc);
		}
		return null;
	}

	protected ModuleId lastModuleId() {
		if (inProcess.isEmpty()) return null;
		return inProcess.get(inProcess.size()-1);
	}

	protected void clearCache() {
		cache.clear();
	}

	protected boolean removeCacheEntry(ModuleId id) {
		return cache.remove(id) != null;
	}
}
