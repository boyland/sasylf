package edu.cmu.cs.sasylf.module;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import edu.cmu.cs.sasylf.Main;
import edu.cmu.cs.sasylf.ast.CompUnit;
import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.SASyLFError;
import edu.cmu.cs.sasylf.util.Span;

/**
 * Find modules within the SASyLF library
 */
public class ResourceModuleProvider implements ModuleProvider {

	@Override
	public boolean has(ModuleId id) {
		return getClass().getResource(asResourceString(id)) != null;
	}

	/**
	 * Convert this module id into a resource string
	 * @param id module identifier to convert
	 * @return string
	 */
	protected String asResourceString(ModuleId id) {
		String result = id.asFile(new File("/")).toString();
		return result;
	}

	@Override
	public CompUnit get(PathModuleFinder mf, ModuleId id, Span loc) {
		try {
			InputStream is = getClass().getResourceAsStream(asResourceString(id));
			return Main.parseAndCheck(mf, id.toString(), id, new InputStreamReader(is,"UTF-8"));
		} catch (UnsupportedEncodingException e) {
			ErrorHandler.report(Errors.INTERNAL_ERROR,  e.getMessage(), loc);
		} catch (NullPointerException e) {
			ErrorHandler.report("Module not found: " + id, loc);
		} catch (SASyLFError ex) {
			// already reported
		} catch (RuntimeException e) {
			ErrorHandler.report(Errors.INTERNAL_ERROR,  e.getMessage(), loc);
		}
		return null;
	}

}
