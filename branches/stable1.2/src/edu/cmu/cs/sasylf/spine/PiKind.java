package edu.cmu.cs.sasylf.spine;

public class PiKind extends Kind {
	public PiKind(Type a, Kind b) {
		this.argType = a; this.body = b;
	}
	
	public final Type argType;
	public final Kind body;
}
