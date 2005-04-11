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

package org.codehaus.wadi.sandbox.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.SerializableContent;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.impl.SimpleStreamingStrategy;
import org.codehaus.wadi.sandbox.Session;
import org.codehaus.wadi.sandbox.ValueFactory;
import org.codehaus.wadi.sandbox.ValuePool;
import org.codehaus.wadi.sandbox.AttributesPool;
import org.codehaus.wadi.sandbox.Dirtier;
import org.codehaus.wadi.sandbox.SessionFactory;
import org.codehaus.wadi.sandbox.SessionPool;
import org.codehaus.wadi.sandbox.impl.DistributableManager;
import org.codehaus.wadi.sandbox.impl.DistributableSession;
import org.codehaus.wadi.sandbox.impl.DistributableValueFactory;
import org.codehaus.wadi.sandbox.impl.AtomicAttributesFactory;
import org.codehaus.wadi.sandbox.impl.AtomicAttributesPool;
import org.codehaus.wadi.sandbox.impl.DistributableSessionFactory;
import org.codehaus.wadi.sandbox.impl.SimpleSessionPool;
import org.codehaus.wadi.sandbox.impl.Manager;
import org.codehaus.wadi.sandbox.impl.StandardSession;
import org.codehaus.wadi.sandbox.impl.SimpleValuePool;
import org.codehaus.wadi.sandbox.impl.StandardSessionFactory;
import org.codehaus.wadi.sandbox.impl.StandardValueFactory;
import org.codehaus.wadi.sandbox.impl.WriteDirtier;

