package edu.cmu.cs.sasylf.spine;

public class TypeConstant extends TypeHead {
	public TypeConstant(Kind k) { kind = k; }
	
	public final Kind kind;
	
	@Override
	public Kind getKind(Context ctx) {
		return kind;
	}

}
