package edu.cmu.cs.sasylf.ast;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.sasylf.util.ErrorHandler;
import edu.cmu.cs.sasylf.util.Errors;
import edu.cmu.cs.sasylf.util.Location;
import edu.cmu.cs.sasylf.util.ParseUtil;
import edu.cmu.cs.sasylf.util.SASyLFError;


public class CompUnit extends Node implements Module {
	public CompUnit(PackageDeclaration pack, Location loc, String n) {
		super(loc);
		packageDecl=pack; 
		moduleName = n; 
	}
	
	/**
	 * Add a chunk that is required by this module.
	 * @param c part to add, must not be null
	 */
	public void addParameterChunk(Part c) {
		parts.add(c); //! We need to separate required from provides in a module system.
	}
	
	/**
	 * Add a part to this compilation unit.
	 * @param c
	 */
	public void addChunk(Part c) {
		parts.add(c);
	}

	public PackageDeclaration getPackage() { return packageDecl; }

	/* (non-Javadoc)
	 * @see edu.cmu.cs.sasylf.ast.Module#getName()
	 */
	@Override
	public String getName() { return moduleName; }
	
	private PackageDeclaration packageDecl;
	private String moduleName;
	private List<Part> parts = new ArrayList<Part>();
	
	/* (non-Javadoc)
	 * @see edu.cmu.cs.sasylf.ast.Module#prettyPrint(java.io.PrintWriter)
	 */
	@Override
	public void prettyPrint(PrintWriter out) {
		packageDecl.prettyPrint(out);

		if (moduleName != null) {
			out.println("module " + moduleName);
		}

		for (Part part : parts) {
			part.prettyPrint(out);
		}

		out.flush();
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.sasylf.ast.Module#typecheck()
	 */
	@Override
	public boolean typecheck() {
		return typecheck(new NullModuleFinder(),(ModuleId)null);  
	}
	
	/** Typechecks this compilation unit, returning true if the check was successful,
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
		for (Part part : parts) {
			part.typecheck(ctx);
		}
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
	
	/* (non-Javadoc)
	 * @see edu.cmu.cs.sasylf.ast.Module#collectTopLevel(java.util.Collection)
	 */
	@Override
	public void collectTopLevel(Collection<? super Node> things) {
		for (Part part : parts) {
			part.collectTopLevel(things);
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.cmu.cs.sasylf.ast.Module#collectRuleLike(java.util.Map)
	 */
	@Override
	public void collectRuleLike(Map<String,? super RuleLike> map) {
		for (Part part : parts) {
			part.collectRuleLike(map);
		}
	}

	private Map<String,Object> declCache = new HashMap<String,Object>();
	private int cacheVersion = -1;
	
	@Override
	public Object getDeclaration(Context ctx, String name) {
		if (cacheVersion != ctx.version) {
			declCache.clear();
			Collection<Node> things = new ArrayList<Node>();
			this.collectTopLevel(things);
			this.collectRuleLike(declCache); // doesn't get syntax declarations or judgments
			for (Node n : things) {
				if (n instanceof SyntaxDeclaration) {
					SyntaxDeclaration sd = (SyntaxDeclaration)n;
					for (String s : sd.getAlternates()) {
						declCache.put(s, sd);
					}
				} else if (n instanceof Judgment) {
					Judgment jd = (Judgment)n;
					declCache.put(jd.getName(),jd);
				}
			}
			cacheVersion = ctx.version;
		}
		return declCache.get(name);
	}
}
