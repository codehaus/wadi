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

package org.codehaus.wadi.old.test;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;
import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.SyncMap;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.RWLock;


/**
 * Test concurrency related issues
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  TestConcurrency
  extends TestCase
{
  protected Log _log=LogFactory.getLog(getClass());

  public
    TestConcurrency(String name)
  {
    super(name);
  }

  //----------------------------------------

  protected long
    testMap(Map map)
  {
    long start=System.currentTimeMillis();
    int iters=100;

    for (int i=iters;i>0;i--)
    {
      String s=""+i;
      map.put(s,s);
    }
    for (int i=iters;i>0;i--)
    {
      String s=""+i;
      assertTrue(map.get(s).equals(s));
    }
    for (int i=iters;i>0;i--)
    {
      String s=""+i;
      map.remove(s);
    }
    assertTrue(map.size()==0);

    long end=System.currentTimeMillis();

    return end-start;
  }

  public void
    testMaps()
    throws Exception
  {

    System.out.println("HashMap:                                "+testMap(new HashMap()));
    System.out.println("ConcurrentReaderHashMap:                "+testMap(new ConcurrentReaderHashMap()));
    System.out.println("ConcurrentHashMap:                      "+testMap(new ConcurrentHashMap()));
    System.out.println("HashMap:                                "+testMap(new HashMap()));
    System.out.println("Mutex(HashMap):                         "+testMap(new SyncMap(new HashMap(), new Mutex())));
    System.out.println("WriterPreferenceReadWriteLock(HashMap): "+testMap(new SyncMap(new HashMap(), new WriterPreferenceReadWriteLock())));

    assertTrue(true);
  }

  public void
    testIterator()
  {
    Map map=new ConcurrentReaderHashMap();

    map.put("a","1");
    map.put("b","2");
    map.put("c","3");

    for (Iterator i=map.entrySet().iterator(); i.hasNext();)
    {
      System.out.println("removing element...");
      i.next();
      i.remove();
    }

    assertTrue(map.size()==0);
  }

  protected int _priority=Thread.MAX_PRIORITY+1;

  final int INVALIDATION_PRIORITY=3;
  final int TIMEOUT_PRIORITY=2;
  final int EMMIGRATION_PRIORITY=1;
  final int EVICTION_PRIORITY=0;

  final int MAX_PRIORITY=INVALIDATION_PRIORITY;

  public void
    priority(final boolean acquire)
    throws Exception
    {

      final RWLock lock=new RWLock(MAX_PRIORITY);

      Thread[] threads=new Thread[MAX_PRIORITY+1];

      RWLock.setPriority(EVICTION_PRIORITY);

      lock.readLock().attempt(60000);

      for (int i=0;i<=MAX_PRIORITY;i++)
      {
	final int p=i;
	if (_log.isInfoEnabled()) _log.info("starting: "+p);
	Thread t=new Thread()
	  {
	    public void run()
	    {
	      try
	      {
		RWLock.setPriority(p);
		if (acquire)
		  lock.writeLock().acquire();
		else
		  lock.writeLock().attempt(60000);
		int priority=RWLock.getPriority();
		if (_log.isInfoEnabled()) _log.info("priority: "+priority);
		assertTrue(priority<_priority);
		_priority=priority;
		lock.writeLock().release();
	      }
	      catch (Exception e)
	      {
		_log.warn("oops", e);
	      }
	    }
	  };
	threads[i]=t;
	t.start();
      }

      Thread.yield();
      _log.info("releasing read lock");
      lock.readLock().release();

      for (int i=0;i<=MAX_PRIORITY;i++)
      {
	Thread t=threads[i];
	t.join();
	if (_log.isInfoEnabled()) _log.info("joining: "+i);
      }
    }

  public void
    testPriority()
    throws Exception
  {
    _priority=MAX_PRIORITY+1;
    priority(true);
    _priority=MAX_PRIORITY+1;
    priority(false);
  }

  protected boolean _first=true;

  public void
    testOverlap()
    throws Exception
  {
    final RWLock lock=new RWLock(MAX_PRIORITY);

    {
      lock.readLock().acquire();

      Thread t1=new Thread() {public void run()
 	  {
 	    try
 	    {
	      RWLock.setPriority(EVICTION_PRIORITY);
 	      lock.writeLock().acquire();
 	      _log.info("I lost");
	      assertTrue(_first==false);
 	      lock.writeLock().release();
 	    }
 	    catch (Exception e)
 	    {
 	      _log.warn(e);
 	    }
 	  }
 	};
      t1.start();

      RWLock.setPriority(INVALIDATION_PRIORITY);
      lock.overlap();
      _log.info("I won");
      assertTrue(_first==true);
      _first=false;
      lock.writeLock().release();

      t1.join();
    }
  }
}
