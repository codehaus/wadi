/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.relocation;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.PoolableInvocationWrapper;
import org.codehaus.wadi.Session;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.EndPoint;
import org.codehaus.wadi.impl.StandardManager;
import org.codehaus.wadi.test.MockInvocation;
import org.codehaus.wadi.test.MyHttpServletRequest;
import org.codehaus.wadi.test.MyHttpServletResponse;
import org.codehaus.wadi.test.MyStack;
import org.codehaus.wadi.test.TestUtil;
import org.codehaus.wadi.web.Router;
import org.codehaus.wadi.web.WebSession;

/**
 * 
 * @version $Revision: 1538 $
 */
public class AbstractTestRelocation extends TestCase {
    protected Log _log = LogFactory.getLog(getClass());

    static class MyInvocation implements Invocation {
        protected Log _log = LogFactory.getLog(getClass());
        protected StandardManager _manager;
        protected String _key;
        protected WebSession _session;

        public void init(StandardManager manager, String key) {
            _manager = manager;
            _key = key;
        }

        public String getSessionKey() {
            return _key;
        }

        public boolean isRelocatable() {
            return true;
        }

        public void setSession(Session session) {
            throw new UnsupportedOperationException("NYI");
        }

        public void invoke(PoolableInvocationWrapper wrapper) throws InvocationException {
            throw new UnsupportedOperationException("NYI");
        }

        public void invoke() throws InvocationException {
            assertTrue(_session != null);
        }

        public boolean isProxiedInvocation() {
            throw new UnsupportedOperationException("NYI");
        }

        public void relocate(EndPoint endPoint) {
            throw new UnsupportedOperationException("NYI - relocate to: " + endPoint);
        }

        public void sendError(int code, String message) throws InvocationException {
            throw new UnsupportedOperationException("NYI");
        }

        public void setRouter(Router router) {
            throw new UnsupportedOperationException("NYI");
        }

    }

    public void testSessionRelocation(Dispatcher redD, Dispatcher greenD) throws Exception {
        MyStack red = new MyStack(redD);
        red.start();
        redD = red.getServiceSpace().getDispatcher();

        MyStack green = new MyStack(greenD);
        green.start();
        greenD = green.getServiceSpace().getDispatcher();

        TestUtil.waitForDispatcherSeeOthers(new Dispatcher[] { redD, greenD }, 5000);

        WebSession session = red.getManager().create(null);
        session.onEndProcessing();
        String id = session.getId();

        assertTrue(id != null);

        FilterChain fc = new FilterChain() {
            public void doFilter(ServletRequest req, ServletResponse res) throws IOException, ServletException {
            }
        };

        Invocation invocation = new MockInvocation(new MyHttpServletRequest(id), new MyHttpServletResponse(), fc);
        boolean success = green.getManager().contextualise(invocation);
        assertTrue(success);

        success = red.getManager().contextualise(invocation);
        assertTrue(success);
    }
    
}
