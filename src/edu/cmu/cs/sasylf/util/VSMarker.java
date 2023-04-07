package edu.cmu.cs.sasylf.util;

import java.util.HashMap;

public class VSMarker {
  private HashMap<String, Object> data;

  public VSMarker() { data = new HashMap<String, Object>(); }

  public void setAttribute(String key, Object value) { data.put(key, value); }

  public Object getAttribute(String key) { return data.get(key); }

  public Object getAttribute(String key, Object dft) {
    return data.getOrDefault(key, dft);
  }
}
