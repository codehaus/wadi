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

package org.codehaus.wadi.web;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
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
import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.OswegoConcurrentMotableMap;
import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.core.contextualiser.DummyContextualiser;
import org.codehaus.wadi.core.manager.BasicSessionMonitor;
import org.codehaus.wadi.core.manager.ClusteredManager;
import org.codehaus.wadi.core.manager.DummyManagerConfig;
import org.codehaus.wadi.core.manager.DummyRouter;
import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.manager.Router;
import org.codehaus.wadi.core.manager.SessionIdFactory;
import org.codehaus.wadi.core.manager.SessionMonitor;
import org.codehaus.wadi.core.manager.StandardManager;
import org.codehaus.wadi.core.manager.TomcatSessionIdFactory;
import org.codehaus.wadi.core.session.AttributesFactory;
import org.codehaus.wadi.core.session.StandardAttributesFactory;
import org.codehaus.wadi.core.session.StandardValueFactory;
import org.codehaus.wadi.core.session.ValueFactory;
import org.codehaus.wadi.core.util.SimpleStreamer;
import org.codehaus.wadi.replication.DummyReplicaterFactory;
import org.codehaus.wadi.web.impl.BasicHttpInvocationContextFactory;
import org.codehaus.wadi.web.impl.StandardSessionWrapperFactory;

