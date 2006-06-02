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

package org.codehaus.wadi.test;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Manager;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.ValueFactory;
import org.codehaus.wadi.ValueHelper;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.impl.ClusteredManager;
import org.codehaus.wadi.impl.DummyContextualiser;
import org.codehaus.wadi.impl.DummyManagerConfig;
import org.codehaus.wadi.impl.StandardManager;
import org.codehaus.wadi.impl.SimpleSessionPool;
import org.codehaus.wadi.impl.SimpleValuePool;
import org.codehaus.wadi.impl.TomcatSessionIdFactory;
import org.codehaus.wadi.web.AttributesFactory;
import org.codehaus.wadi.web.Router;
import org.codehaus.wadi.web.WADIHttpSession;
import org.codehaus.wadi.web.WebSession;
import org.codehaus.wadi.web.WebSessionFactory;
import org.codehaus.wadi.web.WebSessionPool;
import org.codehaus.wadi.web.WebSessionWrapperFactory;
import org.codehaus.wadi.web.impl.DistributableSession;
import org.codehaus.wadi.web.impl.DummyRouter;
import org.codehaus.wadi.web.impl.StandardAttributesFactory;
import org.codehaus.wadi.web.impl.StandardSessionFactory;
import org.codehaus.wadi.web.impl.StandardSessionWrapperFactory;
import org.codehaus.wadi.web.impl.StandardValueFactory;

