package edu.cmu.cs.sasylf.module;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;

import edu.cmu.cs.sasylf.util.Errors;

public final class ModuleId {
	public final String[] packageName;
	public final String moduleName;
	private final int hash;


	public ModuleId(File file) {
		//System.out.println("File: " + file);	

		LinkedList<String> pkg = new LinkedList<String>();
		moduleName = checkFileExtension(file.getName());
		File f = file;
		for (;;) {
			f = f.getParentFile();
			if (f == null) break;
			pkg.addFirst(f.getName());
		}
		packageName = pkg.toArray(new String[pkg.size()]);
		hash = computeHash();

		//System.out.println(moduleName);
	}

	public ModuleId(String filename) {
		this(new File(filename));
		//System.out.println("Created module id for " + filename + ": " + this);
	}

	/**
	 * Throws an exception if the file extension is not ".slf", otherwise
	 * we chop that part off and return the new file name.
	 * @param filename the file name to check
	 * @return the new file name
	 */
	private static String checkFileExtension(String filename) {
		if (!filename.endsWith(".slf")) {
			throw new IllegalArgumentException(Errors.BAD_FILE_NAME_SUFFIX.getText());
		}
		return filename.substring(0,filename.length()-4);
	}

	/**
	 * Create a module id from a non-empty array of strings
	 * @param pieces non-empty array of (non-null) strings
	 */
	public ModuleId(String[] pieces) {
		if (pieces.length < 1) throw new IllegalArgumentException("Module ID cannot be empty");
		if (Arrays.asList(pieces).contains(null)) {
			throw new NullPointerException("null package component");
		}
		final int n = pieces.length-1;
		packageName = new String[n];
		for (int i=0; i < n; ++i) {
			packageName[i] = pieces[i];
		}
		moduleName = pieces[n];
		hash = computeHash();
	}
	
	public ModuleId(String[] p, String n) {
		if (Arrays.asList(p).contains(null) || n == null) {
			throw new NullPointerException("null package component or module");
		}
		packageName = p.clone();
		moduleName = n;
		hash = computeHash();
	}

	/**
	 * Computes the hash of this module id.
	 * @return module id hash
	 */
	private int computeHash() {
		return Arrays.hashCode(packageName) + moduleName.hashCode();
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals(Object x) {
		if (!(x instanceof ModuleId)) return false;
		ModuleId other = (ModuleId)x;
		if (other.hash != hash) return false;
		return Arrays.equals(packageName, other.packageName) && moduleName.equals(other.moduleName);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String p : packageName) {
			sb.append(p);
			sb.append('.');
		}
		sb.append(moduleName);
		return sb.toString();
	}

	public File asFile(File dir) {
		File result = dir;
		for (String p : packageName) {
			result = new File(result,p);
		}
		result = new File(result,moduleName+".slf");
		return result;
	}
}