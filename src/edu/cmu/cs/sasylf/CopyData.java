package edu.cmu.cs.sasylf;

import java.util.IdentityHashMap;
import java.util.Map;

/** CloneData stores a mapping between objects and their clones, and has
 * methods for querying and updating the mapping.
 * 
 * <br/><br/>
 * 
 * The algorithm used for cloning objects is called hashconsing. It works
 * by visiting each node in the AST (really a graph), and creating a clone of each object.
 * 
 * <br/><br/>
 * Since the AST is likely a cyclic graph, we keep track of which objects have already
 * been cloned. If an object has already been cloned, we return the clone, and if it hasn't
 * we create a new clone and add it to the mapping.
 * 
 * <br/><br/>
 * 
 * The result is a deep copy of the AST, where each object (by reference) is cloned exactly once.
 */
public class CopyData {
  private final Map<Object, Object> copyMap; // maps objects to their clones

  public CopyData() {
    /*
      We use IdentityHashMap because two objects should become the same clone
      if and only if they are the same object.
    */
    copyMap = new IdentityHashMap<>();
  }

  /**
   * 
   * @param clonedObject The object that is being cloned
   * @param theClone The clone of <code> clonedObject </code>
   */
  public void addCopyFor(Object clonedObject, Object theClone) {
    copyMap.put(clonedObject, theClone);
  }

  /**
   * Returns true if a clone already exists in the mapping for <code> o </code>, and false otherwise.
   * @param o The object to check if a clone already exists for
   * @return Whether a clone already exists for <code> o </code>
   */
  public boolean containsCopyFor(Object o) {
    return copyMap.containsKey(o);
  }

  /**
   * Gets the clone for <code> o </code> if it exists, and null otherwise.
   * @param o
   * @return The clone for <code> o </code> if it exists, and null otherwise
   */
  public Object getCopyFor(Object o) {
    return copyMap.get(o);
  }

}