/**
 * Test WADI's HttpSession implementation
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class TestHttpSession extends TestCase {

    protected Log                     _log=LogFactory.getLog(TestHttpSession.class);
    protected final String            _clusterName="WADI.TEST";
    protected Listener                _listener;
    protected List                    _events=new ArrayList();
    protected Map                     _sessionMap=new HashMap();
    protected boolean                 _accessOnLoad=true;
    protected Router                  _router=new DummyRouter();
    // Standard
    protected Contextualiser          _standardContextualiser=new DummyContextualiser();
    protected WebSessionWrapperFactory   _standardSessionWrapperFactory=new StandardSessionWrapperFactory();
    protected SessionIdFactory        _standardSessionIdFactory=new TomcatSessionIdFactory();
    protected AttributesFactory       _standardAttributesFactory=new StandardAttributesFactory();
    protected WebSessionFactory          _standardSessionFactory=new StandardSessionFactory();
    protected WebSessionPool             _standardSessionPool=new SimpleSessionPool(_standardSessionFactory);
    protected ValueFactory            _standardValueFactory=new StandardValueFactory();
    protected ValuePool               _standardValuePool=new SimpleValuePool(_standardValueFactory);
    protected StandardManager         _standardManager=new StandardManager(_standardSessionPool, _standardAttributesFactory, _standardValuePool, _standardSessionWrapperFactory, _standardSessionIdFactory, _standardContextualiser, _sessionMap, _router, true);
    protected DummyManagerConfig      _standardConfig=new DummyManagerConfig();
//    // Distributable
//    protected HttpProxy               _httpProxy=new StandardHttpProxy("jsessionid");
//    protected InetSocketAddress       _httpAddress; // see setUp();
//    protected Contextualiser          _distributableContextualiser=new DummyContextualiser();
//    protected Streamer                _streamer=new SimpleStreamer();
//    protected AttributesFactory       _distributedAttributesFactory=new DistributableAttributesFactory();
//    protected SessionFactory          _distributableSessionFactory=new DistributableSessionFactory();
//    protected SessionPool             _distributableSessionPool=new SimpleSessionPool(_distributableSessionFactory);
//    protected ValueFactory            _distributableValueFactory=new DistributableValueFactory();
//    protected ValuePool               _distributableValuePool=new SimpleValuePool(_distributableValueFactory);
//    protected StandardManager         _distributableManager=new ClusteredManager(_distributableSessionPool, _distributedAttributesFactory, _distributableValuePool, _standardSessionWrapperFactory, _standardSessionIdFactory, _distributableContextualiser, _sessionMap, _router, true, _streamer, _accessOnLoad, new DummyReplicater(), _httpAddress, _httpProxy, _clusterUri, _clusterName, "node0", 24);
//    protected DummyManagerConfig      _distributableConfig=new DummyManagerConfig();
//    // LazyValue
//    protected SessionPool             _lazyValueSessionPool=new SimpleSessionPool(_distributableSessionFactory);
//    protected ValueFactory            _lazyValueFactory=new LazyValueFactory();
//    protected ValuePool               _lazyValuePool=new SimpleValuePool(_lazyValueFactory);
//    protected StandardManager         _lazyValueManager=new ClusteredManager(_lazyValueSessionPool, _distributedAttributesFactory, _lazyValuePool, _standardSessionWrapperFactory, _standardSessionIdFactory, _distributableContextualiser, _sessionMap, _router, true, _streamer, _accessOnLoad, new DummyReplicater(), _httpAddress, _httpProxy, _clusterUri, _clusterName, "node1", 24);
//    protected DummyManagerConfig      _lazyValueConfig=new DummyManagerConfig();
//    // LazyAttributes
//    protected SessionPool             _lazyAttributesSessionPool=new SimpleSessionPool(_distributableSessionFactory);
//    protected AttributesFactory       _lazyAttributesFactory=new LazyAttributesFactory();
//    protected StandardManager         _lazyAttributesManager=new ClusteredManager(_lazyAttributesSessionPool, _lazyAttributesFactory,_distributableValuePool, _standardSessionWrapperFactory, _standardSessionIdFactory, _distributableContextualiser, _sessionMap, _router, true, _streamer, _accessOnLoad, new DummyReplicater(), _httpAddress, _httpProxy, _clusterUri, _clusterName, "node2", 24);
//    protected DummyManagerConfig      _lazyAttributesConfig=new DummyManagerConfig();
//    // LazyBoth
//    protected SessionPool             _lazyBothSessionPool=new SimpleSessionPool(_distributableSessionFactory);
//    protected StandardManager         _lazyBothManager=new ClusteredManager(_lazyBothSessionPool, _lazyAttributesFactory,_lazyValuePool, _standardSessionWrapperFactory, _standardSessionIdFactory, _distributableContextualiser, _sessionMap, _router, true, _streamer, _accessOnLoad, new DummyReplicater(), _httpAddress, _httpProxy, _clusterUri, _clusterName, "node3", 24);
//    protected DummyManagerConfig      _lazyBothConfig=new DummyManagerConfig();


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
    //     public HttpSession create().getWrapper() {return create().getSession();}
    //     public void addEventListener(EventListener l){addSessionListener(l);}
    //   }

    class Listener
    implements
    HttpSessionListener,
    HttpSessionAttributeListener,
    HttpSessionBindingListener,
    Serializable
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
        // HttpSessionActivationListener
        public void sessionDidActivate   (HttpSessionEvent e){e.getSession().getId();_events.add(new Pair("sessionDidActivate",e));}
        public void sessionWillPassivate (HttpSessionEvent e){e.getSession().getId();_events.add(new Pair("sessionWillPassivate",e));}
    }

    static class BindingListener
    implements
    HttpSessionBindingListener,
    Serializable
    {
        public static List _events=new ArrayList();
        // HttpSessionBindingListener
        public void valueBound           (HttpSessionBindingEvent e) {e.getSession().getId();_events.add(new Pair("valueBound",e));}
        public void valueUnbound         (HttpSessionBindingEvent e) {e.getSession().getId();_events.add(new Pair("valueUnbound",e));}
    }

    static class SerialisationListener
    implements
    Serializable
    {
        public static List _events=new ArrayList();
        protected static Log _log=LogFactory.getLog(SerialisationListener.class);

        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            _events.add(new Pair("serialised",null));
        }

        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            _events.add(new Pair("deserialised",null));
        }

    }

    protected void setUp() throws Exception {
//        _httpAddress=new InetSocketAddress(InetAddress.getLocalHost(), 8888);
        _listener=new Listener();
        HttpSessionListener[] sessionListeners=new HttpSessionListener[]{_listener};
        HttpSessionAttributeListener[] attributeListeners=new HttpSessionAttributeListener[]{_listener};
        _standardManager.setSessionListeners(sessionListeners);
        _standardManager.setAttributelisteners(attributeListeners);
        _standardManager.init(_standardConfig);
//        _distributableManager.setSessionListeners(sessionListeners);
//        _distributableManager.setAttributelisteners(attributeListeners);
//        _distributableManager.init(_distributableConfig);
//        _lazyValueManager.setSessionListeners(sessionListeners);
//        _lazyValueManager.setAttributelisteners(attributeListeners);
//        _lazyValueManager.init(_lazyValueConfig);
//        _lazyAttributesManager.setSessionListeners(sessionListeners);
//        _lazyAttributesManager.setAttributelisteners(attributeListeners);
//        _lazyAttributesManager.init(_lazyAttributesConfig);
//        _lazyBothManager.setSessionListeners(sessionListeners);
//        _lazyBothManager.setAttributelisteners(attributeListeners);
//        _lazyBothManager.init(_lazyBothConfig);
    }

    protected void tearDown() {
//        _lazyBothManager.setSessionListeners(null);
//        _lazyBothManager.setAttributelisteners(null);
//        _lazyBothManager.destroy();
//        _lazyAttributesManager.setSessionListeners(null);
//        _lazyAttributesManager.setAttributelisteners(null);
//        _lazyAttributesManager.destroy();
//        _lazyValueManager.setSessionListeners(null);
//        _lazyValueManager.setAttributelisteners(null);
//        _lazyValueManager.destroy();
//        _distributableManager.setSessionListeners(null);
//        _distributableManager.setAttributelisteners(null);
//        _distributableManager.destroy();
        _standardManager.setSessionListeners(null);
        _standardManager.setAttributelisteners(null);
        _standardManager.destroy();
        _listener=null;
    }

    //----------------------------------------

    public void testCreateHttpSession() {
        testCreateHttpSession(_standardManager);
//        testCreateHttpSession(_distributableManager);
//        testCreateHttpSession(_lazyValueManager);
//        testCreateHttpSession(_lazyAttributesManager);
//        testCreateHttpSession(_lazyBothManager);
    }

    public void
    testCreateHttpSession(Manager manager)
    {
        _events.clear();

        // create a session
        HttpSession session= ((WADIHttpSession) manager.create(null)).getWrapper();
        assertTrue(!session.getAttributeNames().hasMoreElements());
        assertTrue(session.getValueNames().length==0);
        Pair pair=(Pair)_events.remove(0);
        assertTrue(pair!=null);
        assertTrue(pair.getType().equals("sessionCreated"));
        assertTrue(pair.getEvent().getSession()==session);
        assertTrue(_events.size()==0);
    }

    public void
    testDestroyHttpSessionWithListener() throws Exception {
        testDestroyHttpSessionWithListener(_standardManager);
//        testDestroyHttpSessionWithListener(_distributableManager);
//        testDestroyHttpSessionWithListener(_lazyValueManager);
//        testDestroyHttpSessionWithListener(_lazyAttributesManager);
//        testDestroyHttpSessionWithListener(_lazyBothManager);
    }

    public void
    testDestroyHttpSessionWithListener(StandardManager manager)
    throws Exception
    {
        // create session
        WADIHttpSession session= (WADIHttpSession) manager.create(null);
        HttpSession wrapper=session.getWrapper();

        // set up test
        String key="foo";
        Object val=new Listener();
        wrapper.setAttribute(key, val);
        wrapper.setAttribute("bar", "baz");
        _events.clear();

        // destroy session
        manager.destroy(null, session);

        // analyse results
        assertTrue(_events.size()==4);
        {
            Pair pair=(Pair)_events.get(0);
            assertTrue(pair!=null);
            assertTrue(pair.getType().equals("valueUnbound"));
            HttpSessionEvent e=pair.getEvent();
            assertTrue(wrapper==e.getSession());
            HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
            assertTrue(be.getName()==key);
            assertTrue(be.getValue()==val);
        }
        {
            Pair pair=(Pair)_events.get(1);
            assertTrue(pair!=null);
            assertTrue(pair.getType().equals("attributeRemoved"));
            HttpSessionEvent e=pair.getEvent();
            assertTrue(wrapper==e.getSession());
            //HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
        }
        {
            Pair pair=(Pair)_events.get(2);
            assertTrue(pair!=null);
            assertTrue(pair.getType().equals("attributeRemoved"));
            HttpSessionEvent e=pair.getEvent();
            assertTrue(wrapper==e.getSession());
            //HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
        }
        {
            Pair pair=(Pair)_events.get(3);
            assertTrue(pair!=null);
            assertTrue(pair.getType().equals("sessionDestroyed"));
            HttpSessionEvent e=pair.getEvent();
            assertTrue(wrapper==e.getSession());
        }
        _events.clear();

        // TODO -
        // try without an HttpSessionAttributeListener registered
        // try with a serialised session
        // track what is serialised and what is not...
    }

    public void testDestroyHttpSessionWithoutListener() throws Exception {
        testDestroyHttpSessionWithoutListener(_standardManager);
//        testDestroyHttpSessionWithoutListener(_distributableManager);
//        testDestroyHttpSessionWithoutListener(_lazyValueManager);
//        testDestroyHttpSessionWithoutListener(_lazyAttributesManager);
//        testDestroyHttpSessionWithoutListener(_lazyBothManager);
    }

    public void
    testDestroyHttpSessionWithoutListener(StandardManager manager)
    throws Exception
    {
        // remove Listener
        manager.setSessionListeners(new HttpSessionListener[]{});
        manager.setAttributelisteners(new HttpSessionAttributeListener[]{});

        // create session
        WADIHttpSession session= (WADIHttpSession) manager.create(null);
        HttpSession wrapper=session.getWrapper();

        // set up test
        String key="foo";
        Object val=new Listener();
        wrapper.setAttribute(key, val);
        wrapper.setAttribute("bar", "baz");
        _events.clear();

        // destroy session
        manager.destroy(null, session);

        // analyse results
        assertTrue(_events.size()==1);
        {
            Pair pair=(Pair)_events.get(0);
            assertTrue(pair!=null);
            assertTrue(pair.getType().equals("valueUnbound"));
            HttpSessionEvent e=pair.getEvent();
            assertTrue(wrapper==e.getSession());
            HttpSessionBindingEvent be=(HttpSessionBindingEvent)e;
            assertTrue(be.getName()==key);
            assertTrue(be.getValue()==val);
        }
        _events.clear();

        // TODO -
        // try without an HttpSessionAttributeListener registered
        // try with a serialised session
        // track what is serialised and what is not...
    }


    public void
    testInvalidate() throws Exception {
        testInvalidate(_standardManager);
//        testInvalidate(_distributableManager);
//        testInvalidate(_lazyValueManager);
//        testInvalidate(_lazyAttributesManager);
//        testInvalidate(_lazyBothManager);
    }

    public void
    testInvalidate(Manager manager)
    {
        HttpSession session= ((WADIHttpSession) manager.create(null)).getWrapper();
        session.invalidate();
        // TODO - what should we test here ?
    }

    public void
    testSetAttribute()
    {
        testSetAttribute(_standardManager);
//        testSetAttribute(_distributableManager);
//        testSetAttribute(_lazyValueManager);
//        testSetAttribute(_lazyAttributesManager);
//        testSetAttribute(_lazyBothManager);
    }

    public void
    testSetAttribute(Manager manager)
    {
        HttpSession session= ((WADIHttpSession) manager.create(null)).getWrapper();
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
//        testPutValue(_distributableManager);
//        testPutValue(_lazyValueManager);
//        testPutValue(_lazyAttributesManager);
//        testPutValue(_lazyBothManager);
    }

    public void
    testPutValue(Manager manager)
    {
        HttpSession session= ((WADIHttpSession) manager.create(null)).getWrapper();
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
//        testGetAttribute(_distributableManager);
//        testGetAttribute(_lazyValueManager);
//        testGetAttribute(_lazyAttributesManager);
//        testGetAttribute(_lazyBothManager);
    }

    public void
    testGetAttribute(Manager manager)
    {
        HttpSession session= ((WADIHttpSession) manager.create(null)).getWrapper();
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
//        testGetValue(_distributableManager);
//        testGetValue(_lazyValueManager);
//        testGetValue(_lazyAttributesManager);
//        testGetValue(_lazyBothManager);
    }

    public void
    testGetValue(Manager manager)
    {
        HttpSession session= ((WADIHttpSession) manager.create(null)).getWrapper();
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
//        testRemoveAttribute(_distributableManager);
//        testRemoveAttribute(_lazyValueManager);
//        testRemoveAttribute(_lazyAttributesManager);
//        testRemoveAttribute(_lazyBothManager);
    }

    public void
    testRemoveAttribute(Manager manager)
    {
        HttpSession session= ((WADIHttpSession) manager.create(null)).getWrapper();
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

        // try removing it again !
        session.removeAttribute(key);
        assertTrue(_events.size()==0);

    }

    public void
    testRemoveValue()
    {
        testRemoveValue(_standardManager);
//        testRemoveValue(_distributableManager);
//        testRemoveValue(_lazyValueManager);
//        testRemoveValue(_lazyAttributesManager);
//        testRemoveValue(_lazyBothManager);
    }

    public void
    testRemoveValue(Manager manager)
    {
        HttpSession session= ((WADIHttpSession) manager.create(null)).getWrapper();
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
//        testSetAttributeNull(_distributableManager);
//        testSetAttributeNull(_lazyValueManager);
//        testSetAttributeNull(_lazyAttributesManager);
//        testSetAttributeNull(_lazyBothManager);
    }

    public void
    testSetAttributeNull(Manager manager)
    {
        HttpSession session= ((WADIHttpSession) manager.create(null)).getWrapper();
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
//        testPutValueNull(_distributableManager);
//        testPutValueNull(_lazyValueManager);
//        testPutValueNull(_lazyAttributesManager);
//        testPutValueNull(_lazyBothManager);
    }

    public void
    testPutValueNull(Manager manager)
    {
        HttpSession session= ((WADIHttpSession) manager.create(null)).getWrapper();
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
//        testReplaceAttribute(_distributableManager);
//        testReplaceAttribute(_lazyValueManager);
//        testReplaceAttribute(_lazyAttributesManager);
//        testReplaceAttribute(_lazyBothManager);
    }

    public void
    testReplaceAttribute(Manager manager)
    {
        HttpSession session= ((WADIHttpSession) manager.create(null)).getWrapper();
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
//        testReplaceValue(_distributableManager);
//        testReplaceValue(_lazyValueManager);
//        testReplaceValue(_lazyAttributesManager);
//        testReplaceValue(_lazyBothManager);
    }

    public void
    testReplaceValue(Manager manager)
    {
        HttpSession session= ((WADIHttpSession) manager.create(null)).getWrapper();
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
//        testGetAttributeNames(_distributableManager);
//        testGetAttributeNames(_lazyValueManager);
//        testGetAttributeNames(_lazyAttributesManager);
//        testGetAttributeNames(_lazyBothManager);
    }

    public void
    testGetAttributeNames(Manager manager)
    {
        HttpSession session= ((WADIHttpSession) manager.create(null)).getWrapper();
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
//        testGetValueNames(_distributableManager);
//        testGetValueNames(_lazyValueManager);
//        testGetValueNames(_lazyAttributesManager);
//        testGetValueNames(_lazyBothManager);
    }

    public void
    testGetValueNames(Manager manager)
    {
        HttpSession session= ((WADIHttpSession) manager.create(null)).getWrapper();
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
        testMaxInactiveInterval(_standardManager);
//        testMaxInactiveInterval(_distributableManager);
//        testMaxInactiveInterval(_lazyValueManager);
//        testMaxInactiveInterval(_lazyAttributesManager);
//        testMaxInactiveInterval(_lazyBothManager);
    }

    public void
    testMaxInactiveInterval(Manager manager)
    {
        HttpSession session= ((WADIHttpSession) manager.create(null)).getWrapper();
        {
            int interval=60*60;
            session.setMaxInactiveInterval(interval);
            assertTrue(session.getMaxInactiveInterval()==interval);
        }
        {
            int interval=-1;
            session.setMaxInactiveInterval(interval);
            assertTrue(session.getMaxInactiveInterval()==interval);
        }
    }

    public void
    testIsNew()
    {
        testIsNew(_standardManager, _standardSessionPool);
//        testIsNew(_distributableManager, _distributableSessionPool);
//        testIsNew(_lazyValueManager, _distributableSessionPool);
//        testIsNew(_lazyAttributesManager, _distributableSessionPool);
//        testIsNew(_lazyBothManager, _distributableSessionPool);
    }

    public void
    testIsNew(Manager manager, WebSessionPool sessionPool)
    {
        WADIHttpSession s= (WADIHttpSession) sessionPool.take();
        HttpSession session=s.getWrapper();
        assertTrue(session.isNew());
        s.setLastAccessedTime(System.currentTimeMillis()+1);
        assertTrue(!session.isNew());
    }

    public void
    testNullName()
    {
        testNullName(_standardManager);
//        testNullName(_distributableManager);
//        testNullName(_lazyValueManager);
//        testNullName(_lazyAttributesManager);
//        testNullName(_lazyBothManager);
    }

    public void
    testNullName(Manager manager)
    {
        HttpSession session= ((WADIHttpSession) manager.create(null)).getWrapper();
        try{session.setAttribute(null,"a");assertTrue(false);}catch(IllegalArgumentException e){}
        try{session.getAttribute(null);assertTrue(false);}catch(IllegalArgumentException e){}
        try{session.removeAttribute(null);assertTrue(false);}catch(IllegalArgumentException e){}
        try{session.putValue(null,"a");assertTrue(false);}catch(IllegalArgumentException e){}
        try{session.getValue(null);assertTrue(false);}catch(IllegalArgumentException e){}
        try{session.removeValue(null);assertTrue(false);}catch(IllegalArgumentException e){}
    }

    public void testStandard() throws Exception
    {
        testStandardValidation(_standardManager);
    }

//    public void testDistributable() throws Exception
//    {
//        testActivation(_distributableManager, _distributableSessionPool);
//        testActivation(_lazyValueManager, _distributableSessionPool);
//        testActivation(_lazyAttributesManager, _distributableSessionPool);
//        testActivation(_lazyBothManager, _distributableSessionPool);
//        testMigration(_distributableManager, _distributableSessionPool);
//        testMigration(_lazyValueManager, _distributableSessionPool);
//        testMigration(_lazyAttributesManager, _distributableSessionPool);
//        testMigration(_lazyBothManager, _distributableSessionPool);
//        testDistributableValidation(_distributableManager);
//        testDistributableValidation(_lazyValueManager);
//        testDistributableValidation(_lazyAttributesManager);
//        testDistributableValidation(_lazyBothManager);
//        testCustomSerialisation((ClusteredManager)_distributableManager);
//        testCustomSerialisation((ClusteredManager)_lazyValueManager);
//        testCustomSerialisation((ClusteredManager)_lazyAttributesManager);
//        testCustomSerialisation((ClusteredManager)_lazyBothManager);
//        testDeserialisationOnReplacementWithListener((ClusteredManager)_distributableManager);
//        testDeserialisationOnReplacementWithListener((ClusteredManager)_lazyValueManager);
//        testDeserialisationOnReplacementWithListener((ClusteredManager)_lazyAttributesManager);
//        testDeserialisationOnReplacementWithListener((ClusteredManager)_lazyBothManager);
//        testDeserialisationOnReplacementWithoutListener((ClusteredManager)_distributableManager);
//        testDeserialisationOnReplacementWithoutListener((ClusteredManager)_lazyValueManager);
//        testDeserialisationOnReplacementWithoutListener((ClusteredManager)_lazyAttributesManager);
//        testDeserialisationOnReplacementWithoutListener((ClusteredManager)_lazyBothManager);
//    }

    public void
    testActivation(Manager manager, WebSessionPool pool) // Distributable only
    throws Exception
    {
        DistributableSession s0=(DistributableSession)pool.take();
        List events=ActivationListener._events;
        events.clear();

        String key="test";
        ActivationListener l=new ActivationListener();
        s0.setAttribute(key, l);
        byte[] bytes=s0.getBodyAsByteArray();
        assertTrue(events.size()==1);
        {
            Pair pair=(Pair)events.get(0);
            assertTrue(pair!=null);
            assertTrue(pair.getType().equals("sessionWillPassivate"));
            HttpSessionEvent e=pair.getEvent();
            assertTrue(s0.getWrapper()==e.getSession());
        }
        events.clear();

        DistributableSession s1=(DistributableSession)pool.take();
        s1.setBodyAsByteArray(bytes);
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
    testMigration(Manager manager, WebSessionPool pool) // Distributable only
    throws Exception
    {
        // test that a 'copy' (the basis of all motion/migration) completely copies
        // a sessions contents, by VALUE not reference...
        DistributableSession s0=(DistributableSession)pool.take();
        long time=System.currentTimeMillis();
        s0.init(time, time, 60*60, "a-session");
        for (int i=0; i<10; i++)
            s0.setAttribute("key-"+i, "val-"+i);

        Thread.sleep(1);

        DistributableSession s1=(DistributableSession)pool.take();
        s1.copy(s0);
        DistributableSession s2=(DistributableSession)pool.take();
        s2.copy(s0);

        assertTrue(s1.getCreationTime()==s2.getCreationTime());
        assertTrue(s1.getLastAccessedTime()==s2.getLastAccessedTime());
        assertTrue(s1.getMaxInactiveInterval()==s2.getMaxInactiveInterval());
        assertTrue(s1.getName()!=s2.getName());
        assertTrue(s1.getName().equals(s2.getName()));
        assertTrue(s1.getAttributeNameSet().equals(s2.getAttributeNameSet()));
        {
            Iterator i=s1.getAttributeNameSet().iterator();
            Iterator j=s2.getAttributeNameSet().iterator();
            while(i.hasNext() && j.hasNext())
                assertTrue(i.next()!=j.next());
        }
        for (Iterator i=s1.getAttributeNameSet().iterator(); i.hasNext(); ) {
            String key=(String)i.next();
            assertTrue(s1.getAttribute(key)!=s2.getAttribute(key));
            assertTrue(s1.getAttribute(key).equals(s2.getAttribute(key)));
        }

    }


    public void testDeserialisationOnReplacementWithListener(ClusteredManager manager) throws Exception {
        testDeserialisationOnReplacement(manager);
        // TODO - test context level events here...
    }

    public void testDeserialisationOnReplacementWithoutListener(ClusteredManager manager) throws Exception {
        manager.setSessionListeners(new HttpSessionListener[]{});
        manager.setAttributelisteners(new HttpSessionAttributeListener[]{});
        testDeserialisationOnReplacement(manager);
        // TODO - test context level events here...
    }

    public void testDeserialisationOnReplacement(ClusteredManager manager) throws Exception {
        DistributableSession s0=(DistributableSession)manager.create(null);
        DistributableSession s1=(DistributableSession)manager.create(null);

        s0.setAttribute("dummy", "dummy");
        s0.setAttribute("binding-listener", new BindingListener());
        s0.setAttribute("activation-listener", new ActivationListener());
        _events.clear();
        List activationEvents=ActivationListener._events;
        activationEvents.clear();
        List bindingEvents=BindingListener._events;
        bindingEvents.clear();

        s1.copy(s0);

        s1.setAttribute("activation-listener", new ActivationListener());

        assertTrue(activationEvents.size()==2);
        {
            Pair pair=(Pair)activationEvents.get(0);
            assertTrue(pair!=null);
            assertTrue(pair.getType().equals("sessionWillPassivate"));
            HttpSessionEvent e=pair.getEvent();
            assertTrue(s0.getWrapper()==e.getSession());
        }
        {
            Pair pair=(Pair)activationEvents.get(1);
            assertTrue(pair!=null);
            assertTrue(pair.getType().equals("sessionDidActivate"));
            HttpSessionEvent e=pair.getEvent();
            assertTrue(s1.getWrapper()==e.getSession());
        }
        activationEvents.clear();

        s1.setAttribute("binding-listener", new BindingListener());

        assertTrue(bindingEvents.size()==2);
        {
            Pair pair=(Pair)bindingEvents.get(0);
            assertTrue(pair!=null);
            assertTrue(pair.getType().equals("valueUnbound"));
            HttpSessionEvent e=pair.getEvent();
            assertTrue(s1.getWrapper()==e.getSession());
        }
        {
            Pair pair=(Pair)bindingEvents.get(1);
            assertTrue(pair!=null);
            assertTrue(pair.getType().equals("valueBound"));
            HttpSessionEvent e=pair.getEvent();
            assertTrue(s1.getWrapper()==e.getSession());
        }
        bindingEvents.clear();

    }

    public void
    testStandardValidation(Manager manager) // Distributable only
    throws Exception
    {
        HttpSession session= ((WADIHttpSession) manager.create(null)).getWrapper();
        // try some Serializables...
        session.setAttribute("0", "foo");
        session.setAttribute("1", new Integer(1));
        session.setAttribute("2", new Float(1.1));
        session.setAttribute("3", new Date());
        session.setAttribute("4", new byte[256]);
        // and some non-Serializables...
        session.setAttribute("5", new Object());
    }

    public void
    testDistributableValidation(Manager manager) // Distributable only
    throws Exception
    {
        HttpSession session= ((WADIHttpSession) manager.create(null)).getWrapper();
        // try some Serializables...
        session.setAttribute("0", "foo");
        session.setAttribute("1", new Integer(1));
        session.setAttribute("2", new Float(1.1));
        session.setAttribute("3", new Date());
        session.setAttribute("4", new byte[256]);
        // and some non-Serializables...
        try {
            session.setAttribute("5", new Object());
            assertTrue(false);
        } catch (IllegalArgumentException ignore) {
            // expected
        }
    }

    static class NotSerializable {
        final String _content;
        public NotSerializable(String content) {_content=content;}
    }

    static class IsSerializable implements Serializable {
        String _content;
        public IsSerializable(){/* empty */} // for Serialising...
        public IsSerializable(String content){_content=content;} // for Helper
        private Object readResolve() {return new NotSerializable(_content);}
    }

    static class NotSerializableHelper implements ValueHelper {
        public Serializable replace(Object object) {return new IsSerializable(((NotSerializable)object)._content);}
    }

    public void testCustomSerialisation(ClusteredManager manager) throws Exception {
        String content="foo";
        NotSerializable val0=new NotSerializable(content);
        Class type=val0.getClass();
        manager.registerHelper(Integer.class, new NotSerializableHelper()); // this one will not be used
        manager.registerHelper(type, new NotSerializableHelper());

        WebSession s0=manager.create(null);
        s0.setAttribute(content, val0);
        WebSession s1=manager.create(null);
        s1.setBodyAsByteArray(s0.getBodyAsByteArray());
        NotSerializable val1=(NotSerializable)s1.getAttribute(content);
        assertTrue(val0._content.equals(val1._content));

        assertTrue(manager.deregisterHelper(type));
        assertTrue(!manager.deregisterHelper(type));
        assertTrue(manager.deregisterHelper(Integer.class));
        assertTrue(!manager.deregisterHelper(Integer.class));

    }

