
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.cache.Cache;
import javax.cache.CacheEntry;
import javax.cache.CacheException;
import javax.cache.CacheLoader;
import javax.cache.EvictionStrategy;
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

//----------------------------------------

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

  class
    Value
    implements SerializableContent
  {
    protected String _value;

    Value(){}

    Value(String value)
    {
      _value=value;
    }

    public String
      getValue()
    {
      return _value;
    }

    public String
      toString()
    {
      return "<"+getClass().getName()+":"+_value+">";
    }

    public void
      readContent(ObjectInput is)
      throws IOException, ClassNotFoundException
    {
      _value=(String)(is.readObject());
    }

    public void
      writeContent(ObjectOutput os)
      throws IOException, ClassNotFoundException
    {
      os.writeObject(_value);
    }
  }

  // modelled on Doug Lea's Takable/Channel
  class
    Pool
    implements SerializableContentPool
  {
    public SerializableContent poll(long msecs){return new Value();}
    public SerializableContent take(){return new Value();}
  }

  class
    NoEvictionPolicy
    implements EvictionPolicy
  {
    public boolean evict(CacheEntry ce){return false;}
  }

  public void
    testLocalDiscCache()
    throws Exception
  {
    boolean success=false;

    try
    {
      File dir=new File("/tmp");
      Cache shared=new BasicCache();
      CacheInnards ci=new LocalDiscCacheInnards(dir, new Pool(), new SimpleStreamingStrategy(), shared, new NoEvictionPolicy());
      Cache ld=new BasicCache(ci, ci);

      String key="key-1";
      Value val=new Value("value-1");

      ld.put(key, val);
      _log.info(ld.get(key));
      ld.remove(key);

      ld.evict();
      ld.get("key-2");

      success=true;
    }
    catch (Exception e)
    {
      _log.error("test failed", e);
    }

    assertTrue(success);
  }
}
