
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

package org.codehaus.wadi.test;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheLoader;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.SerializableContent;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.impl.SimpleStreamingStrategy;
import ri.cache.BasicCache;
import ri.cache.eviction.LRUChainEvictionStrategy;
import ri.cache.eviction.NullEvictionStrategy;
import ri.cache.loader.AbstractCacheLoader;
import ri.cache.loader.FromCacheLoader;
import ri.cache.loader.NullCacheLoader;


// WADI should use a JSR107 compatible API for its session Map

// InVM Cache - in memory map of session-id:session
// LocalStore Cache - evicted sessions move to an EvictionStore (e.g. spooled to Disc)
// SharedStore Cache - When there are less than the required number of copies i.e. DataBase...
// Distributed Tier - a Session may be 'owned' by another node - ownership may be changed by 'migration'
// Replicated Tier -A Session may be replicated to allow redundant copies to be held off-node

// is it local ?
// has it been evicted ?
// can it be migrated ?
// can it be restored from a replicated copy ?

// N.B.

// If the node (n1) owning 'xyz' fails and a request for 'xyz' falls
// on another node (n2) what happens ?

// notification of n1's death may not have reached everybody, so no-one
// will have had time to adopt n's orphans.

// n2 must local n1's buddies. everyone must agree that n1 has
// died. n2 may then adopt ownership of 'xyz'.

// what happens to remaining orphanned sessions ?

// they must be immediately adopted (i.e. pushed up into higher tiers
// of cache) since this will improve cluster performance and they are
// at risk if only held by (n-1) buddies where n is the lowest safe
// number of copies specified by the admin.

// so n2 may adopt a slice of n1's sessions and other nodes may adopt
// the rest...

// if n1 was buddying for anyone, that node will be responsible for
// recruiting a new buddy and synching them with the partition's
// state.

// This all needs to be backed by a LARGE, SHARED store. When no node
// in the cluster is running, sessions should be persisted to this
// store...

// last-one-out-turns-out-the-lights might be good enough, provided
// that the first/last node does not crash... (crashing is a
// kill-9-level concern, so replication should protect against this -
// how?).

// first node up (n1) should promote a COPY of shared store to it's own
// local store.

// n2 should join n1's buddy group and replicate its VM and Local tiers...

// etc... until n is reached, when SharedTier may be emptied.

// if all nodes die before n is reached, new n1 will reread from
// SharedTier...

// as node numbers fall below n - what happens ? does everyone evict
// and replicate direct to shared store ?

// perhaps the sharedStore tier could maintain a ref-count on each
// item, so it knows when it is safe and can be promoted/removed ?
// more thought needed...

public class
  TestJCache
  extends TestCase
{
  protected Log _log=LogFactory.getLog(TestJCache.class);

  public TestJCache(String name) {super(name);}

  protected void
    setUp()
    throws Exception
    {
      _log.info("starting test");
    }

  protected void
    tearDown()
    throws InterruptedException
    {
      _log.info("stopping test");
    }

  public interface
    FileCacheable		// needs a better name
    extends SerializableContent
  {
    String getId();
  }

  // should probably expect Cache values to be e.g. FileCacheable -
  // have a uid and serialisable content.

  // assumption - we are the only people reading and writing to this
  // directory... - could be dynamically made up...

  // if we know the cache is homogeneous (only stores one type), we do
  // not need to write type information into every evicted file...
  static class
    HomogeneousLocalDiscCacheLoader
    implements CacheLoader
  {
    protected Log _log=LogFactory.getLog(HomogeneousLocalDiscCacheLoader.class);

    protected File              _dir;
    protected FileCacheablePool _entryPool;
    protected StreamingStrategy _streamingStrategy;
    protected Cache             _cache;

    public
      HomogeneousLocalDiscCacheLoader(File dir, FileCacheablePool entryPool, StreamingStrategy streamingStrategy, Cache cache)
    {
      assert dir.exists();
      assert dir.isDirectory();
      assert dir.canRead();
      assert dir.canWrite();
      assert entryPool!=null;
      assert streamingStrategy!=null;
      assert cache!=null;

      _dir               =dir;
      _entryPool         =entryPool;
      _streamingStrategy =streamingStrategy;
      _cache             =cache;

      _log.info("created: "+this);
    }

    public Object
      load(Object key)
      throws CacheException
    {
      Object entry=null;

      File file=new File(_dir, key.toString()+_streamingStrategy.getSuffix()); // need to know UID here...
      if (file.exists())
      {
	try
	{
	  FileCacheable fc=_entryPool.take();
	  ObjectInput oi=_streamingStrategy.getInputStream(new FileInputStream(file));
	  fc.readContent(oi);
	  oi.close();

	  entry=fc;		// data WILL be returned
	  file.delete();	// even if file removal fails...

	  _log.info("loaded from local disc: "+key+":"+entry);
	}
	catch (Exception e)
	{
	  _log.error("cache load failure", e);
	}
      }
      else
      {
	entry=_cache.get(key);
	_log.info("loaded from cache: "+key+":"+entry);
      }

      return entry;
    }

    public Map
      loadAll(Collection keys)
      throws CacheException
    {
      _log.info("loadAll: "+keys);

      // do we throw an exception if we fail to get one item, or if we
      // fail to get any ?

      Map map=new HashMap(keys.size());
      for (Iterator i=keys.iterator(); i.hasNext();)
      {
	Object key=i.next();
	map.put(key, load(key));
      }

      return map;
    }

    public String
      toString()
    {
      return "<HomogeneousLocalDiscCacheLoader:"+_dir+", "+_entryPool+", "+_cache+">";
    }

  }

  class
    Entry
    implements FileCacheable
  {
    protected String _id;

    Entry(String id)
    {
      _id=id;
    }

    public String
      getId()
    {
      return _id;
    }

    public void
      readContent(ObjectInput is)
      throws IOException, ClassNotFoundException
    {
      _id=(String)(is.readObject());
    }

    public void
      writeContent(ObjectOutput os)
      throws IOException, ClassNotFoundException
    {
      os.writeObject(_id);
    }
  }

  class
    FileCacheablePool
  {
    int _counter=0;
    FileCacheable poll(long msecs){return new Entry(""+_counter++);}
    FileCacheable take(){return new Entry(""+_counter++);}
  }

  public void
    testLocalDiscCache()
    throws Exception
  {
    File dir=new File("/tmp");
    Cache shared=new BasicCache();
    CacheLoader localDisc=new HomogeneousLocalDiscCacheLoader(dir, new FileCacheablePool(), new SimpleStreamingStrategy(), shared);
    Cache local=new BasicCache(localDisc);

    local.put("0", "a");
    local.get("0");
    local.get("1");
  }
}