//    public void
//    LATERtestReplication(Manager manager, SessionPool pool)
//    throws Exception
//    {
//        Session s0=pool.take(manager);
//        //s0.init(_manager, "1234", 0L, 60, 60);
//        List events=ActivationListener._events;
//        events.clear();
//
//        String key="test";
//        ActivationListener l=new ActivationListener();
//        s0.setAttribute(key, l);
//        byte[] buffer=marshall(s0);
//        s0.getAttribute(key); // force lazy activation to trigger...
//        // listener should now have been passivated and activated...
//        assertTrue(events.size()==2);
//        {
//            Pair pair=(Pair)events.remove(0);
//            assertTrue(pair!=null);
//            assertTrue(pair.getType().equals("sessionWillPassivate"));
//            HttpSessionEvent e=pair.getEvent();
//            assertTrue(s0.getWrapper()==e.getSession());
//        }
//        {
//            Pair pair=(Pair)events.remove(0);
//            assertTrue(pair!=null);
//            assertTrue(pair.getType().equals("sessionDidActivate"));
//            HttpSessionEvent e=pair.getEvent();
//            assertTrue(s0.getWrapper()==e.getSession());
//        }
//
//        Session s1=_distributableSessionPool.take(_distributableManager);
//        demarshall(s1, buffer);
//        //s1.setWadiManager(_manager); // TODO - yeugh!
//        // listener should not have yet been activated (we do it lazily)
//        assertTrue(events.size()==0);
//        s1.getAttribute(key);
//        // now it should...
//        assertTrue(events.size()==1);
//        {
//            Pair pair=(Pair)events.remove(0);
//            assertTrue(pair!=null);
//            assertTrue(pair.getType().equals("sessionDidActivate"));
//            HttpSessionEvent e=pair.getEvent();
//            assertTrue(s1.getWrapper()==e.getSession());
//        }
//    }

