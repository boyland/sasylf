package edu.cmu.cs.sasylf.module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import edu.cmu.cs.sasylf.Main;
import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Span;

/**
 * Module finder that uses a root directory and caches results.
 */
public class RootModuleFinder extends AbstractModuleFinder {
	private final File rootDirectory;
	public RootModuleFinder(File dir) {
		rootDirectory = dir;
	}

	@Override
	protected boolean lookupCandidate(ModuleId id) {
		File f = id.asFile(rootDirectory);
		return f.isFile();
	}

	@Override
	protected CompUnit loadModule(ModuleId id, Span location) {
		CompUnit result;
		File f = id.asFile(rootDirectory);
		result = parseAndCheck(f,id,location);
		return result;
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
}
