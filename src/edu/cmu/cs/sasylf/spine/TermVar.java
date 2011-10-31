package edu.cmu.cs.sasylf.spine;

public class TermVar extends Head {
	public TermVar(int index) { this.index = index; }
	
	public final int index;
	
	@Override
	public Type getType(Context ctx) {
		return ctx.typeContext.get(index);
	}

}