//  public void
//  testMigration()
//  {
//  Session impl1=new Session();
//  String id="test-httpsession";
//  impl1.init(null, id, System.currentTimeMillis(), 30*60, 30*60);
//  impl1.setWadiManager(_manager);
//  HttpSession s1=impl1.getWrapper();
//  List events=ActivationListener._events;
//  events.clear();
//
//  String key="test";
//  ActivationListener l=new ActivationListener();
//  impl1.setAttribute(key, l, false);
//
//  FilePassivationStrategy fmp=new FilePassivationStrategy(new File("/tmp/wadi/"+System.getProperty("node.name")));
//  fmp.setStreamingStrategy(new GZIPStreamingStrategy());
//  assertTrue(fmp.passivate(impl1));
//
//  // listener should now have been passivated
//  assertTrue(events.size()==1);
//  {
//  Pair pair=(Pair)events.remove(0);
//  assertTrue(pair!=null);
//  assertTrue(pair.getType().equals("sessionWillPassivate"));
//  HttpSessionEvent e=pair.getEvent();
//  assertTrue(s1==e.getSession());
//  }
//
//  Session impl2=new Session();
//  assertTrue(fmp.activate(id, impl2));
//  impl2.setWadiManager(_manager);
//  HttpSession s2=impl2.getWrapper();
//  // listener should not have yet been activated (we do it lazily)
//  assertTrue(events.size()==0);
//  impl2.getAttribute(key);
//  // now it should...
//  assertTrue(events.size()==1);
//  {
//  Pair pair=(Pair)events.remove(0);
//  assertTrue(pair!=null);
//  assertTrue(pair.getType().equals("sessionDidActivate"));
//  HttpSessionEvent e=pair.getEvent();
//  assertTrue(s2==e.getSession());
//  }
//  assertTrue(events.size()==0);
//
//  assertTrue(s1.getValueNames().length==s2.getValueNames().length);
//  Enumeration e1=s1.getAttributeNames();
//  Enumeration e2=s2.getAttributeNames();
//  while (e1.hasMoreElements() && e2.hasMoreElements())
//  assertTrue(((String)e1.nextElement()).equals((String)e2.nextElement()));
//  }

