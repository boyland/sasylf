package edu.cmu.cs.sasylf.util;

import java.util.*;

public class Relation<T1, T2> implements Iterable<Pair<T1,T2>> {
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

	public void putAll(Relation<T1,T2> relation) {
		/*
	  for (Map.Entry<T1,Set<T2>> forw : relation.forwardMap.entrySet()) {
	    getAll(forw.getKey()).addAll(forw.getValue());
	  }
    for (Map.Entry<T2,Set<T1>> back : relation.reverseMap.entrySet()) {
      getAllReverse(back.getKey()).addAll(back.getValue());
    }
		 */
		for (Pair<T1,T2> pair : relation) {
			put(pair.first,pair.second);
		}
	}

	private <Ta,Tb> Set<Tb> getSet(Map<Ta, Set<Tb>> map, Ta key) {
		Set<Tb> set = map.get(key);
		if (set == null) {
			set = makeInitialEdgeSet(key);
			map.put(key, set);
		}
		return set;
	}

	protected <Ta,Tb> Set<Ta> makeInitialEdgeSet(Tb key) {
		return new HashSet<Ta>();
	}

	private class PairIterator implements Iterator<Pair<T1,T2>> {
		private Iterator<Map.Entry<T1, Set<T2>>> iterator1 = forwardMap.entrySet().iterator();
		private Iterator<T2> iterator2 = null;
		private T1 currentKey1 = null;
		private T2 currentKey2 = null;

		@Override
		public boolean hasNext() {
			if (iterator2 != null && iterator2.hasNext()) return true;
			if (!iterator1.hasNext()) return false;
			Map.Entry<T1, Set<T2>> entry = iterator1.next();
			currentKey1 = entry.getKey();
			iterator2 = entry.getValue().iterator();
			return hasNext();
		}

		@Override
		public Pair<T1, T2> next() {
			if (!hasNext()) throw new NoSuchElementException("no more pairs in relation");
			currentKey2 = iterator2.next();
			return new Pair<T1, T2>(currentKey1, currentKey2);
		}

		@Override
		public void remove() {
			// Not hard to implement if we want it...
			throw new UnsupportedOperationException("remove not yet implemented for relation iterator");
		}
	}

	@Override
	public Iterator<Pair<T1, T2>> iterator() {
		return new PairIterator();
	}

}
