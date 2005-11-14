
/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
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

import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.sql.Connection;
import java.sql.Statement;
import javax.cache.Cache;
import javax.cache.CacheEntry;
import javax.sql.DataSource;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.axiondb.jdbc.AxionDataSource;
import org.codehaus.wadi.SerializableContent;
import org.codehaus.wadi.impl.SimpleStreamer;

import ri.cache.BasicCache;

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

// first node up (n1) should promote a COPY of shared store to its own
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

// shared vs. remote - how far down stack should we search for state...

// use serialisation adaptor rather than insisting state implements
// e.g. SerializableContent...

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
      if ( _log.isInfoEnabled() ) {

          _log.info("starting test");
      }
    }

  protected void
    tearDown()
    throws InterruptedException
    {
      if ( _log.isInfoEnabled() ) {

          _log.info("stopping test");
      }
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
      throws IOException
    {
      os.writeObject(_value);
    }

    public boolean
      equals(Object o)
    {
      if (o==this)
	return true;

      if (o==null || !(o instanceof Value))
	return false;

      Value that=(Value)o;

      return this._value.equals(that._value);
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

  protected void
    testJCache(Cache c, boolean byRef)
  {
    boolean success=false;

    try
    {
      String key="key-1";
      Value val=new Value("value-1");

      c.put(key, val);

      Value first=(Value)c.get(key);
      assertTrue(val.equals(first));
      Value second=(Value)c.get(key);
      assertTrue(val.equals(second));
      assertTrue(byRef?(first==second):(first.equals(second)));

      c.containsKey(key);
      c.containsKey(key);
      c.containsKey(key);
      c.containsKey(key);
      c.containsKey(key);
      c.containsKey(key);

//       c.remove(key);
//       assertTrue(c.get(key)==null);

//       c.evict();
//       c.get("key-2");

      success=true;
    }
    catch (Exception e)
    {
      _log.error("test failed", e);
    }

    assertTrue(success);
  }

  public void
    testRI()
  {
    Cache cache=new BasicCache();

    testJCache(cache, true);
  }

  public void
    testDisc()
    throws Exception
  {
    // ugly - is there a better way - createTempDir ?
    File tmp=File.createTempFile("TestJCache-", "", new File("/tmp"));
    String name=tmp.toString();
    tmp.delete();
    File dir=new File(name);
      if ( _log.isInfoEnabled() ) {

          _log.info("dir=" + dir);
      }
    assertTrue(dir.mkdirs());

    Cache backing=new BasicCache();
    CacheInnards ci=new LocalDiscCacheInnards(dir, new Pool(), new SimpleStreamer(), backing, new NoEvictionPolicy());
    Cache cache=new BasicCache(ci, ci);

    testJCache(cache, false);

    dir.delete();
  }

  // once we have a working DB-based cache we can back the file-based
  // on onto it, so expiring sessions are demoted to persistant
  // storage...
  public void
    testDB()
    throws Exception
  {
    boolean success=false;

    try
    {
      DataSource ds=new AxionDataSource("jdbc:axiondb:testdb");	// db springs into existance in-vm beneath us
      String table="MyTable";

      {
	Connection c=ds.getConnection();
	Statement s=c.createStatement();
	// TODO - should parameterise the column names when code stabilises...
	s.execute("create table "+table+"(MyKey varchar, MyValue java_object)");
	s.close();
	c.close();
      }

      Cache backing=new BasicCache();
      CacheInnards ci=new SharedDBCacheInnards(ds, table, new Pool(), new SimpleStreamer(), backing, new NoEvictionPolicy());
      Cache cache=new BasicCache(ci, ci);

      testJCache(cache, false);

      {
	Connection c=ds.getConnection();
	Statement s=c.createStatement();
	s.execute("drop table "+table);
	s.execute("SHUTDOWN");
	s.close();
	c.close();
      }

      success=true;
    }
    catch (Exception e)
    {
      _log.error("test failed", e);
    }

    assertTrue(success);
  }
}
