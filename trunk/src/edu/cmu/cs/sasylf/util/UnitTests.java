package edu.cmu.cs.sasylf.util;


public class UnitTests extends SimpleTestSuite {

  public UnitTests() { }

  protected <T,U> void assertRelation(String description, T[] elem1, U[] elem2, boolean[][] related, Relation<T,U> relation) {
    int m = elem1.length;
    int n = elem2.length;
    for (int i=0; i < m; ++i) {
      T t1 = elem1[i];
      for (int j=0; j < n; ++j) {
        U t2 = elem2[j];
        assertEqual(description + "(" + t1 + "," + t2 + ")", related[i][j], relation.contains(t1, t2));
      }
    }
  }
  
  private void testTransitiveRelation() {
    TransitiveRelation<Integer> cubeLattice = new TransitiveRelation<Integer>(false);
    Integer[] members = new Integer[]{0,1,2,3,4,5,6,7};
    cubeLattice.put(0, 1);
    cubeLattice.put(4, 6);
    cubeLattice.put(2, 3);
    cubeLattice.put(5, 7);
    boolean X = true;
    boolean O = false;
    assertRelation("initial",members,members,
        new boolean[][]{
          new boolean[] { O, X, O, O, O, O, O, O},
          new boolean[] { O, O, O, O, O, O, O, O},
          new boolean[] { O, O, O, X, O, O, O, O},
          new boolean[] { O, O, O, O, O, O, O, O},
          new boolean[] { O, O, O, O, O, O, X, O},
          new boolean[] { O, O, O, O, O, O, O, X},
          new boolean[] { O, O, O, O, O, O, O, O},
          new boolean[] { O, O, O, O, O, O, O, O}}, cubeLattice);

    cubeLattice.put(0, 2);
    cubeLattice.put(1, 3);
    cubeLattice.put(3, 7);
    cubeLattice.put(6, 7);
    assertRelation("initial",members,members,
        new boolean[][]{
          new boolean[] { O, X, X, X, O, O, O, X},
          new boolean[] { O, O, O, X, O, O, O, X},
          new boolean[] { O, O, O, X, O, O, O, X},
          new boolean[] { O, O, O, O, O, O, O, X},
          new boolean[] { O, O, O, O, O, O, X, X},
          new boolean[] { O, O, O, O, O, O, O, X},
          new boolean[] { O, O, O, O, O, O, O, X},
          new boolean[] { O, O, O, O, O, O, O, O}}, cubeLattice);
    
    cubeLattice.put(0, 4);
    cubeLattice.put(1, 5);
    cubeLattice.put(2, 6);
    cubeLattice.put(4, 5);
    assertRelation("initial",members,members,
        new boolean[][]{
          new boolean[] { O, X, X, X, X, X, X, X},
          new boolean[] { O, O, O, X, O, X, O, X},
          new boolean[] { O, O, O, X, O, O, X, X},
          new boolean[] { O, O, O, O, O, O, O, X},
          new boolean[] { O, O, O, O, O, X, X, X},
          new boolean[] { O, O, O, O, O, O, O, X},
          new boolean[] { O, O, O, O, O, O, O, X},
          new boolean[] { O, O, O, O, O, O, O, O}}, cubeLattice);
    
    TransitiveRelation<Integer> cr = new TransitiveRelation<Integer>(true);
    assertRelation("empty reflexive",members,members,
        new boolean[][]{
        new boolean[] { X, O, O, O, O, O, O, O},
        new boolean[] { O, X, O, O, O, O, O, O},
        new boolean[] { O, O, X, O, O, O, O, O},
        new boolean[] { O, O, O, X, O, O, O, O},
        new boolean[] { O, O, O, O, X, O, O, O},
        new boolean[] { O, O, O, O, O, X, O, O},
        new boolean[] { O, O, O, O, O, O, X, O},
        new boolean[] { O, O, O, O, O, O, O, X}}, cr);
    
    cr.putAll(cubeLattice);
    assertRelation("initial",members,members,
        new boolean[][]{
          new boolean[] { X, X, X, X, X, X, X, X},
          new boolean[] { O, X, O, X, O, X, O, X},
          new boolean[] { O, O, X, X, O, O, X, X},
          new boolean[] { O, O, O, X, O, O, O, X},
          new boolean[] { O, O, O, O, X, X, X, X},
          new boolean[] { O, O, O, O, O, X, O, X},
          new boolean[] { O, O, O, O, O, O, X, X},
          new boolean[] { O, O, O, O, O, O, O, X}}, cr);
        
  }
  
  @Override
  protected void runTests() {
    testTransitiveRelation();
  }
  
  public static void main(String[] args) {
    new UnitTests().run();
  }

}
