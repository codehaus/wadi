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

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;
import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.SyncMap;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.Sync;

import org.codehaus.wadi.shared.RWLock;
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

  public void
    priority(final boolean acquire)
    throws Exception
    {
      final ReadWriteLock lock=new RWLock();

      Thread[] threads=new Thread[Thread.MAX_PRIORITY-Thread.MIN_PRIORITY+1];

      Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

      lock.readLock().attempt(60000);

      for (int i=Thread.MIN_PRIORITY;i<=Thread.MAX_PRIORITY;i++)
      {
	_log.info("starting: "+i);
	Thread t=new Thread()
	  {
	    public void run()
	    {
	      try
	      {
		if (acquire)
		  lock.writeLock().acquire();
		else
		  lock.writeLock().attempt(60000);
		int priority=Thread.currentThread().getPriority();
		_log.info("priority: "+priority);
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
	t.setPriority(i);
	threads[i-Thread.MIN_PRIORITY]=t;
	t.start();
      }

      Thread.yield();
      _log.info("releasing read lock");
      lock.readLock().release();

      for (int i=Thread.MIN_PRIORITY;i<=Thread.MAX_PRIORITY;i++)
      {
	Thread t=threads[i-Thread.MIN_PRIORITY];
	t.join();
	_log.info("joining: "+i);
      }
    }

  public void
    testPriority()
    throws Exception
  {
    _priority=Thread.MAX_PRIORITY+1;
    priority(true);
    _priority=Thread.MAX_PRIORITY+1;
    priority(false);
  }

//   public void
//     testOverlap()
//     throws Exception
//   {
//     final ReadWriteLock lock=new RWLock();

//     {
//       lock.readLock().acquire();

//       Thread t1=new Thread() {public void run()
// 	  {
// 	    try
// 	    {
// 	      lock.writeLock().acquire();
// 	      _log.info("you lost");
// 	      lock.writeLock().release();
// 	    }
// 	    catch (Exception e)
// 	    {
// 	      _log.warn(e);
// 	    }
// 	  }
// 	};
//       t1.start();

//       ((RWLock)lock).overlap();
//       _log.info("I won");
//       lock.writeLock().release();

//       t1.join();
//     }
//   }
}
