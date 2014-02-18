package edu.cmu.cs.sasylf.spine;

import java.util.List;

public class TypeApplication extends Type {
	TypeApplication(TypeHead h, List<Term> s) { head = h; spine = s; }
	
	public final TypeHead head;
	public final List<Term> spine;

	@Override
	public Kind getKind(Context ctx) {
		// TODO Auto-generated method stub
		return null;
	}

}