/**
 * Test WADI's HttpSession implementation
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  TestHttpSession
 
  extends TestCase
{
  protected Log               _log=LogFactory.getLog(TestHttpSession.class);
  protected Listener          _listener;
  protected List              _events=new ArrayList();
  protected boolean           _evictObjectRepASAP=false;
  protected boolean           _evictByteRepASAP=false;
  protected AtomicAttributesFactory _attributesFactory=new AtomicAttributesFactory();
  protected AttributesPool          _attributesPool=new AtomicAttributesPool(_attributesFactory);
  // Standard
  protected SessionFactory          _standardSessionFactory=new StandardSessionFactory(_attributesFactory);
  protected SessionPool             _standardSessionPool=new SimpleSessionPool(_standardSessionFactory);
  protected ValueFactory            _standardValueFactory=new StandardValueFactory();
  protected ValuePool               _standardValuePool=new SimpleValuePool(_standardValueFactory);
  protected Manager                 _standardManager=new Manager(_standardSessionPool, _attributesPool, _standardValuePool);
  // Distributable
  protected Dirtier                 _dirtier=new WriteDirtier();
  protected StreamingStrategy       _streamer=new SimpleStreamingStrategy();
  protected SessionFactory          _distributableSessionFactory=new DistributableSessionFactory(_attributesFactory);
  protected SessionPool             _distributableSessionPool=new SimpleSessionPool(_distributableSessionFactory);
  protected ValueFactory            _distributableValueFactory=new DistributableValueFactory();
  protected ValuePool               _distributableValuePool=new SimpleValuePool(_distributableValueFactory);
  protected Manager                 _distributableManager=new DistributableManager(_distributableSessionPool, _attributesPool, _distributableValuePool, _streamer, _dirtier);
  
  public TestHttpSession(String name)
  {
    super(name);
  }

  static class Pair
    implements Serializable
  {
    String _type;
    HttpSessionEvent _event;

    Pair(String type, HttpSessionEvent event)
    {
      _type=type;
      _event=event;
    }

    String getType(){return _type;}
    HttpSessionEvent getEvent(){return _event;}
    public String toString() {return "<"+_event.getSession().getId()+":"+_type+">";}
  }

  // try to run up TC session manager within this test - how the hell does it work ?
  //   class Manager
  //     extends org.apache.catalina.session.StandardManager
  //   {
  //     public HttpSession newHttpSession() {return createSession().getSession();}
  //     public void addEventListener(EventListener l){addSessionListener(l);}
  //   }

  class Listener
    implements
      HttpSessionListener,
      HttpSessionAttributeListener,
      HttpSessionBindingListener
  {
    // HttpSessionListener
    public void sessionCreated       (HttpSessionEvent e)        {e.getSession().getId();_events.add(new Pair("sessionCreated",e));}
    public void sessionDestroyed     (HttpSessionEvent e)        {e.getSession().getId();_events.add(new Pair("sessionDestroyed",e));}
    // HttpSessionAttributeListener
    public void attributeAdded       (HttpSessionBindingEvent e) {e.getSession().getId();_events.add(new Pair("attributeAdded",e));}
    public void attributeRemoved     (HttpSessionBindingEvent e) {e.getSession().getId();_events.add(new Pair("attributeRemoved",e));}
    public void attributeReplaced    (HttpSessionBindingEvent e) {e.getSession().getId();_events.add(new Pair("attributeReplaced",e));}
    // HttpSessionBindingListener
    public void valueBound           (HttpSessionBindingEvent e) {e.getSession().getId();_events.add(new Pair("valueBound",e));}
    public void valueUnbound         (HttpSessionBindingEvent e) {e.getSession().getId();_events.add(new Pair("valueUnbound",e));}
  }

  static class ActivationListener
    implements
      HttpSessionActivationListener,
      Serializable
  {
    public static List _events=new ArrayList();
    protected static Log _log=LogFactory.getLog(ActivationListener.class);

    // HttpSessionActivationListener
    public void
      sessionDidActivate(HttpSessionEvent e)
    {
      e.getSession().getId();
      _events.add(new Pair("sessionDidActivate",e));
      _log.trace("ACTIVATING");
    }

    public void
      sessionWillPassivate(HttpSessionEvent e)
    {
      e.getSession().getId();
      _events.add(new Pair("sessionWillPassivate",e));
      _log.trace("PASSIVATING");
    }
  }

  protected void
    setUp()
    throws Exception {
    _listener=new Listener();
    _standardManager.addEventListener(_listener);
    _distributableManager.addEventListener(_listener);
  }

  protected void
    tearDown() {
      _distributableManager.removeEventListener(_listener);
      _standardManager.removeEventListener(_listener);
    _listener=null;
  }

  //----------------------------------------

  public void testCreateHttpSession() {
      testCreateHttpSession(_standardManager);
      testCreateHttpSession(_distributableManager);
  }

  public void
  testCreateHttpSession(Manager manager)
  {
    _events.clear();

    // create a session
    HttpSession session=manager.newHttpSession();
    assertTrue(!session.getAttributeNames().hasMoreElements());
    assertTrue(session.getValueNames().length==0);
    Pair pair=(Pair)_events.remove(0);
    assertTrue(pair!=null);
    assertTrue(pair.getType().equals("sessionCreated"));
    assertTrue(pair.getEvent().getSession()==session);
    assertTrue(_events.size()==0);
  }

  // TODO
  //   public void
  //     testDestroyHttpSession()
  //     throws Exception
  //   {
  // _log.info("[0] foo");
  //     int interval=10;//TODO - dodgy test - how can it be improved...?
  //     int oldInterval= _manager.getHouseKeepingInterval();
  //     _manager.stop();
  // _log.info("[1] foo");
  //     _manager.setHouseKeepingInterval(interval); // seconds
  //     _manager.removeEventListener(_listener);
  //     _manager.addEventListener(_listener);
  // _log.info("[1.2] foo");
  //     _manager.start();
  // _log.info("[2] foo");
  //     // set up test
  //     HttpSession session=_manager.newHttpSession();
  // _log.info("[2.1] foo");
  //     String key="foo";
  // _log.info("[2.2] foo");
  //     Object val=new Listener();
  // _log.info("[2.3] foo");
  //     session.setAttribute(key, val);
  // _log.info("[2.4] foo");
  //     _events.clear();
  // _log.info("[2.5] foo");

  //     // destroy session
  //     session.invalidate();
  // _log.info("[2.6] foo");
  //     Thread.sleep(2000*interval); //2*interval - millis
  // _log.info("[2.7] foo");

  //     {
  // _log.info("[2.8] foo");
  //       Pair pair=(Pair)_events.remove(0);
  // _log.info("[2.8.1] foo");
  //       assertTrue(pair!=null);
  // _log.info("[2.8.2] foo");
  //       assertTrue(pair.getType().equals("sessionDestroyed"));
  // _log.info("[2.8.3] foo");
  //       HttpSessionEvent e=pair.getEvent();
  // _log.info("[2.8.4] foo");
  //       assertTrue(session==e.getSession());
  // _log.info("[2.9] foo");
  //     }
  //     {
  // _log.info("[2.10] foo");
  //       Pair pair=(Pair)_events.remove(0);
  //       assertTrue(pair!=null);
  //       assertTrue(pair.getType().equals("valueUnbound"));
  //       HttpSessionEvent e=pair.getEvent();
  //       assertTrue(session==e.getSession());
  //       HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
  //       assertTrue(be.getName()==key);
  //       assertTrue(be.getValue()==val);
  // _log.info("[2.11] foo");
  //     }
  //     {
  // _log.info("[2.12] foo");
  //       Pair pair=(Pair)_events.remove(0);
  //       assertTrue(pair!=null);
  //       assertTrue(pair.getType().equals("attributeRemoved"));
  //       HttpSessionEvent e=pair.getEvent();
  //       assertTrue(session==e.getSession());
  //       HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
  //       assertTrue(be.getName()==key);
  //       assertTrue(be.getValue()==val);
  // _log.info("[2.13] foo");
  //     }
  //     assertTrue(_events.size()==0);

  //     _manager.stop();
  // _log.info("[3] foo");
  //     _manager.setHouseKeepingInterval(oldInterval);
  //     _manager.start();
  // _log.info("[4] foo");
  //   }

  public void
  testSetAttribute()
  {  
      testSetAttribute(_standardManager);
      testSetAttribute(_distributableManager);
  }
  
  public void
  testSetAttribute(Manager manager)
  {   
      HttpSession session=manager.newHttpSession();
      assertTrue(_events.size()==1); // sessionCreated
      _events.clear();

    String key="foo";
    Object val=new Listener();
    session.setAttribute(key, val);
    assertTrue(_events.size()==2); // valueBound, attributeAdded
    {
        Pair pair=(Pair)_events.get(0);
        assertTrue(pair!=null);
        assertTrue(pair.getType().equals("valueBound"));
        HttpSessionEvent e=pair.getEvent();
        assertTrue(session==e.getSession());
        HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
        assertTrue(be.getName()==key);
        assertTrue(be.getValue()==val);
      }
    {
      Pair pair=(Pair)_events.get(1);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("attributeAdded"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==val);
    }
    _events.clear();
    assertTrue(_events.size()==0);
  }

  public void
  testPutValue()
  {
      testPutValue(_standardManager);
      testPutValue(_distributableManager);
  }
  
  public void
  testPutValue(Manager manager)
  {
    HttpSession session=manager.newHttpSession();
    assertTrue(_events.size()==1); // sessionCreated
    _events.clear();

    String key="foo";
    Object val=new Listener();
    session.putValue(key, val);
    assertTrue(_events.size()==2); // valueBound, attributeAdded
    {
        Pair pair=(Pair)_events.get(0);
        assertTrue(pair!=null);
        assertTrue(pair.getType().equals("valueBound"));
        HttpSessionEvent e=pair.getEvent();
        assertTrue(session==e.getSession());
        HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
        assertTrue(be.getName()==key);
        assertTrue(be.getValue()==val);
      }
    {
      Pair pair=(Pair)_events.get(1);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("attributeAdded"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==val);
    }
    _events.clear();
    assertTrue(_events.size()==0);
  }

  public void
  testGetAttribute()
  {
      testGetAttribute(_standardManager);
      testGetAttribute(_distributableManager);
  }
  
  public void
  testGetAttribute(Manager manager)
  {
    HttpSession session=manager.newHttpSession();
    String key="foo";
    Object val=new Listener();
    session.setAttribute(key, val);
    _events.clear();

    assertTrue(session.getAttribute(key)==val);
  }

  public void
  testGetValue()
  {
      testGetValue(_standardManager);
      testGetValue(_distributableManager);
  }
  
  public void
    testGetValue(Manager manager)
  {
    HttpSession session=manager.newHttpSession();
    String key="foo";
    Object val=new Listener();
    session.setAttribute(key, val);
    _events.clear();

    assertTrue(session.getValue(key)==val);
  }

  public void
  testRemoveAttribute()
  {
      testRemoveAttribute(_standardManager);
      testRemoveAttribute(_distributableManager);
  }
  
  public void
    testRemoveAttribute(Manager manager)
  {
    HttpSession session=manager.newHttpSession();
    assertTrue(_events.size()==1); // sessionCreated
    String key="foo";
    Object val=new Listener();
    session.setAttribute(key, val);
    assertTrue(_events.size()==3); // valueBound, attributeAdded
    _events.clear();

    session.removeAttribute(key);
    assertTrue(_events.size()==2); // valueUnBound, attributeRemoved
    {
      Pair pair=(Pair)_events.get(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("valueUnbound"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==val);
    }
    {
      Pair pair=(Pair)_events.get(1);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("attributeRemoved"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==val);
    }
    _events.clear();
    assertTrue(_events.size()==0);
    assertTrue(session.getAttribute(key)==null);
  }

  public void
  testRemoveValue()
  {
      testRemoveValue(_standardManager);
      testRemoveValue(_distributableManager);
  }
 
  public void
    testRemoveValue(Manager manager)
  {
    HttpSession session=manager.newHttpSession();
    assertTrue(_events.size()==1); // sessionCreated
    String key="foo";
    Object val=new Listener();
    session.setAttribute(key, val);
    assertTrue(_events.size()==3); // valueBound, attributeAdded
    _events.clear();

    session.removeValue(key);
    assertTrue(_events.size()==2); // valueUnBound, attributeRemoved

    {
      Pair pair=(Pair)_events.get(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("valueUnbound"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==val);
    }
    {
      Pair pair=(Pair)_events.get(1);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("attributeRemoved"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==val);
    }
    _events.clear();
    assertTrue(_events.size()==0);
    assertTrue(session.getAttribute(key)==null);
  }

  public void
  testSetAttributeNull()
  {
      testSetAttributeNull(_standardManager);
      testSetAttributeNull(_distributableManager);
  }

  public void
  testSetAttributeNull(Manager manager)
  {
      HttpSession session=manager.newHttpSession();
      assertTrue(_events.size()==1); // sessionCreated
      String key="foo";
      Object val=new Listener();
      session.setAttribute(key, val);
      assertTrue(_events.size()==3); // valueBound, attributeAdded
      _events.clear();

      session.setAttribute(key, null);
      assertTrue(_events.size()==2); // valueUnBound, attributeRemoved

      {
        Pair pair=(Pair)_events.get(0);
        assertTrue(pair!=null);
        assertTrue(pair.getType().equals("valueUnbound"));
        HttpSessionEvent e=pair.getEvent();
        assertTrue(session==e.getSession());
        HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
        assertTrue(be.getName()==key);
        assertTrue(be.getValue()==val);
      }
      {
        Pair pair=(Pair)_events.get(1);
        assertTrue(pair!=null);
        assertTrue(pair.getType().equals("attributeRemoved"));
        HttpSessionEvent e=pair.getEvent();
        assertTrue(session==e.getSession());
        HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
        assertTrue(be.getName()==key);
        assertTrue(be.getValue()==val);
      }
      _events.clear();
      assertTrue(_events.size()==0);
      assertTrue(session.getAttribute(key)==null);
  }
  
  public void
  testPutValueNull()
  {
      testPutValueNull(_standardManager);
      testPutValueNull(_distributableManager);
  }
  
  public void
  testPutValueNull(Manager manager)
  {
      HttpSession session=manager.newHttpSession();
      assertTrue(_events.size()==1); // sessionCreated
      String key="foo";
      Object val=new Listener();
      session.setAttribute(key, val);
      assertTrue(_events.size()==3); // valueBound, attributeAdded
      _events.clear();

      session.putValue(key, null);
      assertTrue(_events.size()==2); // valueUnBound, attributeRemoved

      {
        Pair pair=(Pair)_events.get(0);
        assertTrue(pair!=null);
        assertTrue(pair.getType().equals("valueUnbound"));
        HttpSessionEvent e=pair.getEvent();
        assertTrue(session==e.getSession());
        HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
        assertTrue(be.getName()==key);
        assertTrue(be.getValue()==val);
      }
      {
        Pair pair=(Pair)_events.get(1);
        assertTrue(pair!=null);
        assertTrue(pair.getType().equals("attributeRemoved"));
        HttpSessionEvent e=pair.getEvent();
        assertTrue(session==e.getSession());
        HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
        assertTrue(be.getName()==key);
        assertTrue(be.getValue()==val);
      }
      _events.clear();
      assertTrue(_events.size()==0);
      assertTrue(session.getAttribute(key)==null);
  }

  public void
  testReplaceAttribute()
  {
      testReplaceAttribute(_standardManager);
      testReplaceAttribute(_distributableManager);
  }
  
  public void
  testReplaceAttribute(Manager manager)
  {
    HttpSession session=manager.newHttpSession();
    assertTrue(_events.size()==1); // sessionCreated
    String key="foo";
    Object oldVal=new Listener();
    Object newVal=new Listener();
    session.setAttribute(key, oldVal);
    assertTrue(_events.size()==3); // valueBound, attributeAdded
    _events.clear();

    session.setAttribute(key, newVal);
    assertTrue(_events.size()==3); // valueUnbound, valueBound, attributeReplaced
    {
      Pair pair=(Pair)_events.get(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("valueUnbound"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==oldVal);
    }
    {
      Pair pair=(Pair)_events.get(1);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("valueBound"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==newVal);
    }
    {
      Pair pair=(Pair)_events.get(2);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("attributeReplaced"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==oldVal);
    }
    _events.clear();
    assertTrue(_events.size()==0);
    assertTrue(session.getValue(key)==newVal);
  }

  
  public void
  testReplaceValue()
  {
      testReplaceValue(_standardManager);
      testReplaceValue(_distributableManager);
  }
  
  public void
  testReplaceValue(Manager manager)
  {
    HttpSession session=manager.newHttpSession();
    String key="foo";
    Object oldVal=new Listener();
    Object newVal=new Listener();
    session.setAttribute(key, oldVal);
    _events.clear();

    session.putValue(key, newVal);
    {
      Pair pair=(Pair)_events.remove(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("valueUnbound"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==oldVal);
    }
    {
      Pair pair=(Pair)_events.remove(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("valueBound"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==newVal);
    }
    {
      Pair pair=(Pair)_events.remove(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("attributeReplaced"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==oldVal);
    }
    assertTrue(session.getValue(key)==newVal);
    assertTrue(_events.size()==0);
  }

  protected int
    enumerationLength(Enumeration e)
  {
    int i=0;
    while (e.hasMoreElements())
    {
      e.nextElement();
      i++;
    }
    return i;
  }

  public void
  testGetAttributeNames()
  {
      testGetAttributeNames(_standardManager);
      testGetAttributeNames(_distributableManager);
  }
  
  public void
  testGetAttributeNames(Manager manager)
  {
    HttpSession session=manager.newHttpSession();
    assertTrue(enumerationLength(session.getAttributeNames())==0);
    session.setAttribute("foo", "bar");
    assertTrue(enumerationLength(session.getAttributeNames())==1);
    session.setAttribute("bar", "baz");
    assertTrue(enumerationLength(session.getAttributeNames())==2);
    session.setAttribute("baz", "foo");
    assertTrue(enumerationLength(session.getAttributeNames())==3);
    session.setAttribute("baz", null);
    assertTrue(enumerationLength(session.getAttributeNames())==2);
    session.setAttribute("bar", null);
    assertTrue(enumerationLength(session.getAttributeNames())==1);
    session.setAttribute("foo", null);
    assertTrue(enumerationLength(session.getAttributeNames())==0);
  }

  public void
  testGetValueNames()
  {
      testGetValueNames(_standardManager);
      testGetValueNames(_distributableManager);
  }
  
  public void
  testGetValueNames(Manager manager)
  {
    HttpSession session=manager.newHttpSession();
    assertTrue(session.getValueNames().length==0);
    session.setAttribute("foo", "bar");
    assertTrue(session.getValueNames().length==1);
    session.setAttribute("bar", "baz");
    assertTrue(session.getValueNames().length==2);
    session.setAttribute("baz", "foo");
    assertTrue(session.getValueNames().length==3);
    session.setAttribute("baz", null);
    assertTrue(session.getValueNames().length==2);
    session.setAttribute("bar", null);
    assertTrue(session.getValueNames().length==1);
    session.setAttribute("foo", null);
    assertTrue(session.getValueNames().length==0);
  }

  public void
    testMaxInactiveInterval()
  {
    // TODO
  }

  // TODO
//  public void
//    testInvalidate()
//  {
//    HttpSession session=_manager.newHttpSession();
//    session.getId();
//    session.invalidate();
//    try{session.getId();assertTrue(false);}catch(IllegalStateException e){}
//    try{session.invalidate();assertTrue(false);}catch(IllegalStateException e){}
//  }

  public void
  testIsNew()
  {
      testIsNew(_standardManager, _standardSessionPool);
      testIsNew(_distributableManager, _distributableSessionPool);
  }
  
  public void
  testIsNew(Manager manager, SessionPool sessionPool)
  {
      Session s=sessionPool.take(manager);
      HttpSession session=s.getWrapper();
      assertTrue(session.isNew());
      s.setLastAccessedTime(System.currentTimeMillis()+1);
      assertTrue(!session.isNew());
  }

  
  public void
  testNullName()
  {
      testNullName(_standardManager);
      testNullName(_distributableManager);
  }
  
  public void
  testNullName(Manager manager)
  {
    HttpSession session=manager.newHttpSession();
    try{session.setAttribute(null,"a");assertTrue(false);}catch(IllegalArgumentException e){}
    try{session.getAttribute(null);assertTrue(false);}catch(IllegalArgumentException e){}
    try{session.removeAttribute(null);assertTrue(false);}catch(IllegalArgumentException e){}
    try{session.putValue(null,"a");assertTrue(false);}catch(IllegalArgumentException e){}
    try{session.getValue(null);assertTrue(false);}catch(IllegalArgumentException e){}
    try{session.removeValue(null);assertTrue(false);}catch(IllegalArgumentException e){}
  }

  public byte[]
    marshall(SerializableContent writer)
    throws Exception
  {
    ByteArrayOutputStream baos=new ByteArrayOutputStream();
    ObjectOutputStream    oos =new ObjectOutputStream(baos);
    writer.writeContent(oos);
    oos.flush();
    return baos.toByteArray();
  }

  public void
    demarshall(SerializableContent reader, byte[] buffer)
    throws Exception
  {
    ByteArrayInputStream bais=new ByteArrayInputStream(buffer);
    ObjectInputStream    ois =new ObjectInputStream(bais);
    reader.readContent(ois);
  }

  public void
  testActivation() throws Exception
  {
      testActivation(_distributableManager, _distributableSessionPool);
  }
  
  public void
  testActivation(Manager manager, SessionPool pool) // Distributable only
  throws Exception
  {
      DistributableSession s0=(DistributableSession)pool.take(manager);
      List events=ActivationListener._events;
      events.clear();
      
      String key="test";
      ActivationListener l=new ActivationListener();
      s0.setAttribute(key, l);
      byte[] bytes=s0.getBytes();
      assertTrue(events.size()>=1); // passivation MUST have occurred - activation may be lazy
      {
          Pair pair=(Pair)events.get(0);
          assertTrue(pair!=null);
          assertTrue(pair.getType().equals("sessionWillPassivate"));
          HttpSessionEvent e=pair.getEvent();
          assertTrue(s0.getWrapper()==e.getSession());
      }
      s0.getAttribute(key); // force lazy activation to trigger...
      assertTrue(events.size()==2); // [re]activation MUST now have occurred
      {
          Pair pair=(Pair)events.get(1);
          assertTrue(pair!=null);
          assertTrue(pair.getType().equals("sessionDidActivate"));
          HttpSessionEvent e=pair.getEvent();
          assertTrue(s0.getWrapper()==e.getSession());
      }
      events.clear();

    StandardSession s1=(DistributableSession)pool.take(_distributableManager);
    s1.setBytes(bytes);
    // listsners may be activated lazily - so:
    s1.getAttribute(key);
    // now activation MUST have occurred
    assertTrue(events.size()==1);
    {
      Pair pair=(Pair)events.get(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("sessionDidActivate"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(s1.getWrapper()==e.getSession());
    }
    events.clear();
  }
  
  public void
  LATERtestReplication(Manager manager, SessionPool pool)
  throws Exception
  {
    Session s0=pool.take(manager);
    //s0.init(_manager, "1234", 0L, 60, 60);
    List events=ActivationListener._events;
    events.clear();

    String key="test";
    ActivationListener l=new ActivationListener();
    s0.setAttribute(key, l);
    byte[] buffer=marshall(s0);
    s0.getAttribute(key); // force lazy activation to trigger...
    // listener should now have been passivated and activated...
    assertTrue(events.size()==2);
    {
      Pair pair=(Pair)events.remove(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("sessionWillPassivate"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(s0.getWrapper()==e.getSession());
    }
    {
      Pair pair=(Pair)events.remove(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("sessionDidActivate"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(s0.getWrapper()==e.getSession());
    }

    Session s1=_distributableSessionPool.take(_distributableManager);
    demarshall(s1, buffer);
    //s1.setWadiManager(_manager); // TODO - yeugh!
    // listener should not have yet been activated (we do it lazily)
    assertTrue(events.size()==0);
    s1.getAttribute(key);
    // now it should...
    assertTrue(events.size()==1);
    {
      Pair pair=(Pair)events.remove(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("sessionDidActivate"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(s1.getWrapper()==e.getSession());
    }
  }

//  public void
//    testMigration()
//  {
//    Session impl1=new Session();
//    String id="test-httpsession";
//    impl1.init(null, id, System.currentTimeMillis(), 30*60, 30*60);
//    impl1.setWadiManager(_manager);
//    HttpSession s1=impl1.getWrapper();
//    List events=ActivationListener._events;
//    events.clear();
//
//    String key="test";
//    ActivationListener l=new ActivationListener();
//    impl1.setAttribute(key, l, false);
//
//    FilePassivationStrategy fmp=new FilePassivationStrategy(new File("/tmp"));
//    fmp.setStreamingStrategy(new GZIPStreamingStrategy());
//    assertTrue(fmp.passivate(impl1));
//
//    // listener should now have been passivated
//    assertTrue(events.size()==1);
//    {
//      Pair pair=(Pair)events.remove(0);
//      assertTrue(pair!=null);
//      assertTrue(pair.getType().equals("sessionWillPassivate"));
//      HttpSessionEvent e=pair.getEvent();
//      assertTrue(s1==e.getSession());
//    }
//
//    Session impl2=new Session();
//    assertTrue(fmp.activate(id, impl2));
//    impl2.setWadiManager(_manager);
//    HttpSession s2=impl2.getWrapper();
//    // listener should not have yet been activated (we do it lazily)
//    assertTrue(events.size()==0);
//    impl2.getAttribute(key);
//    // now it should...
//    assertTrue(events.size()==1);
//    {
//      Pair pair=(Pair)events.remove(0);
//      assertTrue(pair!=null);
//      assertTrue(pair.getType().equals("sessionDidActivate"));
//      HttpSessionEvent e=pair.getEvent();
//      assertTrue(s2==e.getSession());
//    }
//    assertTrue(events.size()==0);
//
//    assertTrue(s1.getValueNames().length==s2.getValueNames().length);
//    Enumeration e1=s1.getAttributeNames();
//    Enumeration e2=s2.getAttributeNames();
//    while (e1.hasMoreElements() && e2.hasMoreElements())
//      assertTrue(((String)e1.nextElement()).equals((String)e2.nextElement()));
//  }

//   public void
//     testLotsOfSessions()
//     throws Exception
//   {
//     _manager.stop();
//     _manager.removeEventListener(_listener); //  otherwise we get lots of events :-)
//     _manager.start();

//     //  put some code in here to figure out how much mem they use...
//     long start=System.currentTimeMillis();
//     int numSessions=100;	//000;
//     HttpSession[] sessions=new HttpSession[numSessions];

//     for (int i=0;i<numSessions;i++)
//     {
//       sessions[i]=_manager.newHttpSession();
//       // session impls are locked, since they are assumed to still be
//       // in use by the thread that created them...
//       sessions[i].setAttribute("foo", "bar");
//     }

//     for (int i=0;i<numSessions;i++)
//     {
//       String id=sessions[i].getId();
//       sessions[i].invalidate();
//       sessions[i]=null;
//       _manager.remove(id);
//     }

//     sessions=null;
//     long stop=System.currentTimeMillis();

//     if (_log.isInfoEnabled()) _log.info("create/destroy "+numSessions+" sessions: "+(stop-start)+" millis");

//     _manager.stop();
//     _manager.addEventListener(_listener);
//     _manager.start();
//   }

  //   public void
  //     testDistributedSessions()
  //   {
  //     _manager.start();
  //     _log.info("Testing Distributed Sessions");

  //     try
  //     {
  //       HttpSession session=_manager.newHttpSession();
  //       javax.naming.InitialContext context=new javax.naming.InitialContext();
  //       // can session handle EJBHome ?
  //       session.setAttribute("home", javax.rmi.PortableRemoteObject.narrow(context.lookup("jetty/CMPState"), org.mortbay.j2ee.session.interfaces.CMPStateHome.class));
  //       org.mortbay.j2ee.session.interfaces.CMPStateHome home=(org.mortbay.j2ee.session.interfaces.CMPStateHome)session.getAttribute("home");;
  //       // can session handle EJBObject ?
  //       session.setAttribute("state", javax.rmi.PortableRemoteObject.narrow(home.create("/","state",60,60*60*24), org.mortbay.j2ee.session.interfaces.CMPState.class));
  //       org.mortbay.j2ee.session.interfaces.CMPState state=(org.mortbay.j2ee.session.interfaces.CMPState)session.getAttribute("state");;
  //     }
  //     catch (Exception e)
  //     {
  //       System.err.println("something went wrong"+e);
  //     }

  //     try
  //     {
  //       HttpSession session=_manager.newHttpSession();
  //       int interval=2;
  //       String key="time-out test";
  //       session.setAttribute(key, ""+interval+" seconds");
  //       ((org.mortbay.j2ee.session.StateAdaptor)session).setLastAccessedTime(System.currentTimeMillis());
  //       session.setMaxInactiveInterval(interval);
  //       Thread.sleep(1+interval*1000);
  //       session.getAttribute(key);
  //       assertTrue(false);
  //     }
  //     catch (IllegalStateException ignore)
  //     {
  //       // test succeeded
  //     }
  //     catch (Exception e)
  //     {
  //       System.err.println("something went wrong"+e);
  //     }

  //     try
  //     {
  //       HttpSession session=_manager.newHttpSession();
  //       int interval=5;
  //       String key="time-out test";
  //       session.setAttribute(key, ""+interval+" seconds");
  //       ((org.mortbay.j2ee.session.StateAdaptor)session).setLastAccessedTime(System.currentTimeMillis());
  //       session.setMaxInactiveInterval(interval);
  //       Thread.sleep((interval-2)*1000);
  //       session.getAttribute(key);
  //       // test succeeded
  //     }
  //     catch (IllegalStateException e)
  //     {
  //       System.err.println("something went wrong");
  //       e.printStackTrace();
  //       assertTrue(false);
  //     }
  //     catch (Exception e)
  //     {
  //       System.err.println("something went wrong"+e);
  //     }

  //     _log.info("Tested Distributed Sessions");
  //     _manager.stop();
  //   }
}

// we need to test the difference between distributed and local
// sessions accepting different types of attribute...
