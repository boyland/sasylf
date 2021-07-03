package edu.cmu.cs.sasylf.module;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import edu.cmu.cs.sasylf.Proof;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Span;

/**
 * Find modules within the SASyLF library
 */
public class ResourceModuleProvider extends AbstractModuleProvider {

	@Override
	public boolean has(ModuleId id) {
		return getClass().getResource(asResourceString(id)) != null;
	}

	/**
	 * Convert this module id into a resource string
	 * @param id module identifier to convert
	 * @return string
	 */
	public String asResourceString(ModuleId id) {
		StringBuilder sb = new StringBuilder("/");
		for (String p : id.packageName) {
			sb.append(p);
			sb.append("/");
		}
		sb.append(id.moduleName);
		sb.append(".slf");
		return sb.toString();
	}

	@Override
	public Proof get(PathModuleFinder mf, ModuleId id, Span loc) {
		Proof results = null;
		try {
			final String resourceName = asResourceString(id);
			InputStream is = getClass().getResourceAsStream(resourceName);
			results = makeResults(resourceName, id);
			results.parseAndCheck(mf, new InputStreamReader(is,"UTF-8"));
		} catch (UnsupportedEncodingException e) {
			ErrorHandler.error(Errors.INTERNAL_ERROR,  e.getMessage(), loc);
		} catch (NullPointerException e) {
			ErrorHandler.error(Errors.MODULE_NOT_FOUND, id.toString(), loc);
		} catch (RuntimeException e) {
			e.printStackTrace();
			ErrorHandler.error(Errors.INTERNAL_ERROR,  e.getMessage(), loc);
		}
		return results;
	}

	protected Proof makeResults(String resourceName, ModuleId id) {
		return new Proof(resourceName, id);
	}
}