//  public void
//  testLotsOfSessions()
//  throws Exception
//  {
//  _manager.stop();
//  _manager.removeEventListener(_listener); //  otherwise we get lots of events :-)
//  _manager.start();

//  //  put some code in here to figure out how much mem they use...
//  long start=System.currentTimeMillis();
//  int numSessions=100;	//000;
//  HttpSession[] sessions=new HttpSession[numSessions];

//  for (int i=0;i<numSessions;i++)
//  {
//  sessions[i]=_manager.create().getWrapper();
//  // session impls are locked, since they are assumed to still be
//  // in use by the thread that created them...
//  sessions[i].setAttribute("foo", "bar");
//  }

//  for (int i=0;i<numSessions;i++)
//  {
//  String id=sessions[i].getId();
//  sessions[i].invalidate();
//  sessions[i]=null;
//  _manager.remove(id);
//  }

//  sessions=null;
//  long stop=System.currentTimeMillis();

//  if (_log.isInfoEnabled()) _log.info("create/destroy "+numSessions+" sessions: "+(stop-start)+" millis");

//  _manager.stop();
//  _manager.addEventListener(_listener);
//  _manager.start();
//  }

    //   public void
    //     testDistributedSessions()
    //   {
    //     _manager.start();
    //     _log.info("Testing Distributed Sessions");

    //     try
    //     {
    //       HttpSession session=_manager.create().getWrapper();
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
    //       HttpSession session=_manager.create().getWrapper();
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
    //       HttpSession session=_manager.create().getWrapper();
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

