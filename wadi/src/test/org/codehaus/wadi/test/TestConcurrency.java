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
import EDU.oswego.cs.dl.util.concurrent.SyncMap;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import junit.framework.TestCase;

public class
  TestConcurrency
  extends TestCase
{
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
}
