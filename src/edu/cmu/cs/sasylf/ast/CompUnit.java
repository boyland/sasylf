package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.ParseUtil;
import edu.cmu.cs.sasylf.util.SASyLFError;


public class CompUnit extends Node {
	public CompUnit(PackageDeclaration pack, Location loc, String n, Set<String> terms, List<Syntax> s, List<Judgment> j, List<Theorem> t) {
		super(loc);
		packageDecl=pack; 
		moduleName = n; 
		part.declaredTerminals = terms; 
		part.syntax=s; part.judgments=j; 
		part.theorems = t; 
	}

	public PackageDeclaration getPackage() { return packageDecl; }

	private PackageDeclaration packageDecl;
	private String moduleName;
	Chunk part = new Chunk();
	
	public Chunk getPart() { return part; }

	@Override
	public void prettyPrint(PrintWriter out) {
		packageDecl.prettyPrint(out);

		if (moduleName != null) {
			out.println("module " + moduleName);
		}

		part.prettyPrint(out);

		out.flush();
	}

	/**
	 * Type check this compilation unit in the default module context.
	 * @return
	 */
	public boolean typecheck() {
		return typecheck(new NullModuleFinder(),(ModuleId)null);  
	}
	
	/** typechecks this compilation unit, returning true if the check was successful,
	 * false if there were one or more errors.
	 */
	public boolean typecheck(ModuleFinder mf, ModuleId id) {
		ErrorHandler.recordLastSpan(this);
		int oldCount = ErrorHandler.getErrorCount();
		Context ctx = new Context(mf,this);
		try {
			typecheck(ctx,id);
		} catch (SASyLFError e) {
			// ignore the error; it has already been reported
			//e.printStackTrace();
		}
		return ErrorHandler.getErrorCount() == oldCount;
	}

	/**
	 * Check this compilation unit in the given context.
	 * @param ctx context, must not be null
	 * @param id identifier declared for this compilation unit, or null if no declared module name
	 */
	public void typecheck(Context ctx, ModuleId id) {
		if (id != null) checkFilename(id);
		part.typecheck(ctx);
	}
	
	private void checkFilename(ModuleId id) {
		packageDecl.typecheck(id.packageName);

		if (moduleName != null) {
			if (!ParseUtil.isLegalIdentifier(id.moduleName)) {
				ErrorHandler.report(Errors.BAD_FILE_NAME,this);
			}
			if (!moduleName.equals(id.moduleName)) {
				ErrorHandler.warning(Errors.WRONG_MODULE_NAME, this, moduleName+"\n"+id.moduleName);
			}
		}
	}
	
	/**
	 * Put all top-level declarations is this compilation unit (module)
	 * into the collection
	 * @param things collection to add to.
	 */
	public void collectTopLevel(Collection<? super Node> things) {
		part.collectTopLevel(things);
	}
	
	public void collectRuleLike(Map<String,? super RuleLike> map) {
		part.collectRuleLike(map);
	}
}
