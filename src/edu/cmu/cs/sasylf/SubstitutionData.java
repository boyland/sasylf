package edu.cmu.cs.sasylf;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public class SubstitutionData {
  private final Map<Object, Boolean> set;
  public final String from;
  public final String to;

  public SubstitutionData(String from, String to) {
    set = new IdentityHashMap<>();
    this.from = from;
    this.to = to;
  }

  public boolean didSubstituteFor(Object o) {
    return set.containsKey(o);
  }

  public void setSubstitutedFor(Object o) {
    set.put(o, Boolean.TRUE);
  }
  
}
