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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.cluster.Abstract2TopologyStrategy;
import org.codehaus.wadi.sandbox.cluster.Cell;
import org.codehaus.wadi.sandbox.cluster.Peer;
import org.codehaus.wadi.sandbox.cluster.RingTopologyStrategy;

public class
  TestTopologies
  extends TestCase
{
  protected Log _log=LogFactory.getLog(getClass());

  public TestTopologies(String name)
  {
    super(name);
  }

  protected void
    setUp()
    throws Exception
    {}

  protected void
    tearDown()
    throws Exception
    {}

  //----------------------------------------

  public void
    testRing()
  {
    int k=2;

    Peer p0=new Peer("0");
    Peer p1=new Peer("1");
    Peer p2=new Peer("2");
    Peer p3=new Peer("3");
    Peer local=p0;

    Collection e=new TreeSet();
    e.add(p0);
    e.add(p1);
    e.add(p2);
    e.add(p3);

      if (_log.isInfoEnabled()) _log.info("in     :" + e);

    Abstract2TopologyStrategy ts=new RingTopologyStrategy(local.getId(), "test", null, null, 2);
    Map result;
    Map control=new TreeMap();
    {
      Collection c;
      c=new ArrayList();
      c.add(p0);
      c.add(p1);
      control.put(Cell.id(c), c);
      c=new ArrayList();
      c.add(p1);
      c.add(p2);
      control.put(Cell.id(c), c);
      c=new ArrayList();
      c.add(p2);
      c.add(p3);
      control.put(Cell.id(c), c);
      c=new ArrayList();
      c.add(p3);
      c.add(p0);
      control.put(Cell.id(c), c);
    }

    Collection control2;
    Collection result2;

    result=ts.combineMap(null, e, k);
    if (_log.isInfoEnabled()) {
        _log.info("out    :" + result);
        _log.info("control:" + control);
    }

    assertTrue(control.equals(result));

    result2=ts.combineCollection(null, e, k);
    control2=new ArrayList(control.values());
    if (_log.isInfoEnabled()) {
        _log.info("out2    :" + result2);
        _log.info("control2:" + control2);
    }

    assertTrue(control2.equals(result2));

    control.remove("1-2");
    control.remove("2-3");

    result=ts.combineMap(local, e, k);
    if (_log.isInfoEnabled()) {
        _log.info("out    :" + result);
        _log.info("control:" + control);
    }

    assertTrue(control.equals(result));

    result2=ts.combineCollection(local, e, k);
    control2=new ArrayList(control.values());
    if (_log.isInfoEnabled()) {
        _log.info("out2    :" + result2);
        _log.info("control2:" + control2);
    }

    assertTrue(control2.equals(result2));

    // now some timings...
    e=new TreeSet();
    k=2;
    for (int i=0; i<500; i++)
      e.add(new Peer(""+i));

    long start;
    long end;

    start=System.currentTimeMillis();
    ts.combineMap(null, e, k);
    end=System.currentTimeMillis();
      if (_log.isInfoEnabled()) _log.info("combineMap Ring x500 :" + ( end - start ) + " milis");

    start=System.currentTimeMillis();
    ts.combineMap(null, e, k);
    end=System.currentTimeMillis();
      if (_log.isInfoEnabled()) _log.info("combineMap Ring x500 :" + ( end - start ) + " milis");

    start=System.currentTimeMillis();
    ts.combineMap(null, e, k);
    end=System.currentTimeMillis();
      if (_log.isInfoEnabled()) _log.info("combineMap Ring x500 :" + ( end - start ) + " milis");

    start=System.currentTimeMillis();
    ts.combineCollection(null, e, k);
    end=System.currentTimeMillis();
      if (_log.isInfoEnabled()) _log.info("combineCollection Ring x500 :" + ( end - start ) + " milis");

    start=System.currentTimeMillis();
    ts.combineCollection(null, e, k);
    end=System.currentTimeMillis();
      if (_log.isInfoEnabled()) _log.info("combineCollection Ring x500 :" + ( end - start ) + " milis");

    start=System.currentTimeMillis();
    ts.combineCollection(null, e, k);
    end=System.currentTimeMillis();
      if (_log.isInfoEnabled()) _log.info("combineCollection Ring x500 :" + ( end - start ) + " milis");
  }

//  public void
//    testNChooseK()
//  {
//    int k=2;
//
//    Peer p0=new Peer("0");
//    Peer p1=new Peer("1");
//    Peer p2=new Peer("2");
//    Peer p3=new Peer("3");
//    Peer local=p0;
//
//    Collection e=new TreeSet();
//    e.add(p0);
//    e.add(p1);
//    e.add(p2);
//    e.add(p3);
//
//    _log.info("in     :"+e);
//
//    Abstract2TopologyStrategy ts=new NChooseKTopologyStrategy(local.getId(), "test", null, null, 2);
//    Map result;
//    Collection result2;
//
//    Map control=new TreeMap();
//    {
//      Collection c;
//      c=new TreeSet();
//      c.add(p0);
//      c.add(p1);
//      control.put(Cell.id(c), c);
//      c=new TreeSet();
//      c.add(p0);
//      c.add(p2);
//      control.put(Cell.id(c), c);
//      c=new TreeSet();
//      c.add(p0);
//      c.add(p3);
//      control.put(Cell.id(c), c);
//      c=new TreeSet();
//      c.add(p1);
//      c.add(p2);
//      control.put(Cell.id(c), c);
//      c=new TreeSet();
//      c.add(p1);
//      c.add(p3);
//      control.put(Cell.id(c), c);
//      c=new TreeSet();
//      c.add(p2);
//      c.add(p3);
//      control.put(Cell.id(c), c);
//    }
//    Collection control2;
//
//    result=ts.combineMap(null, e, k);
//    _log.info("control:"+control);
//    _log.info("out    :"+result);
//    assertTrue(control.equals(result));
//
//    control2=new TreeSet(new CollectionComparator());
//    control2.addAll(control.values());
//
//    result2=ts.combineCollection(null, e, k);
//    _log.info("control2:"+control2);
//    _log.info("out2    :"+result2);
//    assertTrue(control2.equals(result2));
//
//    result=ts.combineMap(local, e, k);
//    control.remove("1-2");
//    control.remove("1-3");
//    control.remove("2-3");
//
//    _log.info("control:"+control);
//    _log.info("out    :"+result);
//    assertTrue(control.equals(result));
//
//    result2=ts.combineCollection(local, e, k);
//    control2=new TreeSet(new CollectionComparator());
//    control2.addAll(control.values());
//
//    _log.info("control2:"+control2);
//    _log.info("out2    :"+result2);
//    assertTrue(control2.equals(result2));
//
//
//    // now some timings...
//    e=new TreeSet();
//    k=2;
//    for (int i=0; i<5; i++)
//      e.add(new Peer(""+i));
//
//    long start;
//    long end;
//
//    start=System.currentTimeMillis();
//    ts.combineMap(null, e, k);
//    end=System.currentTimeMillis();
//    _log.info("combineMap NChooseK x500 :"+(end-start)+" milis");
//
//    start=System.currentTimeMillis();
//    ts.combineMap(null, e, k);
//    end=System.currentTimeMillis();
//    _log.info("combineMap NChooseK x500 :"+(end-start)+" milis");
//
//    start=System.currentTimeMillis();
//    ts.combineMap(null, e, k);
//    end=System.currentTimeMillis();
//    _log.info("combineMap NChooseK x500 :"+(end-start)+" milis");
//
//    start=System.currentTimeMillis();
//    ts.combineCollection(null, e, k);
//    end=System.currentTimeMillis();
//    _log.info("combineCollection NChooseK x500 :"+(end-start)+" milis");
//
//    start=System.currentTimeMillis();
//    ts.combineCollection(null, e, k);
//    end=System.currentTimeMillis();
//    _log.info("combineCollection NChooseK x500 :"+(end-start)+" milis");
//
//    start=System.currentTimeMillis();
//    ts.combineCollection(null, e, k);
//    end=System.currentTimeMillis();
//    _log.info("combineCollection NChooseK x500 :"+(end-start)+" milis");
//  }
}
