
/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.codehaus.wadi.sandbox.jcache;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.cache.Cache;
import javax.cache.CacheEntry;
import javax.cache.CacheException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.StreamingStrategy;

//----------------------------------------

// TODO

// figure out how sessions should be timed out and demoted to next
// cache (Dummy or e.g. DataBase).

// when cache starts it should loadAll from its dir and from its
// backing cache (i.e. D.B.)

// sort out touching, versioning and lastModified ttl seems to be on a
// per-cache basis here, whereas we need it on a per-entry basis.. -
// extend Cache API ? or just ignore ttl and take via extended Value
// interface...

// what metadata should live in the CacheEntry and what in the Value ?

// collapse all timing fields together as calculated from each other

//----------------------------------------

public abstract class
  AbstractCacheInnards
  implements CacheInnards
{
  protected final Log _log=LogFactory.getLog(getClass());

  protected final SerializableContentPool _valuePool;
  protected final StreamingStrategy       _streamingStrategy;
  protected final Cache                   _cache;
  protected final EvictionPolicy          _evictionPolicy;

  //----------------------------------------

  public
    AbstractCacheInnards(SerializableContentPool valuePool, StreamingStrategy streamingStrategy, Cache cache, EvictionPolicy evictionPolicy)
  {
    assert valuePool!=null;
    assert streamingStrategy!=null;
    assert cache!=null;
    assert evictionPolicy!=null;

    _valuePool         =valuePool;
    _streamingStrategy =streamingStrategy;
    _cache             =cache;
    _evictionPolicy    =evictionPolicy;
  }

  //----------------------------------------
  // CacheLoader

  public Object
    load(Object key)
    throws CacheException
  {
    Object value=loadValue(key);

    if (value==null)
      _log.warn("load failed: "+key);
    else
      _log.info("loaded: "+key+" : "+value);

    return value;
  }

  public Map
    loadAll(Collection keys)
    throws CacheException
  {
    // do we throw an exception if we fail to get one item, or if we
    // fail to get any ?

    Map map=new HashMap(keys.size());
    for (Iterator i=keys.iterator(); i.hasNext();)
    {
      Object key=i.next();
      map.put(key, load(key));
    }

    if (map.size()==0)
      _log.warn("loadAll failed: "+keys);
    else
      _log.info("loadAll: "+map);

    return map;
  }

  //----------------------------------------
  // EvictionPolicy

  public CacheEntry
    createEntry(Object key, Object value, long ttl)
  {
    _log.info("createEntry");
    return new _CacheEntry(key, value, ttl);	// Pool these ?
  }

  public void
    discardEntry(CacheEntry e)
  {
    _log.info("discardEntry: "+e);
    removeValue(e.getKey());
  }

  public void
    touchEntry(CacheEntry entry)
  {
    _log.info("touchEntry");
    // update ttl on file ?
  }

  public void
    clear()
  {
    _log.info("clear");
    // could remove files one by one - or whole directory ?
  }

  public Map
    evict(Cache c)
  {
    _log.info("evict: "+c);

    Map evictees=new HashMap();	// TODO - ouch - Maps are expensive - cache ?

    // decide who to evict...
    for (Iterator i=c.values().iterator(); i.hasNext();)
    {
      CacheEntry ce=(CacheEntry)i.next();
      if (_evictionPolicy.evict(ce))
	evictees.put(ce.getKey(), ce.getValue()); // TODO - ouch - do we have to load the value ?
    }

    return evictees;
  }

  //----------------------------------------
  // CacheEntry - a File on local disc

  class
    _CacheEntry
    implements CacheEntry
  {
    protected long    _cost;
    protected long    _creationTime;
    protected long    _expirationTime;
    protected int     _hits;
    protected long    _lastAccessedTime;
    protected long    _lastUpdateTime;
    protected long    _version;
    protected boolean _valid=true;
    protected Object  _key;
    protected long    _ttl;

    _CacheEntry(Object key, Object value, long ttl)
    {
      addValue(_key=key, value);
      _ttl=ttl;
    }

    // CacheEntry

    public long
      getCost()
    {
      _log.info("getCost");
      return _cost;
    }

    public long
      getCreationTime()
    {
      _log.info("getCreationTime");
      return _creationTime;
    }

    public long
      getExpirationTime()
    {
      _log.info("getExpirationTime");
      return _expirationTime;
    }

    public int
      getHits()
    {
      _log.info("getHits");
      return _hits;
    }

    public long
      getLastAccessTime()
    {
      _log.info("getLastAccessTime");
      return _lastAccessedTime;
    }

    public long
      getLastUpdateTime()
    {
      _log.info("getLastUpdateTime");
      return _lastUpdateTime;
    }

    public long
      getVersion()
    {
      _log.info("getVersion");
      return _version;
    }

    public boolean
      isValid()
    {
      _log.info("isValid");
      return _valid;
    }

    public Object getKey(){return _key;}

    public Object
      getValue()
    {
      return loadValue(_key);
    }

    public Object
      setValue(Object value)
    {
      Object oldValue=loadValue(_key); // TODO - optimisation - will this ever be used ?
      storeValue(_key, value);
      return oldValue;
    }

    public String
      toString()
    {
      return "<"+getClass().getName()+": "+_key+">";
    }

    public int
      hashCode()
    {
      _log.info("hashCode");
      throw new UnsupportedOperationException();
    }

    public boolean
      equals()
    {
      _log.info("equals");
      throw new UnsupportedOperationException();
    }
  }

  //----------------------------------------
  // internals

  public abstract String toString();
  protected abstract Object loadValue(Object key);
  protected abstract boolean storeValue(Object key, Object value);
  protected abstract boolean addValue(Object key, Object value);
  protected abstract boolean removeValue(Object key);
}
