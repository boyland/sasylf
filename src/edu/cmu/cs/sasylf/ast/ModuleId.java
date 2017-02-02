package edu.cmu.cs.sasylf.ast;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;

public final class ModuleId {
	public final String[] packageName;
	public final String moduleName;
	private final int hash;

	public ModuleId(String filename) {
		if (!filename.endsWith(".slf")) {
			throw new IllegalArgumentException("SASyLF files must end in '.slf'");
		}
		filename = filename.substring(0,filename.length()-4);
		LinkedList<String> pkg = new LinkedList<String>();
		File f = new File(filename);
		for (;;) {
			String p = f.getParent();
			if (p == null) break;
			f = new File(p);
			pkg.addFirst(f.getName());
		}
		packageName = pkg.toArray(new String[pkg.size()]);
		moduleName = f.getName();
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
	 * @return
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