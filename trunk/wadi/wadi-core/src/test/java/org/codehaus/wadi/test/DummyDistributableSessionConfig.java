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


import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.ValueHelper;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.impl.SimpleStreamer;
import org.codehaus.wadi.impl.SimpleValuePool;
import org.codehaus.wadi.web.AttributesFactory;
import org.codehaus.wadi.web.DistributableSessionConfig;
import org.codehaus.wadi.web.Router;
import org.codehaus.wadi.web.WebSession;
import org.codehaus.wadi.web.WebSessionWrapperFactory;
import org.codehaus.wadi.web.impl.DistributableAttributesFactory;
import org.codehaus.wadi.web.impl.DistributableValueFactory;
import org.codehaus.wadi.web.impl.DummyRouter;
import org.codehaus.wadi.web.impl.StandardSessionWrapperFactory;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DummyDistributableSessionConfig implements DistributableSessionConfig {

    private final Streamer _streamer = new SimpleStreamer();
    private final ValuePool _valuePool = new SimpleValuePool(new DistributableValueFactory());
    private final AttributesFactory _attributesFactory = new DistributableAttributesFactory();
    private final WebSessionWrapperFactory _sessionWrapperFactory = new StandardSessionWrapperFactory();
    private final int _maxInactiveInterval = 30 * 60;
    private final Router _router = new DummyRouter();

    public DummyDistributableSessionConfig() {
    }

    public Streamer getStreamer() {
        return _streamer;
    }

    public ValueHelper findHelper(Class type) {
        throw new UnsupportedOperationException();
    }

    public boolean getHttpSessionAttributeListenersRegistered() {
        return false;
    }

    public ValuePool getValuePool() {
        return _valuePool;
    }

    public AttributesFactory getAttributesFactory() {
        return _attributesFactory;
    }

    public HttpSessionListener[] getSessionListeners() {
        return new HttpSessionListener[0];
    }

    public HttpSessionAttributeListener[] getAttributeListeners() {
        return new HttpSessionAttributeListener[0];
    }

    public ServletContext getServletContext() {
        throw new UnsupportedOperationException();
    }

    public void destroy(Invocation invocation, WebSession session) {
        throw new UnsupportedOperationException();
    }


    public WebSessionWrapperFactory getSessionWrapperFactory() {
        return _sessionWrapperFactory;
    }

    public int getMaxInactiveInterval() {
        return _maxInactiveInterval;
    }

    public Router getRouter() {
        return _router;
    }

}
