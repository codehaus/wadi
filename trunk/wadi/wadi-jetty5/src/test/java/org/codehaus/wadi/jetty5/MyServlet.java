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
package org.codehaus.wadi.jetty5;

import java.net.URI;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.InvocationProxy;
import org.codehaus.wadi.Relocater;
import org.codehaus.wadi.SessionPool;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.impl.StackContext;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.web.Router;
import org.codehaus.wadi.web.impl.DummyRouter;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1846 $
 */
public class MyServlet implements Servlet {
	protected ServletConfig config;
    protected final Log log;
    protected Test test;
    private StackContext stackContext;

    public MyServlet(String nodeName,
            String clusterName,
            SessionPool contextPool,
            Relocater relocater,
            InvocationProxy httpProxy,
            Dispatcher dispatcher) throws Exception {
        log = LogFactory.getLog(getClass().getName() + "#" + nodeName);
        
        StackContext stackContext = new StackContext(new ServiceSpaceName(URI.create("name")), dispatcher) {
            protected Router newRouter() {
                return new DummyRouter();
            }
        };
        stackContext.build();
    }

    public void init(ServletConfig config) {
        this.config = config;
        log.info("Servlet.init()");
        try {
            stackContext.getManager().start();
        } catch (Exception e) {
            log.warn(e);
        }
    }

    public ServletConfig getServletConfig() {
        return config;
    }

    public void service(ServletRequest req, ServletResponse res) {
        String sessionId = ((HttpServletRequest) req).getRequestedSessionId();
        if (log.isInfoEnabled()) {
            log.info("Servlet.service(" + ((sessionId == null) ? "" : sessionId) + ")");
        }
        if (test != null) {
            test.test(req, res);
        }
    }

    public String getServletInfo() {
        return "Test Servlet";
    }

    public void destroy() {
        try {
            stackContext.getManager().stop();
        } catch (Exception e) {
            log.warn("unexpected problem", e);
        }
    }

    interface Test {
        void test(ServletRequest req, ServletResponse res);
    }

    public void setTest(Test test) {
        this.test = test;
    }

    public StackContext getStackContext() {
        return stackContext;
    }

}