/**
 * Test WADI's HttpSession implementation
 * 
 * TODO - reimplement as a true JUnit test
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class TestHttpSession extends TestCase {
    protected Log _log = LogFactory.getLog(TestHttpSession.class);
    protected final String _clusterName = "WADI.TEST";
    protected Listener _listener;
    protected List events = new ArrayList();
    protected ConcurrentMotableMap motableMap = new OswegoConcurrentMotableMap();
    protected boolean accessOnLoad = true;
    protected Router router = new DummyRouter();

    // Standard
    protected SessionMonitor sessionMonitor = new BasicSessionMonitor();
    protected Contextualiser contextualiser = new DummyContextualiser();
    protected WebSessionWrapperFactory webSessionWrapperFactory = new StandardSessionWrapperFactory();
    protected SessionIdFactory sessionIdFactory = new TomcatSessionIdFactory();
    protected ValueFactory valueFactory = new StandardValueFactory();
    protected AttributesFactory attributesFactory = new StandardAttributesFactory(valueFactory);
    protected WebSessionFactory webSessionFactory = new BasicWebSessionFactory(attributesFactory,
            new SimpleStreamer(),
            new DummyReplicaterFactory(),
            router,
            webSessionWrapperFactory);

    protected StandardManager _standardManager = new StandardManager(webSessionFactory,
            sessionIdFactory,
            contextualiser,
            motableMap,
            router,
            sessionMonitor,
            new BasicHttpInvocationContextFactory());
    protected DummyManagerConfig _standardConfig = new DummyManagerConfig();
    private WADIHttpSessionListener wadiHttpSessionListener;

    public TestHttpSession(String name) {
        super(name);
    }

    static class Pair implements Serializable {
        String _type;

        HttpSessionEvent _event;

        Pair(String type, HttpSessionEvent event) {
            _type = type;
            _event = event;
        }

        String getType() {
            return _type;
        }

        HttpSessionEvent getEvent() {
            return _event;
        }

        public String toString() {
            return "<" + _event.getSession().getId() + ":" + _type + ">";
        }
    }

    class Listener implements HttpSessionListener, HttpSessionAttributeListener, HttpSessionBindingListener,
            Serializable {
        // HttpSessionListener
        public void sessionCreated(HttpSessionEvent e) {
            e.getSession().getId();
            events.add(new Pair("sessionCreated", e));
        }

        public void sessionDestroyed(HttpSessionEvent e) {
            e.getSession().getId();
            events.add(new Pair("sessionDestroyed", e));
        }

        // HttpSessionAttributeListener
        public void attributeAdded(HttpSessionBindingEvent e) {
            e.getSession().getId();
            events.add(new Pair("attributeAdded", e));
        }

        public void attributeRemoved(HttpSessionBindingEvent e) {
            e.getSession().getId();
            events.add(new Pair("attributeRemoved", e));
        }

        public void attributeReplaced(HttpSessionBindingEvent e) {
            e.getSession().getId();
            events.add(new Pair("attributeReplaced", e));
        }

        // HttpSessionBindingListener
        public void valueBound(HttpSessionBindingEvent e) {
            e.getSession().getId();
            events.add(new Pair("valueBound", e));
        }

        public void valueUnbound(HttpSessionBindingEvent e) {
            e.getSession().getId();
            events.add(new Pair("valueUnbound", e));
        }
    }

    static class ActivationListener implements HttpSessionActivationListener, Serializable {
        public static List _events = new ArrayList();

        // HttpSessionActivationListener
        public void sessionDidActivate(HttpSessionEvent e) {
            e.getSession().getId();
            _events.add(new Pair("sessionDidActivate", e));
        }

        public void sessionWillPassivate(HttpSessionEvent e) {
            e.getSession().getId();
            _events.add(new Pair("sessionWillPassivate", e));
        }
    }

    static class BindingListener implements HttpSessionBindingListener, Serializable {
        public static List _events = new ArrayList();

        // HttpSessionBindingListener
        public void valueBound(HttpSessionBindingEvent e) {
            e.getSession().getId();
            _events.add(new Pair("valueBound", e));
        }

        public void valueUnbound(HttpSessionBindingEvent e) {
            e.getSession().getId();
            _events.add(new Pair("valueUnbound", e));
        }
    }

    static class SerialisationListener implements Serializable {
        public static List _events = new ArrayList();

        protected static Log _log = LogFactory.getLog(SerialisationListener.class);

        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            _events.add(new Pair("serialised", null));
        }

        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            _events.add(new Pair("deserialised", null));
        }

    }

    protected void setUp() throws Exception {
        _listener = new Listener();
        HttpSessionListener[] sessionListeners = new HttpSessionListener[] { _listener };
        wadiHttpSessionListener = new WADIHttpSessionListener(sessionListeners);
        sessionMonitor.addSessionListener(wadiHttpSessionListener);

        HttpSessionAttributeListener[] attributeListeners = new HttpSessionAttributeListener[] { _listener };
        webSessionFactory.getWebSessionConfig().setAttributeListeners(attributeListeners);
        _standardManager.init(_standardConfig);
    }

    protected void tearDown() {
        sessionMonitor.removeSessionListener(wadiHttpSessionListener);
        webSessionFactory.getWebSessionConfig().setAttributeListeners(null);
        _listener = null;
    }

    // ----------------------------------------

    public void testCreateHttpSession() {
        testCreateHttpSession(_standardManager);
    }

    public void testCreateHttpSession(Manager manager) {
        events.clear();

        // create a session
        HttpSession session = ((WADIHttpSession) manager.create(null)).getWrapper();
        assertTrue(!session.getAttributeNames().hasMoreElements());
        assertTrue(session.getValueNames().length == 0);
        Pair pair = (Pair) events.remove(0);
        assertTrue(pair != null);
        assertTrue(pair.getType().equals("sessionCreated"));
        assertTrue(pair.getEvent().getSession() == session);
        assertTrue(events.size() == 0);
    }

    public void testDestroyHttpSessionWithListener() throws Exception {
        testDestroyHttpSessionWithListener(_standardManager);
    }

    public void testDestroyHttpSessionWithListener(StandardManager manager) throws Exception {
        WADIHttpSession session = (WADIHttpSession) manager.create(null);
        HttpSession wrapper = session.getWrapper();

        String key = "foo";
        Object val = new Listener();
        wrapper.setAttribute(key, val);
        wrapper.setAttribute("bar", "baz");
        events.clear();

        session.destroy();

        assertTrue(events.size() == 4);
        {
            Pair pair = (Pair) events.get(0);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("valueUnbound"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(wrapper == e.getSession());
            HttpSessionBindingEvent be = (HttpSessionBindingEvent) e;
            assertTrue(be.getName() == key);
            assertTrue(be.getValue() == val);
        }
        {
            Pair pair = (Pair) events.get(1);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("attributeRemoved"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(wrapper == e.getSession());
        }
        {
            Pair pair = (Pair) events.get(2);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("attributeRemoved"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(wrapper == e.getSession());
        }
        {
            Pair pair = (Pair) events.get(3);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("sessionDestroyed"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(wrapper == e.getSession());
        }
        events.clear();
    }

    public void testDestroyHttpSessionWithoutListener() throws Exception {
        webSessionFactory.getWebSessionConfig().setAttributeListeners(null);
        sessionMonitor.removeSessionListener(wadiHttpSessionListener);
        wadiHttpSessionListener = new WADIHttpSessionListener(new HttpSessionListener[0]);
        sessionMonitor.addSessionListener(wadiHttpSessionListener);
        testDestroyHttpSessionWithoutListener(_standardManager);
        sessionMonitor.removeSessionListener(wadiHttpSessionListener);
    }

    public void testDestroyHttpSessionWithoutListener(StandardManager manager) throws Exception {
        WADIHttpSession session = (WADIHttpSession) manager.create(null);
        HttpSession wrapper = session.getWrapper();

        String key = "foo";
        Object val = new Listener();
        wrapper.setAttribute(key, val);
        wrapper.setAttribute("bar", "baz");
        events.clear();

        session.destroy();

        // analyse results
        assertTrue(events.size() == 1);
        {
            Pair pair = (Pair) events.get(0);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("valueUnbound"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(wrapper == e.getSession());
            HttpSessionBindingEvent be = (HttpSessionBindingEvent) e;
            assertTrue(be.getName() == key);
            assertTrue(be.getValue() == val);
        }
        events.clear();
    }

    public void testInvalidate() throws Exception {
        testInvalidate(_standardManager);
    }

    public void testInvalidate(Manager manager) {
        HttpSession session = ((WADIHttpSession) manager.create(null)).getWrapper();
        session.invalidate();
        // TODO - what should we test here ?
    }

    public void testSetAttribute() {
        testSetAttribute(_standardManager);
    }

    public void testSetAttribute(Manager manager) {
        HttpSession session = ((WADIHttpSession) manager.create(null)).getWrapper();
        assertTrue(events.size() == 1); // sessionCreated
        events.clear();

        String key = "foo";
        Object val = new Listener();
        session.setAttribute(key, val);
        assertTrue(events.size() == 2); // valueBound, attributeAdded
        {
            Pair pair = (Pair) events.get(0);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("valueBound"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(session == e.getSession());
            HttpSessionBindingEvent be = (HttpSessionBindingEvent) e;
            assertTrue(be.getName() == key);
            assertTrue(be.getValue() == val);
        }
        {
            Pair pair = (Pair) events.get(1);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("attributeAdded"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(session == e.getSession());
            HttpSessionBindingEvent be = (HttpSessionBindingEvent) e;
            assertTrue(be.getName() == key);
            assertTrue(be.getValue() == val);
        }
        events.clear();
        assertTrue(events.size() == 0);
    }

    public void testPutValue() {
        testPutValue(_standardManager);
    }

    public void testPutValue(Manager manager) {
        HttpSession session = ((WADIHttpSession) manager.create(null)).getWrapper();
        assertTrue(events.size() == 1); // sessionCreated
        events.clear();

        String key = "foo";
        Object val = new Listener();
        session.putValue(key, val);
        assertTrue(events.size() == 2); // valueBound, attributeAdded
        {
            Pair pair = (Pair) events.get(0);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("valueBound"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(session == e.getSession());
            HttpSessionBindingEvent be = (HttpSessionBindingEvent) e;
            assertTrue(be.getName() == key);
            assertTrue(be.getValue() == val);
        }
        {
            Pair pair = (Pair) events.get(1);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("attributeAdded"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(session == e.getSession());
            HttpSessionBindingEvent be = (HttpSessionBindingEvent) e;
            assertTrue(be.getName() == key);
            assertTrue(be.getValue() == val);
        }
        events.clear();
        assertTrue(events.size() == 0);
    }

    public void testGetAttribute() {
        testGetAttribute(_standardManager);
    }

    public void testGetAttribute(Manager manager) {
        HttpSession session = ((WADIHttpSession) manager.create(null)).getWrapper();
        String key = "foo";
        Object val = new Listener();
        session.setAttribute(key, val);
        events.clear();

        assertTrue(session.getAttribute(key) == val);
    }

    public void testGetValue() {
        testGetValue(_standardManager);
    }

    public void testGetValue(Manager manager) {
        HttpSession session = ((WADIHttpSession) manager.create(null)).getWrapper();
        String key = "foo";
        Object val = new Listener();
        session.setAttribute(key, val);
        events.clear();

        assertTrue(session.getValue(key) == val);
    }

    public void testRemoveAttribute() {
        testRemoveAttribute(_standardManager);
    }

    public void testRemoveAttribute(Manager manager) {
        HttpSession session = ((WADIHttpSession) manager.create(null)).getWrapper();
        assertTrue(events.size() == 1); // sessionCreated
        String key = "foo";
        Object val = new Listener();
        session.setAttribute(key, val);
        assertTrue(events.size() == 3); // valueBound, attributeAdded
        events.clear();

        session.removeAttribute(key);
        assertTrue(events.size() == 2); // valueUnBound, attributeRemoved
        {
            Pair pair = (Pair) events.get(0);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("valueUnbound"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(session == e.getSession());
            HttpSessionBindingEvent be = (HttpSessionBindingEvent) e;
            assertTrue(be.getName() == key);
            assertTrue(be.getValue() == val);
        }
        {
            Pair pair = (Pair) events.get(1);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("attributeRemoved"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(session == e.getSession());
            HttpSessionBindingEvent be = (HttpSessionBindingEvent) e;
            assertTrue(be.getName() == key);
            assertTrue(be.getValue() == val);
        }
        events.clear();
        assertTrue(events.size() == 0);
        assertTrue(session.getAttribute(key) == null);

        // try removing it again !
        session.removeAttribute(key);
        assertTrue(events.size() == 0);

    }

    public void testRemoveValue() {
        testRemoveValue(_standardManager);
    }

    public void testRemoveValue(Manager manager) {
        HttpSession session = ((WADIHttpSession) manager.create(null)).getWrapper();
        assertTrue(events.size() == 1); // sessionCreated
        String key = "foo";
        Object val = new Listener();
        session.setAttribute(key, val);
        assertTrue(events.size() == 3); // valueBound, attributeAdded
        events.clear();

        session.removeValue(key);
        assertTrue(events.size() == 2); // valueUnBound, attributeRemoved

        {
            Pair pair = (Pair) events.get(0);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("valueUnbound"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(session == e.getSession());
            HttpSessionBindingEvent be = (HttpSessionBindingEvent) e;
            assertTrue(be.getName() == key);
            assertTrue(be.getValue() == val);
        }
        {
            Pair pair = (Pair) events.get(1);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("attributeRemoved"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(session == e.getSession());
            HttpSessionBindingEvent be = (HttpSessionBindingEvent) e;
            assertTrue(be.getName() == key);
            assertTrue(be.getValue() == val);
        }
        events.clear();
        assertTrue(events.size() == 0);
        assertTrue(session.getAttribute(key) == null);
    }

    public void testSetAttributeNull() {
        testSetAttributeNull(_standardManager);
    }

    public void testSetAttributeNull(Manager manager) {
        HttpSession session = ((WADIHttpSession) manager.create(null)).getWrapper();
        assertTrue(events.size() == 1); // sessionCreated
        String key = "foo";
        Object val = new Listener();
        session.setAttribute(key, val);
        assertTrue(events.size() == 3); // valueBound, attributeAdded
        events.clear();

        session.setAttribute(key, null);
        assertTrue(events.size() == 2); // valueUnBound, attributeRemoved

        {
            Pair pair = (Pair) events.get(0);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("valueUnbound"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(session == e.getSession());
            HttpSessionBindingEvent be = (HttpSessionBindingEvent) e;
            assertTrue(be.getName() == key);
            assertTrue(be.getValue() == val);
        }
        {
            Pair pair = (Pair) events.get(1);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("attributeRemoved"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(session == e.getSession());
            HttpSessionBindingEvent be = (HttpSessionBindingEvent) e;
            assertTrue(be.getName() == key);
            assertTrue(be.getValue() == val);
        }
        events.clear();
        assertTrue(events.size() == 0);
        assertTrue(session.getAttribute(key) == null);
    }

    public void testPutValueNull() {
        testPutValueNull(_standardManager);
    }

    public void testPutValueNull(Manager manager) {
        HttpSession session = ((WADIHttpSession) manager.create(null)).getWrapper();
        assertTrue(events.size() == 1); // sessionCreated
        String key = "foo";
        Object val = new Listener();
        session.setAttribute(key, val);
        assertTrue(events.size() == 3); // valueBound, attributeAdded
        events.clear();

        session.putValue(key, null);
        assertTrue(events.size() == 2); // valueUnBound, attributeRemoved

        {
            Pair pair = (Pair) events.get(0);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("valueUnbound"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(session == e.getSession());
            HttpSessionBindingEvent be = (HttpSessionBindingEvent) e;
            assertTrue(be.getName() == key);
            assertTrue(be.getValue() == val);
        }
        {
            Pair pair = (Pair) events.get(1);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("attributeRemoved"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(session == e.getSession());
            HttpSessionBindingEvent be = (HttpSessionBindingEvent) e;
            assertTrue(be.getName() == key);
            assertTrue(be.getValue() == val);
        }
        events.clear();
        assertTrue(events.size() == 0);
        assertTrue(session.getAttribute(key) == null);
    }

    public void testReplaceAttribute() {
        testReplaceAttribute(_standardManager);
    }

    public void testReplaceAttribute(Manager manager) {
        HttpSession session = ((WADIHttpSession) manager.create(null)).getWrapper();
        assertTrue(events.size() == 1); // sessionCreated
        String key = "foo";
        Object oldVal = new Listener();
        Object newVal = new Listener();
        session.setAttribute(key, oldVal);
        assertTrue(events.size() == 3); // valueBound, attributeAdded
        events.clear();

        session.setAttribute(key, newVal);
        assertTrue(events.size() == 3); // valueUnbound, valueBound,
                                            // attributeReplaced
        {
            Pair pair = (Pair) events.get(0);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("valueUnbound"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(session == e.getSession());
            HttpSessionBindingEvent be = (HttpSessionBindingEvent) e;
            assertTrue(be.getName() == key);
            assertTrue(be.getValue() == oldVal);
        }
        {
            Pair pair = (Pair) events.get(1);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("valueBound"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(session == e.getSession());
            HttpSessionBindingEvent be = (HttpSessionBindingEvent) e;
            assertTrue(be.getName() == key);
            assertTrue(be.getValue() == newVal);
        }
        {
            Pair pair = (Pair) events.get(2);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("attributeReplaced"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(session == e.getSession());
            HttpSessionBindingEvent be = (HttpSessionBindingEvent) e;
            assertTrue(be.getName() == key);
            assertTrue(be.getValue() == oldVal);
        }
        events.clear();
        assertTrue(events.size() == 0);
        assertTrue(session.getValue(key) == newVal);
    }

    public void testReplaceValue() {
        testReplaceValue(_standardManager);
    }

    public void testReplaceValue(Manager manager) {
        HttpSession session = ((WADIHttpSession) manager.create(null)).getWrapper();
        String key = "foo";
        Object oldVal = new Listener();
        Object newVal = new Listener();
        session.setAttribute(key, oldVal);
        events.clear();

        session.putValue(key, newVal);
        {
            Pair pair = (Pair) events.remove(0);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("valueUnbound"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(session == e.getSession());
            HttpSessionBindingEvent be = (HttpSessionBindingEvent) e;
            assertTrue(be.getName() == key);
            assertTrue(be.getValue() == oldVal);
        }
        {
            Pair pair = (Pair) events.remove(0);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("valueBound"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(session == e.getSession());
            HttpSessionBindingEvent be = (HttpSessionBindingEvent) e;
            assertTrue(be.getName() == key);
            assertTrue(be.getValue() == newVal);
        }
        {
            Pair pair = (Pair) events.remove(0);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("attributeReplaced"));
            HttpSessionEvent e = pair.getEvent();
            assertTrue(session == e.getSession());
            HttpSessionBindingEvent be = (HttpSessionBindingEvent) e;
            assertTrue(be.getName() == key);
            assertTrue(be.getValue() == oldVal);
        }
        assertTrue(session.getValue(key) == newVal);
        assertTrue(events.size() == 0);
    }

    protected int enumerationLength(Enumeration e) {
        int i = 0;
        while (e.hasMoreElements()) {
            e.nextElement();
            i++;
        }
        return i;
    }

    public void testGetAttributeNames() {
        testGetAttributeNames(_standardManager);
    }

    public void testGetAttributeNames(Manager manager) {
        HttpSession session = ((WADIHttpSession) manager.create(null)).getWrapper();
        assertTrue(enumerationLength(session.getAttributeNames()) == 0);
        session.setAttribute("foo", "bar");
        assertTrue(enumerationLength(session.getAttributeNames()) == 1);
        session.setAttribute("bar", "baz");
        assertTrue(enumerationLength(session.getAttributeNames()) == 2);
        session.setAttribute("baz", "foo");
        assertTrue(enumerationLength(session.getAttributeNames()) == 3);
        session.setAttribute("baz", null);
        assertTrue(enumerationLength(session.getAttributeNames()) == 2);
        session.setAttribute("bar", null);
        assertTrue(enumerationLength(session.getAttributeNames()) == 1);
        session.setAttribute("foo", null);
        assertTrue(enumerationLength(session.getAttributeNames()) == 0);
    }

    public void testGetValueNames() {
        testGetValueNames(_standardManager);
    }

    public void testGetValueNames(Manager manager) {
        HttpSession session = ((WADIHttpSession) manager.create(null)).getWrapper();
        assertTrue(session.getValueNames().length == 0);
        session.setAttribute("foo", "bar");
        assertTrue(session.getValueNames().length == 1);
        session.setAttribute("bar", "baz");
        assertTrue(session.getValueNames().length == 2);
        session.setAttribute("baz", "foo");
        assertTrue(session.getValueNames().length == 3);
        session.setAttribute("baz", null);
        assertTrue(session.getValueNames().length == 2);
        session.setAttribute("bar", null);
        assertTrue(session.getValueNames().length == 1);
        session.setAttribute("foo", null);
        assertTrue(session.getValueNames().length == 0);
    }

    public void testMaxInactiveInterval() {
        testMaxInactiveInterval(_standardManager);
    }

    public void testMaxInactiveInterval(Manager manager) {
        HttpSession session = ((WADIHttpSession) manager.create(null)).getWrapper();
        {
            int interval = 60 * 60;
            session.setMaxInactiveInterval(interval);
            assertTrue(session.getMaxInactiveInterval() == interval);
        }
        {
            int interval = -1;
            session.setMaxInactiveInterval(interval);
            assertTrue(session.getMaxInactiveInterval() == interval);
        }
    }

    public void testIsNew() throws Exception {
        WADIHttpSession session = (WADIHttpSession) _standardManager.createWithName("name");
        HttpSession httpSession = session.getWrapper();
        assertTrue(httpSession.isNew());
        session.onEndProcessing();
        assertTrue(!httpSession.isNew());
    }

    public void testNullName() {
        testNullName(_standardManager);
    }

    public void testNullName(Manager manager) {
        HttpSession session = ((WADIHttpSession) manager.create(null)).getWrapper();
        try {
            session.setAttribute(null, "a");
            assertTrue(false);
        } catch (IllegalArgumentException e) {
        }
        try {
            session.getAttribute(null);
            assertTrue(false);
        } catch (IllegalArgumentException e) {
        }
        try {
            session.removeAttribute(null);
            assertTrue(false);
        } catch (IllegalArgumentException e) {
        }
        try {
            session.putValue(null, "a");
            assertTrue(false);
        } catch (IllegalArgumentException e) {
        }
        try {
            session.getValue(null);
            assertTrue(false);
        } catch (IllegalArgumentException e) {
        }
        try {
            session.removeValue(null);
            assertTrue(false);
        } catch (IllegalArgumentException e) {
        }
    }

    public void testStandard() throws Exception {
        testStandardValidation(_standardManager);
    }

    public void testDeserialisationOnReplacementWithListener(ClusteredManager manager) throws Exception {
        testDeserialisationOnReplacement(manager);
        // TODO - test context level events here...
    }

    public void testDeserialisationOnReplacementWithoutListener(ClusteredManager manager) throws Exception {
        testDeserialisationOnReplacement(manager);
        // TODO - test context level events here...
    }

    public void testDeserialisationOnReplacement(ClusteredManager manager) throws Exception {
        WebSession s0 = (WebSession) manager.create(null);
        WebSession s1 = (WebSession) manager.create(null);

        s0.setAttribute("dummy", "dummy");
        s0.setAttribute("binding-listener", new BindingListener());
        s0.setAttribute("activation-listener", new ActivationListener());
        events.clear();
        List activationEvents = ActivationListener._events;
        activationEvents.clear();
        List bindingEvents = BindingListener._events;
        bindingEvents.clear();

        s1.copy(s0);

        s1.setAttribute("activation-listener", new ActivationListener());

        assertTrue(activationEvents.size() == 2);
        {
            Pair pair = (Pair) activationEvents.get(0);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("sessionWillPassivate"));
            HttpSessionEvent e = pair.getEvent();
        }
        {
            Pair pair = (Pair) activationEvents.get(1);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("sessionDidActivate"));
            HttpSessionEvent e = pair.getEvent();
        }
        activationEvents.clear();

        s1.setAttribute("binding-listener", new BindingListener());

        assertTrue(bindingEvents.size() == 2);
        {
            Pair pair = (Pair) bindingEvents.get(0);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("valueUnbound"));
            HttpSessionEvent e = pair.getEvent();
        }
        {
            Pair pair = (Pair) bindingEvents.get(1);
            assertTrue(pair != null);
            assertTrue(pair.getType().equals("valueBound"));
            HttpSessionEvent e = pair.getEvent();
        }
        bindingEvents.clear();

    }

    public void testStandardValidation(Manager manager) // Distributable only
            throws Exception {
        WebSession session = ((WebSession) manager.create(null));
        // try some Serializables...
        session.setAttribute("0", "foo");
        session.setAttribute("1", new Integer(1));
        session.setAttribute("2", new Float(1.1));
        session.setAttribute("3", new Date());
        session.setAttribute("4", new byte[256]);
        // and some non-Serializables...
        session.setAttribute("5", new Object());
    }

    public void testDistributableValidation(Manager manager) // Distributable
                                                                // only
            throws Exception {
        HttpSession session = ((WADIHttpSession) manager.create(null)).getWrapper();
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

    public void testSeparateAttributes(Manager manager) throws Exception {
        WebSession sess0 = (WebSession) manager.create(null);
        Object val = new String("value");
        String key0 = "foo";
        String key1 = "bar";
        sess0.setAttribute(key0, val);
        sess0.setAttribute(key1, val);
        assertTrue(sess0.getAttribute(key0) == sess0.getAttribute(key1));
        byte[] bytes = sess0.getBodyAsByteArray();
        WebSession sess1 = (WebSession) manager.create(null);
        sess1.setBodyAsByteArray(bytes);
        assertTrue(sess1.getAttribute(key0) != sess1.getAttribute(key1));
        assertTrue(sess1.getAttribute(key0).equals(sess1.getAttribute(key1)));
    }

    public void testRest() {
        testRest(_standardManager);

    }

    public void testRest(Manager manager) {
        long start = System.currentTimeMillis();
        HttpSession session = ((WADIHttpSession) manager.create(null)).getWrapper();
        long end = System.currentTimeMillis();
        assertTrue(session.getSessionContext().getSession(null) == null);
        assertTrue(session.getSessionContext().getIds() != null);
        session.getServletContext(); // cannot really test unless inside a container... - TODO
        assertTrue(session.getCreationTime() >= start && session.getCreationTime() <= end);
        assertTrue(session.getCreationTime() == session.getLastAccessedTime());
    }
}