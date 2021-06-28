package edu.cmu.cs.sasylf.module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import edu.cmu.cs.sasylf.Proof;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.Span;

/**
 * A module provider using a root directory to look things up.
 */
public class RootModuleProvider extends AbstractModuleProvider {
	protected final File rootDirectory;

	/**
	 * Create a root module provider looking at the current working directory.
	 */
	public RootModuleProvider() {
		rootDirectory = null; // new File(System.getProperty("user.dir"));
	}
	
	/**
	 * Create a root module provider looking at this directory.
	 * @param dir directory to use, must not be null
	 */
	public RootModuleProvider(File dir) {
		if (dir == null) throw new NullPointerException("root dir must not be null");
		rootDirectory = dir;
	}

	@Override
	public boolean has(ModuleId id) {
		File f = getFile(id);
		return f.isFile();
	}
	
	/**
	 * Get the {@link File} from a {@link ModuleId}.
	 * @param id the module id to search for
	 * @return the root file created from the given module id
	 */
	protected File getFile(ModuleId id) {
		return id.asFile(rootDirectory);
	}

	@Override
	public Proof get(PathModuleFinder mf, ModuleId id, Span location) {
		return parseAndCheck(mf,getFile(id),id, location);
	}

	protected Proof parseAndCheck(ModuleFinder mf, File f, ModuleId id, Span loc) {
		try {
			return Proof.parseAndCheck(mf, f.toString(), id, new InputStreamReader(new FileInputStream(f),"UTF-8"));
		} catch (UnsupportedEncodingException e) {
			ErrorHandler.error(Errors.INTERNAL_ERROR,  e.getMessage(), loc);
		} catch (FileNotFoundException e) {
			ErrorHandler.error(Errors.MODULE_NOT_FOUND, id.toString(), loc);
		} catch (SASyLFError ex) {
			// already reported
		} catch (RuntimeException e) {
			e.printStackTrace();
			ErrorHandler.error(Errors.INTERNAL_ERROR,  e.getMessage(), loc);
		}
		return null;
	}
}