//    public void testAtomicAttributes() throws Exception {
//        testAtomicAttributes(_distributableManager);
//        testAtomicAttributes(_lazyAttributesManager);
//    }
//
//    public void testAtomicAttributes(StandardManager manager) throws Exception {
//        Session sess0=manager.create();
//        Object val=new String("value");
//        String key0="foo";
//        String key1="bar";
//        sess0.setAttribute(key0, val);
//        sess0.setAttribute(key1, val);
//        assertTrue(sess0.getAttribute(key0)==sess0.getAttribute(key1));
//        byte[] bytes=sess0.getBodyAsByteArray();
//        Session sess1=manager.create();
//        sess1.setBodyAsByteArray(bytes);
//        assertTrue(sess1.getAttribute(key0)==sess1.getAttribute(key1)); // after deserialisation values are still '='
//        assertTrue(sess0.getAttribute(key0)!=sess1.getAttribute(key1));
//        assertTrue(sess0.getAttribute(key0).equals(sess1.getAttribute(key1)));
//        assertTrue(sess1.getAttribute(key0)!=sess0.getAttribute(key1));
//        assertTrue(sess1.getAttribute(key0).equals(sess0.getAttribute(key1)));
//    }
//
//    public void testSeparateAttributes() throws Exception {
//        testSeparateAttributes(_lazyValueManager);
//        testSeparateAttributes(_lazyBothManager);
//    }

    public void testSeparateAttributes(Manager manager) throws Exception {
        WebSession sess0=manager.create(null);
        Object val=new String("value");
        String key0="foo";
        String key1="bar";
        sess0.setAttribute(key0, val);
        sess0.setAttribute(key1, val);
        assertTrue(sess0.getAttribute(key0)==sess0.getAttribute(key1));
        byte[] bytes=sess0.getBodyAsByteArray();
        WebSession sess1=manager.create(null);
        sess1.setBodyAsByteArray(bytes);
        assertTrue(sess1.getAttribute(key0)!=sess1.getAttribute(key1)); // after deserialisation values are no longer '='
        assertTrue(sess1.getAttribute(key0).equals(sess1.getAttribute(key1)));
    }

