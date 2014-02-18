package edu.cmu.cs.sasylf.util;

import java.util.*;

public class Relation<T1, T2> {
	private Map<T1,Set<T2>> forwardMap = new HashMap<T1,Set<T2>>();
	private Map<T2,Set<T1>> reverseMap = new HashMap<T2,Set<T1>>();
	
	public Set<T2> getAll(T1 key) {
		return getSet(forwardMap, key);//forwardMap.get(key);
	}

	public Set<T1> getAllReverse(T2 key) {
		return getSet(reverseMap, key);
		//return reverseMap.get(key);
	}
	
	public boolean contains(T1 key, T2 value) {
		Set<T2> set = forwardMap.get(key);
		return set != null && set.contains(value);
	}
	
	public boolean put(T1 key, T2 value) {
		Set<T2> valueSet = getSet(forwardMap, key);
		boolean result = valueSet.add(value);
		if (result) {
			Set<T1> keySet = getSet(reverseMap, value);
			keySet.add(key);
		}
		return result;
	}

	private <Ta,Tb> Set<Tb> getSet(Map<Ta, Set<Tb>> map, Ta key) {
		Set<Tb> set = map.get(key);
		if (set == null) {
			set = new HashSet<Tb>();
			map.put(key, set);
		}
		return set;
	}
}
