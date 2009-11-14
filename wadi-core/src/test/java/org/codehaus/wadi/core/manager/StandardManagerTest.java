/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codehaus.wadi.core.manager;

import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.core.session.SessionFactory;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class StandardManagerTest extends RMockTestCase {

    private SessionFactory sessionFactory;
    private SessionIdFactory sessionIdFactory;
    private Contextualiser contextualiser;
    private ConcurrentMotableMap motableMap;
    private Router router;
    private SessionMonitor sessionMonitor;
    private StandardManager manager;

    @Override
    protected void setUp() throws Exception {
        sessionFactory = (SessionFactory) mock(SessionFactory.class);
        sessionFactory.setManager(null);
        modify().args(is.NOT_NULL);
        
        sessionIdFactory = (SessionIdFactory) mock(SessionIdFactory.class);
        contextualiser = (Contextualiser) mock(Contextualiser.class);
        motableMap = (ConcurrentMotableMap) mock(ConcurrentMotableMap.class);
        router = (Router) mock(Router.class);
        sessionMonitor = (SessionMonitor) mock(SessionMonitor.class);
        
    }

    public void testCreateWithName() throws Exception {
        Object key = new Object();
        PreRegistrationCallback callback = (PreRegistrationCallback) mock(PreRegistrationCallback.class);

        int maxInactiveInterval = 10;
        Session session = sessionFactory.create();
        session.init(0, 0, maxInactiveInterval, key);
        modify().args(is.gt(0l), is.gt(0l), is.AS_RECORDED, is.AS_RECORDED);

        callback.callback(session);

        motableMap.put(key, session);
        sessionMonitor.notifySessionCreation(session);
        
        startVerification();

        manager = newManager();
        manager.setMaxInactiveInterval(maxInactiveInterval);
        manager.createWithName(key, callback);
    }
    
    private StandardManager newManager() {
        return new StandardManager(sessionFactory,
                sessionIdFactory,
                contextualiser,
                motableMap,
                router,
                sessionMonitor);
    }
    
}
