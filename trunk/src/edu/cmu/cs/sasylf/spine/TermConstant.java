package edu.cmu.cs.sasylf.spine;

public class TermConstant extends Head {
	public TermConstant(Type t) { type = t; }
	
	public final Type type;
	
	@Override
	public Type getType(Context ctx) {
		return type;
	}

}
