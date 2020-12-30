package edu.cmu.cs.sasylf.reduction;

import edu.cmu.cs.sasylf.util.SimpleTestSuite;

public class UnitTests extends SimpleTestSuite {

	public UnitTests() { }

	private void testEquals(InductionSchema s1, InductionSchema s2, boolean m) {
		assertTrue("Expected " + s1 + ".equals(" + s2 + ") = " + m + ", but it wasn't.",
				s1.equals(s2) == m);
	}
	
	private void testMatch(InductionSchema s1, InductionSchema s2, boolean m) {
		assertTrue("Expected " + s1 + ".match(" + s2 + ") = " + m + ", but it wasn't.",
				s1.matches(s2, null, false) == m);
	}
	
	
	private void doMatchTests() {
		InductionSchema s0 = new StructuralInduction(0);
		InductionSchema s1 = new StructuralInduction(1);
		InductionSchema s2 = new StructuralInduction(2);
		testEquals(s0,s0,true);  testMatch(s0,s0,true); 
		testEquals(s0,s1,false); testMatch(s0,s1,true);
		testEquals(s0,s2,false); testMatch(s0,s2,true);
		
		InductionSchema l0 = LexicographicOrder.create(s0);
		InductionSchema l1 = LexicographicOrder.create(s1);
		testEquals(s0,l0,true);  testMatch(s0,l0,true);
		testEquals(l0,l1,false); testMatch(l0,l1,true);
		
		InductionSchema l2 = LexicographicOrder.create(s0,s1);
		InductionSchema l3 = LexicographicOrder.create(s1,s2);
		InductionSchema l4 = LexicographicOrder.create(s1,s1);
		InductionSchema l5 = LexicographicOrder.create(s2,s1);
		InductionSchema l6 = LexicographicOrder.create(s0,s1);
		
		testEquals(s1,l4,false); testMatch(s1,l4,false);
		testEquals(l1,l4,false); testMatch(l1,l4,false);
		
		testEquals(l2,l2,true);  testMatch(l2,l2,true);
		testEquals(l2,l3,false); testMatch(l2,l3,true);
		testEquals(l2,l4,false); testMatch(l2,l4,true);
		testEquals(l2,l5,false); testMatch(l2,l5,true);
		testEquals(l2,l6,true);  testMatch(l2,l6,true);
		testEquals(l3,l3,true);  testMatch(l3,l3,true);
		testEquals(l3,l4,false); testMatch(l3,l4,true);
		testEquals(l3,l5,false); testMatch(l3,l5,true);
		testEquals(l3,l6,false); testMatch(l3,l6,true);
		testEquals(l4,l4,true);  testMatch(l4,l4,true);
		testEquals(l4,l5,false); testMatch(l4,l5,true);
		testEquals(l4,l6,false); testMatch(l4,l6,true);
		testEquals(l5,l5,true);  testMatch(l5,l5,true);
		testEquals(l5,l6,false); testMatch(l5,l6,true);
		
		InductionSchema l7 = LexicographicOrder.create(s0,s1,s2);
		InductionSchema l8 = LexicographicOrder.create(s2,s1,s0);
		InductionSchema l9 = LexicographicOrder.create(
				new StructuralInduction(0),
				new StructuralInduction(1),
				new StructuralInduction(2));
		
		testEquals(l7,l2,false); testMatch(l7,l2,false);
		testEquals(l3,l7,false); testMatch(l3,l7,false);
		testEquals(l7,l8,false); testMatch(l7,l8,true);
		testEquals(l7,l9,true);  testMatch(l7,l9,true);
		testEquals(l8,l9,false); testMatch(l8,l9,true);
		
		InductionSchema u0 = Unordered.create(null, s0);
		InductionSchema u1 = Unordered.create(null, s1);
		testEquals(s0,u0,true);  testMatch(s0,u0,true);
		testEquals(u0,u1,false); testMatch(u0,u1,true);
		testEquals(u1,s1,true);  testMatch(u1,s1,true);
		
		InductionSchema u2 = Unordered.create(null,s0, s1);
		InductionSchema u3 = Unordered.create(null,s1, s2);
		InductionSchema u4 = Unordered.create(null,s1, s1);
		InductionSchema u5 = Unordered.create(null,s2, s1);
		InductionSchema u6 = Unordered.create(null,s0, s1);
		
		testEquals(l2,u2,false); testMatch(l2,u2,false);
		testEquals(l3,u3,false); testMatch(l3,u3,false);
		testEquals(l4,u4,false); testMatch(l4,u4,false);
		
		testEquals(u4,s1,false); testMatch(u4,s1,false);
		testEquals(u1,u4,false); testMatch(u1,u4,false);
		
		testEquals(u2,u2,true);  testMatch(u2,u2,true);
		testEquals(u2,u3,false); testMatch(u2,u3,true);
		testEquals(u2,u4,false); testMatch(u2,u4,true);
		testEquals(u2,u5,false); testMatch(u2,u5,true);
		testEquals(u2,u6,true);  testMatch(u2,u6,true);
		testEquals(u3,u3,true);  testMatch(u3,u3,true);
		testEquals(u3,u4,false); testMatch(u3,u4,true);
		testEquals(u3,u5,true);  testMatch(u3,u5,true);
		testEquals(u3,u6,false); testMatch(u3,u6,true);
		testEquals(u4,u4,true);  testMatch(u4,u4,true);
		testEquals(u4,u5,false); testMatch(u4,u5,true);
		testEquals(u4,u6,false); testMatch(u4,u6,true);
		testEquals(u5,u5,true);  testMatch(u5,u5,true);
		testEquals(u5,u6,false); testMatch(u5,u6,true);

		InductionSchema u7 = Unordered.create(null,s0,s1, s2);
		InductionSchema u8 = Unordered.create(null,s1,s1, s1);
		InductionSchema u9 = Unordered.create(
				null,
				new StructuralInduction(2),
				new StructuralInduction(1), new StructuralInduction(0));
		
		testEquals(u7,u2,false); testMatch(u7,u2,false);
		testEquals(u3,u7,false); testMatch(u3,u7,false);
		testEquals(u7,u8,false); testMatch(u7,u8,true);
		testEquals(u7,u9,true);  testMatch(u7,u9,true);
		testEquals(u8,u9,false); testMatch(u8,u9,true);
		
		testEquals(u4,u8,false); testMatch(u8,u4,false);
		
		InductionSchema c1 = LexicographicOrder.create(s0,u3);
		InductionSchema c2 = LexicographicOrder.create(u3,s0);
		InductionSchema c3 = LexicographicOrder.create(s0,u5);
		InductionSchema c4 = LexicographicOrder.create(s2,u2);
		
		testEquals(c1,c1,true);  testMatch(c1,c1,true);
		testEquals(c1,c2,false); testMatch(c1,c2,false);
		testEquals(c1,c3,true);  testMatch(c1,c3,true);
		testEquals(c1,c4,false); testMatch(c1,c4,true);
		testEquals(c2,c2,true);  testMatch(c2,c2,true);
		testEquals(c2,c3,false); testMatch(c2,c3,false);
		testEquals(c2,c4,false); testMatch(c2,c4,false);
		testEquals(c3,c3,true);  testMatch(c3,c3,true);
		testEquals(c3,c4,false); testMatch(c3,c4,true);
		testEquals(c4,c4,true);  testMatch(c4,c4,true);
		
		
}
	
	@Override
	protected void runTests() {
		doMatchTests();
	}

	public static void main(String[] args) {
		new UnitTests().run();
	}

}
