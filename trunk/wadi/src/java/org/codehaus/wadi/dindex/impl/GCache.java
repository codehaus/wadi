package org.codehaus.wadi.dindex.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.cache.Cache;
import javax.cache.CacheEntry;
import javax.cache.CacheException;
import javax.cache.CacheListener;
import javax.cache.CacheStatistics;



/**
 * Geronimo is going to need a standard API for lookup of sessions across the Cluster.
 * JCache is the obvious choice.
 * This will allow the plugging of either e.g. GCache (WADI), Tangosol's Coherence or IBMs solution without changing of Geronimo code.
 * In fact, this will allow WADI to sit on top of any of these three.
 * 
 * GCache is a JCache compatible interface onto DIndex - WADI's own distributed index, which fulfills
 * WADI's requirements for this lookup...
 * 
 * @author jules
 *
 */
public class GCache implements Cache {

  /*
   * second pass
   */
  public boolean containsKey(Object key) {
	  throw new UnsupportedOperationException();
  }

  /*
   * third pass
   */
  public boolean containsValue(Object value) {
	  throw new UnsupportedOperationException();
  }

  /*
   * third pass
   */
  public Set entrySet() {
	  throw new UnsupportedOperationException();
  }

  /*
   * second pass
   */
  public boolean isEmpty() {
	  throw new UnsupportedOperationException();
  }

  /*
   * second pass
   */
  public Set keySet() {
	  throw new UnsupportedOperationException();
  }

  /*
   * first pass
   */
  public void putAll(Map t) {
    // TODO Auto-generated method stub
  }

  /*
   * first pass
   */
  public int size() {
	  return getCacheStatistics().getObjectCount();
  }

  /*
   * third pass
   */
  public Collection values() {
	  throw new UnsupportedOperationException();
  }

  /*
   * first pass
   */
  public Object get(Object key) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * first/second pass
   */
  public Map getAll(Collection keys) throws CacheException {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * second pass ?
   */
  public void load(Object key) throws CacheException {
	  throw new UnsupportedOperationException();
  }

  /*
   * second pass ?
   */
  public void loadAll(Collection keys) throws CacheException {
	  throw new UnsupportedOperationException();
  }

  /*
   * first pass ?
   */
  public Object peek(Object key) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * first pass ?
   */
  public Object put(Object key, Object value) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * first pass ?
   * interesting - perhaps this is how we make location accessible
   */
  public CacheEntry getCacheEntry(Object key) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * not sure ?
   */
  public CacheStatistics getCacheStatistics() {
    // TODO Auto-generated method stub
    return null;
    
    // needs to return :
    // objectCount
    // hits
    // misses
    
    // we will do best effort on all of these
    // they can be included in each node's distributed state and aggregated on demand
  }

  /*
   * first pass
   */
  public Object remove(Object key) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * third pass
   */
  public void clear() {
	  throw new UnsupportedOperationException();
  }

  /*
   * first pass ?
   */
  public void evict() {
    // TODO Auto-generated method stub
  }

  /*
   * second/third pass
   */
  public void addListener(CacheListener listener) {
	  throw new UnsupportedOperationException();
  }

  /*
   * second/third pass
   */
  public void removeListener(CacheListener listener) {
	  throw new UnsupportedOperationException();
  }

}
