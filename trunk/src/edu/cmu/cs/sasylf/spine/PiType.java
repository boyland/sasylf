package edu.cmu.cs.sasylf.spine;

public class PiType extends Type {
	public PiType(Type a, Type b) {
		this.argType = a; this.body = b;
	}
	
	public final Type argType;
	public final Type body;
	
	@Override
	public Kind getKind(Context ctx) {
		// TODO Auto-generated method stub
		return null;
	}
}
