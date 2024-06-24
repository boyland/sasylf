package edu.cmu.cs.sasylf;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class CloneData {
  private final Map<Object, Object> cloneMap;

  public CloneData() {
    cloneMap = new IdentityHashMap<>();
  }

  public void addCloneFor(Object key, Object value) {
    cloneMap.put(key, value);
  }

  public boolean containsCloneFor(Object key) {
    return cloneMap.containsKey(key);
  }

  public Object getCloneFor(Object key) {
    return cloneMap.get(key);
  }

  public Map<Object, Object> getMap() {
    return cloneMap;
  }

}
