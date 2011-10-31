package edu.cmu.cs.sasylf.spine;

public class Function extends Term {
	public Function(Type t, Term b) { varType = t; body = b; }
	
	public final Type varType;
	public final Term body;

	@Override
	public Type getType(Context ctx) {
		// TODO Auto-generated method stub
		return null;
	}

}
