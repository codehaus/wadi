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
import org.codehaus.wadi.jetty.HttpSessionImpl;
import org.codehaus.wadi.jetty.Manager;
import org.codehaus.wadi.plugins.FilePassivationStrategy;
import org.codehaus.wadi.shared.ObjectInputStream;

import org.mortbay.jetty.servlet.WebApplicationHandler;

//----------------------------------------

public class TestHttpSession
  extends TestCase
{
  protected Log         _log=LogFactory.getLog(TestHttpSession.class);
  protected Manager     _manager;
  protected Listener    _listener;
  protected List        _events=new ArrayList();

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
    throws Exception
  {
    _manager=new Manager();
    _manager.setBucketName("foo"); // TODO: we should be able to work without this...
    org.mortbay.jetty.servlet.WebApplicationHandler handler=new org.mortbay.jetty.servlet.WebApplicationHandler();
    handler.initialize(new org.mortbay.http.HttpContext());
    handler.start();
    _manager.initialize(handler);
    _listener=new Listener();
    _manager.addEventListener(_listener);
    _manager.start();
  }

  protected void
    tearDown()
    throws InterruptedException
  {
    _manager.stop();
    _manager.removeEventListener(_listener);
    _listener=null;
    _manager=null;
  }

  //----------------------------------------

  public void
    testCreateHttpSession()
  {
    _events.clear();

    // create a session
    HttpSession session=_manager.newHttpSession();
    assertTrue(!session.getAttributeNames().hasMoreElements());
    assertTrue(session.getValueNames().length==0);
    Pair pair=(Pair)_events.remove(0);
    assertTrue(pair!=null);
    assertTrue(pair.getType().equals("sessionCreated"));
    assertTrue(pair.getEvent().getSession()==session);
    assertTrue(_events.size()==0);
  }

//  public void
//    testDestroyHttpSession()
//    throws Exception
//  {
//    int interval=10;//TODO - dodgy test - how can it be improved...?
//    int oldInterval= _manager.getHouseKeepingInterval();
//    _manager.stop();
//    _manager.setHouseKeepingInterval(interval); // seconds
//    _manager.removeEventListener(_listener);
//    _manager.addEventListener(_listener);
//    _manager.start();
//    // set up test
//    HttpSession session=_manager.newHttpSession();
//    String key="foo";
//    Object val=new Listener();
//    session.setAttribute(key, val);
//    _events.clear();
//
//    // destroy session
//    session.invalidate();
//    Thread.sleep(2000*interval); //2*interval - millis
//
//    {
//      Pair pair=(Pair)_events.remove(0);
//      assertTrue(pair!=null);
//      assertTrue(pair.getType().equals("sessionDestroyed"));
//      HttpSessionEvent e=pair.getEvent();
//      assertTrue(session==e.getSession());
//    }
//    {
//      Pair pair=(Pair)_events.remove(0);
//      assertTrue(pair!=null);
//      assertTrue(pair.getType().equals("valueUnbound"));
//      HttpSessionEvent e=pair.getEvent();
//      assertTrue(session==e.getSession());
//      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
//      assertTrue(be.getName()==key);
//      assertTrue(be.getValue()==val);
//    }
//    {
//      Pair pair=(Pair)_events.remove(0);
//      assertTrue(pair!=null);
//      assertTrue(pair.getType().equals("attributeRemoved"));
//      HttpSessionEvent e=pair.getEvent();
//      assertTrue(session==e.getSession());
//      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
//      assertTrue(be.getName()==key);
//      assertTrue(be.getValue()==val);
//    }
//    assertTrue(_events.size()==0);
//
//    _manager.stop();
//    _manager.setHouseKeepingInterval(oldInterval);
//    _manager.start();
//  }

  public void
    testSetAttribute()
  {
    HttpSession session=_manager.newHttpSession();
    _events.clear();

    String key="foo";
    Object val=new Listener();
    session.setAttribute(key, val);

    {
      Pair pair=(Pair)_events.remove(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("valueBound"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==val);
    }
    {
      Pair pair=(Pair)_events.remove(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("attributeAdded"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==val);
    }
    assertTrue(_events.size()==0);
  }

  public void
    testPutValue()
  {
    HttpSession session=_manager.newHttpSession();
    _events.clear();

    String key="foo";
    Object val=new Listener();
    session.putValue(key, val);

    {
      Pair pair=(Pair)_events.remove(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("valueBound"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==val);
    }
    {
      Pair pair=(Pair)_events.remove(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("attributeAdded"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==val);
    }
    assertTrue(_events.size()==0);
  }

  public void
    testGetAttribute()
  {
    HttpSession session=_manager.newHttpSession();
    String key="foo";
    Object val=new Listener();
    session.setAttribute(key, val);
    _events.clear();

    assertTrue(session.getAttribute(key)==val);
  }

  public void
    testGetValue()
  {
    HttpSession session=_manager.newHttpSession();
    String key="foo";
    Object val=new Listener();
    session.setAttribute(key, val);
    _events.clear();

    assertTrue(session.getValue(key)==val);
  }

  public void
    testRemoveAttribute()
  {
    HttpSession session=_manager.newHttpSession();
    String key="foo";
    Object val=new Listener();
    session.setAttribute(key, val);
    _events.clear();

    session.removeAttribute(key);

    {
      Pair pair=(Pair)_events.remove(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("valueUnbound"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==val);
    }
    {
      Pair pair=(Pair)_events.remove(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("attributeRemoved"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==val);
    }
    assertTrue(session.getAttribute(key)==null);
    assertTrue(_events.size()==0);
  }

  public void
    testRemoveValue()
  {
    HttpSession session=_manager.newHttpSession();
    String key="foo";
    Object val=new Listener();
    session.setAttribute(key, val);
    _events.clear();

    session.removeValue(key);

    {
      Pair pair=(Pair)_events.remove(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("valueUnbound"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==val);
    }
    {
      Pair pair=(Pair)_events.remove(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("attributeRemoved"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==val);
    }
    assertTrue(session.getAttribute(key)==null);
    assertTrue(_events.size()==0);
  }

  public void
    testSetAttributeNull()
  {
    HttpSession session=_manager.newHttpSession();
    String key="foo";
    Object val=new Listener();
    session.setAttribute(key, val);
    _events.clear();

    session.setAttribute(key, null);

    {
      Pair pair=(Pair)_events.remove(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("valueUnbound"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==val);
    }
    {
      Pair pair=(Pair)_events.remove(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("attributeRemoved"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==val);
    }
    assertTrue(session.getAttribute(key)==null);
    assertTrue(_events.size()==0);
  }

  public void
    testPutValueNull()
  {
    HttpSession session=_manager.newHttpSession();
    String key="foo";
    Object val=new Listener();
    session.setAttribute(key, val);
    _events.clear();

    session.putValue(key, null);

    {
      Pair pair=(Pair)_events.remove(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("valueUnbound"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==val);
    }
    {
      Pair pair=(Pair)_events.remove(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("attributeRemoved"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(session==e.getSession());
      HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
      assertTrue(be.getName()==key);
      assertTrue(be.getValue()==val);
    }
    assertTrue(session.getValue(key)==null);
    assertTrue(_events.size()==0);
  }

  public void
    testReplaceAttribute()
  {
    HttpSession session=_manager.newHttpSession();
    String key="foo";
    Object oldVal=new Listener();
    Object newVal=new Listener();
    session.setAttribute(key, oldVal);
    _events.clear();

    session.setAttribute(key, newVal);
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

  public void
    testReplaceValue()
  {
    HttpSession session=_manager.newHttpSession();
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
    HttpSession session=_manager.newHttpSession();
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
    HttpSession session=_manager.newHttpSession();
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

  public void
    testInvalidate()
  {
    HttpSession session=_manager.newHttpSession();
    session.getId();
    session.invalidate();
    try{session.getId();assertTrue(false);}catch(IllegalStateException e){}
  }

  public void
    testIsNew()
  {
    HttpSession session=_manager.newHttpSession();
    assertTrue(session.isNew());
    ((org.codehaus.wadi.jetty.HttpSession)session).access();
    assertTrue(!session.isNew());
  }

  public void
    testNullName()
  {
    HttpSession session=_manager.newHttpSession();
    try{session.setAttribute(null,"a");assertTrue(false);}catch(IllegalArgumentException e){}
    try{session.getAttribute(null);assertTrue(false);}catch(IllegalArgumentException e){}
    try{session.removeAttribute(null);assertTrue(false);}catch(IllegalArgumentException e){}
    try{session.putValue(null,"a");assertTrue(false);}catch(IllegalArgumentException e){}
    try{session.getValue(null);assertTrue(false);}catch(IllegalArgumentException e){}
    try{session.removeValue(null);assertTrue(false);}catch(IllegalArgumentException e){}
  }

  public void
    testActivation()
  {
    HttpSession s0=_manager.newHttpSession();
    List events=ActivationListener._events;
    events.clear();

    String key="test";
    ActivationListener l=new ActivationListener();
    s0.setAttribute(key, l);
    byte[] buffer=ObjectInputStream.marshall(s0);
    s0.getAttribute(key); // force lazy activation to trigger...
    // listener should now have been passivated and activated...
    assertTrue(events.size()==2);
    {
      Pair pair=(Pair)events.remove(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("sessionWillPassivate"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(s0==e.getSession());
    }
    {
      Pair pair=(Pair)events.remove(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("sessionDidActivate"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(s0==e.getSession());
    }

    HttpSession s1=(HttpSession)ObjectInputStream.demarshall(buffer);
    ((org.codehaus.wadi.shared.HttpSession)s1).setWadiManager(_manager); // TODO - yeugh!
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
      assertTrue(s1==e.getSession());
    }
  }

  public void
    testMigration()
  {
    HttpSessionImpl impl1=new HttpSessionImpl();
    String id="test-httpsession";
    impl1.init(null, id, System.currentTimeMillis(), 30*60, 30*60);
    List events=ActivationListener._events;
    events.clear();

    String key="test";
    ActivationListener l=new ActivationListener();
    impl1.setAttribute(key, l, false);

    FilePassivationStrategy fmp=new FilePassivationStrategy(new File("/tmp"));
    fmp.passivate(impl1);

    // listener should now have been passivated
    _log.info("SIZE: "+events.size());
    assertTrue(events.size()==1);
    {
      Pair pair=(Pair)events.remove(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("sessionWillPassivate"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(impl1==e.getSession());
    }

    HttpSessionImpl impl2=(HttpSessionImpl)fmp.activate(id);
    // listener should not have yet been activated (we do it lazily)
    assertTrue(events.size()==0);
    impl2.getAttribute(key);
    // now it should...
    assertTrue(events.size()==1);
    {
      Pair pair=(Pair)events.remove(0);
      assertTrue(pair!=null);
      assertTrue(pair.getType().equals("sessionDidActivate"));
      HttpSessionEvent e=pair.getEvent();
      assertTrue(impl1==e.getSession());
    }

    //    assertTrue(impl1.equals(impl2));
  }

//   public void
//     testLotsOfSessions()
//     throws Exception
//   {
//     _manager.stop();
//     _manager.removeEventListener(_listener); // otherwise we get lots of events :-)
//     _manager.start();

//     // put some code in here to figure out how much mem they use...
//     long start=System.currentTimeMillis();
//     int numSessions=100;//000;
//     HttpSession[] sessions=new HttpSession[numSessions];

//     for (int i=0;i<numSessions;i++)
//     {
//       sessions[i]=_manager.newHttpSession();
//       sessions[i].setAttribute("foo", "bar");
//     }

//     for (int i=0;i<numSessions;i++)
//     {
//       sessions[i].invalidate();
//       sessions[i]=null;
//     }

//     sessions=null;
//     long stop=System.currentTimeMillis();

//     _log.info("create/destroy "+numSessions+" sessions: "+(stop-start)+" millis");

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