//    public void testLaziness() throws Exception {
//        // lazy attributes:
//
//        // (1) add an activation and a serialisation listener, migrate, get one, both should be called
//        {
//            StandardManager manager=_lazyAttributesManager;
//            manager.setSessionListeners(new HttpSessionListener[]{});
//            manager.setAttributelisteners(new HttpSessionAttributeListener[]{});
//            Session s0=manager.create();
//            s0.setAttribute("activation-listener", new ActivationListener());
//            s0.setAttribute("serialisation-listener", new SerialisationListener());
//            Session s1=manager.create();
//            s1.setBodyAsByteArray(s0.getBodyAsByteArray());
//            List activationEvents=ActivationListener._events;
//            activationEvents.clear();
//            List serialisationEvents=SerialisationListener._events;
//            serialisationEvents.clear();
//
//            s1.getAttribute("serialisation-listener");
//            assertTrue(activationEvents.size()==1);
//            activationEvents.clear();
//            assertTrue(serialisationEvents.size()==1);
//            serialisationEvents.clear();
//        }
//
//        // (2) add an activation, a binding and a serialisation listener, migrate, invalidate - all should be called
//        {
//            StandardManager manager=_lazyAttributesManager;
//            manager.setSessionListeners(new HttpSessionListener[]{});
//            manager.setAttributelisteners(new HttpSessionAttributeListener[]{});
//            Session s0=manager.create();
//            s0.setAttribute("activation-listener", new ActivationListener());
//            s0.setAttribute("binding-listener", new BindingListener());
//            s0.setAttribute("serialisation-listener", new SerialisationListener());
//            Session s1=manager.create();
//            s1.setBodyAsByteArray(s0.getBodyAsByteArray());
//            List activationEvents=ActivationListener._events;
//            activationEvents.clear();
//            List bindingEvents=BindingListener._events;
//            bindingEvents.clear();
//            List serialisationEvents=SerialisationListener._events;
//            serialisationEvents.clear();
//
//            manager.destroy(s1);
//
//            assertTrue(activationEvents.size()==1);
//            activationEvents.clear();
//            assertTrue(bindingEvents.size()==1);
//            bindingEvents.clear();
//            assertTrue(serialisationEvents.size()==1);
//            serialisationEvents.clear();
//        }
//
//        // LazyValue
//        // (1) add an activation and a serialisation listener, migrate, get one, only that one should be called
//        {
//            StandardManager manager=_lazyValueManager;
//            manager.setSessionListeners(new HttpSessionListener[]{});
//            manager.setAttributelisteners(new HttpSessionAttributeListener[]{});
//            Session s0=manager.create();
//            s0.setAttribute("activation-listener", new ActivationListener());
//            s0.setAttribute("serialisation-listener", new SerialisationListener());
//            Session s1=manager.create();
//            s1.setBodyAsByteArray(s0.getBodyAsByteArray());
//            List activationEvents=ActivationListener._events;
//            activationEvents.clear();
//            List serialisationEvents=SerialisationListener._events;
//            serialisationEvents.clear();
//
//            s1.getAttribute("activation-listener");
//            assertTrue(activationEvents.size()==1);
//            assertTrue(serialisationEvents.size()==0);
//
//            s1.getAttribute("serialisation-listener");
//            assertTrue(activationEvents.size()==1);
//            assertTrue(serialisationEvents.size()==1);
//
//            activationEvents.clear();
//            serialisationEvents.clear();
//        }
//
//        // (2) add an activation, a binding and a serialisation listener, migrate, invalidate - serialisation should not be called
//        // LATER - none should be called, until they are dereffed from the event itself...
//        {
//            StandardManager manager=_lazyValueManager;
//            manager.setSessionListeners(new HttpSessionListener[]{});
//            manager.setAttributelisteners(new HttpSessionAttributeListener[]{});
//
//            Session s0=manager.create();
//            s0.setAttribute("activation-listener", new ActivationListener());
//            s0.setAttribute("binding-listener", new BindingListener());
//            s0.setAttribute("serialisation-listener", new SerialisationListener());
//            Session s1=manager.create();
//            s1.setBodyAsByteArray(s0.getBodyAsByteArray());
//            List activationEvents=ActivationListener._events;
//            activationEvents.clear();
//            List bindingEvents=BindingListener._events;
//            bindingEvents.clear();
//            List serialisationEvents=SerialisationListener._events;
//            serialisationEvents.clear();
//
//            manager.destroy(s1);
//
//            assertTrue(activationEvents.size()==1);
//            assertTrue(bindingEvents.size()==1);
//            assertTrue(serialisationEvents.size()==0);
//
//            activationEvents.clear();
//            bindingEvents.clear();
//        }
//
//    }

    public void
    testRest()
    {
        testRest(_standardManager);

    }

    public void
    testRest(Manager manager)
    {
        long start=System.currentTimeMillis();
        HttpSession session= ((WADIHttpSession) manager.create(null)).getWrapper();
        long end=System.currentTimeMillis();
        assertTrue(session.getSessionContext().getSession(null)==null);
        assertTrue(session.getSessionContext().getIds()!=null);
        session.getServletContext(); // cannot really test unless inside a container... - TODO
        assertTrue(session.getCreationTime()>=start && session.getCreationTime()<=end);
        assertTrue(session.getCreationTime()==session.getLastAccessedTime());
    }
}

// we need to test the difference between distributed and local
// sessions accepting different types of attribute...
